package net.z0kai.refreshlayout;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

import net.z0kai.refreshlayout.view.IFooterView;
import net.z0kai.refreshlayout.view.IHeaderView;

/**
 * Created by Z_0Kai on 16/9/29.
 */

public class KKRefreshLayout extends FrameLayout implements NestedScrollingParent,
        NestedScrollingChild {

    private final static String TAG = KKRefreshLayout.class.getSimpleName();
    private final static long MIN_REFRESH_TIME = 500;

    private boolean isRefreshing = false;
    private boolean isLoadingMore = false;
    private boolean isRefreshEnable = true;
    private boolean isLoadMoreEnable = false;

    private final NestedScrollingParentHelper mNestedScrollingParentHelper;
    private final NestedScrollingChildHelper mNestedScrollingChildHelper;
    private final int[] mParentScrollConsumed = new int[2];
    private final int[] mParentOffsetInWindow = new int[2];

    private View mTarget; // the target of the gesture
    private IHeaderView mHeaderView;
    private IFooterView mFooterView;

    private KKRefreshListener mListener;

    private float mOffset;
    private long mStartRefreshTime;
    private Runnable mMoveBackRunnable = new Runnable() {
        @Override
        public void run() {
            smoothScrollBack(mOffset, 0);
        }
    };

    public KKRefreshLayout(Context context) {
        this(context, null);
    }

    public KKRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mNestedScrollingParentHelper = new NestedScrollingParentHelper(this);
        mNestedScrollingChildHelper = new NestedScrollingChildHelper(this);

        mHeaderView = KKRefreshLayoutConfig.getHeaderViewProvider().get(context);
        addView(mHeaderView.getView());

        mFooterView = KKRefreshLayoutConfig.getFooterViewProvider().get(context);
        addView(mFooterView.getView());
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (getChildCount() == 0) {
            return;
        }
        if (mTarget == null) {
            ensureTarget();
        }
        if (mTarget == null) {
            return;
        }
        layoutChildren();
    }

    public void startRefresh() {
        if (isRefreshing || !isRefreshEnable) {
            return;
        }
        mStartRefreshTime = System.currentTimeMillis();
        isRefreshing = true;
        mHeaderView.startRefresh();

        ValueAnimator valueAnimator = ObjectAnimator.ofFloat(0, mHeaderView.getRefreshingSize()).setDuration(300);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                mOffset = value;
                layoutChildren();
                if (value == mHeaderView.getRefreshingSize() && mListener != null) {
                    mListener.onRefresh();
                }
            }
        });
        valueAnimator.setInterpolator(new DecelerateInterpolator());
        valueAnimator.setStartDelay(100);
        valueAnimator.start();
    }

    public void startLoadMore() {
        if (isLoadingMore || !isLoadMoreEnable) {
            return;
        }
        isLoadingMore = true;
        if (mListener != null) {
            mListener.onLoadMore();
        }
    }

    public void setRefreshEnable(boolean enable) {
        isRefreshEnable = enable;
    }

    public void setLoadMoreEnable(boolean enable) {
        isLoadMoreEnable = enable;
    }

    public void finishRefresh() {
        if (isRefreshing) {
            long refreshTime = System.currentTimeMillis() - mStartRefreshTime;
            if (refreshTime < MIN_REFRESH_TIME) {
                postDelayed(mMoveBackRunnable, MIN_REFRESH_TIME - refreshTime);
            } else {
                mMoveBackRunnable.run();
            }
        }
    }

    public void finishLoadMore() {
        if(isLoadingMore) {
            isLoadingMore = false;
            mOffset = 0;
        }
    }

    public void setRefreshListener(KKRefreshListener listener) {
        mListener = listener;
    }

    private void layoutChildren() {
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int offset = (int) mOffset;
//        int offset = (int) (mDecelerateInterpolator.getInterpolation(mOffset/getHeight()) * getHeight());
        int left, right, top, bottom;

        if (mHeaderView != null) {
            View view = mHeaderView.getView();
            left = paddingLeft;
            top = paddingTop - view.getMeasuredHeight() + offset;
            right = left + view.getMeasuredWidth();
            bottom = top + view.getMeasuredHeight();
            view.layout(left, top, right, bottom);
            mHeaderView.onScroll(offset);
        }

        if (mFooterView != null) {
            View view = mFooterView.getView();
            left = paddingLeft;
            top = paddingTop + getHeight() + offset;
            right = left + view.getMeasuredWidth();
            bottom = top + view.getMeasuredHeight();
            view.layout(left, top, right, bottom);
        }

        if (mTarget != null) {
            View view = mTarget;
            left = paddingLeft;
            right = left + getWidth();
            if (offset > 0) {
                top = paddingTop + offset;
                bottom = top + getHeight();
            } else {
                top = paddingTop;
                bottom = top + getHeight() + offset;
            }
            view.layout(left, top, right, bottom);
        }
    }

    private void ensureTarget() {
        // Don't bother getting the parent height if the parent hasn't been laid
        // out yet.
        if (mTarget == null) {
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (!(child instanceof IHeaderView) && !(child instanceof IFooterView)) {
                    mTarget = child;
                    break;
                }
            }
        }
    }

    /**
     * @return Whether it is possible for the child view of this layout to
     *         scroll up. Override this if the child view is a custom view.
     */
    public boolean canChildScrollUp() {
        return ViewCompat.canScrollVertically(mTarget, -1);
    }

    public boolean canChildScrollDown() {
        return ViewCompat.canScrollVertically(mTarget, 1);
    }

    private void finishSpinner(float overScrollTop) {
        if (overScrollTop >= mHeaderView.getMinRefreshSize()) {
            if (mListener != null) {
                mListener.onRefresh();
            }
            mStartRefreshTime = System.currentTimeMillis();
            mHeaderView.startRefresh();
            isRefreshing = true;
            smoothScrollBack(overScrollTop, mHeaderView.getRefreshingSize());
        } else {
            smoothScrollBack(overScrollTop, 0);
        }
    }

    private void smoothScrollBack(final float fromOffset, float toOffset) {
        if (fromOffset <= toOffset) {
            return;
        }
        ValueAnimator valueAnimator = ObjectAnimator.ofFloat(fromOffset, toOffset).setDuration(300);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                mOffset = value;
                layoutChildren();
                if (value == 0) {
                    isRefreshing = false;
                    isLoadingMore = false;
                    mHeaderView.stopRefresh();
                }
            }
        });
        valueAnimator.setInterpolator(new DecelerateInterpolator());
        valueAnimator.setStartDelay(100);
        valueAnimator.start();
    }

    private void offsetUp(int dy) {
        if (mOffset < getHeight() / 3) {
            dy /= 2;
        } else if (mOffset < getHeight() / 2){
            dy /= 3;
        } else {
            dy /= 4;
        }
        mOffset += dy;
    }

    // NestedScrollingParent
    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        Log.e(TAG, "onStartNestedScroll, " + nestedScrollAxes);
