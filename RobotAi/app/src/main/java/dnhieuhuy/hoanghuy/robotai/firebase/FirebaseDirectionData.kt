package dnhieuhuy.hoanghuy.robotai.firebase

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import com.google.firebase.database.*
import dnhieuhuy.hoanghuy.robotai.tensorflowclassification.ImageClassification
import java.util.*
import dnhieuhuy.hoanghuy.robotai.laserlight.LaserLightManage
import dnhieuhuy.hoanghuy.robotai.sensorobjectmodel.Sensors
import dnhieuhuy.hoanghuy.robotai.sensorobjectmodel.pantilt.PointXY
import dnhieuhuy.hoanghuy.robotai.usbserialarduino.UsbSerialArduinoManager


/**
 * Created by Administrator on 01/10/2017.
 */

/**
 * Get direction data from realtime database to control
 * motor of the robot
 * */
class FirebaseDirectionData
{

        private val TAG = FirebaseDirectionData::class.java.simpleName

        private val DIR_GO_FORWARD = "GO FORWARD"
        private val DIR_GO_BACK = "GO BACK"
        private val DIR_TURN_LEFT = "TURN LEFT"
        private val DIR_TURN_RIGHT = "TURN RIGHT"
        private val DIR_STOP = "STOP"

        //Text To Speech
        private var mTtsEngine: TextToSpeech? = null
        private lateinit var speechToTextFB: SpeechToTextFB

        //Firebase
        private var rootDB: DatabaseReference? = null


        /**
         * This is changed data listener from Firebase
         * */
        fun firebaseDataListener(context: Context, mdataBaseFb: FirebaseDatabase,
                                 imageClassification: ImageClassification,
                                 mLaserLightManage: LaserLightManage?,
                                 mUsbSerialArduino: UsbSerialArduinoManager) {
            //Speech To Text Data get From Firebase
            speechToTextFB = SpeechToTextFB()

            //Text To Speech Init
            mTtsEngine = TextToSpeech(context,
                    TextToSpeech.OnInitListener { status ->
                        if (status == TextToSpeech.SUCCESS) {
                            mTtsEngine!!.setLanguage(Locale.US)
                        } else {
                            Log.w(TAG, "Could not open TTS Engine (onInit status=" + status
                                    + "). Ignoring text to speech")
                            mTtsEngine = null
                        } })

            //Data Event is changed on the Firebase
            rootDB = mdataBaseFb.getReference()
            val dataListener = object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot?) {
                    taskProcessingSnapShotData(context, dataSnapshot, imageClassification
                                              , mdataBaseFb , mLaserLightManage, mUsbSerialArduino)
                }

                override fun onCancelled(databaseError: DatabaseError?) {
                    Log.w(TAG, "onCancelled", databaseError?.toException())
                }
            }
            rootDB?.addValueEventListener(dataListener)
        }

    /**
     * Processing SnapshotData
     * */
    private fun taskProcessingSnapShotData(context: Context, dataSnapshot: DataSnapshot?,
                                           imageClassification: ImageClassification, mdataBaseFb: FirebaseDatabase,
                                           mLaserLightManage: LaserLightManage?, mUsbSerialArduino: UsbSerialArduinoManager)
    {
        /**
         * Here you can use Object Data to store JSON Data that's gotten from Firebase
         * */
        val directionData: String = dataSnapshot?.child("direction")?.getValue().toString()
        val directionTakeAPhoto: String = dataSnapshot?.child("TakeAPhoto")?.getValue().toString()
        val directionTakeAPhotoClassification: String = dataSnapshot?.child("ImgClassifier")?.getValue().toString()
        val directionTurnOnLaser: String = dataSnapshot?.child("TurnOnLaser")?.getValue().toString()
        val XYZ = dataSnapshot?.child("Accelerometer")?.getValue().toString()
        val pointXY = dataSnapshot?.child("PointXY")?.getValue(PointXY.javaClass)

        /**
         * POINTXY is coordinate of the Object, There is the position to control of the Pantilt positions.
         * */
        mUsbSerialArduino.writeToUsbSerial(pointXY!!.x, pointXY.y)
        controlChassisWithObjectPosition(context, pointXY, rootDB!!)

        /**
         * If USTART -> Send Data to Firebase
         * Else USTOP -> Not Send Anything relating to Distance.
         * */
        if(dataSnapshot.child("Ultrasonic")?.getValue().toString() == "USTART"
                || dataSnapshot.child("Ultrasonic")?.getValue().toString() == "USTOP")
            Sensors.mUltrasonicDistance =  if(dataSnapshot.child("Ultrasonic")?.getValue().toString() == "USTART") "USTART" else "USTOP"


        Log.e(TAG, "FIREBASE: " + directionData)
        Log.e(TAG, "FIREBASE: " + directionTakeAPhoto)
        Log.e(TAG, "FIREBASE: " + XYZ)


        /**
         * Chassis Direction: Go Forwar/Back , Turn Left/Right (Motor DC)
         * */
        directionOfMotor(context, directionData = directionData, rootDB = rootDB!!, xyz = XYZ)

        /**
         * Take A Photo, Send and store Image Byte Array to Firebase Real Time DB
         * */
        if(directionTakeAPhoto.toBoolean() == true )
        {
            //Camera PI3 v1.3 is being using
            imageClassification.mCameraHandler?.initializeCameraTF()
            imageClassification.takeAphotoFB()
            rootDB?.child("TakeAPhoto")?.setValue(false)
            Thread.sleep(2000)

        }
            
        /**
         * Tensorflow Image Classification
         * */
        else if (directionTakeAPhotoClassification.toBoolean() == true)
        {
            imageClassification.mCameraHandler?.initializeCameraFB(mdataBaseFb)
            imageClassification.takeAphotoFB()
            rootDB?.child("ImgClassifier")?.setValue(false)
            Thread.sleep(2000)
        }

        /**
         * Turn On/Off LaserLight
         * */
        mLaserLightManage?.turnOnLaserLight(directionTurnOnLaser.toBoolean())
    }

    /**
     * As Detected object 's coming out the camera screen
     * */
    private fun controlChassisWithObjectPosition(context: Context, pointXY: PointXY?, rootDB: DatabaseReference) {
        if(pointXY?.y!! > 420)
        {
            directionOfMotor(context, DIR_TURN_LEFT, rootDB = rootDB, xyz = "")
        }else if(pointXY.y  < 25 && pointXY.y != 0)
        {
            directionOfMotor(context, DIR_TURN_RIGHT, rootDB = rootDB, xyz = "")
        }

    }

    /**
     * Control Chassis Motor DC Direction
     * */
    fun directionOfMotor(context: Context, directionData: String, rootDB: DatabaseReference, xyz: String?) {
        speechToTextFB.speechToTextFB(context, directionData, rootDB, mTtsEngine)
    }
}