package dnhieuhuy.hoanghuy.robotai.firebase

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.util.Log
import com.google.firebase.database.DatabaseReference
import dnhieuhuy.hoanghuy.robotai.chassis.ChassisDirectionManager
import dnhieuhuy.hoanghuy.robotai.laserlight.LaserLightManage
import dnhieuhuy.hoanghuy.robotai.tensorflowclassification.TtsSpeaker


/**
 * All of the Speech To Text From Mobile
 */
class SpeechToTextFB
{
    private val DIR_GO_FORWARD = "GO FORWARD"
    private val DIR_GO_BACK = "GO BACK"
    private val DIR_TURN_LEFT = "TURN LEFT"
    private val DIR_TURN_RIGHT = "TURN RIGHT"
    private val DIR_STOP = "STOP"

    /**
     * Laser Light
     * */
    private var mLaserLightManage: LaserLightManage? = null

    /**
     * Media Player
     * */
    private var mp: MediaPlayer? = null

    constructor()
    {
        //Media Player
        mp = MediaPlayer()
        //Laser Light
        this.mLaserLightManage = mLaserLightManage
    }

    fun speechToTextFB(context: Context, directionData: String, rootDB: DatabaseReference, mTtsEngine: TextToSpeech?)
    {
        when (directionData) {
            DIR_GO_FORWARD -> {
                ChassisDirectionManager.startMotor(false, true, true, false, "hasOstacle")
            }
            DIR_GO_BACK -> {
                ChassisDirectionManager.startMotor(true, false, false, true,"hasOstacle")
            }
            DIR_TURN_LEFT -> {
                ChassisDirectionManager.startMotor(false, true, false, true, "hasOstacle")
                Thread.sleep(100)
                ChassisDirectionManager.startMotor(true, true, true, true, "hasOstacle")
                rootDB.child("direction").setValue("STOP")
            }
            DIR_TURN_RIGHT -> {
                ChassisDirectionManager.startMotor(true, false, true, false, "hasOstacle")
                Thread.sleep(100)
                ChassisDirectionManager.startMotor(true, true, true, true, "hasOstacle")
                rootDB.child("direction").setValue("STOP")
            }
            DIR_STOP -> {
                ChassisDirectionManager.startMotor(true, true, true, true, "hasOstacle")
            }

            "ANDY" ->{
                TtsSpeaker.speakYes(mTtsEngine)
                rootDB.child("direction").setValue("STOP")
            }
            "I AM SAD" ->{
                TtsSpeaker.speakTurnOnMusic(context, mTtsEngine)
                Thread.sleep(3800)
                val afd = context.assets.openFd("bientinh.mp3")
                mp?.setDataSource(afd.fileDescriptor,afd.startOffset,afd.length)
                mp?.prepare()
                mp?.start()
                rootDB.child("direction").setValue("STOP")
            }
            "STOP MUSIC"->{
                mp?.stop()
                TtsSpeaker.speakHowDoYouFelt(context, mTtsEngine)
                rootDB.child("direction").setValue("STOP")
            }
            "TURN LIGHT ON"->{
                mLaserLightManage?.turnOnLaserLight(true)
            }
            "TURN LIGHT OFF"->{
                mLaserLightManage?.turnOnLaserLight(false)
            }
            "TEMPERATURE IS IN HERE"->{
                TtsSpeaker.speakTemperature(context, mTtsEngine)
                rootDB.child("direction").setValue("STOP")
            }
            else -> {
                Log.e("SPEECHTOTEXTFB", "No Direction Is: " + directionData + "!!!")
            }
        }
    }
}