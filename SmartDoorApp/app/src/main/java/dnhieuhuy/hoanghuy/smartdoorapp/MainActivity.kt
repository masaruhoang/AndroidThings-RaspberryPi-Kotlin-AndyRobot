package dnhieuhuy.hoanghuy.smartdoorapp

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.speech.RecognizerIntent
import android.util.Base64
import android.util.Log
import android.widget.Toast
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ValueEventListener
import dnhieuhuy.hoanghuy.smartdoorapp.information.RobotInformationActivity
import dnhieuhuy.hoanghuy.smartdoorapp.radar.RadarActivity


class MainActivity : AppCompatActivity() {

    //Parameter of the Database from Firebase
    private var mDatabase: DatabaseReference? = null
    private var mTurnOnOf: Boolean? = false
    private var mTurnOnOffAcc: Boolean? = false

    //SPEECH INPUT VAIRIABLE
    private var mSpeechInput: String? = null


    //Parameter of the Layout
    private val REQ_CODE_SPEECH_INPUT = 100

    //Accelerometer
    private var sensorManager: SensorManager? = null
    private var sensorList: List<Sensor>? = null

        //HandlerAccelerometer
        private var mAccHandler: Handler? = null
        private var mAccHandlerThread: HandlerThread? = null

        //Accelerometer's Parameters
        private var x: Int? = null
        private var y: Int? = null
        private var z: Int? = null


    /**
     * The Ultrasonic distance values won't be get anymore,
     * until RadarActivity is opened.
     * */

    override fun onResume() {
        super.onResume()
        mDatabase?.child("Ultrasonic")?.setValue("USTOP")
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        addControls()
        addEvents()
    }

    private fun addEvents() {
        /**
         * Rotot State
         * */
        btnRobotState.setOnClickListener {
            val intent = Intent(this, RobotInformationActivity::class.java)
            startActivity(intent)
        }

        /**
         * Radar Activity
         * */
        btnRadar.setOnClickListener {
            val intent = Intent(this, RadarActivity::class.java)
            startActivity(intent)
        }

        /**
         * Turn Off/On Accelerometer Mode
        */
        btnAcc.setOnClickListener {
            if(mTurnOnOffAcc == false)
            {
                mDatabase?.child("direction")?.setValue("ACC OFF")
                btnAcc.text = "Acc OFF"
                mTurnOnOffAcc = true
            }else
            {
                mDatabase?.child("direction")?.setValue("ACC ON")
                btnAcc.text = "Acc ON"
                mTurnOnOffAcc = false
            }
        }

        /**
         * By Button Turn On/Off Laser Light
         * */
        btnLaserLight.setOnClickListener {
            if(mTurnOnOf == false)
            {
                mDatabase?.child("TurnOnLaser")?.setValue(true)
                btnLaserLight.text = "Laser Off"
                mTurnOnOf = true
            }else
            {
                mDatabase?.child("TurnOnLaser")?.setValue(false)
                btnLaserLight.text = "Laser On"
                mTurnOnOf = false
            }
        }

        /**
         * By Button Take A Photo And Image Classification through
         * TensorFlow
         * */
        btnImgClassificatier.setOnClickListener {

            mImageClassification(true)

        }

        /**
         * By Recognize Speech
         * */
        btnSpeak.setOnClickListener {
            promptSpeechInput()
        }

        /**
         * Button Take A Photo And Get Image back mobile
         * */
        btnTakePhotoGetImg.setOnClickListener {
            getImageFromFireBase()
        }

        /**
         * Go Forward of the MOTOR
         * */
        btnGoFoward.setOnClickListener {
            mDatabase?.child("direction")?.setValue("GO FORWARD")
        }

        btnGoBack.setOnClickListener {
            mDatabase?.child("direction")?.setValue("GO BACK")
        }

        btnLeft.setOnClickListener{
            mDatabase?.child("direction")?.setValue("TURN LEFT")
        }

        btnRight.setOnClickListener {
            mDatabase?.child("direction")?.setValue("TURN RIGHT")
        }

        btnStop.setOnClickListener {
            mDatabase?.child("direction")?.setValue("STOP")
        }


    }