//        return super.onStartNestedScroll(child, target, nestedScrollAxes);
        return isEnabled()
                && !isRefreshing && !isLoadingMore
                && (isRefreshEnable || isLoadMoreEnable)
                && (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int axes) {
        // Reset the counter of how much leftover scroll needs to be consumed.
        mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes);
        // Dispatch up to the nested parent
        startNestedScroll(axes & ViewCompat.SCROLL_AXIS_VERTICAL);
        Log.e(TAG, "onNestedScrollAccepted, " + axes);
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        Log.e(TAG, "onNestedPreScroll, " + "dx:" + dx + ", dy:" + dy + ", consumed:" + consumed);
        // If we are in the middle of consuming, a scroll, then we want to move the spinner back up
        // before allowing the list to scroll
        if (dy > 0 && mOffset > 0) {
            if (dy > mOffset) {
                consumed[1] = dy - (int) mOffset;
                mOffset = 0;
            } else {
                mOffset -= dy;
                consumed[1] = dy;
            }
            layoutChildren();
        }

        // Now let our nested parent consume the leftovers
        final int[] parentConsumed = mParentScrollConsumed;
        if (dispatchNestedPreScroll(dx - consumed[0], dy - consumed[1], parentConsumed, null)) {
            consumed[0] += parentConsumed[0];
            consumed[1] += parentConsumed[1];
        }
    }

    @Override
    public int getNestedScrollAxes() {
        Log.e(TAG, "getNestedScrollAxes");
        return mNestedScrollingParentHelper.getNestedScrollAxes();
    }

    @Override
    public void onStopNestedScroll(View target) {
        Log.e(TAG, "onStopNestedScroll");
        mNestedScrollingParentHelper.onStopNestedScroll(target);
        // Finish the spinner for nested scrolling if we ever consumed any
        // unconsumed nested scroll
        if (mOffset > 0) {
            finishSpinner(mOffset);
//            mTotalUnconsumed = 0;
        }
        // Dispatch up our nested parent
        stopNestedScroll();
    }

    @Override
    public void onNestedScroll(final View target, final int dxConsumed, final int dyConsumed,
                               final int dxUnconsumed, final int dyUnconsumed) {
        Log.e(TAG, "onNestedScroll, " + "dxConsumed:" + dxConsumed + ", dyConsumed:" + dyConsumed
                + ", dxUnconsumed:" + dxUnconsumed + ", dyUnconsumed:" + dyUnconsumed);
        // Dispatch up to the nested parent first
        dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, mParentOffsetInWindow);

        // This is a bit of a hack. Nested scrolling works from the bottom up, and as we are
        // sometimes between two nested scrolling views, we need a way to be able to know when any
        // nested scrolling parent has stopped handling events. We do that by using the
        // 'offset in window 'functionality to see if we have been moved from the event.
        // This is a decent indication of whether we should take over the event stream or not.
        final int dy = dyUnconsumed + mParentOffsetInWindow[1];
        if (dy < 0 && !canChildScrollUp() && isRefreshEnable) {
//            mOffset += Math.abs(dy);
            offsetUp(-dy);
            layoutChildren();
        }

        if (dy > 0 && !canChildScrollDown() && isLoadMoreEnable) {
            if (!isLoadingMore) {
                isLoadingMore = true;
                if (mListener != null) {
                    mListener.onLoadMore();
                }
            }
            mOffset -= Math.abs(dy);
            if (-mOffset > mFooterView.getSize()) {
                mOffset = -mFooterView.getSize();
            }
            layoutChildren();
        }
    }

    // NestedScrollingChild

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        Log.e(TAG, "setNestedScrollingEnabled");
        mNestedScrollingChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        Log.e(TAG, "isNestedScrollingEnabled");
        return mNestedScrollingChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        Log.e(TAG, "startNestedScroll, " + axes);
        return mNestedScrollingChildHelper.startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll() {
        Log.e(TAG, "stopNestedScroll");
        mNestedScrollingChildHelper.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        Log.e(TAG, "hasNestedScrollingParent");
        return mNestedScrollingChildHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                                        int dyUnconsumed, int[] offsetInWindow) {
        Log.e(TAG, "dispatchNestedScroll, " + "dxConsumed:" + dxConsumed + ", dyConsumed:" + dyConsumed
                + ", dxUnconsumed:" + dxUnconsumed + ", dyUnconsumed:" + dyUnconsumed
                + ", offsetInWindow:" + offsetInWindow);
        return mNestedScrollingChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed,
                dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        Log.e(TAG, "dispatchNestedPreScroll," + "dx:" + dx + ", dy:" + dy);
        return mNestedScrollingChildHelper.dispatchNestedPreScroll(
                dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX,
                                    float velocityY) {
        Log.e(TAG, "onNestedPreFling," + "velocityX:" + velocityX + ", velocityY:" + velocityY);
        return dispatchNestedPreFling(velocityX, velocityY);
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY,
                                 boolean consumed) {
        Log.e(TAG, "onNestedFling," + "consumed:" + consumed);
        return dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        Log.e(TAG, "dispatchNestedFling, " + "consumed:" + consumed);
        return super.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mNestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }

}