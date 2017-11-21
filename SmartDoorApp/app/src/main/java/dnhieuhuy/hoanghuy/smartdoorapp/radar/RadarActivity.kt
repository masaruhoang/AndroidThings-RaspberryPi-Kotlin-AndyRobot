package dnhieuhuy.hoanghuy.smartdoorapp.radar

import android.util.Log
import com.google.firebase.database.*
import processing.core.*

class RadarActivity : PApplet() {

    internal var f1: PFont? = null
    internal var f2: PFont? = null

    internal var MAX_DISTANCE = 400f
    internal var angle = -80
    internal var distance: Int = 0

    internal var miRadar: Radar? = null

    //Parameter of the Database
    private var mDatabase: DatabaseReference? = FirebaseDatabase.getInstance().getReference()




    override fun onResume() {
        super.onResume()
        mDatabase?.child("Ultrasonic")?.setValue("USTART")

    }

    //-------------------------------------------------------------------
    // Standard Processing Procedures
    //-------------------------------------------------------------------
    override fun setup() {
        orientation(PConstants.LANDSCAPE)
        miRadar = Radar(displayHeight, displayWidth)
        f1 = loadFont("ArialMT-20.vlw")
        f2 = loadFont("ArialMT-15.vlw")
        stroke(255)

        //Draw Angle And Distance Text
        textSize(32f)
        fill(0f, 102f, 153f)
        text("Distance: " + distance , (height/2).toFloat(), (width/2).toFloat())

        /**
         * Changed data will be get here in the ValueEventListener
         * */
        val rootDB = mDatabase?.child("Ultrasonic")
        val dataListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot?) {
                val ultrasonicValues = dataSnapshot?.getValue().toString()
                Log.e("ULTRASONIC", "FIREBASE: " + ultrasonicValues)

                if (ultrasonicValues != null) {
                    val ultrasonicTrim= ultrasonicValues.trim() {it <= ' '}
                    val values: List<String> = ultrasonicTrim.split( ',')
                    try {
                        angle = PApplet.parseInt(map(values[0].toFloat(), 15f, 165f, -80f, 80f))
                        distance = PApplet.parseInt(map(values[1].substring(0,values[1].indexOf(".")).toFloat(),
                                    1f, MAX_DISTANCE,1f, miRadar?.radio!!.toFloat() ))
                        Log.e("ULTRASONIC", "VALUES: " + angle)
                        Log.e("ULTRASONIC", "VALUES: " + distance )
                    } catch (ex: Exception) {}
                }

            }