    private fun addControls() {
        mDatabase = FirebaseDatabase.getInstance().getReference()

        //Handler Thread Init
        mAccHandlerThread = HandlerThread("BackgroundAccelerometer")
        mAccHandlerThread?.start()
        mAccHandler = Handler(mAccHandlerThread?.looper)

        /* Get a SensorManager instance */
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager?
        sensorList =  sensorManager?.getSensorList(Sensor.TYPE_ACCELEROMETER)
        if(sensorList?.size!! > 0)
        {
            sensorManager?.registerListener(sel, sensorList!!.get(0), SensorManager.SENSOR_DELAY_NORMAL)
        }else
        {
            Toast.makeText(baseContext, "Error: No Accelerometer.", Toast.LENGTH_LONG).show()
        }
    }

    private fun mImageClassification(beAtHomeCheck: Boolean)
    {

        mDatabase?.child("ImgClassifier")?.setValue(beAtHomeCheck,
                DatabaseReference.CompletionListener { databaseError, databaseReference ->
                    if (databaseError == null)
                    {
                        Toast.makeText(this, "TensorFlow was opened.", Toast.LENGTH_SHORT).show()
                        val mp = MediaPlayer.create(applicationContext, R.raw.robot)
                        mp.start()
                    } else
                    {
                        Toast.makeText(this, "TensorFlow have some errors.", Toast.LENGTH_SHORT).show()
                    }
                })

       firebaseDataListener()
    }



