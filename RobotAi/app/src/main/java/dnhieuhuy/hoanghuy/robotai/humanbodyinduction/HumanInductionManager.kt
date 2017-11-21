package dnhieuhuy.hoanghuy.robotai.humanbodyinduction

import android.content.ContentValues
import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.speech.tts.TextToSpeech
import android.util.Log
import com.google.android.things.pio.Gpio
import com.google.android.things.pio.GpioCallback
import com.google.android.things.pio.PeripheralManagerService
import com.google.firebase.database.FirebaseDatabase
import dnhieuhuy.hoanghuy.robotai.boarddefaults.BoardDefaults
import dnhieuhuy.hoanghuy.robotai.tensorflowclassification.TtsSpeaker
import java.io.IOException
import java.util.*

/**
 * Created by Administrator on 15/10/2017.
 */
class HumanInductionManager: AutoCloseable
{
    private val TAG = HumanInductionManager::class.java.simpleName

    //Database
    private lateinit var mDatabase: FirebaseDatabase

    //Gpio
    private var mGpioHCSR501: Gpio? = null

    //Handler Thread
    private var mHumanInductionHandler: Handler? = null
    private var mHumanInductionHandlerThread: HandlerThread? = null

    //Text To Speech
    private var mTtsEngine: TextToSpeech? = null

    //Context
    private var context: Context? = null

    //Counter for spacing detection to say at TTSpeech
    private var detectedTimes: Int = 0

    fun initHumanInductionSensor(context: Context, service: PeripheralManagerService, mDatabase: FirebaseDatabase)
    {
        this.context = context

        //Database
        this.mDatabase = mDatabase

        mHumanInductionHandlerThread = HandlerThread("Background HCSR501")
        mHumanInductionHandlerThread?.start()
        mHumanInductionHandler = Handler(mHumanInductionHandlerThread?.looper)

        try {
            mGpioHCSR501 = service.openGpio(BoardDefaults.getHumanInductionPort())
            mGpioHCSR501?.setActiveType(Gpio.ACTIVE_HIGH)
            mGpioHCSR501?.setEdgeTriggerType(Gpio.EDGE_BOTH)
            mDatabase.getReference().child("RobotState").child("InfradedHumon").setValue("OK")

        }catch (e: Exception)
        {
            Log.e(TAG, "Error on PeripheralIO API", e)
            mDatabase.getReference().child("RobotState").child("InfradedHumon").setValue("ERROR")
        }

        //Text To Speech Init
        mTtsEngine = TextToSpeech(this.context,
                TextToSpeech.OnInitListener { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        mTtsEngine!!.setLanguage(Locale.US)
                    } else {
                        Log.w(TAG, "Could not open TTS Engine (onInit status=" + status
                                + "). Ignoring text to speech")
                        mTtsEngine = null
                    } })
    }

    /**
     * Runnable Will help us processing run multi-threading task in Background
     * to avoid blocks UI Thread
     * */
    private val mGpioHumanBodyInductionCallBack = object : GpioCallback()
    {
        override fun onGpioEdge(gpio: Gpio?): Boolean {
            try {
                if(gpio!!.value == true)
                {
                    //Detected human of the activities
                    if(detectedTimes == 0){
                        TtsSpeaker.speakWhoAreYou(context, mTtsEngine)
                        Log.i(ContentValues.TAG, "Anybody is in here!!!!!")
                    }
                    ++detectedTimes
                }else
                {
                    if(detectedTimes == 10) { detectedTimes = 0 }
                    //A certain time elapsed without human movement
                    Log.i(ContentValues.TAG, "Nobody is in here!!!!!")
                }

              //  TtsSpeaker.speakWhoAreYou(context, mTtsEngine)
            }catch (ex: IOException)
            {
                Log.e(ContentValues.TAG, "Maybe Sensor always without not running")
            }
            return true
        }

        override fun onGpioError(gpio: Gpio?, error: Int) {
            super.onGpioError(gpio, error)
        }
    }

    fun start()
    {
        if(mGpioHCSR501 != null)
        {
            try {
                //Register GPIO Callback
                mGpioHCSR501!!.registerGpioCallback(mGpioHumanBodyInductionCallBack, mHumanInductionHandler)
            }catch (ex: IOException)
            {
                ex.printStackTrace()
            }
        }
    }

    override fun close() {
        //Close CallBack
        mGpioHCSR501?.unregisterGpioCallback(mGpioHumanBodyInductionCallBack)

        // Remove pending Runnable from the handler.
        mHumanInductionHandlerThread?.quitSafely()
        //Close Port
        Log.i(TAG, "Closing port")
        try {
            mGpioHCSR501!!.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error on PeripheralIO API", e)
        } finally {
            mGpioHCSR501 = null
        }
    }
}