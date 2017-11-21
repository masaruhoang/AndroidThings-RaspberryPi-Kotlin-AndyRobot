package dnhieuhuy.hoanghuy.robotai.NOTUSEultrasonicsensor

import com.google.android.things.pio.Gpio
import java.io.IOException



/**
 * Created by Administrator on 01/10/2017.
 */
class UltrasonicSensorManager
{
    private val SOUND_SPEED = 340.29f // speed of sound in m/s
    private val TRIG_DURATION_IN_MICROS = 10 // trigger duration
    private val TIMEOUT = 2100

    private var echoPin: Gpio? = null
    private var trigPin: Gpio? = null

    constructor(echoPin: Gpio, trigPin: Gpio)
    {
        this.echoPin = echoPin
        this.trigPin = trigPin
        echoPin.setDirection(Gpio.DIRECTION_IN)
        trigPin.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
    }

    /*
    * This method returns the distance measured by the sensor in cm
    *
    * @throws TimeoutException if a timeout occurs
    */
    @Throws(Exception::class)
    fun measureDistance(): Float {
        synchronized(this){
            triggerSensor()
            waitForSignal()
            val duration = measureSignal()
            return duration * SOUND_SPEED / 20000f
        }
    }

    /**
     * Put a high on the trig pin for TRIG_DURATION_IN_MICROS
     */
    private fun triggerSensor() {
        try {
            trigPin?.setValue(true)
            Thread.sleep(0, TRIG_DURATION_IN_MICROS * 1000)
            trigPin?.setValue(false)
        } catch (ex: InterruptedException) {

        } catch (ex: IOException) {
        }
    }

    /**
     * Wait for a high on the echo pin
     */
    @Throws(Exception::class)
    private fun waitForSignal() {
        var countdown = 10

        while (!echoPin?.getValue()!! && countdown > 0) {
            countdown--
        }

        if (countdown <= 0) {
            throw Exception("Timeout waiting for signal start")
        }
    }

    /**
     * @return the duration of the signal in micro seconds
     * @throws Exception
     */
    @Throws(Exception::class)
    private fun measureSignal(): Long {
        var countdown = TIMEOUT * 10
        val start = System.nanoTime()
        while (echoPin?.getValue()!! && countdown > 0) {
            countdown--
        }
        val end = System.nanoTime()

        if (countdown <= 0) {
            throw Exception("Timeout waiting for signal end")
        }

        return Math.ceil((end - start) / 1000.0).toLong() // Return micro seconds
    }

}