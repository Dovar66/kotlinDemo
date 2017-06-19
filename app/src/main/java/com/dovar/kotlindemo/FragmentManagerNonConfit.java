package com.dovar.kotlindemo;

import java.util.List;

/**
 * Created by heweizong on 2017/5/19.
 */

public class FragmentManagerNonConfit {
    private final List<MyFragment> mFragments;
    private final List<FragmentManagerNonConfit> mChildNonConfigs;

    FragmentManagerNonConfit(List<MyFragment> fragments,
                             List<FragmentManagerNonConfit> childNonConfigs) {
        mFragments = fragments;
        mChildNonConfigs = childNonConfigs;
    }

    /**
     * @return the retained instance fragments returned by a FragmentManager
     */
    List<MyFragment> getFragments() {
        return mFragments;
    }

    /**
     * @return the FragmentManagerNonConfigs from any applicable fragment's child FragmentManager
     */
    List<FragmentManagerNonConfit> getChildNonConfigs() {
        return mChildNonConfigs;
    }
}
