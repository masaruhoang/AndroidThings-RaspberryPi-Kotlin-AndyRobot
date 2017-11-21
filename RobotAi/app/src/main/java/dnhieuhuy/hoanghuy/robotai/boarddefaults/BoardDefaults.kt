package dnhieuhuy.hoanghuy.robotai.boarddefaults

import android.os.Build
import com.google.android.things.pio.PeripheralManagerService

/**
 * Created by Administrator on 01/10/2017.
 */
class BoardDefaults
{
    companion object {
        private val DEVICE_EDISON_ARDUINO = "edison_arduino"
        private val DEVICE_EDISON = "edison"
        private val DEVICE_JOULE = "joule"
        private val DEVICE_RPI3 = "rpi3"
        private val DEVICE_IMX6UL_PICO = "imx6ul_pico"
        private val DEVICE_IMX6UL_VVDN = "imx6ul_iopb"
        private val DEVICE_IMX7D_PICO = "imx7d_pico"
        private var sBoardVariant = ""


        /**
         * Return the HC-SR501 Infrared Human body GPIO pin that the  is connected on.
         */
        fun getHumanInductionPort(): String {
            when (getBoardVariant()) {
                DEVICE_EDISON_ARDUINO -> return "IO12"
                DEVICE_EDISON -> return "GP44"
                DEVICE_JOULE -> return "J7_71"
                DEVICE_RPI3 -> return "BCM26"
                DEVICE_IMX6UL_PICO -> return "GPIO4_IO20"
                DEVICE_IMX6UL_VVDN -> return "GPIO3_IO01"
                DEVICE_IMX7D_PICO -> return "GPIO_174"
                else -> throw IllegalStateException("Unknown Build.DEVICE " + Build.DEVICE)
            }
        }

        /**
         * Return the Laser Light GPIO pin that the  is connected on.
         */
        fun getLaserPort(): String {
            when (getBoardVariant()) {
                DEVICE_EDISON_ARDUINO -> return "IO12"
                DEVICE_EDISON -> return "GP44"
                DEVICE_JOULE -> return "J7_71"
                DEVICE_RPI3 -> return "BCM19"
                DEVICE_IMX6UL_PICO -> return "GPIO4_IO20"
                DEVICE_IMX6UL_VVDN -> return "GPIO3_IO01"
                DEVICE_IMX7D_PICO -> return "GPIO_174"
                else -> throw IllegalStateException("Unknown Build.DEVICE " + Build.DEVICE)
            }
        }



        /**
         * Return the LCD Display I2C pin that the  is connected on.
         */
        fun getI2CPort(): String {
            when (getBoardVariant()) {
                DEVICE_EDISON_ARDUINO -> return "I2C6"
                DEVICE_EDISON -> return "I2C1"
                DEVICE_JOULE -> return "I2C0"
                DEVICE_RPI3 -> return "I2C1"
                DEVICE_IMX6UL_PICO -> return "I2C2"
                DEVICE_IMX6UL_VVDN -> return "I2C4"
                DEVICE_IMX7D_PICO -> return "I2C1"
                else -> throw IllegalStateException("Unknown Build.DEVICE " + Build.DEVICE)
            }
        }

        /**
         * Return the LCD Display I2C pin that the  is connected on.
         */
        fun getChassisControlPort(port: String): String {
            when (port) {
                "A_IA" -> return "BCM4"
                "A_IB" -> return "BCM17"
                "B_IA" -> return "BCM27"
                "B_IB" -> return "BCM22"
                else -> throw IllegalStateException("Unknown Build.DEVICE " + Build.DEVICE)
            }
        }


        private fun getBoardVariant(): String {
            if (!sBoardVariant.isEmpty()) {
                return sBoardVariant
            }
            sBoardVariant = Build.DEVICE
            // For the edison check the pin prefix
            // to always return Edison Breakout pin name when applicable.
            if (sBoardVariant == DEVICE_EDISON) {
                val pioService = PeripheralManagerService()
                val gpioList = pioService.gpioList
                if (gpioList.size != 0) {
                    val pin = gpioList[0]
                    if (pin.startsWith("IO")) {
                        sBoardVariant = DEVICE_EDISON_ARDUINO
                    }
                }
            }
            return sBoardVariant
        }
    }
}