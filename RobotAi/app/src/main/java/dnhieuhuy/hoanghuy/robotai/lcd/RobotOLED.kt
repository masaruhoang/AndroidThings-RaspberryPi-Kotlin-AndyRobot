package dnhieuhuy.hoanghuy.robotai.lcd

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.google.android.things.contrib.driver.ssd1306.Ssd1306
import android.content.Context.WIFI_SERVICE
import com.google.firebase.database.FirebaseDatabase
import dnhieuhuy.hoanghuy.robotai.sensorobjectmodel.Sensors
import java.io.IOException
import java.math.BigInteger
import java.net.InetAddress
import java.net.UnknownHostException
import java.nio.ByteOrder

/**
 * Created by Administrator on 03/10/2017.
 */
class RobotOLED: AutoCloseable
{
    private val TAG: String = RobotOLED::class.java.simpleName

    //Database
    private lateinit var mDatabase: FirebaseDatabase

    /**
     * LCD OLED Handler
     * */
    private var mHandlerOLED: Handler? = null
    private var mHandlerOLEDThread: HandlerThread? = null

    /**
     * Parameters of LCD OLED
     * */
    private var mScreen: Ssd1306? = null
    private var context: Context? = null
    private var cur_ip: String = " "


    @Throws(Exception::class)
    constructor(context: Context, i2cBusAddress: String, mDatabase: FirebaseDatabase)
    {
        this.context = context.applicationContext

        //Database
        this.mDatabase = mDatabase

        Log.d(TAG, "RobotLcd() called with: context, i2cBusAddress = [$i2cBusAddress]")
        mHandlerOLEDThread = HandlerThread("BackgroundLCDThread")
        mHandlerOLEDThread?.start()
        mHandlerOLED = Handler(mHandlerOLEDThread?.looper)

        try {
            mScreen = Ssd1306(i2cBusAddress)
            drawStringCentered("STARTED", Font.FONT_5X8, 10, true)
            mScreen!!.show()
            mDatabase.getReference().child("RobotState").child("OLEDScreen").setValue("OK")
        } catch (e: IOException) {
            mDatabase.getReference().child("RobotState").child("OLEDScreen").setValue("ERROR")
            e.printStackTrace()
        }

        showWifiInfo()
    }

    private fun drawStringCentered(textDisplay: String, font: Font, y: Int, on: Boolean) {
        try{
        val strSizeX: Int = textDisplay.length * font.outterWidth
        val x: Int = (mScreen?.lcdWidth!! - strSizeX)/ 2
        drawString(textDisplay, font, x, y, on)
        }catch(ex: Exception)
        {
            ex.printStackTrace()
        }
    }

    private fun drawString(textDisplay: String, font: Font, x: Int, y: Int, on: Boolean) {
        var posX: Int = x
        var posY: Int = y
        for (c in textDisplay.toCharArray()) {
            if (c == '\n') {
                posY += font.outterHeight
                posX = x
            } else {
                if (posX >= 0 && posX + font.width < mScreen!!.getLcdWidth()
                        && posY >= 0 && posY + font.height < mScreen!!.getLcdHeight()) {
                    font.drawChar(mScreen, c, posX, posY, on)
                }
                posX += font.outterWidth
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun wifiIpAddress(context: Context): String?
    {
        val wifiManager: WifiManager = context.getSystemService(WIFI_SERVICE) as WifiManager
        var ipAddress: Int = wifiManager.connectionInfo.ipAddress

        if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
            ipAddress = Integer.reverseBytes(ipAddress)
        }

        val ipByteArray = BigInteger.valueOf(ipAddress.toLong()).toByteArray()
        var ipAddressString: String?
        try {
            ipAddressString = InetAddress.getByAddress(ipByteArray).hostAddress
            mDatabase.getReference().child("RobotState").child("WifiIP").setValue(ipAddressString)
        } catch (ex: UnknownHostException) {
            Log.e(TAG, "Unable to get host address.")
            mDatabase.getReference().child("RobotState").child("WifiIP").setValue("NO IP")
            ipAddressString = null
        }


        return ipAddressString
    }

    fun showWifiInfo()
    {
        mHandlerOLED?.post(mOLEDRunnable)
    }

    private var mOLEDRunnable = object: Runnable
    {
        override fun run() {
            val ip = wifiIpAddress(context!!)
            if (ip != null && !cur_ip.equals(ip, ignoreCase = true))
            {
                cur_ip = ip
                Log.d(TAG, "run() called ip: " + ip)

                //Clear OLED Screen
                mScreen?.clearPixels()
                drawStringCentered("DWAYNE HOANG", Font.FONT_5X8, 0, true)
                drawStringCentered("ROBOT", Font.FONT_5X8, 10, true)
                Fonts.drawString(mScreen, 10, 30, "IP: " + ip, Fonts.Type.fontZxpix)
                Fonts.drawString(mScreen, 25, 50, "-=-=-=-=-", Fonts.Type.font5x5)
                try {
                    mScreen?.show()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            } else if (ip == null) {
                Log.d(TAG, "run() called ip is null")
                mScreen?.clearPixels()

                drawStringCentered("DWAYNE HOANG", Font.FONT_5X8, 0, true)
                drawStringCentered("ROBOT", Font.FONT_5X8, 10, true)
                drawStringCentered("no ip address", Font.FONT_5X8, 30, true)
                try {
                    mScreen?.show()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            if(Sensors.mTemHum != "" || Sensors.mFlameGas != "")
            {
                //Clear OLED Screen
                mScreen?.clearPixels()
                drawStringCentered("DWAYNE HOANG", Font.FONT_5X8, 0, true)
                drawStringCentered("ANDY ROBOT", Font.FONT_5X8, 10, true)
                Fonts.drawString(mScreen, 10, 30, "IP: " + ip, Fonts.Type.fontZxpix)
                Fonts.drawString(mScreen, 10, 40, "Temp: ${Sensors.mTemHum.substring(0,
                        Sensors.mTemHum.indexOf("|"))} *C"
                        , Fonts.Type.fontZxpix)
                Fonts.drawString(mScreen, 10, 50, "Hum: ${Sensors.mTemHum.substring(
                        Sensors.mTemHum.indexOf("|")+1)}%"
                        , Fonts.Type.fontZxpix)
                try {
                    mScreen?.show()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            mHandlerOLED?.postDelayed(this, 10000)
        }

    }


    override fun close() {
        mHandlerOLED?.removeCallbacks(mOLEDRunnable)
        mHandlerOLEDThread?.quitSafely()
        try {
            mScreen?.close()
        } catch (e: IOException) {
            Log.e(TAG, "onDestroy: Error on PeripheralIO API ", e)
        }finally {
            mScreen = null
        }
    }
}