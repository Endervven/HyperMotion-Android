package dev.endervven.hypermotion

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.MotionEvent
import android.view.Choreographer
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.graphics.RectF
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.Path
import android.content.Context
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(11, 16, 32)
        window.navigationBarColor = Color.rgb(11, 16, 32)
        window.decorView.systemUiVisibility = 0
        setContentView(MotionView(this))
    }
}

private class Spring(var value: Float = 0f) {
    var target = value
    private var velocity = 0f

    fun update(dt: Float) {
        val stiffness = 240f
        val damping = 25f
        val acceleration = (target - value) * stiffness - velocity * damping
        velocity += acceleration * dt
        value += velocity * dt
    }
}

private class MotionView(context: Context) : View(context), Choreographer.FrameCallback {
    private val bg = Color.rgb(11, 16, 32)
    private val white = Color.rgb(244, 246, 255)
    private val muted = Color.rgb(151, 161, 187)
    private val purple = Color.rgb(124, 108, 255)
    private val cyan = Color.rgb(76, 224, 255)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val card = RectF()
    private val scale = Spring(1f)
    private val orb = Spring(0f)
    private val wave = Spring(0f)

    private var lastFrame = 0L
    private var time = 0f
    private var taps = 0
    private var running = true
    private var downY = 0f

    private val density = resources.displayMetrics.density
    private fun dp(v: Float) = v * density

    init {
        isClickable = true
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        running = true
        Choreographer.getInstance().postFrameCallback(this)
    }

    override fun onDetachedFromWindow() {
        running = false
        Choreographer.getInstance().removeFrameCallback(this)
        super.onDetachedFromWindow()
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!running) return
        val dt = if (lastFrame == 0L) 0.016f
        else ((frameTimeNanos - lastFrame) / 1_000_000_000f).coerceIn(0f, 0.032f)
        lastFrame = frameTimeNanos
        time += dt
        scale.update(dt)
        orb.update(dt)
        wave.update(dt)
        invalidate()
        Choreographer.getInstance().postFrameCallback(this)
    }

    private fun text(c: Canvas, value: String, x: Float, y: Float,
                     size: Float, color: Int, bold: Boolean = false) {
        paint.shader = null
        paint.style = Paint.Style.FILL
        paint.color = color
        paint.textSize = dp(size)
        paint.typeface = if (bold) Typeface.create("sans-serif", Typeface.BOLD)
                         else Typeface.create("sans-serif", Typeface.NORMAL)
        c.drawText(value, x, y, paint)
    }

    private fun roundRect(c: Canvas, l: Float, t: Float, r: Float, b: Float,
                          radius: Float, color: Int) {
        paint.shader = null
        paint.color = color
        paint.style = Paint.Style.FILL
        c.drawRoundRect(dp(l), dp(t), dp(r), dp(b), dp(radius), dp(radius), paint)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val c = canvas
        val w = width / density
        val h = height / density
        c.drawColor(bg)

        // Soft animated ambient lights.
        paint.shader = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            intArrayOf(Color.rgb(29, 36, 76), bg, Color.rgb(21, 29, 57)),
            null, Shader.TileMode.CLAMP
        )
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null

        paint.color = Color.argb(22, 124, 108, 255)
        paint.style = Paint.Style.FILL
        val glowX = w * (0.72f + 0.06f * sin(time * 0.7f))
        c.drawCircle(dp(glowX), dp(155f + 12f * sin(time)), dp(105f), paint)

        text(c, "ENDERVVEN  /  LAB", 24f, 43f, 11f, cyan, true)
        text(c, "HyperMotion", 24f, 88f, 32f, white, true)
        text(c, "A new feeling of motion.", 24f, 116f, 14f, muted)

        // Animated hero orb.
        val cx = w / 2f
        val cy = min(h * 0.37f, 285f)
        val radius = dp(55f * scale.value)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = dp(1f)
        paint.color = Color.argb(65, 124, 108, 255)
        c.drawCircle(dp(cx), dp(cy), radius * 1.55f, paint)
        c.drawCircle(dp(cx), dp(cy), radius * 1.28f, paint)

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            dp(cx - 60), dp(cy - 60), dp(cx + 60), dp(cy + 60),
            intArrayOf(cyan, purple, Color.rgb(50, 62, 145)),
            null, Shader.TileMode.CLAMP
        )
        c.drawCircle(dp(cx), dp(cy), radius, paint)
        paint.shader = null

        paint.color = Color.argb(100, 255, 255, 255)
        c.drawCircle(dp(cx - 17f), dp(cy - 20f), dp(12f), paint)

        // Small orbiting particles.
        for (i in 0 until 8) {
            val a = time * 0.75f + i * (Math.PI.toFloat() / 4f)
            val rr = dp(76f + 4f * sin(time * 1.3f + i))
            paint.color = if (i % 2 == 0) cyan else purple
            c.drawCircle(
                dp(cx) + cos(a) * rr,
                dp(cy) + sin(a) * rr,
                dp(if (i % 2 == 0) 3f else 2f), paint
            )
        }

        text(c, "SPRING PHYSICS", 24f, cy + 100f, 11f, cyan, true)
        text(c, "Tap anywhere to animate", 24f, cy + 124f, 14f, white)

        // Demo controls.
        val top = min(cy + 158f, h - 195f)
        val bottom = h - 91f
        val gap = 10f
        val left = 20f
        val right = w - 20f
        val cw = (right - left - gap) / 2f

        roundRect(c, left, top, left + cw, top + 76f, 18f, Color.rgb(28, 35, 61))
        roundRect(c, left + cw + gap, top, right, top + 76f, 18f, Color.rgb(28, 35, 61))
        text(c, "01", left + 14f, top + 25f, 10f, cyan, true)
        text(c, "Elastic", left + 14f, top + 48f, 16f, white, true)
        text(c, "Spring movement", left + 14f, top + 65f, 10f, muted)
        text(c, "02", left + cw + gap + 14f, top + 25f, 10f, cyan, true)
        text(c, "Fluid", left + cw + gap + 14f, top + 48f, 16f, white, true)
        text(c, "Smooth response", left + cw + gap + 14f, top + 65f, 10f, muted)

        val btnY = max(top + 91f, bottom - 54f)
        roundRect(c, left, btnY, right, btnY + 50f, 16f, purple)
        text(c, "PLAY MOTION  ↗", left + 18f, btnY + 31f, 14f, white, true)

        text(c, "CUSTOM ENGINE  •  60 FPS TARGET", 24f, h - 28f, 10f, muted)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downY = event.y / density
                return true
            }
            MotionEvent.ACTION_UP -> {
                val w = width / density
                val h = height / density
                val y = event.y / density
                taps++
                scale.target = if (scale.target > 1f) 0.82f else 1.18f
                orb.target = if (orb.target > 0f) 0f else 1f
                wave.target = 1f - wave.target
                performClick()
                invalidate()
                return true
            }
            MotionEvent.ACTION_CANCEL -> return true
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
