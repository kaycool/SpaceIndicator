package com.kai.wang.space.indicator.lib

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.support.v4.view.NestedScrollingChild
import android.support.v4.view.NestedScrollingChildHelper
import android.support.v4.view.NestedScrollingParentHelper
import android.support.v4.view.ViewPager
import android.support.v4.widget.ViewDragHelper.INVALID_POINTER
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.OverScroller

/**
 * @author kai.w
 * @des  $des
 */
class MultiFlowIndicator : ViewGroup, NestedScrollingChild {
    private lateinit var mViewPager: ViewPager
    private var mSpaceFlowAdapter: MultiFlowAdapter? = null
    private val mTitles by lazy { mutableListOf<String>() }

    private val mScreenWidth: Int
        get() {
            val displayMetrics = resources.displayMetrics
            val cf = resources.configuration
            val ori = cf.orientation
            return when (ori) {
                Configuration.ORIENTATION_LANDSCAPE -> displayMetrics.heightPixels
                Configuration.ORIENTATION_PORTRAIT -> displayMetrics.widthPixels
                else -> 0
            }
        }
    private val mScreenHeight: Int
        get() {
            val displayMetrics = resources.displayMetrics
            val cf = resources.configuration
            val ori = cf.orientation
            return when (ori) {
                Configuration.ORIENTATION_LANDSCAPE -> displayMetrics.widthPixels
                Configuration.ORIENTATION_PORTRAIT -> displayMetrics.heightPixels
                else -> 0
            }
        }
    private var mTextSelectedColor = Color.RED
    private var mTextUnSelectedColor = Color.BLACK
    private var mTextSelectedSize = resources.getDimension(R.dimen.sp_10)
    private var mTextUnSelectedSize = resources.getDimension(R.dimen.sp_10)
    private var mIndicatorHeight = resources.getDimension(R.dimen.dimen_3)
    private var mIndicatorWidth = resources.getDimension(R.dimen.dimen_8)
    private var mMaxHeight = mScreenHeight.toFloat()
    private var mIndicatorColor = Color.RED
    private val mPaint by lazy {
        Paint().apply {
            this.color = mIndicatorColor
            this.isAntiAlias = true
            this.flags = Paint.ANTI_ALIAS_FLAG
            this.style = Paint.Style.FILL
        }
    }
    private val mNestedScrollingChildHelper by lazy { NestedScrollingChildHelper(this) }
    private val mNestedScrollingParentHelper by lazy { NestedScrollingParentHelper(this) }
    private var mTouchSlop: Int = 0
    private var mMinimumVelocity: Int = 0
    private var mMaximumVelocity: Int = 0
    private var mOverscrollDistance: Int = 0
    private var mOverflingDistance: Int = 0
    private var mLastX = 0f
    private var mLastY = 0f

    //    private var mVerticalScrollFactor: Float = 0.toFloat()
    private var mActivePointerId = INVALID_POINTER
    private val mOverScroller by lazy { OverScroller(context) }
    private lateinit var mVelocityTracker: VelocityTracker
    private var mMode = MODE.HORIZONL
    private var mCurrentTab = 0
    private var mCurrentTabOffsetPixel = 0
    private var mCurrentTabOffset = 0f

    constructor(context: Context?) : this(context, null)
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        setWillNotDraw(false)
        val configuration = ViewConfiguration.get(context)
        mTouchSlop = configuration.scaledTouchSlop
        mMinimumVelocity = configuration.scaledMinimumFlingVelocity
        mMaximumVelocity = configuration.scaledMaximumFlingVelocity
        mOverscrollDistance = configuration.scaledOverscrollDistance
        mOverflingDistance = configuration.scaledOverflingDistance
//        mVerticalScrollFactor = configuration.scaledVerticalScrollFactor

        obtainAttributes(attrs)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val parentWidth = MeasureSpec.getSize(widthMeasureSpec)

