package dnhieuhuy.hoanghuy.robotai.chassis

import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.google.android.things.pio.Gpio
import com.google.android.things.pio.PeripheralManagerService
import com.google.firebase.database.FirebaseDatabase
import dnhieuhuy.hoanghuy.robotai.boarddefaults.BoardDefaults
import java.io.IOException

/**
 * Created by Administrator on 01/10/2017.
 */
class ChassisDirectionManager
{
    companion object {

        //Database
        private lateinit var mDatabase: FirebaseDatabase

        //Parameters of the MotorDC Direction
        private val TAG = ChassisDirectionManager::class.java.getSimpleName()
        private var lCw: Gpio? = null
        private var lCcw: Gpio? = null
        private var rCw: Gpio? = null
        private var rCcw: Gpio? = null

        //Parameters of the Motor Driver Module (L9110 Dual-Channel H Bridge)
        private var A_IA: Boolean = true
        private var A_IB: Boolean = true
        private var B_IA: Boolean = true
        private var B_IB: Boolean = true

        var stateDirectionMotor: List<Boolean>? = null
        private val INTERVAL_MS = 5000

        /**
         * Handler Thread what 'll help your project to do not block UI
         * by running Background.
         * */
        private var mHandlerMotor: Handler? = null
        private var mHandlerThreadMotor: HandlerThread? = null

        /**
         * Chassis Init
         * */
        fun initMotorChassis(service: PeripheralManagerService, mDatabase: FirebaseDatabase) {
            //Database
             this.mDatabase = mDatabase

            stateDirectionMotor = listOf(true, true, true, true)

            mHandlerThreadMotor = HandlerThread("BackgroundThreadMotor")
            mHandlerThreadMotor!!.start()
            mHandlerMotor = Handler(mHandlerThreadMotor!!.looper)

            //Setting init and state of the Port
            try {
                lCw = service.openGpio(BoardDefaults.getChassisControlPort("A_IA"))
                lCw?.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
                lCw?.setActiveType(Gpio.ACTIVE_HIGH)

                lCcw = service.openGpio(BoardDefaults.getChassisControlPort("A_IB"))
                lCcw?.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
                lCcw?.setActiveType(Gpio.ACTIVE_HIGH)

                rCw = service.openGpio(BoardDefaults.getChassisControlPort("B_IA"))
                rCw?.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
                rCw?.setActiveType(Gpio.ACTIVE_HIGH)

                rCcw = service.openGpio(BoardDefaults.getChassisControlPort("B_IB"))
                rCcw?.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
                rCcw?.setActiveType(Gpio.ACTIVE_HIGH)

                mHandlerMotor!!.post(mMotorDCRunnable)

                //Send current state of the Chassis module to Firebase
                mDatabase.getReference().child("RobotState").child("Chassis").setValue("OK")
            } catch (ex: Exception) {
                Log.w(TAG, "Could not open MOTOR's GPIO pins", ex)
                mDatabase.getReference().child("RobotState").child("Chassis").setValue("ERROR")
            }
        }

        /**
         * Robot 's direction will be set in here
         *
         * GO_FORWARD -> false, true, true, false
         * GO_BACK -> true, false, false, true
         * TURN_LEFT -> false, true, false, true
         * TURN_RIGHT -> true, false, true, false
         * STOP -> true, true, true, true
         *
         * */
        fun startMotor(a_ia: Boolean, a_ib: Boolean, b_ia: Boolean, b_ib: Boolean, state: String) {
            try {
                if(state == "hasOstacle"){
                    stateDirectionMotor = listOf(a_ia, a_ib, b_ia, b_ib)
                }

                A_IA = a_ia
                A_IB = a_ib
                B_IA = b_ia
                B_IB = b_ib
                Thread.sleep(100)
                mHandlerMotor!!.postDelayed(mMotorDCRunnable, INTERVAL_MS.toLong())
            } catch (ex: IOException) {
                Log.e(TAG, "Error on PeripheralIO API", ex)
            }
        }

        /**
         * Runnable Will help us processing run multi-threading task in Background
         * to avoid blocks UI Thread
         * */
        private val mMotorDCRunnable = object : Runnable {
            override fun run() {
                if (lCcw == null || lCw == null || rCcw == null || rCw == null) {
                    return
                }

                try {
                    lCw?.setValue(A_IA)
                    lCcw?.setValue(A_IB)
                    rCw?.setValue(B_IA)
                    rCcw?.setValue(B_IB)
                    mHandlerMotor?.postDelayed(this, INTERVAL_MS.toLong())
                } catch (ex: IOException) {
                    Log.e(TAG, "run: Error on PeripheralIO API", ex)
                }
            }
        }

        fun onCloseChassisPort() {
            mHandlerMotor?.removeCallbacks(mMotorDCRunnable)
            mHandlerThreadMotor?.quitSafely()
            try {
                lCw?.close()
                lCcw?.close()
                rCw?.close()
                rCcw?.close()
            } catch (e: IOException) {
                Log.e(TAG, "onDestroy: Error on PeripheralIO API ", e)
            } finally {
                lCcw = null
                lCw = null
                rCcw = null
                rCw = null
            }
            Log.e(TAG, "onDestroy: ChassisPort is closed")
        }
    }
}

