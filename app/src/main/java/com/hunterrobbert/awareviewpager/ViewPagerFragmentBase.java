package com.hunterrobbert.awareviewpager;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;


/**
 * Handles keeping track of its child fragment recyclerView and it's scroll position in relation to other fragments in the viewPager
 */

public class ViewPagerFragmentBase extends Fragment {

    private static final String TAG = ViewPagerFragmentBase.class.getSimpleName();

    public static final String LESSON_TO_MODULE_BROADCAST = "lesson_to_module_broadcast";
    public static final String BROADCAST_TYPE = "key_lesson_broadcast_type";
    public static final String BROADCAST_TYPE_UPDATE_SCROLL_POSITION = "broadcast_lesson_update_module_scroll_position";
    public static final String BROADCAST_KEY_SCROLL_POSITION = "key_scroll_position";
    public static final String BROADCAST_KEY_OFFSET_POSITION = "key_offset_position";

    protected ObservableRecyclerView mRecyclerView;
    protected LinearLayoutManager mLinearLayoutManager;

    protected int scrollCumulator = 0;

    //interface for sending scroll events back to the base activity
    public interface FragmentListener {
        public void onFragmentHeaderChanged(int position, int offset, int dx, int dy);
        public void onFragmentScrollStateChanged(int newState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public void onResume() {
        super.onResume();
        initiateScrollPosition();

        //view pager events and scroll position updates are sent to fragments through a local broadcast. Register the receiver
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mBroadcastReceiver,
                new IntentFilter(LESSON_TO_MODULE_BROADCAST));
    }

    @Override
    public void onStop() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mBroadcastReceiver);

        super.onStop();
    }



    protected int getHeaderHeight() {
        return ((MainActivity) getActivity()).getHeaderHeight();
    }

    protected void setupRecyclerView(RecyclerView recyclerView) {
        mRecyclerView = (ObservableRecyclerView) recyclerView;
        mRecyclerView.setOnScrollListener(mRecyclerScrollListener);
        mLinearLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLinearLayoutManager);

    }

    protected void initiateScrollPosition() {
        int scroll = ((MainActivity) getActivity()).getScrollPositionWatcherInt();
        int offset = ((MainActivity) getActivity()).getOffsetPositionWatcherInt();

        updateScrollPosition(scroll, offset);
    }

   private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String broadcastType = intent.getStringExtra(BROADCAST_TYPE);
            switch (broadcastType) {
                case BROADCAST_TYPE_UPDATE_SCROLL_POSITION :
                    int position = intent.getIntExtra(BROADCAST_KEY_SCROLL_POSITION,0);
                    int offset = intent.getIntExtra(BROADCAST_KEY_OFFSET_POSITION,0);
                    updateScrollPosition(position,offset);
                    break;
            }



        }
    };

    /**
     * Used to track the scroll position of all module fragments
     * recyclerViews and send the data to the lesson activity. The lesson
     * activity will then translate the appropriate views in sync with the scrolling
     * of the recyclerview. */
    protected RecyclerView.OnScrollListener mRecyclerScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            if (getActivity() instanceof FragmentListener) {
                ((FragmentListener) getActivity()).onFragmentScrollStateChanged(newState);
            }
        }

        //Scroll calculation with a recyclerView is based on position and offset.  position is the first child visible of the
        //recycler view.  Offset is the amount of that child view which is visible.  For example,
        //if position 0 is visible with offset of mHeaderHeight, that means the entire header is visible.
        //if position 0 is visible with offset of half mHeaderHeight, that means half of the header has been scrolled off screen and half is still visible


        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);

            //If the header is still in view (firstVisibleItemPosition == 0) we want to have the lesson activity's
            //contents (header image, titlebox, tabs, fab) scrolled.  The scrollCumulator will act as an offset watcher for position 0 and will be
            //passed to the lesson activity to tell all dependant views how much to be translated by.
            if (mLinearLayoutManager.findFirstVisibleItemPosition() == 0) {
                scrollCumulator = scrollCumulator + dy;
                if (getActivity() instanceof FragmentListener) {
                    ((FragmentListener) getActivity()).onFragmentHeaderChanged(0, scrollCumulator, dx, dy);
                }
            } else if (mLinearLayoutManager.findFirstVisibleItemPosition() >= 1) {

                //If the firstVisibleItemPosition is no longer 0, we know we have completely scrolled the
                //head view off of the screen.  There is no need at this point to keep translating the dependant views
                //in the lesson activity.  Notify the lesson activity of the firstVisibleItemPosition so it can make sure
                //all views are completely scrolled out of the way

                if (getActivity() instanceof FragmentListener) {
                    ((FragmentListener) getActivity()).onFragmentHeaderChanged(mLinearLayoutManager.findFirstVisibleItemPosition(), 0, dx, dy);
                }
            }
        }
    };

    /** Called by the base activity whenever a user starts to swipe horizontally between module fragments.
     * This assures that the adjacent fragments have reflected any scroll change that occurred in the fragment being scrolled from
     * and makes a seamless transition from one module to the next.
    */
    public void updateScrollPosition(int position,int offset) {

        if (mLinearLayoutManager != null) {

            // if the current fragment is at its starting point(position==0 && offset==0), put the adjacent fragments at their starting point as well - fully scrolled down.
            if (position == 0 && offset == 0) {
                mLinearLayoutManager.scrollToPositionWithOffset(0,0);

            //If the current fragment has been scrolled but the current scroll position
            //is not enough to hide the header, update the adjacent fragments to
            //the proper value
            } else if (position == 0){
                mLinearLayoutManager.scrollToPositionWithOffset(1,getHeaderHeight() - offset);

            //If the current fragments header has been completely scrolled off the screen, update the adjacent fragments
            //to have item 1 (which is the first item after the header. also the first cell containing real content) to be
            //at the top of the screen and with no header showing.
            } else if (position >= 1) {
                //Only scroll to position 1 if the first position is 0.  If the first position is 1 or greater, we know the
                //fragments recyclerView is already taking up the whole screen.  This acts as a scroll position holder.
                if (mLinearLayoutManager.findFirstVisibleItemPosition() < 1) {
                    //TODO: the sticky header will be on top of the content.  Fix this?
                    mLinearLayoutManager.scrollToPositionWithOffset(1,0);
                }
            }


        }
    }
}
