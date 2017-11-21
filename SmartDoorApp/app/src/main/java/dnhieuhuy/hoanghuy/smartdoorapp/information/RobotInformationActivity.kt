package dnhieuhuy.hoanghuy.smartdoorapp.information

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dnhieuhuy.hoanghuy.smartdoorapp.R
import kotlinx.android.synthetic.main.activity_robot_information.*

class RobotInformationActivity : AppCompatActivity() {

    private lateinit var setColorForTV: SetColorForTextView

    //DataBASE of the Firebase
    private lateinit var mDatabase: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_robot_information)

        addControls()
        addEvents()
    }

    private fun addControls() {
        setColorForTV = SetColorForTextView(this)
        mDatabase = FirebaseDatabase.getInstance()
        mDatabase.getReference().child("RobotState").addValueEventListener(object: ValueEventListener{
            override fun onCancelled(p0: DatabaseError?) {

            }

            override fun onDataChange(dataSnapshot: DataSnapshot?) {
                val robotState: RobotState? = dataSnapshot?.getValue(RobotState.javaClass)

                txtLaserLight.text = robotState?.LaserLight
                setColorForTV.setColorTV(robotState?.LaserLight!!, txtLaserLight)

                txtFLGAS.text = robotState.FLGAS
                setColorForTV.setColorTV(robotState.FLGAS, txtFLGAS)

                txtUltrasonic.text = robotState?.UltrasonicSS
                setColorForTV.setColorTV(robotState.UltrasonicSS, txtUltrasonic)

                txtCamera.text = robotState?.Camera
                setColorForTV.setColorTV(robotState.Camera, txtCamera)

                txtOLED.text = robotState?.OLEDScreen
                setColorForTV.setColorTV(robotState.OLEDScreen, txtOLED)

                txtHumanSS.text = robotState?.InfradedHumon
                setColorForTV.setColorTV(robotState.InfradedHumon, txtHumanSS)

                txtArduConn.text = robotState?.ArduConn
                setColorForTV.setColorTV(robotState.ArduConn, txtArduConn)

                txtChassis.text = robotState?.Chassis
                setColorForTV.setColorTV(robotState.Chassis, txtChassis)

                txtLaserLight.text = robotState?.LaserLight
                setColorForTV.setColorTV(robotState.LaserLight, txtLaserLight)

                txtWifiIP.text = robotState?.WifiIP
                setColorForTV.setColorTV(robotState.WifiIP, txtWifiIP)

                txtTTSpeech.text = robotState?.TTSpeech
                setColorForTV.setColorTV(robotState.TTSpeech, txtTTSpeech)

                if(robotState!!.TempHum.length > 3 && robotState.TempHum.length <= 8 )
                {
                    txtTemp.text = robotState.TempHum.substring(0,robotState.TempHum.indexOf("|")) + "°C"
                    txtHum.text = robotState.TempHum.substring(robotState.TempHum.indexOf("\n")+1
                            , robotState.TempHum.length) + "%"
                }

            }

        })
    }

    private fun addEvents() {

    }
}
