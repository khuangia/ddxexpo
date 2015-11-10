//khuangia@gmail.com

package com.kr.apkexpo.listview;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ListView;

public class JListViewCtrl extends ListView {

    public interface IOnOverScrolledListener {
        void onOverScrolled();
    }

    private static final int MAX_OVER_SCROLL_BASE = 200;

    private int mMaxOverScroll = 0;
    private JListViewItemLayout mFocusedItemView;
    private IOnOverScrolledListener mOnOverScrolledListener;

    public JListViewCtrl(Context context) {
        super(context);
        init(context);
    }

    public JListViewCtrl(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public JListViewCtrl(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {

        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        float density = metrics.density;
        mMaxOverScroll = (int) (density * MAX_OVER_SCROLL_BASE);
    }

    @Override
    protected boolean overScrollBy(int deltaX, int deltaY, int scrollX,
                                   int scrollY, int scrollRangeX, int scrollRangeY,
                                   int maxOverScrollX, int maxOverScrollY, boolean isTouchEvent) {

        return super.overScrollBy(deltaX, deltaY, scrollX, scrollY,
                scrollRangeX, scrollRangeY, maxOverScrollX, mMaxOverScroll,
                isTouchEvent);
    }

    @Override
    protected void onOverScrolled(int scrollX, int scrollY,
                                  boolean clampedX, boolean clampedY) {
        if (mOnOverScrolledListener != null) {
            mOnOverScrolledListener.onOverScrolled();
        }
    }

    public void setOnOverScrolledListener(IOnOverScrolledListener listener) {
        mOnOverScrolledListener = listener;
    }

    public void shrinkListItem(int position) {
        View item = getChildAt(position);

        if (item != null) {
            try {
                ((JListViewItemLayout) item).shrink();
            } catch (ClassCastException e) {
                e.printStackTrace();
            }
        }
    }

    public void onTouchEventDown(MotionEvent event) {

        int x = (int) event.getX();
        int y = (int) event.getY();
        int position = pointToPosition(x, y);

        if (position != INVALID_POSITION) {

            JListViewItem data = (JListViewItem) getItemAtPosition(position);
            mFocusedItemView = data.view;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction()) {

        case MotionEvent.ACTION_DOWN:
                onTouchEventDown(event);
            break;

        default:
            break;
        }

        if (mFocusedItemView != null) {
            mFocusedItemView.onRequireTouchEvent(event);
        }

        return super.onTouchEvent(event);
    }
}
