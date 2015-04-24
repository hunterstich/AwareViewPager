package com.hunterrobbert.awareviewpager;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;


public class ViewPagerFragment extends ViewPagerFragmentBase {

    private final static String TAG = ViewPagerFragment.class.getSimpleName();

    protected View mRoot;
    protected Bundle mBundle;
    protected ObservableRecyclerView mRecyclerView;
    private String mModuleTypeString;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRoot = inflater.inflate(R.layout.viewpager_fragment, container, false);
        mBundle = getArguments();
        mModuleTypeString = mBundle.getString(MainActivity.KEY_VIEW_PAGER_FRAG_TYPE_STRING);

        return mRoot;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mRecyclerView = (ObservableRecyclerView) mRoot.findViewById(R.id.recyclerView);
        setupRecyclerView(mRecyclerView);


        updateModuleRecyclerData(getRandomizedData());

    }

    private ArrayList<String> getRandomizedData() {
        ArrayList<String> arrayList = new ArrayList<>();

        int random = (int )(Math.random() * 40 + 5);
        for (int i = 0; i < random; i++) {
            arrayList.add(mModuleTypeString + " number " + i);
        }

        return arrayList;
    }


    public void updateModuleRecyclerData(ArrayList<String> arrayList) {
        if (!isAdded()) {
            return;
        }
        mRecyclerView.setAdapter(new HeaderAutoFooterRecyclerAdapter(getActivity(),arrayList, R.layout.empty_list_item,getHeaderHeight()));
        initiateScrollPosition();

    }

}