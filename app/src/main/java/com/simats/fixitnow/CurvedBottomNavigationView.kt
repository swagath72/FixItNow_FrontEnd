package com.simats.fixitnow

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import com.google.android.material.bottomnavigation.BottomNavigationView

class CurvedBottomNavigationView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : BottomNavigationView(context, attrs, defStyleAttr) {

    private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mPillPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var mNavigationBarWidth = 0
    private var mNavigationBarHeight = 0

    private var mSelectedPosX = -1f
    private var mPillWidth = 0f

    private var mSourcePosX = 0f
    private var mSourceWidth = 0f
    private var mAnimProgress = 1f
    private var mTargetIndex = -1

    private var mAnimator: ValueAnimator? = null

    init {
        mPaint.style = Paint.Style.FILL
        mPaint.color = Color.parseColor("#2D60FF")

        mPillPaint.style = Paint.Style.FILL
        mPillPaint.color = Color.WHITE

        setBackgroundColor(Color.TRANSPARENT)
        elevation = 0f

        // Add top padding to vertically center the menu items
        val topPadding = (12 * resources.displayMetrics.density).toInt()
        setPadding(0, topPadding, 0, 0)

        // Disable the Material 3 active indicator
        itemActiveIndicatorColor = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
        itemIconSize = (24 * resources.displayMetrics.density).toInt()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mNavigationBarWidth = w
        mNavigationBarHeight = h
        post(::updateSelectionImmediately)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        mNavigationBarWidth = width
        mNavigationBarHeight = height

        if (changed || mSelectedPosX == -1f) {
            post(::updateSelectionImmediately)
        }
    }

    private fun updateSelectionImmediately() {
        val menuView = getChildAt(0) as? ViewGroup ?: return
        val index = (0 until menu.size()).indexOfFirst { menu.getItem(it).itemId == selectedItemId }
        if (index != -1) {
            val selectedItemView = menuView.getChildAt(index) ?: return
            mSelectedPosX = selectedItemView.left + selectedItemView.width / 2f
            mPillWidth = selectedItemView.width * 0.95f
            mAnimProgress = 1f
            invalidate()
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        // Draw the blue capsule background
        val bgRect = RectF(0f, 0f, mNavigationBarWidth.toFloat(), mNavigationBarHeight.toFloat())
        val cornerRadius = mNavigationBarHeight / 2f
        canvas.drawRoundRect(bgRect, cornerRadius, cornerRadius, mPaint)

        // Draw the white selection pill
        if (mSelectedPosX != -1f) {
            val pillHeight = mNavigationBarHeight * 0.9f
            // Adjust pillTop to account for the padding
            val pillTop = (mNavigationBarHeight - getPaddingTop() - pillHeight) / 2f + getPaddingTop()

            val pillRect = RectF(
                mSelectedPosX - mPillWidth / 2f,
                pillTop,
                mSelectedPosX + mPillWidth / 2f,
                pillTop + pillHeight
            )
            val pillCornerRadius = pillHeight / 2f
            canvas.drawRoundRect(pillRect, pillCornerRadius, pillCornerRadius, mPillPaint)
        }

        // Let the super class draw icons and text
        super.dispatchDraw(canvas)
    }

    override fun onDraw(canvas: Canvas) {}

    fun setSelectedItem(index: Int) {
        val menuView = getChildAt(0) as? ViewGroup ?: return
        mTargetIndex = index

        if (mSelectedPosX == -1f) {
            updateSelectionImmediately()
        }

        mSourcePosX = mSelectedPosX
        mSourceWidth = mPillWidth

        mAnimator?.cancel()
        mAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 450
            interpolator = OvershootInterpolator(0.7f)
            addUpdateListener { anim ->
                val progress = anim.animatedValue as Float
                mAnimProgress = progress

                val targetView = menuView.getChildAt(mTargetIndex) ?: return@addUpdateListener
                val targetCenter = targetView.left + targetView.width / 2f
                val targetW = targetView.width * 0.95f

                mSelectedPosX = mSourcePosX + (targetCenter - mSourcePosX) * progress
                mPillWidth = mSourceWidth + (targetW - mSourceWidth) * progress

                invalidate()
            }
            start()
        }
    }
}