    /**
     * Call Google Speech Input Intent where users can say something...
     * */
    private fun promptSpeechInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, R.string.speech_prompt)

        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT)
        }catch (ex: ActivityNotFoundException)
        {
            Toast.makeText(this, R.string.speech_not_supported, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Speech Results will be gotten here, and then storge onto Firebase
     * */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode)
        {
            REQ_CODE_SPEECH_INPUT->
            {
                if(resultCode == Activity.RESULT_OK && data != null)
                {
                    val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    txtSpeechInput!!.text = result[0]
                    mSpeechInput = result[0].toUpperCase()

                    //PUSH mSpeechInput into Firebase
                    when (mSpeechInput)
                    {
                        getString(R.string.TAKE_A_PHOTO_TF)-> {
                            setValueToFirebase(true, getString(R.string.TAKE_A_PHOTO_TF), "TakeAPhoto")
                        }
                        getString(R.string.GO_FORWARD)-> {
                            setValueToFirebase(true, getString(R.string.GO_FORWARD), "direction")
                        }
                        getString(R.string.GO_BACK)-> {
                            setValueToFirebase(true, getString(R.string.GO_BACK), "direction")
                        }
                        getString(R.string.TURN_LEFT)-> {
                            setValueToFirebase(true, getString(R.string.TURN_LEFT), "direction")
                        }
                        getString(R.string.TURN_RIGHT)-> {
                            setValueToFirebase(true, getString(R.string.TURN_RIGHT), "direction")
                        }
                        getString(R.string.ANDY)-> {
                            setValueToFirebase(true, getString(R.string.ANDY), "direction")
                        }
                        getString(R.string.I_AM_SAD)-> {
                            setValueToFirebase(true, getString(R.string.I_AM_SAD), "direction")
                        }
                        getString(R.string.STOP_MUSIC)-> {
                            setValueToFirebase(true, getString(R.string.STOP_MUSIC), "direction")
                        }
                        getString(R.string.TURN_LIGHT_ON)-> {
                            setValueToFirebase(true, getString(R.string.TURN_LIGHT_ON), "direction")
                        }
                        getString(R.string.TURN_LIGHT_OFF)-> {
                            setValueToFirebase(true, getString(R.string.TURN_LIGHT_OFF), "direction")
                        }
                        getString(R.string.temp_hum)-> {
                            setValueToFirebase(true, getString(R.string.temp_hum), "direction")
                        }

                    }
                }
            }
        }
    }



    /**
     * Set value to FIREBASE
     * */
    private fun setValueToFirebase(takeAPhoto: Boolean, speechTextFirebase: String, childFirebase: String)
    {
        if(childFirebase == "TakeAPhoto")
        {
            mDatabase?.child("TakeAPhoto")?.setValue(takeAPhoto,
                    DatabaseReference.CompletionListener { databaseError, databaseReference ->
                        if (databaseError == null)
                        {
                            Toast.makeText(this, "TensorFlow was opened.", Toast.LENGTH_SHORT).show()
                            val mp = MediaPlayer.create(applicationContext, R.raw.robot)
                            mp.start()
                        } else
                        {
                            Toast.makeText(this, "TensorFlow have some errors.", Toast.LENGTH_SHORT).show()
                        }
                    })
            firebaseDataListener()
        }else if(childFirebase == "direction")
        {
            mDatabase?.child(childFirebase)?.setValue(speechTextFirebase,
                    DatabaseReference.CompletionListener { databaseError, databaseReference ->
                        if (databaseError == null)
                        {
                            Toast.makeText(this, "TensorFlow was opened.", Toast.LENGTH_SHORT).show()
                            val mp = MediaPlayer.create(applicationContext, R.raw.robot)
                            mp.start()
                        } else
                        {
                            Toast.makeText(this, "TensorFlow have some errors.", Toast.LENGTH_SHORT).show()
                        }
                    })
        }


    }

    /**
     * Changing Data Listener in Firebase
     * */

    private fun firebaseDataListener() {
        mDatabase!!.child("ImageCameraPi3").child("image").addValueEventListener(object : ValueEventListener{
            override fun onDataChange(dataSnapShoot: DataSnapshot?) {

                val data: String? = dataSnapShoot?.getValue() as String

                // Decode image data encoded by the Cloud Vision library
                val imageBytes = Base64.decode(data, Base64.NO_WRAP or Base64.URL_SAFE)
                val bitmap: Bitmap? = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                imgCameraDoor.setImageBitmap(bitmap)
            }

            override fun onCancelled(de: DatabaseError?) {
                Log.w("TAG", "Failed to read value.", de?.toException())
            }
        })

    }

    private fun getImageFromFireBase() {
        mDatabase?.child("TakeAPhoto")?.setValue(true)
    }

    /**
     * Sensor Event Listener
     * */
    internal var sel = object : SensorEventListener
    {
        override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}
        override fun onSensorChanged(event: SensorEvent?) {
            val values = event?.values
            sendXYZToFirebase(values)
        }
    }

    //Handle x, y, z processing in Firebase and Cloud Vision
    private fun sendXYZToFirebase(values: FloatArray?)
    {
        if(values == null) return
        x = Math.round(values[0])
        y = Math.round(values[1])
        z = Math.round(values[2])
        txtAccelerometer.setText("x: ${values[0]}\n y: ${values[1]}\n z: ${values[2]}")
        mAccHandler?.postDelayed(mAccRunnable, 1000)

    }

    //Accelerometer Runnable
    private var mAccRunnable = object: Runnable
    {
        override fun run() {
            /*val rootFB = mDatabase?.child("Accelerometer")



            //Balance state
            if((x == 0 && y == 0 && z == 10)) {
                mAccFirebaseXYZData(rootFB!!)
            }
            //Downward
            else if((x == 0 && y == -3 && z == 9) )
            {
                mAccFirebaseXYZData(rootFB!!)
            }
            //Upward
            else if((x == 0 && y == 5 && z == 8))
            {
                mAccFirebaseXYZData(rootFB!!)
            }
            //Leaning Left
            else if((x == 4 && y == 4 && z == 7))
            {
                mAccFirebaseXYZData(rootFB!!)
            }
            //Leaning Right
            else if((x == -4 && y == 4 && z == 7))
            {
                mAccFirebaseXYZData(rootFB!!)
            }*/

        }
    }
    private fun mAccFirebaseXYZData(rootFB: DatabaseReference)
    {
        rootFB.child("x")?.setValue(x)
        rootFB.child("y")?.setValue(y)
        rootFB.child("z")?.setValue(z)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (sensorList?.size!! > 0) {
            sensorManager?.unregisterListener(sel)
        }
        mAccHandler?.removeCallbacks(mAccRunnable)
        mAccHandlerThread?.quitSafely()


    }

}
