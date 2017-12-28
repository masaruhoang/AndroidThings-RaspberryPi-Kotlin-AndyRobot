package dnhieuhuy.hoanghuy.robotai.NOTUSEultrasonicsensor

import android.os.Handler
import android.os.HandlerThread
import com.google.android.things.pio.Gpio
import java.io.IOException
import android.content.ContentValues.TAG
import android.util.Log
import dnhieuhuy.hoanghuy.robotai.chassis.ChassisDirectionManager
import java.util.concurrent.TimeUnit


/**
 * Created by Administrator on 02/10/2017.
 */
class ObstacleSensor(): AutoCloseable
{
    override fun close() {
        mHandlerThread?.quitSafely()
        mHandler?.removeCallbacks(pollDist)

        try {
            trig?.close()
            echo?.close()
        }catch (ex: Exception)
        {
            Log.e("Obstacle Sensor", "onDestroy: Error on PeripheralIO API ", ex)
        }finally {
            trig = null
            echo = null
        }


        Log.e("Obstacle Sensor", "onDestroy: Ultrasonic sensor is closed")
    }

    /**
     * Parameters of the HC_SR04 PORT
     * */
    private var trig: Gpio? = null
    private var echo: Gpio? = null
    private var hcsr04: UltrasonicSensorManager? = null

    private var mHandlerThread: HandlerThread? = null
    private var mHandler: Handler? = null
    private var pollDist: Runnable? = null

    /**
     * CONSTRUCTOR
     * */
    @Throws(IOException::class)
    constructor(trig: Gpio, echo: Gpio) : this() {
        this.trig = trig
        this.echo = echo

        hcsr04 = UltrasonicSensorManager(echo, trig)
        mHandlerThread = HandlerThread("Distance Sensor")
        mHandlerThread!!.start()
        mHandler = Handler(mHandlerThread!!.getLooper())


        //Runnable
        pollDist = object : Runnable
        {
            override fun run() {
                var count = 3
                var d : Float
                while (count > 0) {
                    try {
                        /**
                         * Stop robot if in front of ones has something
                         * obstacles.
                         * */
                        d = hcsr04!!.measureDistance()
                        //CLOSE
                        if(d <= 20)
                        {
                            ChassisDirectionManager.startMotor(true, true, true, true)
                        }else
                        {
                            ChassisDirectionManager.startMotor(
                                    ChassisDirectionManager.stateDirectionMotor?.get(0)!!,
                                    ChassisDirectionManager.stateDirectionMotor?.get(1)!!,
                                    ChassisDirectionManager.stateDirectionMotor?.get(2)!!,
                                    ChassisDirectionManager.stateDirectionMotor?.get(3)!!
                            )
                        }
                    Log.d(TAG, "Distance: " + d)
                        break
                    } catch (e: Exception) {
                        count--
                        Log.w(TAG, "Distance warning: " + e.message)
                    }
                    try {
                        TimeUnit.MILLISECONDS.sleep(100)
                    } catch ( e: InterruptedException) {
                    }
                }
                try {
                    TimeUnit.MILLISECONDS.sleep(500)
                } catch ( e: InterruptedException) {

                }
                mHandler!!.post(pollDist)
                }
            }
        mHandler!!.post(pollDist)
        }
}
