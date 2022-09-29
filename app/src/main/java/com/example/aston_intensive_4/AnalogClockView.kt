package com.example.aston_intensive_4

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import java.time.LocalTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin


const val ARC_IS_INCREASING = "com.example.android.aston_intensive_4.ARC_IS_INCREASING"
const val SUPER_SAVE_STATE = "com.example.android.aston_intensive_4.SUPER_SAVE_STATE"

class AnalogClockView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Attrs
    private val a = context.theme.obtainStyledAttributes(attrs, R.styleable.AnalogClockView, 0, 0)

    private val padding = a.getDimension(R.styleable.AnalogClockView_innerPadding, 50f.toDp())
    private val handsLengthMultiplier = a.getFloat(R.styleable.AnalogClockView_handsLengthMultiplier, 1f)
    private val arcWidth = a.getDimension(R.styleable.AnalogClockView_arcWidth, 70f.toDp())
    private val accentColor = a.getColor(R.styleable.AnalogClockView_accentColor, ContextCompat.getColor(context, R.color.testarossa_red))

    private var centerX = 0f
    private var centerY = 0f
    private var radius = 0f

    private var hourHandSize = 0f
    private var minuteHandSize = 0f
    private var secondHandSize = 0f
    private var tailSize = 130f.toDp()

    private var oldMinutes = 0
    private var isIncreasing = true

    private var isInit = false

    private val paint by lazy {
        Paint().apply {
            isAntiAlias = true
        }
    }

    private val rect = Rect()
    private val numbers = arrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)

    private fun init() {

        centerX = width / 2f
        centerY = height / 2f

        val minimum = min(height.toFloat(), width.toFloat())
        radius = minimum / 2f - padding

        hourHandSize = radius - radius / 2
        minuteHandSize = radius - radius / 4 - (arcWidth / 2)
        secondHandSize = radius - radius / 6

        oldMinutes = LocalTime.now().minute

        isInit = true
    }

    override fun onSaveInstanceState(): Parcelable {
        val bundle = Bundle()
        bundle.putParcelable(SUPER_SAVE_STATE, super.onSaveInstanceState())
        bundle.putBoolean(ARC_IS_INCREASING, isIncreasing)
        return bundle
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        var viewState = state
        if (viewState is Bundle) {
            isIncreasing = viewState.getBoolean(ARC_IS_INCREASING)
            viewState = viewState.getParcelable(SUPER_SAVE_STATE) ?: state
        }
        super.onRestoreInstanceState(viewState)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (!isInit)
            init()

        drawWatchFace(canvas)
        postInvalidateDelayed(1000)
    }

    private fun drawWatchFace(canvas: Canvas) {
        val time = LocalTime.now()

        // Arc behaviour
        if (time.minute != oldMinutes) {
            oldMinutes = time.minute
            isIncreasing = !isIncreasing
        }

        // region Arc
        var rotation = time.second * 6f
        setPaintAttrs(accentColor, Paint.Style.STROKE, arcWidth)
        drawFaceArc(canvas, rotation, isIncreasing)
        // endregion

        // region Numerals and ticks
        setPaintAttrs(accentColor, Paint.Style.FILL, 15f.toDp())
        drawNumerals(canvas)
        setPaintAttrs(ContextCompat.getColor(context, R.color.grey), Paint.Style.FILL, 15f.toDp())
        drawTicks(canvas)
        // endregion

        // region Hands
        setPaintAttrs(Color.BLACK, Paint.Style.FILL, 18f.toDp())
        canvas.save()
        canvas.rotate(rotation, centerX, centerY)

        // secondHand
        drawHand(canvas, secondHandSize)

        canvas.restore()

        setPaintAttrs(Color.BLACK, Paint.Style.FILL, 30f.toDp())
        rotation = time.second * 0.1f + 6 * time.minute
        canvas.save()
        canvas.rotate(rotation, centerX, centerY)

        // minuteHand
        drawHand(canvas, minuteHandSize)

        canvas.restore()

        setPaintAttrs(accentColor, Paint.Style.FILL, 45f.toDp())
        rotation = time.second * 0.008f + 0.5f * time.minute + 30 * (time.hour % 12)
        canvas.save()
        canvas.rotate(rotation, centerX, centerY)

        // hourHand
        drawHand(canvas, hourHandSize)

        canvas.restore()
        // endregion

        // Center pin
        canvas.drawCircle(centerX, centerY, 50f.toDp(), paint)
    }

    private fun drawFaceArc(canvas: Canvas, rotation: Float, isIncreasing: Boolean) {

        val arcRadius = radius - radius / 4

        val oval = RectF(
            centerX - arcRadius,
            centerY - arcRadius,
            centerX + arcRadius,
            centerY + arcRadius
        )
        if (isIncreasing)
            canvas.drawArc(oval, 270f, rotation, false, paint)
        else
            canvas.drawArc(oval, 270f, -360f + rotation, false, paint)
    }

    private fun drawHand(canvas: Canvas, length: Float) {
        canvas.drawLine(centerX, centerY + tailSize, centerX, centerY - length * handsLengthMultiplier, paint)
    }

    private fun drawNumerals(canvas: Canvas) {
        paint.textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, 18f,
            resources.displayMetrics
        )
        val numRadius = radius - radius / 2.5
        for (number in numbers) {
            val num = number.toString()
            paint.getTextBounds(num, 0, num.length, rect)
            val angle = Math.PI / 6 * (number - 3)
            val x = (centerX + cos(angle) * numRadius - rect.width() / 2).toInt()
            val y = (centerY + sin(angle) * numRadius + rect.height() / 2).toInt()

            canvas.drawText(num, x.toFloat(), y.toFloat(), paint)
        }
    }

    private fun drawTicks(canvas: Canvas) {
        val length1 = radius - radius / 2 - 70f.toDp()
        val length2 = radius - radius / 2
        for (i in 0..59) {
            val angle = i * PI * 2 / 60
            val sinVal = sin(angle)
            val cosVal = cos(angle)
            val len: Float = if (i % 5 == 0) length1 else length1 + 40f.toDp()
            val x1 = (sinVal * len).toFloat()
            val y1 = (-cosVal * len).toFloat()
            val x2 = (sinVal * length2).toFloat()
            val y2 = (-cosVal * length2).toFloat()
            canvas.drawLine(
                centerX + x1, centerY + y1, centerX + x2,
                centerY + y2, paint
            )
        }
    }

    private fun setPaintAttrs(color: Int, stroke: Paint.Style, strokeWidth: Float) {
        paint.reset()
        paint.color = color
        paint.style = stroke
        paint.strokeWidth = strokeWidth
    }

    private fun Float.toDp() =
        this / this@AnalogClockView.context.resources.displayMetrics.density

}