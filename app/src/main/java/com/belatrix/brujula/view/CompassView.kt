package com.belatrix.brujula.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.AttributeSet
import android.view.View
import android.view.accessibility.AccessibilityEvent
import androidx.appcompat.app.AppCompatActivity
import com.belatrix.brujula.R

class CompassView : View, SensorEventListener {

    private var bearing: Float = 0.toFloat()
        set(_bearing) {
            field = _bearing
            sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED)
        }

    private var markerPaint: Paint? = null
    private var textPaint: Paint? = null
    private var circlePaint: Paint? = null
    private var northString: String? = null
    private var eastString: String? = null
    private var southString: String? = null
    private var westString: String? = null
    private var textHeight: Int = 0

    var sensorManager: SensorManager? = null
    var giroscopio: Sensor? = null
    var acelerometro: Sensor? = null
    var mGravity: FloatArray? = null
    var mGeomagnetic: FloatArray? = null

    constructor(context: Context) : super(context) {
        initCompassView()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initCompassView()
    }

    constructor(context: Context, ats: AttributeSet, defaultStyle: Int) :
            super(context, ats, defaultStyle) {
        initCompassView()
    }

    protected fun initCompassView() {
        sensorManager = context.getSystemService(AppCompatActivity.SENSOR_SERVICE) as SensorManager?
        acelerometro = sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        giroscopio = sensorManager!!.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        if (giroscopio != null) {
            sensorManager!!.registerListener(this, giroscopio, SensorManager.SENSOR_DELAY_NORMAL)
            sensorManager!!.registerListener(this, acelerometro, SensorManager.SENSOR_DELAY_NORMAL)
        }

        isFocusable = true
        val r = this.resources

        circlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        circlePaint!!.color = r.getColor(R.color.background_color)
        circlePaint!!.strokeWidth = 1f
        circlePaint!!.style = Paint.Style.FILL_AND_STROKE

        northString = r.getString(R.string.cardinal_north)
        eastString = r.getString(R.string.cardinal_east)
        southString = r.getString(R.string.cardinal_south)
        westString = r.getString(R.string.cardinal_west)

        textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        textPaint!!.color = r.getColor(R.color.text_color)
        textPaint!!.textSize = 24f

        textHeight = textPaint!!.measureText("yY").toInt()

        markerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        markerPaint!!.color = r.getColor(R.color.marker_color)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val measuredWidth = measure(widthMeasureSpec)
        val measuredHeight = measure(heightMeasureSpec)

        val d = Math.min(measuredWidth, measuredHeight)

        setMeasuredDimension(d, d)
    }

    private fun measure(measureSpec: Int): Int {
        var result = 0
        val specMode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)

        if (specMode == MeasureSpec.UNSPECIFIED) {
            result = 200
        } else {
            result = specSize
        }
        return result
    }

    override fun onDraw(canvas: Canvas) {
        val mMeasuredWidth = measuredWidth
        val mMeasuredHeight = measuredHeight

        val px = mMeasuredWidth / 2
        val py = mMeasuredHeight / 2

        val radius = Math.min(px, py)
        // Draw the background
        canvas.drawCircle(px.toFloat(), py.toFloat(), radius.toFloat(), circlePaint!!)

        // Rotate
        canvas.save()
        canvas.rotate(bearing, px.toFloat(), py.toFloat())

        val textWidth = textPaint!!.measureText("W").toInt()
        val cardinalX = px - textWidth / 2
        val cardinalY = py - radius + textHeight

        for (i in 0..23) {
            // Draw a marker.
            canvas.drawLine(
                px.toFloat(),
                (py - radius).toFloat(),
                px.toFloat(),
                (py - radius + 10).toFloat(),
                markerPaint!!
            )

            canvas.save()
            canvas.translate(0f, textHeight.toFloat())

            // Draw the cardinal points
            if (i % 6 == 0) {
                var dirString: String? = ""
                when (i) {
                    0 -> {
                        dirString = northString
                        val arrowY = 2 * textHeight
                        canvas.drawLine(
                            px.toFloat(),
                            arrowY.toFloat(),
                            px.toFloat() - 5,
                            3 * textHeight.toFloat(),
                            markerPaint
                        )
                        canvas.drawLine(
                            px.toFloat(),
                            arrowY.toFloat(),
                            px.toFloat() + 5,
                            3 * textHeight.toFloat(),
                            markerPaint
                        )
                    }
                    6 -> dirString = eastString
                    12 -> dirString = southString
                    18 -> dirString = westString
                }
                canvas.drawText(dirString!!, cardinalX.toFloat(), cardinalY.toFloat(), textPaint!!)
            }
            canvas.restore()
            canvas.rotate(15f, px.toFloat(), py.toFloat())
        }
        canvas.restore()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> mGravity = event.values
                Sensor.TYPE_MAGNETIC_FIELD -> mGeomagnetic = event.values
            }
        }
        if (mGravity != null && mGeomagnetic != null) {
            val RotationMatrix = FloatArray(16)
            val success =
                SensorManager.getRotationMatrix(RotationMatrix, null, mGravity, mGeomagnetic)
            if (success) {
                val orientation = FloatArray(3)
                SensorManager.getOrientation(RotationMatrix, orientation)
                bearing = orientation[0] * (180 / Math.PI.toFloat()) * -1
            }
        }
        this.invalidate()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

}