        var measureWidth = 0
        var measureHeight = 0
        when (mMode) {
            MODE.HORIZONL -> {
                for (i in 0 until childCount) {
                    val childView = getChildAt(i)
                    measureChild(childView, widthMeasureSpec, heightMeasureSpec)
                    val layoutParams = childView.layoutParams as MarginLayoutParams
                    measureWidth += childView.measuredWidth + layoutParams.leftMargin + layoutParams.rightMargin
                    if (measureHeight < childView.measuredHeight + layoutParams.topMargin + layoutParams.bottomMargin) {
                        measureHeight = childView.measuredHeight + layoutParams.topMargin + layoutParams.bottomMargin
                    }
                }
            }
            MODE.VERTICAL -> {
                for (i in 0 until childCount) {
                    val childView = getChildAt(i)
                    measureChild(childView, widthMeasureSpec, heightMeasureSpec)
                    val layoutParams = childView.layoutParams as MarginLayoutParams
                    measureWidth += childView.measuredWidth + layoutParams.leftMargin + layoutParams.rightMargin
                    if (measureHeight < childView.measuredHeight + layoutParams.topMargin + layoutParams.bottomMargin) {
                        measureHeight = childView.measuredHeight + layoutParams.topMargin + layoutParams.bottomMargin
                    }
                    if (measureWidth > parentWidth) {
                        measureWidth = childView.measuredWidth + layoutParams.leftMargin + layoutParams.rightMargin
                        measureHeight += childView.measuredHeight + layoutParams.topMargin + layoutParams.bottomMargin
                    }

                }
            }
        }
        setMeasuredDimension(
            Math.max(measureWidth, parentWidth)
            , Math.min(measureHeight, mMaxHeight.toInt())
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        var left = l
        var top = t
        var right = 0
        var bottom = 0
        when (mMode) {
            MODE.HORIZONL -> {
                for (i in 0 until childCount) {
                    val childView = getChildAt(i)
                    val layoutParams = childView.layoutParams as MarginLayoutParams
                    top = layoutParams.topMargin
                    right = left + childView.measuredWidth
                    bottom = top + childView.measuredHeight
                    childView.layout(left, top, right, bottom)
                    left = right + layoutParams.rightMargin
                }
            }
            MODE.VERTICAL -> {
                for (i in 0 until childCount) {
                    val childView = getChildAt(i)
                    val layoutParams = childView.layoutParams as MarginLayoutParams
                    right = left + childView.measuredWidth
                    if (right > measuredWidth) {
                        left = l
                        right = left + childView.measuredWidth
                        top += childView.measuredHeight + layoutParams.bottomMargin
                    }
                    bottom = top + childView.measuredHeight
                    childView.layout(left, top + layoutParams.topMargin, right, bottom)
                    left = right + layoutParams.rightMargin
                }
            }
            else -> {

            }
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        if (childCount > mCurrentTab + 1) {
            val drawChildView = getChildAt(mCurrentTab)

            canvas?.drawRect(
                drawChildView.left.toFloat(), drawChildView.bottom.toFloat() - mIndicatorHeight
                , drawChildView.right.toFloat(), drawChildView.bottom.toFloat(), mPaint
            )
        }

    }


    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        return super.dispatchTouchEvent(ev)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return super.onInterceptTouchEvent(ev)
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        initVelocityTrackerIfNotExists()
        when (event.action) {
            MotionEvent.ACTION_POINTER_DOWN -> {
            }
            MotionEvent.ACTION_DOWN -> {
                mLastX = event.x
                mLastY = event.y

                mActivePointerId = event.getPointerId(0)
            }
            MotionEvent.ACTION_MOVE -> {
                val activePointerId = mActivePointerId
                if (activePointerId == INVALID_POINTER) {
                    // If we don't have a valid id, the touch down wasn't on content.
                    return false
                }

                val pointerIndex = event.findPointerIndex(activePointerId)
                if (pointerIndex == -1) {
                    return false
                }

                val moveX = event.getX(pointerIndex)
                val moveY = event.getY(pointerIndex)

                val delX = (mLastX - moveX).toInt()
                val delY = (mLastY - moveY).toInt()

                when {
                    Math.abs(delX) > Math.abs(delY)
                            && (canScrollHorizontally(-1)
                            || canScrollHorizontally(1)) -> {
                        val dx = when {
                            scrollX + delX < 0 -> -scrollX
                            scrollX + delX > getScrollRangeX() -> getScrollRangeX() - scrollX
                            else -> delX
                        }
                        mOverScroller.startScroll(
                            scrollX,
                            0,
                            dx,
                            0
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            postInvalidateOnAnimation()
                        } else {
                            postInvalidate()
                        }
                    }

                    Math.abs(delY) > Math.abs(delX)
                            && (canScrollVertically(-1)
                            || canScrollVertically(1)) -> {

                        val dy = when {
                            scrollY + delY < 0 -> -scrollY
                            scrollY + delY > getScrollRangeY() -> getScrollRangeY() - scrollY
                            else -> delY
                        }

                        mOverScroller.startScroll(
                            0,
                            scrollY,
                            0,
                            dy
                        )

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            postInvalidateOnAnimation()
                        } else {
                            postInvalidate()
                        }
                    }
                    else -> {
                    }
                }

                mLastX = moveX
                mLastY = moveY
            }
            MotionEvent.ACTION_POINTER_UP -> {
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                Log.d("MultiFlowIndicator", "MotionEvent.ACTION_UP or MotionEvent.ACTION_CANCEL")
                mLastX = 0f
                mLastY = 0f

                val velocityTracker = mVelocityTracker
                velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity.toFloat())
                val velocityX = -velocityTracker.getXVelocity(mActivePointerId).toInt()
                val velocityY = -velocityTracker.getYVelocity(mActivePointerId).toInt()

                when {
                    Math.abs(velocityX) > Math.abs(velocityY) && Math.abs(velocityX) > mMinimumVelocity -> {
                        val canFling = (scrollX > 0 || velocityX > 0) && (scrollX < getScrollRangeX()
                                || velocityX < 0)
                        if (!dispatchNestedPreFling(velocityX.toFloat(), 0f)) {
                            dispatchNestedFling(velocityX.toFloat(), 0f, canFling)
                            if (canFling) {
                                mOverScroller.fling(
                                    scrollX, scrollY, velocityX, 0, 0, Math.max(0, getScrollRangeX()), 0,
                                    0, mScreenWidth / 3, 0
                                )
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                    postInvalidateOnAnimation()
                                } else {
                                    postInvalidate()
                                }
                            }
                        }
                    }
                    Math.abs(velocityX) < Math.abs(velocityY) && Math.abs(velocityY) > mMinimumVelocity -> {
                        val canFling = (scrollY > 0 || velocityY > 0) && (scrollY < getScrollRangeY() || velocityY < 0)
                        if (!dispatchNestedPreFling(0f, velocityY.toFloat())) {
                            dispatchNestedFling(0f, velocityY.toFloat(), canFling)
                            if (canFling) {
                                mOverScroller.fling(
                                    scrollX, scrollY, 0, velocityY, 0, 0, 0,
                                    Math.max(0, getScrollRangeY()), 0, measuredHeight / 3
                                )
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                    postInvalidateOnAnimation()
                                } else {
                                    postInvalidate()
                                }
                            }
                        }
                    }
                    else -> {
                    }
                }
                recycleVelocityTracker()
            }
            else -> {
            }
        }

        mVelocityTracker.addMovement(event)
        return true
    }

    override fun computeScroll() {
        if (mOverScroller.computeScrollOffset()) {
//            Log.d("MultiFlowIndicator", "computeScroll")

            val oldX = scrollX
            val oldY = scrollY
            val x = mOverScroller.currX
            val y = mOverScroller.currY

            if (oldX != x || oldY != y) {
                overScrollBy(
                    x - oldX, y - oldY, oldX, oldY, getScrollRangeX(), getScrollRangeY(),
                    mOverflingDistance, mOverflingDistance, false
                )
            }

            scrollTo(mOverScroller.currX, mOverScroller.currY)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                postInvalidateOnAnimation()
            } else {
                postInvalidate()
            }
        }
    }

    override fun canScrollVertically(direction: Int): Boolean {
        return if (direction > 0) {//down
            if (childCount > 0) {
                val childView = getChildAt(childCount - 1)
                scrollY < childView.bottom - measuredHeight
            } else {
                false
            }
        } else {
            if (childCount > 0) {
                scrollY > 0
            } else {
                false
            }
        }
    }

    override fun canScrollHorizontally(direction: Int): Boolean {
        return if (direction > 0) {//right
            if (childCount > 0) {
                scrollX < measuredWidth - mScreenWidth
            } else {
                false
            }
        } else {
            if (childCount > 0) {
                scrollX > 0
            } else {
                false
            }
        }
    }

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams {
        return MarginLayoutParams(context, attrs)
    }

    override fun generateLayoutParams(p: LayoutParams?): LayoutParams {
        return MarginLayoutParams(p)
    }

    override fun generateDefaultLayoutParams(): LayoutParams {
        return MarginLayoutParams(MarginLayoutParams.WRAP_CONTENT, MarginLayoutParams.WRAP_CONTENT)
    }


    fun obtainAttributes(attrs: AttributeSet?) {
        attrs?.apply {
            val a = context.obtainStyledAttributes(attrs, R.styleable.MultiIndicator)

            mTextSelectedColor = a.getColor(R.styleable.MultiIndicator_si_text_selected_color, Color.RED)
            mTextUnSelectedColor = a.getColor(R.styleable.MultiIndicator_si_text_unselected_color, Color.BLACK)
            mTextSelectedSize = a.getDimension(
                R.styleable.MultiIndicator_si_text_selected_size,
                resources.getDimension(R.dimen.sp_10)
            )
            mTextUnSelectedSize = a.getDimension(
                R.styleable.MultiIndicator_si_text_unselected_size,
                resources.getDimension(R.dimen.sp_10)
            )
            mIndicatorHeight =
                    a.getDimension(
                        R.styleable.MultiIndicator_si_indicator_height,
                        resources.getDimension(R.dimen.dimen_3)
                    )
            mIndicatorWidth = a.getDimension(
                R.styleable.MultiIndicator_si_indicator_width,
                resources.getDimension(R.dimen.dimen_8)
            )
            mMaxHeight = a.getDimension(R.styleable.MultiIndicator_si_max_height, mScreenHeight.toFloat())
            mIndicatorColor =
                    a.getColor(R.styleable.MultiIndicator_si_indicator_color, Color.RED)

            a.recycle()
        }
    }


    //调用此方法滚动到目标位置
    fun smoothScrollTo(fx: Int, fy: Int) {
        val dx = fx - mOverScroller.finalX
        val dy = fy - mOverScroller.finalY
        smoothScrollBy(dx, dy)
    }

    //调用此方法设置滚动的相对偏移
    fun smoothScrollBy(dx: Int, dy: Int) {
        //设置mScroller的滚动偏移量
        mOverScroller.startScroll(mOverScroller.finalX, mOverScroller.finalY, dx, dy)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            postInvalidateOnAnimation()
        } else {
            postInvalidate()
        }
    }

    fun getScrollRangeY(): Int {
        val childView = if (childCount > 0) {
            getChildAt(childCount - 1)
        } else {
            getChildAt(0)
        }
        return childView.bottom - measuredHeight
    }

    fun getScrollRangeX(): Int {
        return measuredWidth - mScreenWidth
    }

    private fun initOrResetVelocityTracker() {
        if (!this::mVelocityTracker.isInitialized) {
            mVelocityTracker = VelocityTracker.obtain()
        } else {
            mVelocityTracker.clear()
        }
    }

    private fun initVelocityTrackerIfNotExists() {
        if (!this::mVelocityTracker.isInitialized) {
            mVelocityTracker = VelocityTracker.obtain()
        }
    }

    private fun recycleVelocityTracker() {
        if (!this::mVelocityTracker.isInitialized) {
            mVelocityTracker.recycle()
        }
    }


    fun setViewPager(viewPager: ViewPager) {
        this.mViewPager = viewPager

        this.mViewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {

            override fun onPageScrollStateChanged(p0: Int) {

            }

            override fun onPageScrolled(p0: Int, p1: Float, p2: Int) {
                Log.d(TAG, "onPageScrolled p0=$p0  p1=$p1  p2=$p2")

                val scrollChildIndex = if (p0 == mCurrentTab && mCurrentTabOffset < p1) {//右翻
                    p0 + 1
                } else {
                    p0
                }

                when (mMode) {
                    MODE.HORIZONL -> {
                        if (childCount > scrollChildIndex) {
                            val childView = getChildAt(scrollChildIndex)
                            val centerLeftX = childView.left + childView.measuredWidth.toFloat() / 2
                            var dx = (centerLeftX - mScreenWidth.toFloat() / 2).toInt()
                            dx = when {
                                dx < 0 -> -scrollX
                                dx > getScrollRangeX() -> getScrollRangeX() - scrollX
                                else -> dx - scrollX
                            }
                            mOverScroller.startScroll(
                                scrollX,
                                scrollY,
                                dx,
                                0
                            )
                        }
                    }
                    MODE.VERTICAL -> {
                        if (childCount > scrollChildIndex) {
                            val childView = getChildAt(scrollChildIndex)
                            val centerTopY = childView.top + childView.measuredHeight.toFloat() / 2
                            var dy = (centerTopY - measuredHeight.toFloat() / 2).toInt()
                            dy = when {
                                dy < 0 -> -scrollY
                                dy > getScrollRangeY() -> getScrollRangeY() - scrollY
                                else -> dy - scrollY
                            }
                            mOverScroller.startScroll(
                                scrollX,
                                scrollY,
                                0,
                                dy
                            )
                        }
                    }
                    else -> {
                        changedMode(MODE.HORIZONL)
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    postInvalidateOnAnimation()
                } else {
                    postInvalidate()
                }

                mCurrentTab = p0
                mCurrentTabOffset = p1
                mCurrentTabOffsetPixel = p2
            }

            override fun onPageSelected(p0: Int) {
            }
        })

        this.mViewPager.adapter?.apply {
            for (i in 0 until this.count) {
                mTitles.add(this.getPageTitle(i).toString())
            }
        }

    }

    fun changedMode(mode: MODE) {
        mOverScroller.startScroll(scrollX, scrollY, -scrollX, -scrollY)
//        mMode = mode
        mMode = if (mMode != MODE.HORIZONL) {
            MODE.HORIZONL
        } else {
            MODE.VERTICAL
        }

        requestLayout()

        post {
            when (mMode) {
                MODE.HORIZONL -> {
                    if (childCount > mCurrentTab) {
                        val childView = getChildAt(mCurrentTab)
                        val centerLeftX = childView.left + childView.measuredWidth.toFloat() / 2
                        var dx = (centerLeftX - mScreenWidth.toFloat() / 2).toInt()
                        dx = when {
                            scrollX + dx < 0 -> -scrollX
                            scrollX + dx > getScrollRangeX() -> getScrollRangeX() - scrollX
                            else -> dx - scrollX
                        }
                        mOverScroller.startScroll(
                            scrollX,
                            scrollY,
                            dx,
                            -scrollY
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            postInvalidateOnAnimation()
                        } else {
                            postInvalidate()
                        }
                    }
                }
                MODE.VERTICAL -> {
                    if (childCount > mCurrentTab) {
                        val childView = getChildAt(mCurrentTab)
                        val centerTopY = childView.top + childView.measuredHeight.toFloat() / 2
                        var dy = (centerTopY - measuredHeight.toFloat() / 2).toInt()
                        dy = when {
                            scrollY + dy < 0 -> -scrollY
                            scrollY + dy > getScrollRangeY() -> getScrollRangeY() - scrollY
                            else -> dy - scrollY
                        }
                        mOverScroller.startScroll(
                            scrollX,
                            scrollY,
                            -scrollX,
                            dy
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            postInvalidateOnAnimation()
                        } else {
                            postInvalidate()
                        }
                    }
                }
                else -> {
                }
            }
        }
    }

    fun setAdapter(spaceFlowAdapter: MultiFlowAdapter) {
        this.mSpaceFlowAdapter = spaceFlowAdapter
    }


    enum class MODE {
        HORIZONL, VERTICAL
    }

    companion object {
        val TAG = "MultiFlowIndicator"
    }
}