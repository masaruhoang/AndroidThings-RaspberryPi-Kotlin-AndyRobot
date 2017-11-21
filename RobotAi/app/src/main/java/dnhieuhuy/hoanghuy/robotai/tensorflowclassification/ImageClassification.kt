package dnhieuhuy.hoanghuy.robotai.tensorflowclassification

import android.content.Context
import android.graphics.Bitmap
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.google.android.things.contrib.driver.button.ButtonInputDriver
import com.google.firebase.database.FirebaseDatabase
import dnhieuhuy.hoanghuy.robotai.tensorflowclassification.classifier.TensorFlowImageClassifier

import java.util.*



/**
 * Created by Administrator on 07/10/2017.
 */
class ImageClassification: AutoCloseable
{


    private val TAG = ImageClassification::class.java.simpleName
    private var mImagePreprocessor: ImagePreprocessor? = null
    private var mTtsEngine: TextToSpeech? = null
    var mTtsSpeaker: TtsSpeaker? = null
    var mCameraHandler: CameraHandler? = null
    private var mTensorFlowClassifier: TensorFlowImageClassifier? = null

    private var mBackgroundTensorflowThread: HandlerThread? = null
    private var mBackgroundTensorflowHandler: Handler? = null

    private var mCloudHandler: Handler? = null
    private var mCloudThread: HandlerThread? = null


    private var mResultViews: Array<String>? = null

    private var mButtonDriver: ButtonInputDriver? = null
    private var context: Context?  = null
    private var mDatabase: FirebaseDatabase? = null
    private var isCameraFB: Int? = null

    fun initImageClassification(context: Context, mDatabase: FirebaseDatabase?)
    {
        this.context  = context
        this.mDatabase = mDatabase
        this.isCameraFB = isCameraFB

        //Tensorflow Image Classificatier
        mResultViews = arrayOf()
        mBackgroundTensorflowThread = HandlerThread("BackgroundThread")
        mBackgroundTensorflowThread!!.start()
        mBackgroundTensorflowHandler = Handler(mBackgroundTensorflowThread!!.getLooper())
        mBackgroundTensorflowHandler!!.post(mInitializeOnBackground)

        //Handler
        mCloudThread = HandlerThread("CloudThread")
        mCloudThread!!.start()
        mCloudHandler = Handler(mCloudThread!!.getLooper())


    }

    private val mInitializeOnBackground = Runnable {

            mImagePreprocessor = ImagePreprocessor()

            mTtsSpeaker = TtsSpeaker()
            mTtsSpeaker!!.setHasSenseOfHumor(true)
            mTtsEngine = TextToSpeech(context,
                    TextToSpeech.OnInitListener { status ->
                        if (status == TextToSpeech.SUCCESS) {
                            mTtsEngine!!.setLanguage(Locale.US)
                            mTtsEngine!!.setOnUtteranceProgressListener(utteranceListener)
                            mTtsSpeaker!!.speakReady(mTtsEngine)
                            mDatabase!!.getReference().child("RobotState").child("TTSpeech").setValue("OK")
                        } else {
                            mDatabase!!.getReference().child("RobotState").child("TTSpeech").setValue("ERROR")
                            Log.w(TAG, "Could not open TTS Engine (onInit status=" + status
                                    + "). Ignoring text to speech")
                            mTtsEngine = null
                        }
                    })

            mCameraHandler = CameraHandler.getInstance()
            mCameraHandler!!.initializeCamera(
                    context,
                    mDatabase,
                    mBackgroundTensorflowHandler,
                    mOnImageAvailableListener)

            mCameraHandler = CameraHandler.getInstance()
            mCameraHandler!!.initializeCameraTF()

            mTensorFlowClassifier = TensorFlowImageClassifier(context)



    }

    private val mBackgroundClickHandler = Runnable {
        if (mTtsEngine != null) {
            mTtsSpeaker?.speakShutterSound(mTtsEngine)
        }
        mCameraHandler?.takePicture()
    }

    /**
     * Take A photo
     * */
     fun takeAphotoFB()
    {
        mBackgroundTensorflowHandler?.post(mBackgroundClickHandler)
    }

    /**
     * Listener for new camera images.
     */
    private val mOnImageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        var bitmap: Bitmap? = null
        reader.acquireLatestImage().use {
            image -> bitmap = mImagePreprocessor!!.preprocessImage(image)
        }


        val results = mTensorFlowClassifier?.doRecognize(bitmap)

        Log.d(TAG, "Got the following results from Tensorflow: " + results)
        if (mTtsEngine != null) {
            // speak out loud the result of the image recognition
            mTtsSpeaker?.speakResults(mTtsEngine, results)
        } else {
            // if theres no TTS, we don't need to wait until the utterance is spoken, so we set
            // to ready right away.
        }


            for (i in mResultViews?.indices!!) {
                if (results?.size!! > i) {
                    val r = results[i]
                    mResultViews?.set(i, r.title + " : " + r.confidence.toString())

                } else {
                    mResultViews?.set(i, null.toString())
                }
            }


    }

    private val utteranceListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String) {

        }

        override fun onDone(utteranceId: String) {

        }

        override fun onError(utteranceId: String) {

        }
    }



    override fun close() {
        try {
            if (mBackgroundTensorflowThread != null) mBackgroundTensorflowThread?.quitSafely()
            // mScreen.close();
        } catch (t: Throwable) {
            // close quietly
        }

        mBackgroundTensorflowHandler = null
        mBackgroundTensorflowThread = null
        //mScreen = null;

        try {
            if (mCameraHandler != null) mCameraHandler!!.shutDown()
        } catch (t: Throwable) {
            // close quietly
        }


        try {
            if (mTensorFlowClassifier != null) mTensorFlowClassifier!!.destroyClassifier()
        } catch (t: Throwable) {
            // close quietly
        }

        try {
            if (mButtonDriver != null) mButtonDriver!!.close()
        } catch (t: Throwable) {
            // close quietly
        }


        if (mTtsEngine != null) {
            mTtsEngine!!.stop()
            mTtsEngine!!.shutdown()
        }
    }

}