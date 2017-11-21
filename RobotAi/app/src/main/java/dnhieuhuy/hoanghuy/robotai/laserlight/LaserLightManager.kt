package dnhieuhuy.hoanghuy.robotai.laserlight

import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.google.android.things.pio.Gpio
import com.google.android.things.pio.PeripheralManagerService
import com.google.firebase.database.FirebaseDatabase
import dnhieuhuy.hoanghuy.robotai.boarddefaults.BoardDefaults

/**
 * Created by Administrator on 17/10/2017.
 */
class LaserLightManage: AutoCloseable
{
        //Database
        private lateinit var mDatabase: FirebaseDatabase

        private var TAG = LaserLightManage::class.java.simpleName
        private var gpioLaserLight: Gpio? = null

        //Handler Thread
        private var mLaserLightHandler: Handler? = null
        private var mLaserLightHandlerThread: HandlerThread? = null

        //Turn On/off State
        private var mOnOffState: Boolean? = null

         fun initLaserLight(service: PeripheralManagerService, mDatabase: FirebaseDatabase){
             //Database
             this.mDatabase = mDatabase

            //Init Handler Threads
            mLaserLightHandlerThread = HandlerThread("Background LaserLight")
            mLaserLightHandlerThread?.start()
            mLaserLightHandler = Handler(mLaserLightHandlerThread?.looper)

            try {
                gpioLaserLight = service.openGpio(BoardDefaults.getLaserPort())
                gpioLaserLight?.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)

                mDatabase.getReference().child("RobotState").child("LaserLight").setValue("OK")
            }catch (ex: Exception)
            {
                ex.printStackTrace()
                mDatabase.getReference().child("RobotState").child("LaserLight").setValue("ERROR")
            }

        }

        fun turnOnLaserLight(mOnOffState: Boolean)
        {
            this.mOnOffState = mOnOffState
            mLaserLightHandler?.post(mLaserLightRunnable)
        }

        private val mLaserLightRunnable = object : Runnable
        {
            override fun run() {
                if(gpioLaserLight == null) return
                when(mOnOffState)
                {
                    true-> gpioLaserLight?.value = true
                    false-> gpioLaserLight?.value = false
                }
            }

        }

    override fun close() {
        mLaserLightHandler?.removeCallbacks(mLaserLightRunnable)
        mLaserLightHandlerThread?.quitSafely()
        try {
            gpioLaserLight?.close()
        }catch (ex: Exception)
        {
            ex.printStackTrace()
        }finally {
            gpioLaserLight = null
        }
    }
}