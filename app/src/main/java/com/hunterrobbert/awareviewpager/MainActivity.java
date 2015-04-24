package com.hunterrobbert.awareviewpager;

import android.app.FragmentManager;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Outline;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nineoldandroids.view.ViewHelper;
import com.nineoldandroids.view.ViewPropertyAnimator;


public class MainActivity extends ActionBarActivity implements ViewPagerFragmentBase.FragmentListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    public static final String KEY_VIEW_PAGER_FRAG_TYPE_STRING = "key_view_pager_frag_type_string";

    //To add or remove viewPager fragments, add or remove titles
    int[] mViewPagerFragmentTitles = new int[] {
            R.string.fragment_title_1,
            R.string.fragment_title_2,
            R.string.fragment_title_3,
            R.string.fragment_title_4,
    };

    //base views
    protected ViewPager mViewPager;
    protected FragmentViewPagerAdapter mFragmentViewPagerAdapter;
    protected RelativeLayout mHeader;
    protected ImageView mHeaderImage;
    protected SlidingTabLayout mSlidingTabLayout;
    protected LinearLayout mTitleBox;
    protected View mTitleBackground;
    protected TextView mTitleText;
    protected TextView mSubtitleText;
    protected View mFauxToolbar;
    protected View mToolbarBackButtonContainer;
    protected ImageView mOverFlow;
    protected View mFab;

   //view translation constants
    protected int mHeaderHeight; //the total height of the recyclerViews header
    protected int mActionBarHeight; // 56dp by default. 168px
    protected int mFlexibleSpaceSize = 380; //default value to avoid null errors before measure
    protected float mTitleBoxOriginalSize = 200; //default value to avoid null errors before measure
    protected double mMinimumFontScale; //used to calculate the maximum amount to scale the title text by
    protected float mTitleTop; //the mOffset amount that the title should stick
    protected float mOverFlowTop; //the mOffset amount that the overflow three dot menu should stick
    protected float mTabTop; //the mOffset amount that the SlidingTabLayout should stick
    protected float mCompactToolbarSize = 0; //the size of the shrunk action bar (56dp by default) and the slidingTabLayout
    protected float mBeginTransformations; //where all views should start transforming into the compact toolbar

    protected int mGeneralScrollWatch = 0; //overall movement of the recyclerView
    protected int mPreviousOffset = 0; //used to keep watch of the scroll direction
    protected int mPosition = 0; //the first visible item of the recyclerView in view
    protected int mOffset = 0; //the scroll position of the fragments 0 position header.  Only changes if mPosition 0 is in view (aka header)
    protected float diffHolder = 1; //used to keep track of compact toolbar recall.

    private Handler mHandler;

    protected boolean mainFabShown = false;
    protected boolean fabOpen = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //measure view sizes for reference later that don't require first being added to view hierarchy
        mHeaderHeight = getDimPx(R.dimen.header_size);
        mActionBarHeight = getDimPx(R.dimen.large_fab_size);
        mMinimumFontScale = (double) getDimPx(R.dimen.action_bar_text_size) / (double) getDimPx(R.dimen.title_text_size);

        mBeginTransformations = (mHeaderHeight / 4);

        mHandler = new Handler();


        //instantiate views
        //base views
        mFauxToolbar =  findViewById(R.id.faux_toolbar);
        mToolbarBackButtonContainer = findViewById(R.id.back_button_container);
        mHeader = (RelativeLayout) findViewById(R.id.header_box);
        mHeaderImage = (ImageView) findViewById(R.id.header_image);
        mViewPager = (ViewPager) findViewById(R.id.view_pager);
        mTitleBox = (LinearLayout) findViewById(R.id.title_box);
        mTitleText = (TextView) findViewById(R.id.title_text);
        mSubtitleText = (TextView) findViewById(R.id.level_text);
        mTitleBackground = findViewById(R.id.title_background);
        mSlidingTabLayout = (SlidingTabLayout) findViewById(R.id.sliding_tabs);
        mOverFlow = (ImageView) findViewById(R.id.overflow_button);
        mFab = findViewById(R.id.fab_layout);

        //set up fab and its initial position
        //the fab has to be pushed down to center itself on the line between the header image and the titlebox
        int fabTop = getDimPx(R.dimen.header_size)
                - getDimPx(R.dimen.bar_height)
                - getDimPx(R.dimen.small_tab_size)
                - (getDimPx(R.dimen.large_fab_size)/2);
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mFab.getLayoutParams();
        layoutParams.setMargins(0,fabTop,getDimPx(R.dimen.keyline_1),0);
        clipAndElevate(mFab, getResources().getInteger(R.integer.fab_elevation));


        int backgroundHeight = getDimPx(R.dimen.bar_height) + getDimPx(R.dimen.small_tab_size);
        ViewGroup.LayoutParams titleBackgroundParams = mTitleBackground.getLayoutParams();
        titleBackgroundParams.height = backgroundHeight;
        mTitleBackground.requestLayout();

        //set viewpager adapter
        mFragmentViewPagerAdapter = new FragmentViewPagerAdapter(getFragmentManager());
        mViewPager.setAdapter(mFragmentViewPagerAdapter);



        //set up sliding tabs to go with viewpager
        mSlidingTabLayout.setCustomTabView(R.layout.tab_indicator, android.R.id.text1);
        Resources res = getResources();
        mSlidingTabLayout.setSelectedIndicatorColors(res.getColor(R.color.colorAccent));
        mSlidingTabLayout.setDistributeEvenly(true);
        mSlidingTabLayout.setViewPager(mViewPager);
        setSlidingTabLayoutContentDescriptions();


        //position views dependent on other views positions. Views need to be added before calculations can be done.
        //TODO: if possible, change all view dimensions to constants in dimens file to allow for measurement in onCreate
        adjustLayoutSetConstants();

    }


    /** Watches the state of the current fragments scrolling and translates views accordingly */
    //for more details, see comments in ViewPagerFragmentBase
    @Override
    public void onFragmentHeaderChanged(int position, int offset, int dx, int dy) {
        mGeneralScrollWatch = mGeneralScrollWatch + dy; //used in comparison with mPreviousScrollPosition to determine scroll direction
        mPosition = position; //first visible child in the fragments recyclerView

        //Calculate the maximum amounts view should be scrolled
        mTitleTop = mFlexibleSpaceSize + ((mTitleBoxOriginalSize - (mTitleText.getHeight() + mSubtitleText.getHeight())) / 2) - ((mActionBarHeight - mTitleText.getHeight()) / 2);
        mOverFlowTop = (mFlexibleSpaceSize + (mTitleBoxOriginalSize/2)) - ((mActionBarHeight - mOverFlow.getHeight())/2);
        mTabTop = mFlexibleSpaceSize + (mTitleBoxOriginalSize - mActionBarHeight);


        //the header of the fragments recyclerView is in view
        if (position == 0) {

            mOffset = mOffset + dy;

            float newYPosition = Math.min(mOffset, mTitleTop);
            float newTabYPosition = Math.min(mOffset, mTabTop);

            if (mOffset >= 0) {

                //We're transforming between the unscrolled, expanded titleBox and its fully scrolled, compacted version.
                //What are we going as the scroll between those two states changes :
                //  1. TranslateY the titleBox (this contains the title and subtitle) with a parallax factor to have the titleText
                //      end up in its final position just as the scroll finishes
                //  2. Scale the title text to end up as an appropriate toolbar title text size
                //  3. Alpha out the subtitle
                //  4. TranslateY the overflow menu to end up in the correct spot in the compacted final toolbar
                //  5. Scroll the header image (with a parallax factor)

                //scale title text
                double titleScale = getScaleBetweenRange(mOffset,mBeginTransformations,mTabTop,1,mMinimumFontScale);
                ViewHelper.setPivotY(mTitleText, mTitleText.getHeight() / 2);
                ViewHelper.setScaleY(mTitleText, (float) titleScale);
                ViewHelper.setScaleX(mTitleText, (float) titleScale);

                //alpha of the subTitleText
                double alphaScale = getScaleBetweenRange(mOffset,mBeginTransformations, mTitleTop,1,0);
                ViewHelper.setAlpha(mSubtitleText, (float) alphaScale);

                //titleBox parallax factor
                double parallaxScale = getScaleBetweenRange(mOffset,mBeginTransformations,mTabTop,1,(mTitleTop /mTabTop));
                mTitleBox.setTranslationY((float) (-newTabYPosition * parallaxScale));

                //overflow parallax factor
                double overFlowParallax = getScaleBetweenRange(mOffset,mBeginTransformations,mTabTop,1,(mOverFlowTop/mTabTop));
                mOverFlow.setTranslationY((float) (-newTabYPosition * overFlowParallax));


                //Views that scroll in direct relation to the offset
                mTitleBackground.setTranslationY(-newTabYPosition);
                mSlidingTabLayout.setTranslationY(-newTabYPosition);
                mFab.setTranslationY(-newTabYPosition);

                //scroll the header image with a parallax factor
                mHeaderImage.setTranslationY(-newYPosition * 0.5f);


                //bring elevation back down if was increased by the compactActionBar return
                setHeaderGroupElevation(0);



                //Once the titleBox has fully transformed into the its compact toolbar form, translating off the screen is done
                //by translating all of the header views containing view - mHeader, along with the "faux" toolbar (the back button)

                //get the range that mHeader and mFauxToolbar should be translated between
                double headerTrans = getScaleBetweenRange(mOffset, mTabTop,mTabTop + mCompactToolbarSize,0, mCompactToolbarSize);


                if (mPreviousOffset < mGeneralScrollWatch) {
                    //moving back up. scrolling/translating should be handled by getScaleBetweenRange. set diffHolder to
                    //1 to prevent scroll/translate by diffHolder
                    //for more of what diffHolder does, look at the block which handles the sticky toolbar recall
                    diffHolder = 1;
                }

                //if the compact toolbar is showing at all, diffHeader will be less than or equal to 0. Start translating from its
                //current position instead of resetting it to -mCompactToolbarSize like headerTrans would do
                if (diffHolder <= 0) {
                    diffHolder = diffHolder - dy;
                    //account for dy suddenly throwing diffHolder into the positive range and causing the compactActionBar to jump to headerTrans value
                    if (diffHolder > 0) {
                        diffHolder = 0;
                    }
                    mHeader.setTranslationY(diffHolder);
                    mFauxToolbar.setTranslationY(diffHolder);
                } else {
                    //default scroll/translation by getScaleBetweenRange
                    mHeader.setTranslationY((float) -headerTrans);
                    mFauxToolbar.setTranslationY((float) -headerTrans);
                }



            } else {
                //the recyclerView has been scrolled all the way to the top (the entire header is in view). Set the views back to their original spots
                //account for dy suddenly throwing mOffset into negative numbers and translating views too far down
                mTitleBox.setTranslationY(0);
                mOverFlow.setTranslationY(0);
                mTitleBackground.setTranslationY(0);
                mSlidingTabLayout.setTranslationY(0);
                mHeaderImage.setTranslationY(0);
                mHeader.setTranslationY(0);
                mFab.setTranslationY(0);

            }

        } else {
            //position 0 (or the header) of the recycler view is now out of view.  dy will not always capture the full range of
            //values and leave views with tails showing.  To prevent this, set views to their max trans value here.
            mOffset = mHeaderHeight;
            mTitleBox.setTranslationY(-mTitleTop);
            mOverFlow.setTranslationY(-mOverFlowTop);
            mTitleBackground.setTranslationY(-mTabTop);
            mSlidingTabLayout.setTranslationY(-mTabTop);
            mHeaderImage.setTranslationY(-mFlexibleSpaceSize * 0.5f);
            mFab.setTranslationY(-mTabTop);


            //this block is responsible for pulling in and pushing back up the compact toolbar
            //on scroll down, if compact toolbar is gone, bring it into view
            if (mPreviousOffset > mGeneralScrollWatch) {
                //is starting to scroll down. start pulling compactAB down
                float transHeader = mHeader.getTranslationY() - dy;
                if (transHeader < 0) {
                    mHeader.setTranslationY(transHeader);
                    mFauxToolbar.setTranslationY(transHeader);
                    diffHolder = transHeader;
                } else {
                    mHeader.setTranslationY(0);
                    mFauxToolbar.setTranslationY(0);
                    diffHolder = 0;

                }

                //set an elevation to the compacted toolbar/titleBox thing.  This will be removed once mPosition 0 comes back into view
                setHeaderGroupElevation(getResources().getInteger(R.integer.toolbar_elevation));


            } else if (mPreviousOffset != mGeneralScrollWatch) { //make sure its actually moving so the compact toolbar doesn't just jump back to -mCompactToolbarSize
                //recyclerView is scrolling back up
                diffHolder = 1;
                float transHeader = mHeader.getTranslationY() - dy;

                if (transHeader < 0 && transHeader > -mCompactToolbarSize) {
                    mHeader.setTranslationY(transHeader);
                    mFauxToolbar.setTranslationY(transHeader);
                } else if (transHeader > -mCompactToolbarSize) {
                    mHeader.setTranslationY(-mCompactToolbarSize);
                    mFauxToolbar.setTranslationY(-mCompactToolbarSize);
                } else {
                    mHeader.setTranslationY(-mCompactToolbarSize);
                    mFauxToolbar.setTranslationY(-mCompactToolbarSize);
                }
            }
        }

        //hide show the fab once scroll has reached a defined point
        if (mOffset > mBeginTransformations) {
            if (mainFabShown) {
                mainFabShown = false;
                hideFab(mFab);
            }
        }  else {
            if (!mainFabShown) {
                mainFabShown = true;
                showFab(mFab);
            }
        }



        mPreviousOffset = mGeneralScrollWatch;
    }



    //given a value, if that value is between a defined range, will return the linear equivalent of another range
    //for example. given the range [100-200] as the input range and [5-1] as the output range, given a value of 100, getScaleBetweenRange will
    //return 5.
    //if given a value outside of [100-200] such as 95, getScaleBetweenRange will default to the min value, which in this case would be 5.
    /**
     *
     * @param value input value to return linear output equivalent
     * @param inputMin bottom end of input range. If value is less than inputMin, value will default to inputMin
     * @param inputMax top end of input range.  If value is more than inputMax, value will default to inputMax
     * @param outputMin bottom end of output range to be returned. for value of inputMin, outputMin will be returned
     * @param outputMax top end of output range to be returned. for value of inputMax, outputMax will be returned
     * @return linear equivalent of value between outputMin and outputMax. default return for values less than inputMin is outputMin.
     *         default return for values more than inputMax is outputMax
     */
    private double getScaleBetweenRange(float value, float inputMin, float inputMax, double outputMin, double outputMax) {
        if (value < inputMin) {
            return outputMin;
        } else if (value > inputMax) {
            return outputMax;
        } else {
            return (outputMin * (1 - ((value-inputMin)/(inputMax-inputMin)))) + (outputMax * ((value-inputMin)/(inputMax-inputMin)));
        }
    }

    //The title area and slidingTabLayout is not elevated when scrolled all the way down, but when pulled back down as a sticky
    //header in its compacted form, it does.
    private void setHeaderGroupElevation(int elevation) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            mFauxToolbar.setElevation(elevation);
            mOverFlow.setElevation(elevation);
            mSlidingTabLayout.setElevation(elevation);
            mTitleBackground.setElevation(elevation);
            mTitleBox.setElevation(elevation);
       } else {
            ViewCompat.setElevation(mFauxToolbar, elevation);
            ViewCompat.setElevation(mOverFlow, elevation);
            ViewCompat.setElevation(mSlidingTabLayout, elevation);
            ViewCompat.setElevation(mTitleBackground, elevation);
            ViewCompat.setElevation(mTitleBox, elevation);
        }
    }


    /** Broaadcasts an update to all attached fragments once the current fragment stops scrolling */
    //originally, updates were sent whenever the view pager started to scroll, but the issue was
    //when flipping between viewPager fragments quickly, recyclerViews would be scrolled to the proper
    //position just a bit too late and the out of place list was visible for a split second.
    @Override
    public void onFragmentScrollStateChanged(int newState) {
        //reset mOffset if it has dipped below 0 once scrolling stops or starts.  Helps prevents views from over translating
        //new state 0 = idle. 1 = scrolling
        if (newState == 0) {
            if (mOffset < 0) {
                mOffset = 0;
            }

            broadcastScrollStateUpdate();
        }
    }

    //handles broadcasting updates to all subscribing fragments
    private void broadcastScrollStateUpdate() {
        Intent intent = new Intent(ViewPagerFragmentBase.LESSON_TO_MODULE_BROADCAST);
        intent.putExtra(ViewPagerFragmentBase.BROADCAST_TYPE, ViewPagerFragmentBase.BROADCAST_TYPE_UPDATE_SCROLL_POSITION);
        intent.putExtra(ViewPagerFragmentBase.BROADCAST_KEY_SCROLL_POSITION, getScrollPositionWatcherInt());
        intent.putExtra(ViewPagerFragmentBase.BROADCAST_KEY_OFFSET_POSITION,getOffsetPositionWatcherInt()); //don't let anything bellow 0 to be sent
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    public int getScrollPositionWatcherInt() {
        return mPosition;
    }

    public int getOffsetPositionWatcherInt() {
        return Math.max(mOffset,0);
    }


    private int getDimPx(int resourceId) {
        return getResources().getDimensionPixelSize(resourceId);
    }

    public void clipAndElevate(View view, int elevation) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view.setElevation(elevation);
            ViewOutlineProvider viewOutlineProvider = new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setOval(0,0,view.getWidth(),view.getHeight());
                }
            };
            view.setOutlineProvider(viewOutlineProvider);
            view.setClipToOutline(true);
        }

    }

    //measure view positions that are dependent on first adding them to the view hierarchy.
    private void adjustLayoutSetConstants() {
        final int activity_vertical_margin = getDimPx(R.dimen.keyline_1);

        ViewTreeObserver vto = mTitleBox.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {


                float topY = mTitleBox.getY();

                //These are being set for reference
                mTitleBoxOriginalSize = mTitleBox.getHeight();
                mFlexibleSpaceSize = Math.round(topY);
                mCompactToolbarSize = mActionBarHeight + mSlidingTabLayout.getHeight();

                //translate the overflow menu to the correct position
                float boxMiddle = topY + (mTitleBoxOriginalSize / 2);
                RelativeLayout.LayoutParams overflowParams = (RelativeLayout.LayoutParams) mOverFlow.getLayoutParams();
                overflowParams.setMargins(0, Math.round(boxMiddle), activity_vertical_margin, 0);
                mOverFlow.setVisibility(View.VISIBLE);


                ViewTreeObserver rvto = mTitleBox.getViewTreeObserver();
                rvto.removeOnGlobalLayoutListener(this);
            }
        });

    }

    public void showFab(View fabView) {
        ViewPropertyAnimator.animate(fabView).cancel();
        ViewPropertyAnimator.animate(fabView).scaleX(1).scaleY(1).setDuration(200).start();
    }

    public void hideFab(View fabView) {
        ViewPropertyAnimator.animate(fabView).cancel();
        ViewPropertyAnimator.animate(fabView).scaleX(0).scaleY(0).setDuration(200).start();
    }







    private void setSlidingTabLayoutContentDescriptions() {
        for (int i = 0; i < mViewPagerFragmentTitles.length; i++) {
            mSlidingTabLayout.setContentDescription(i, getString(mViewPagerFragmentTitles[i]));
        }
    }

    private class FragmentViewPagerAdapter extends FragmentPagerAdapter {

        public FragmentViewPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public android.app.Fragment getItem(int i) {
            ViewPagerFragment moduleFragment = new ViewPagerFragment();
            Bundle args = new Bundle();
            args.putString(MainActivity.KEY_VIEW_PAGER_FRAG_TYPE_STRING, getString(mViewPagerFragmentTitles[i]));
            moduleFragment.setArguments(args);
            return moduleFragment;

        }

        @Override
        public int getCount() {
            return mViewPagerFragmentTitles.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return getString(mViewPagerFragmentTitles[position]);
        }

    }



}