            override fun onCancelled(databaseError: DatabaseError?) {
            }
        }
        rootDB?.addValueEventListener(dataListener)


    }

    override fun draw() {

        drawShape()
    }


    //---------------------------------------------------------------------------------
    //  We draw and show on the screen of the mobile device the data obtained
    //----------------------------------------------------------------------------------
    fun drawShape() {
        background(0f, 0f, 0f)
        miRadar?.dibRadar()
        miRadar?.dibTracking(angle)
        miRadar?.dibFound(angle, distance)


    }


    //----------------------------------------------------------------------
    //              Class Radar
    //----------------------------------------------------------------------

    internal inner class Radar

    //*************************************************
    //Constructor
    //*************************************************
    (var centerY: Int, width: Int) {



        var SIDE_LENGTH: Int = 0
        var ANGLE_BOUNDS = 80
        var HISTORY_SIZE = 10
        var POINTS_HISTORY_SIZE = 100

        var radio: Int = 0
        var x: Float = 0.toFloat()
        var y: Float = 0.toFloat()
        var leftAngleRad: Float = 0.toFloat()
        var rightAngleRad: Float = 0.toFloat()
        var historyX: FloatArray
        var historyY: FloatArray
        var pointS: Array<Punto?>? = null
        var centerX: Int = 0


        init {
            SIDE_LENGTH = (centerY - 100) * 2
            radio = SIDE_LENGTH / 2
            centerX = width / 2
            leftAngleRad = PApplet.radians((-ANGLE_BOUNDS).toFloat()) - PConstants.HALF_PI
            rightAngleRad = PApplet.radians(ANGLE_BOUNDS.toFloat()) - PConstants.HALF_PI
            historyX = FloatArray(HISTORY_SIZE)
            historyY = FloatArray(HISTORY_SIZE)
            pointS = arrayOfNulls(POINTS_HISTORY_SIZE)
        }

        //*************************************************
        //More about drawing the radar background
        //*************************************************
        fun dibRadar() {

            stroke(100)  // color of the lines
            noFill()
            strokeWeight(2f) // width of the lines

            //we draw the semicircles as references of the distance
            for (i in 0..SIDE_LENGTH / 100) {
                arc(centerX.toFloat(), centerY.toFloat(), (100 * i).toFloat(), (100 * i).toFloat(), leftAngleRad, rightAngleRad)
            }

            //draw lines as angle reference
            for (i in 0..ANGLE_BOUNDS * 2 / 20) {
                val angle = (-ANGLE_BOUNDS + i * 20).toFloat()
                val radAngle = PApplet.radians(angle)
                line(centerX.toFloat(), centerY.toFloat(), centerX + radio * PApplet.sin(radAngle), centerY - radio * PApplet.cos(radAngle))
            }

        }



        //*******************************************************
        //More to draw the lines that simulate tracking
        //*******************************************************
        fun dibTracking(angle: Int) {
            val radian = PApplet.radians(angle.toFloat())
            x = radio * PApplet.sin(radian)
            y = radio * PApplet.cos(radian)
            val px = centerX + x
            val py = centerY - y
            historyX[0] = px
            historyY[0] = py
            strokeWeight(2f)
            for (i in 0..HISTORY_SIZE - 1) {
                stroke(50f, 190f, 50f, (255 - 25 * i).toFloat())
                line(centerX.toFloat(), centerY.toFloat(), historyX[i], historyY[i])
            }
            shiftHistoryArray()
        }

        //*******************************************************
        //More to draw objects that are detected
        //*******************************************************
        fun dibFound(angle: Int, distance: Int) {

            if (distance < 400) {
                val radian = PApplet.radians(angle.toFloat())
                x = distance * PApplet.sin(radian)
                y = distance * PApplet.cos(radian)
                val px = (centerX + x).toInt()
                val py = (centerY - y).toInt()
                pointS!![0] = Punto(px, py)
            } else {
                pointS!![0] = Punto(0, 0)
            }
            for (i in 0..POINTS_HISTORY_SIZE - 1) {
                val points = pointS!![i]
                if (points != null) {
                    val x = points.x
                    val y = points.y
                    if (x == 0 && y == 0) continue
                    val colorAlfa = PApplet.map(i.toFloat(), 0f, POINTS_HISTORY_SIZE.toFloat(), 50f, 0f).toInt()
                    val size = PApplet.map(i.toFloat(), 0f, POINTS_HISTORY_SIZE.toFloat(), 30f, 5f).toInt()
                    fill(190f, 40f, 40f, colorAlfa.toFloat())
                    noStroke()
                    ellipse(x.toFloat(), y.toFloat(), size.toFloat(), size.toFloat())
                }
            }
            shiftPointsArray()
        }

        fun shiftHistoryArray() {
            for (i in HISTORY_SIZE downTo 2) {
                historyX[i - 1] = historyX[i - 2]
                historyY[i - 1] = historyY[i - 2]
            }
        }

        fun shiftPointsArray() {
            for (i in POINTS_HISTORY_SIZE downTo 2) {
                val oldPoint = pointS!![i - 2]
                if (oldPoint != null) {
                    val punto = Punto(oldPoint.x, oldPoint.y)
                    pointS!![i - 1] = punto
                }
            }
        }

    }

    //----------------------------------------------------------------------
    //              Class Point
    //----------------------------------------------------------------------
    internal inner class Punto(var x: Int, var y: Int)

}
