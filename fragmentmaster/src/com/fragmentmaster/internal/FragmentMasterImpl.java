package com.fragmentmaster.internal;

import com.fragmentmaster.R;
import com.fragmentmaster.app.FragmentMaster;
import com.fragmentmaster.app.IMasterFragment;
import com.fragmentmaster.app.MasterActivity;
import com.fragmentmaster.app.Request;

import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.View;
import android.view.ViewGroup;

public class FragmentMasterImpl extends FragmentMaster {

    // The id of fragments' real container.
    public final static int FRAGMENT_CONTAINER_ID = R.id.fragment_container;

    private FragmentsAdapter mAdapter;

    private FragmentMasterPager mViewPager;

    private boolean mScrolling = false;

    private int mState = ViewPager.SCROLL_STATE_IDLE;

    private OnPageChangeListener mOnPageChangeListener = new OnPageChangeListener() {

        @Override
        public void onPageSelected(int position) {
        }

        @Override
        public void onPageScrolled(int position, float positionOffset,
                int positionOffsetPixels) {
            if (mState == ViewPager.SCROLL_STATE_IDLE) {
                setUpAnimator(getPrimaryFragment());
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            mState = state;
            if (state == ViewPager.SCROLL_STATE_IDLE) {
                mViewPager.post(new Runnable() {
                    @Override
                    public void run() {
                        setUpAnimator(getPrimaryFragment());
                    }
                });
                onScrollIdle();
            }
        }

    };

    private Runnable mCleanUpRunnable = new Runnable() {
        @Override
        public void run() {
            cleanUp();
        }
    };

    public FragmentMasterImpl(MasterActivity activity) {
        super(activity);
    }

    @Override
    protected void performInstall(ViewGroup container) {
        mAdapter = new FragmentsAdapter();
        mViewPager = new FragmentMasterPager(this);
        mViewPager.setId(FRAGMENT_CONTAINER_ID);
        mViewPager.setOffscreenPageLimit(Integer.MAX_VALUE);
        mViewPager.setAdapter(mAdapter);
        mViewPager.setOnPageChangeListener(mOnPageChangeListener);

        container.addView(mViewPager);
    }

    @Override
    protected int getFragmentContainerId() {
        return FRAGMENT_CONTAINER_ID;
    }

    @Override
    protected void onFragmentStarted(IMasterFragment fragment) {
        mAdapter.notifyDataSetChanged();
        int nextItem = mAdapter.getCount() - 1;
        // Perform "smooth scroll" if the page has a PageAnimator and more than
        // one item.
        boolean smoothScroll = hasPageAnimator() && nextItem > 0;
        mViewPager.setCurrentItem(nextItem, smoothScroll);
        if (smoothScroll) {
            mScrolling = true;
        }
    }

    @Override
    protected void onFinishFragment(final IMasterFragment fragment,
            final int resultCode, final Request data) {
        final int index = getFragments().indexOf(fragment);
        int curItem = mViewPager.getCurrentItem();

        if (hasPageAnimator() && curItem == index && index != 0) {
            // If there's a PageAnimator, and the fragment to finish is the
            // primary fragment, scroll back smoothly.
            // When scrolling is stopped, real finish will be done by
            // cleanUp method.
            mViewPager.setCurrentItem(index - 1, true);
            mScrolling = true;
        }
        if (mScrolling) {
            // If pager is scrolling, do real finish when cleanUp.
            deliverFragmentResult(fragment, resultCode, data);
            return;
        }
        super.onFinishFragment(fragment, resultCode, data);
    }

    @Override
    protected void onFragmentFinished(IMasterFragment fragment) {
        mAdapter.notifyDataSetChanged();
    }

    private void onScrollIdle() {
        mScrolling = false;
        // When scrolling stopped, do cleanup.
        mViewPager.removeCallbacks(mCleanUpRunnable);
        mViewPager.post(mCleanUpRunnable);
    }

    boolean isScrolling() {
        return mScrolling;
    }

    private void cleanUp() {
        // check whether there are any fragments above the primary one, and
        // finish them.
        IMasterFragment[] fragments = getFragments().toArray(
                new IMasterFragment[getFragments().size()]);
        IMasterFragment primaryFragment = getPrimaryFragment();
        IMasterFragment f = null;
        // determine whether f is above primary fragment.
        boolean abovePrimary = true;
        for (int i = fragments.length - 1; i >= 0; i--) {
            f = fragments[i];

            if (f == primaryFragment) {
                abovePrimary = false;
            }

            if (abovePrimary) {
                // All fragments above primary fragment should be finished.
                if (isInFragmentMaster(f)) {
                    if (isFinishPending(f)) {
                        doFinishFragment(f);
                    } else {
                        f.finish();
                    }
                }
            } else {
                if (isFinishPending(f) && !mScrolling) {
                    doFinishFragment(f);
                }
            }
        }
    }

    private class FragmentsAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return getFragments().size();
        }

        @Override
        public int getItemPosition(Object object) {
            int position = getFragments().indexOf(object);
            return position == -1 ? POSITION_NONE : position;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            IMasterFragment fragment = getFragments().get(position);
            return fragment;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position,
                Object object) {
            setPrimaryFragment((IMasterFragment) object);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return ((IMasterFragment) object).getView() == view;
        }
    }

}
