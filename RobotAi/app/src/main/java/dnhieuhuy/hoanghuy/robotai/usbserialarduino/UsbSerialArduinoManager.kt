package dnhieuhuy.hoanghuy.robotai.usbserialarduino

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Handler
import android.os.HandlerThread
import android.speech.tts.TextToSpeech
import android.util.Log
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface
import com.google.firebase.database.FirebaseDatabase
import dnhieuhuy.hoanghuy.robotai.MainActivity
import dnhieuhuy.hoanghuy.robotai.chassis.ChassisDirectionManager
import dnhieuhuy.hoanghuy.robotai.lcd.RobotOLED
import dnhieuhuy.hoanghuy.robotai.sensorobjectmodel.Sensors
import dnhieuhuy.hoanghuy.robotai.tensorflowclassification.TtsSpeaker
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import java.util.*

/**
 * Created by Administrator on 20/10/2017.
 */
class UsbSerialArduinoManager// Detach events are sent as a system-wide broadcast
(context: Context, activity: MainActivity, mDatabase: FirebaseDatabase) : AutoCloseable
{
    private val TAG = UsbSerialArduinoManager::class.java.simpleName

    //Context
    private var context: Context? = context

    //Database
    private var mDatabase = mDatabase


    //USB Information
    /** There are some command using in Terminal, It will help you
     * find out USB_PRODUCT_ID from following command lines .
     *
     * adb shell dmesg ^
    New USB device found, idVendor=2341, idProduct=0001 ^
    New USB device strings: Mfr=1, Product=2, SerialNumber=220 ^
    Product: Arduino Uno ^
    Manufacturer: Arduino (www.arduino.cc)
     *
     *
     * */
    private val USB_VENDOR_ID = 0x2341  //---> 9025
    private val USB_PRODUCT_ID = 0x0043 //---> Depend on each the RPI

    //USB Parameters
    private var usbManager: UsbManager? = null
    private var usbDeviceConnection: UsbDeviceConnection? = null
    private var usbSerialDevice: UsbSerialDevice? = null

    //Text To Speech
    private var mTtsEngine: TextToSpeech? = null

    //Handler Thread
    private var mTTSpeechHandler: Handler? = null
    private var mTTSpeechHandlerThread: HandlerThread? = null

    //BroadcastReciever
    private val usbDetachedReceiver = object :BroadcastReceiver()
    {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action: String = intent!!.action

            if(UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action))
            {
                val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                if(device != null && device.vendorId == USB_VENDOR_ID
                        && device.getProductId() == USB_PRODUCT_ID)
                {
                    Log.i(TAG, "USB device detached")
                    stopUsbConnection()
                }
            }
        }
    }

    fun startUsbConnection()
    {

        val connectedDevices: Map<String, UsbDevice> = usbManager?.deviceList!!

        if(!connectedDevices.isEmpty())
        {
            for(device in connectedDevices.values)
            {
                if(device.vendorId == USB_VENDOR_ID && device.productId == USB_PRODUCT_ID)
                {
                    Log.i(TAG, "Device found: " + device.deviceName)
                    startSerialConnection(device)
                    return
                }
            }
        }
        Log.w(TAG, "Could not start USB connection - No devices found")
    }

    /**
     * Starting Connection between RPI and Arduino
     * */
    private fun startSerialConnection(device: UsbDevice)
    {

        Log.i(TAG, "Ready to open USB device connection")
        usbDeviceConnection = usbManager?.openDevice(device)
        usbSerialDevice = UsbSerialDevice.createUsbSerialDevice(device, usbDeviceConnection)
        if(usbSerialDevice != null)
        {
            if (usbSerialDevice!!.open())
            {
                usbSerialDevice?.setBaudRate(115200)
                usbSerialDevice?.setDataBits(UsbSerialInterface.DATA_BITS_8)
                usbSerialDevice?.setStopBits(UsbSerialInterface.STOP_BITS_1)
                usbSerialDevice?.setParity(UsbSerialInterface.PARITY_NONE)
                usbSerialDevice?.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF)
                usbSerialDevice?.read(callback)
                Log.i(TAG, "Serial connection opened")
                mDatabase.getReference().child("RobotState").child("ArduConn").setValue("IS CONNECTED")
                mDatabase.getReference().child("RobotState").child("UltrasonicSS").setValue("OK")
                mDatabase.getReference().child("RobotState").child("FLGAS").setValue("OK")
            }
        }
        else
        {
            Log.w(TAG, "Could not create Usb Serial Device")
            mDatabase.getReference().child("RobotState").child("ArduConn").setValue("CONNECTING ERROR")
            mDatabase.getReference().child("RobotState").child("UltrasonicSS").setValue("ERROR")
            mDatabase.getReference().child("RobotState").child("FLGAS").setValue("ERROR")
        }
    }

    private fun onSerialDataReceived(data: String) {
        if(data == "") return

        try {
            //FlameGas Values
            if(data == "FLAMEGAS"){
                Sensors.mFlameGas = data
                mTTSpeechHandler?.postDelayed(mTTSpeechRunnable,1000)

            }
            //Temperature And Humity
            else if (data.contains("|"))
            {
                Sensors.mTemHum = data
                //mDatabase.getReference().child("RobotState").child("TempHum").setValue(data)


            }
            //Ultrasonic values
            else if(data.contains("."))
            {
                if (data.indexOf(".") < 6) return
                if(Sensors.mUltrasonicDistance == "USTART")
                    mDatabase.getReference().child("Ultrasonic").setValue(data)

                Sensors.mUltraValue = data.substring(data.indexOf(",")+1,data.indexOf(".")).toInt()
                if(Sensors.mUltraValue < 10) //<--- If Has Any Obstacle in front of Robot, immediately stop (10 = 10cm)
                {
                    ChassisDirectionManager.startMotor(true,true,true,true)
                }
            }
            // Add whatever you want here
            Log.i(TAG, "Serial data received: " + data)
        }catch (ex: Exception)
        {
            ex.printStackTrace()
        }

    }

    /**
     * Send commands into Arduino
     * */
    fun writeToUsbSerial(x: Int,y: Int)
    {

        val valuesWH = "$x,$y\n"
        val bytes = valuesWH.toByteArray(Charset.forName("ASCII"))
        //write data to UART device
        usbSerialDevice?.write(bytes)
        Log.e(TAG, "DATA WAS SENDED: $valuesWH" )
    }

    /**
     * Read Data was send from Arduino
     * */
    private val mTTSpeechRunnable = object : Runnable
    {
        override fun run() {
            TtsSpeaker.speakWarningFlame(context, mTtsEngine)
        }
    }

    private val callback = UsbSerialInterface.UsbReadCallback { data ->
        try {
            val dataUtf8 = data.toString(Charset.forName("UTF-8"))
            val dataStr = dataUtf8.trim { it <= ' ' }
            activity.runOnUiThread  {
                onSerialDataReceived(dataStr)
            }
        } catch (e: UnsupportedEncodingException) {
            Log.e(TAG, "Error receiving USB data", e)
        }
    }


    /**
     * Stop connection of the USB When Arduino USB
     * was Plugged-out
     * */
    private fun stopUsbConnection() {
        try {
            if (usbSerialDevice != null) {
                usbSerialDevice?.close()
            }

            if (usbDeviceConnection != null) {
                usbDeviceConnection?.close()
            }
        } finally {
            usbSerialDevice = null
            usbDeviceConnection = null
        }
    }

    override fun close() {
        context?.unregisterReceiver(usbDetachedReceiver)
        stopUsbConnection()
    }

    /**
     * Init for USB
     * */
    init {
        usbManager = context.getSystemService<UsbManager>(UsbManager::class.java)
        val intentFilter = IntentFilter(UsbManager.ACTION_USB_ACCESSORY_DETACHED)
        context.registerReceiver(usbDetachedReceiver, intentFilter)

        //Text To Speech
        mTtsEngine = TextToSpeech(context,
                TextToSpeech.OnInitListener { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        mTtsEngine!!.setLanguage(Locale.US)
                    } else {
                        Log.w(TAG, "Could not open TTS Engine (onInit status=" + status
                                + "). Ignoring text to speech")
                        mTtsEngine = null
                    } })

        mTTSpeechHandlerThread = HandlerThread("BackgroundTTSpeech")
        mTTSpeechHandlerThread?.start()
        mTTSpeechHandler = Handler(mTTSpeechHandlerThread?.looper)
        mTTSpeechHandler?.post(mTTSpeechRunnable)
    }
}