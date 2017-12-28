package dnhieuhuy.hoanghuy.robotai

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import com.google.android.things.pio.PeripheralManagerService
import com.google.firebase.database.FirebaseDatabase
import dnhieuhuy.hoanghuy.robotai.boarddefaults.BoardDefaults
import dnhieuhuy.hoanghuy.robotai.chassis.ChassisDirectionManager
import dnhieuhuy.hoanghuy.robotai.firebase.FirebaseDirectionData
import dnhieuhuy.hoanghuy.robotai.humanbodyinduction.HumanInductionManager
import dnhieuhuy.hoanghuy.robotai.laserlight.LaserLightManage
import dnhieuhuy.hoanghuy.robotai.lcd.RobotOLED
import dnhieuhuy.hoanghuy.robotai.tensorflowclassification.ImageClassification
import dnhieuhuy.hoanghuy.robotai.usbserialarduino.UsbSerialArduinoManager
import java.io.IOException


/**
 * Skeleton of an Android Things activity.
 *
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 *
 * <pre>{@code
 * val service = PeripheralManagerService()
 * val mLedGpio = service.openGpio("BCM6")
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
 * mLedGpio.value = true
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 *
 */
class MainActivity : Activity() {
    private val TAG = MainActivity::class.java.simpleName

    //Parameters of Firebase's realtime database
    private var mDatabase: FirebaseDatabase? = null

    //Parameter of the Peripheral Manager Service
    private var service: PeripheralManagerService? = null

    //CLASS
    private var mChassis: ChassisDirectionManager? = null
    private var mFirebaseDirection: FirebaseDirectionData? = null


    //SSd1306 OLED
    private var mRobotOLED: RobotOLED? = null


    //HCSR501 Human Body Induction
    private var mHumanInduction: HumanInductionManager? = null

    //Brain of Robot (Artificial Intelligence)
    private var imageClassification: ImageClassification? = null

    //Laser Light
    private var mLaserLightManage: LaserLightManage? = null

    //USB Serial
    private var mUsbSerialArduino: UsbSerialArduinoManager? = null
    private var mainActivity: MainActivity? = null


    override fun onDestroy() {
        super.onDestroy()
        ChassisDirectionManager.onCloseChassisPort()
        mRobotOLED?.close()
        imageClassification?.close()
        mHumanInduction?.close()
        mLaserLightManage?.close()
        mUsbSerialArduino?.close()

    }


    override fun onResume() {
        super.onResume()
        mUsbSerialArduino?.startUsbConnection()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        addControls()
    }

    private fun addControls() {
        service = PeripheralManagerService()
        mDatabase = FirebaseDatabase.getInstance()
        mChassis = ChassisDirectionManager()
        mFirebaseDirection = FirebaseDirectionData()
        imageClassification = ImageClassification()
        mHumanInduction = HumanInductionManager()
        mLaserLightManage = LaserLightManage()
        mainActivity = MainActivity()
        mRobotOLED = RobotOLED(this, BoardDefaults.getI2CPort(), mDatabase!!)
        mUsbSerialArduino = UsbSerialArduinoManager(this, mainActivity!!, mDatabase!!)


        //Laser Light
        mLaserLightManage?.initLaserLight(service!!, mDatabase!!)

        Log.d(TAG, "Laser Module is created")

        //TensorFlow Camera PI3 v1.3
        imageClassification?.initImageClassification(this, mDatabase)


        //Init Chassis
        try {
            ChassisDirectionManager.initMotorChassis(service = service!!, mDatabase = mDatabase!!)
            mFirebaseDirection!!.firebaseDataListener( this, mDatabase!!,
                                imageClassification!!, mLaserLightManage, mUsbSerialArduino!!)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        Log.d(TAG, "ChassisDirectionManager activity is created")
        Log.d(TAG, "Firebase activity is created")


        //Ssd1306 OLED Screen
        Log.d(TAG, "OLED screen activity is created")

        //HCSR501 Human Body Induction Module

        try {
            mHumanInduction?.initHumanInductionSensor(this, service!!, mDatabase!!)
            mHumanInduction?.start()
        } catch (e: IOException) {
            Log.e(TAG, "Error while opening screen", e)
            throw RuntimeException(e)
        }
        Log.d(TAG, "Human Body Induction Module activity is created")

    }

}
