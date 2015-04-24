package com.hunterrobbert.awareviewpager;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by hunter on 2/14/15.
 */
public class ObservableRecyclerView extends RecyclerView {

    private RecyclerListener mListener;

    public interface RecyclerListener {
        public void onScrollChanged(int deltaX, int deltaY);
        public void onInterceptTouch(MotionEvent ev);
        public void onTouch(MotionEvent ev);
    }

    public void setRecyclerListener(RecyclerListener listener) {
        mListener = listener;
    }


    public ObservableRecyclerView(Context context) {
        super(context);
    }

    public ObservableRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ObservableRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (mListener != null) {
            mListener.onScrollChanged(l-oldl,t-oldt);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        if (mListener != null) {
            mListener.onInterceptTouch(e);
        }
        return super.onInterceptTouchEvent(e);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (mListener != null) {
            mListener.onTouch(e);
        }
        return super.onTouchEvent(e);

    }
}
