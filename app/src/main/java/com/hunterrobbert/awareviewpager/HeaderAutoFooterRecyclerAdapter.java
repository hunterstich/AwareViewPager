/*
 * Copyright 2014 Soichiro Kashima
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hunterrobbert.awareviewpager;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Adapter is capable of adding a blank header and footer.
 * Header size is determined by size given to the adapter.
 * Footer size is dynamic and designed to fill the parent view so the attached RecyclerView can be scrolled to the top of the screen.
 * On VIEW_TYPE_FOOTER created, VIEW_TYPE_ITEM is measured and multiplied by number.  If total list size, excluding the header, is less
 * than the screen size, the created footer is set to the remaining size.
 */


public class HeaderAutoFooterRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final static String TAG = HeaderAutoFooterRecyclerAdapter.class.getSimpleName();

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ITEM = 1;
    private static final int VIEW_TYPE_FOOTER = 2;


    private Context mContext;
    private LayoutInflater mInflater;

    private int mItemLayout;
    private View mHeaderView;
    private View mFooterView;

    private HashMap<Integer,Integer> mAllItemHeightTotalMap; //used to store the height of all the views

    private ArrayList<String> mItemList;


    public HeaderAutoFooterRecyclerAdapter(Context context, ArrayList<String> itemList, int itemLayout, int headerHeight) {
        mContext = context;
        mInflater = LayoutInflater.from(context);

        mHeaderView = createPlaceHolder(headerHeight);
        mFooterView = createPlaceHolder(0);

        mItemLayout = itemLayout;
        mItemList = itemList;

        mAllItemHeightTotalMap = new HashMap<>();
    }

    protected View createPlaceHolder(int placeHolderHeight) {
        View paceHolderView = new View(mContext);
        paceHolderView.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, placeHolderHeight));
        paceHolderView.setMinimumHeight(placeHolderHeight);
        paceHolderView.setClickable(true);
        return paceHolderView;
    }

    public int getHeaderFooterOffset() {
        if (mHeaderView == null) {
            return 0;
        }
        return 1;
    }

    @Override
    public int getItemCount() {
        if (mItemList != null) {
            if (mHeaderView == null) {
                return mItemList.size();
            } else {
                return mItemList.size() + 2;
            }
        } else {
            if (mHeaderView == null) {
                return 1;
            } else {
                return 2;
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (mItemList != null) {
            if (position == 0) {
                return VIEW_TYPE_HEADER;
            } else if (position == mItemList.size() + 1) {
                return VIEW_TYPE_FOOTER;
            } else {
                return VIEW_TYPE_ITEM;
            }
        } else {
            if (position == 0) {
                return VIEW_TYPE_HEADER;
            } else if (position == 1) {
                return VIEW_TYPE_FOOTER;
            } else {
                return VIEW_TYPE_ITEM;
            }
        }

    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            return new PlaceHolderViewHolder(mHeaderView);
        } else if (viewType == VIEW_TYPE_FOOTER) {


            DisplayMetrics display = Resources.getSystem().getDisplayMetrics();
            int height = display.heightPixels;

            int totalListHeight = getTotalHeightOfListViewItems();

            int newFooterHeight = Math.max(height - totalListHeight,0);

            mFooterView.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, newFooterHeight));
            mFooterView.setMinimumHeight(newFooterHeight);
            mFooterView.setBackgroundColor(mContext.getResources().getColor(android.R.color.white));

            return new PlaceHolderViewHolder(mFooterView);
        }else {
            return new ItemViewHolder(mInflater.inflate(mItemLayout,parent,false));
        }
    }



    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
        int position = i - getHeaderFooterOffset();

       if (viewHolder instanceof ItemViewHolder) {
           ItemViewHolder itemViewHolder = ((ItemViewHolder)viewHolder);
           itemViewHolder.mTextView.setText(mItemList.get(position));
           storeHeightOfView(itemViewHolder.mView,position);

       }
    }



    static class PlaceHolderViewHolder extends RecyclerView.ViewHolder {
        public PlaceHolderViewHolder(View view) {
            super(view);
        }
    }


    static class ItemViewHolder extends RecyclerView.ViewHolder {

      TextView mTextView;
      View mView;

        public ItemViewHolder(View v) {
            super(v);
            mView = v;
            mTextView = (TextView) v.findViewById(R.id.item_text_view);
        }

    }




    //if different fragments pass the adapter different layouts or the text changes the size of a view or whatnot,
    //the total size of all the views cannot be easily calculated.  instead, as views are added, measure them.
    //ideally, when it's time to draw the footer, all the views will be added and the appropriate size of the footer necessary
    //will be available.
    private void storeHeightOfView(final View view, final int position) {
        ViewTreeObserver vto = view.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

                if (mAllItemHeightTotalMap.get(position) != null) {
                    //view already added. update it
                    mAllItemHeightTotalMap.remove(position);
                    mAllItemHeightTotalMap.put(position,view.getHeight());
                } else {
                    mAllItemHeightTotalMap.put(position,view.getHeight());
                }

                view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    private int getTotalHeightOfListViewItems() {
        int total = 0;
        Iterator iterator = mAllItemHeightTotalMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry pair = (Map.Entry) iterator.next();
            total = total + (Integer) pair.getValue();
        }

        return total;
    }






}
