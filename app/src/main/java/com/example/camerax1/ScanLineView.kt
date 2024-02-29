package com.example.camerax1

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat

class ScanLineView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val linePaint: Paint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.white) // Set color as needed
        strokeWidth = 5f // Set line thickness as needed
    }

    private lateinit var scanLineAnimator: ObjectAnimator
    private var lineY: Float = 0f

    init {
        setupAnimation()
    }

    private fun setupAnimation() {
        scanLineAnimator = ObjectAnimator.ofFloat(this, "lineY", 0f, 0f).apply {
            duration = 2000 // Adjust animation duration as needed
            repeatCount = ObjectAnimator.INFINITE
            interpolator = LinearInterpolator()
        }
    }

    fun setLineY(y: Float) {
        lineY = y
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawLine(0f, lineY, width.toFloat(), lineY, linePaint)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        scanLineAnimator.setFloatValues(0f, h.toFloat()) // Update animation range
    }

    fun attachToPreview(previewView: PreviewView) {
        previewView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            startAnimation()
        }
    }

    private fun startAnimation() {
        scanLineAnimator.start()
    }

    fun stopAnimation() {
        scanLineAnimator.cancel()
    }
}
