package com.dovar.kotlindemo;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.CallSuper;
import android.support.annotation.StringRes;
import android.support.v4.app.FragmentContainer;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.os.BuildCompat;
import android.support.v4.util.ArraySet;
import android.support.v4.util.DebugUtils;
import android.support.v4.util.LogWriter;
import android.support.v4.util.Pair;
import android.support.v4.view.LayoutInflaterFactory;
import android.support.v4.view.ViewCompat;
import android.util.AndroidRuntimeException;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.ScaleAnimation;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * Created by heweizong on 2017/5/19.
 * Container for fragments associated with an activity.
 */
public class MyFragmentManagerImp implements LayoutInflaterFactory{

    /**
     * Flag for {@link #popBackStack(String, int)}
     * and {@link #popBackStack(int, int)}: If set, and the name or ID of
     * a back stack entry has been supplied, then all matching entries will
     * be consumed until one that doesn't match is found or the bottom of
     * the stack is reached.  Otherwise, all entries up to but not including that entry
     * will be removed.
     */
    public static final int POP_BACK_STACK_INCLUSIVE = 1<<0;

    //    final class FragmentManagerImpl extends FragmentManager implements LayoutInflaterFactory {
    static boolean DEBUG = false;
    static final String TAG = "FragmentManager";

    static final boolean HONEYCOMB = android.os.Build.VERSION.SDK_INT >= 11;

    static final String TARGET_REQUEST_CODE_STATE_TAG = "android:target_req_state";
    static final String TARGET_STATE_TAG = "android:target_state";
    static final String VIEW_STATE_TAG = "android:view_state";
    static final String USER_VISIBLE_HINT_TAG = "android:user_visible_hint";

    static class AnimateOnHWLayerIfNeededListener implements Animation.AnimationListener {
        private Animation.AnimationListener mOriginalListener;
        private boolean mShouldRunOnHWLayer;
        View mView;

        public AnimateOnHWLayerIfNeededListener(final View v, Animation anim) {
            if (v == null || anim == null) {
                return;
            }
            mView = v;
        }

        public AnimateOnHWLayerIfNeededListener(final View v, Animation anim,
                                                Animation.AnimationListener listener) {
            if (v == null || anim == null) {
                return;
            }
            mOriginalListener = listener;
            mView = v;
            mShouldRunOnHWLayer = true;
        }

        @Override
        @CallSuper
        public void onAnimationStart(Animation animation) {
            if (mOriginalListener != null) {
                mOriginalListener.onAnimationStart(animation);
            }
        }

        @Override
        @CallSuper
        public void onAnimationEnd(Animation animation) {
            if (mView != null && mShouldRunOnHWLayer) {
                // If we're attached to a window, assume we're in the normal performTraversals
                // drawing path for Animations running. It's not safe to change the layer type
                // during drawing, so post it to the View to run later. If we're not attached
                // or we're running on N and above, post it to the view. If we're not on N and
                // not attached, do it right now since existing platform versions don't run the
                // hwui renderer for detached views off the UI thread making changing layer type
                // safe, but posting may not be.
                // Prior to N posting to a detached view from a non-Looper thread could cause
                // leaks, since the thread-local run queue on a non-Looper thread would never
                // be flushed.
                if (ViewCompat.isAttachedToWindow(mView) || BuildCompat.isAtLeastN()) {
                    mView.post(new Runnable() {
                        @Override
                        public void run() {
                            ViewCompat.setLayerType(mView, ViewCompat.LAYER_TYPE_NONE, null);
                        }
                    });
                } else {
                    ViewCompat.setLayerType(mView, ViewCompat.LAYER_TYPE_NONE, null);
                }
            }
            if (mOriginalListener != null) {
                mOriginalListener.onAnimationEnd(animation);
            }
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
            if (mOriginalListener != null) {
                mOriginalListener.onAnimationRepeat(animation);
            }
        }
    }

    /**
     * Callback interface for listening to fragment state changes that happen
     * within a given FragmentManager.
     */
    public abstract static class FragmentLifecycleCallbacks {
        /**
         * Called right before the fragment's {@link MyFragment#onAttach(Context)} method is called.
         * This is a good time to inject any required dependencies for the fragment before any of
         * the fragment's lifecycle methods are invoked.
         *
         * @param fm      Host FragmentManager
         * @param f       MyFragment changing state
         * @param context Context that the MyFragment is being attached to
         */
        public void onFragmentPreAttached(MyFragmentManagerImp fm, MyFragment f, Context context) {
        }

        /**
         * Called after the fragment has been attached to its host. Its host will have had
         * <code>onAttachFragment</code> called before this call happens.
         *
         * @param fm      Host FragmentManager
         * @param f       MyFragment changing state
         * @param context Context that the MyFragment was attached to
         */
        public void onFragmentAttached(MyFragmentManagerImp fm, MyFragment f, Context context) {
        }

        /**
         * Called after the fragment has returned from the FragmentManager's call to
         * {@link MyFragment#onCreate(Bundle)}. This will only happen once for any given
         * fragment instance, though the fragment may be attached and detached multiple times.
         *
         * @param fm                 Host FragmentManager
         * @param f                  MyFragment changing state
         * @param savedInstanceState Saved instance bundle from a previous instance
         */
        public void onFragmentCreated(MyFragmentManagerImp fm, MyFragment f, Bundle savedInstanceState) {
        }

        /**
         * Called after the fragment has returned from the FragmentManager's call to
         * {@link MyFragment#onActivityCreated(Bundle)}. This will only happen once for any given
         * fragment instance, though the fragment may be attached and detached multiple times.
         *
         * @param fm                 Host FragmentManager
         * @param f                  MyFragment changing state
         * @param savedInstanceState Saved instance bundle from a previous instance
         */
        public void onFragmentActivityCreated(MyFragmentManagerImp fm, MyFragment f,
                                              Bundle savedInstanceState) {
        }

        /**
         * Called after the fragment has returned a non-null view from the FragmentManager's
         * request to {@link MyFragment#onCreateView(LayoutInflater, ViewGroup, Bundle)}.
         *
         * @param fm                 Host FragmentManager
         * @param f                  MyFragment that created and owns the view
         * @param v                  View returned by the fragment
         * @param savedInstanceState Saved instance bundle from a previous instance
         */
        public void onFragmentViewCreated(MyFragmentManagerImp fm, MyFragment f, View v,
                                          Bundle savedInstanceState) {
        }

        /**
         * Called after the fragment has returned from the FragmentManager's call to
         * {@link MyFragment#onStart()}.
         *
         * @param fm Host FragmentManager
         * @param f  MyFragment changing state
         */
        public void onFragmentStarted(MyFragmentManagerImp fm, MyFragment f) {
        }

        /**
         * Called after the fragment has returned from the FragmentManager's call to
         * {@link MyFragment#onResume()}.
         *
         * @param fm Host FragmentManager
         * @param f  MyFragment changing state
         */
        public void onFragmentResumed(MyFragmentManagerImp fm, MyFragment f) {
        }

        /**
         * Called after the fragment has returned from the FragmentManager's call to
         * {@link MyFragment#onPause()}.
         *
         * @param fm Host FragmentManager
         * @param f  MyFragment changing state
         */
        public void onFragmentPaused(MyFragmentManagerImp fm, MyFragment f) {
        }

        /**
         * Called after the fragment has returned from the FragmentManager's call to
         * {@link MyFragment#onStop()}.
         *
         * @param fm Host FragmentManager
         * @param f  MyFragment changing state
         */
        public void onFragmentStopped(MyFragmentManagerImp fm, MyFragment f) {
        }

        /**
         * Called after the fragment has returned from the FragmentManager's call to
         * {@link MyFragment#onSaveInstanceState(Bundle)}.
         *
         * @param fm       Host FragmentManager
         * @param f        MyFragment changing state
         * @param outState Saved state bundle for the fragment
         */
        public void onFragmentSaveInstanceState(MyFragmentManagerImp fm, MyFragment f, Bundle outState) {
        }

        /**
         * Called after the fragment has returned from the FragmentManager's call to
         * {@link MyFragment#onDestroyView()}.
         *
         * @param fm Host FragmentManager
         * @param f  MyFragment changing state
         */
        public void onFragmentViewDestroyed(MyFragmentManagerImp fm, MyFragment f) {
        }

        /**
         * Called after the fragment has returned from the FragmentManager's call to
         * {@link MyFragment#onDestroy()}.
         *
         * @param fm Host FragmentManager
         * @param f  MyFragment changing state
         */
        public void onFragmentDestroyed(MyFragmentManagerImp fm, MyFragment f) {
        }

        /**
         * Called after the fragment has returned from the FragmentManager's call to
         * {@link MyFragment#onDetach()}.
         *
         * @param fm Host FragmentManager
         * @param f  MyFragment changing state
         */
        public void onFragmentDetached(MyFragmentManagerImp fm, MyFragment f) {
        }
    }

    public interface BackStackEntry {
        /**
         * Return the unique identifier for the entry.  This is the only
         * representation of the entry that will persist across activity
         * instances.
         */
        public int getId();

        /**
         * Get the name that was supplied to
         * {@link FragmentTransaction#addToBackStack(String)
         * FragmentTransaction.addToBackStack(String)} when creating this entry.
         */
        public String getName();

        /**
         * Return the full bread crumb title resource identifier for the entry,
         * or 0 if it does not have one.
         */
        @StringRes
        public int getBreadCrumbTitleRes();

        /**
         * Return the short bread crumb title resource identifier for the entry,
         * or 0 if it does not have one.
         */
        @StringRes
        public int getBreadCrumbShortTitleRes();

        /**
         * Return the full bread crumb title for the entry, or null if it
         * does not have one.
         */
        public CharSequence getBreadCrumbTitle();

        /**
         * Return the short bread crumb title for the entry, or null if it
         * does not have one.
         */
        public CharSequence getBreadCrumbShortTitle();
    }

    /**
     * Interface to watch for changes to the back stack.
     */
    public interface OnBackStackChangedListener {
        /**
         * Called whenever the contents of the back stack change.
         */
        public void onBackStackChanged();
    }

    ArrayList<OpGenerator> mPendingActions;
    Runnable[] mTmpActions;
    boolean mExecutingActions;

    ArrayList<MyFragment> mActive;
    ArrayList<MyFragment> mAdded;
    ArrayList<Integer> mAvailIndices;
    ArrayList<MyBackStackRecord> mBackStack;
    ArrayList<MyFragment> mCreatedMenus;

    // Must be accessed while locked.
    ArrayList<MyBackStackRecord> mBackStackIndices;
    ArrayList<Integer> mAvailBackStackIndices;

    ArrayList<OnBackStackChangedListener> mBackStackChangeListeners;
    private CopyOnWriteArrayList<Pair<FragmentLifecycleCallbacks, Boolean>> mLifecycleCallbacks;

    int mCurState = MyFragment.INITIALIZING;
    MyFragmentHostCallBack mHost;
    FragmentContainer mContainer;
    MyFragment mParent;

    static Field sAnimationListenerField = null;

    boolean mNeedMenuInvalidate;
    boolean mStateSaved;
    boolean mDestroyed;
    String mNoTransactionsBecause;
    boolean mHavePendingDeferredStart;

    // Temporary vars for optimizing execution of BackStackRecords:
    ArrayList<MyBackStackRecord> mTmpRecords;
    ArrayList<Boolean> mTmpIsPop;
    ArrayList<MyFragment> mTmpAddedFragments;

    // Temporary vars for state save and restore.
    Bundle mStateBundle = null;
    SparseArray<Parcelable> mStateArray = null;

    // Postponed transactions.
    ArrayList<StartEnterTransitionListener> mPostponedTransactions;

    Runnable mExecCommit = new Runnable() {
        @Override
        public void run() {
            execPendingActions();
        }
    };

    static boolean modifiesAlpha(Animation anim) {
        if (anim instanceof AlphaAnimation) {
            return true;
        } else if (anim instanceof AnimationSet) {
            List<Animation> anims = ((AnimationSet) anim).getAnimations();
            for (int i = 0; i < anims.size(); i++) {
                if (anims.get(i) instanceof AlphaAnimation) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean shouldRunOnHWLayer(View v, Animation anim) {
        return Build.VERSION.SDK_INT >= 19
                && ViewCompat.getLayerType(v) == ViewCompat.LAYER_TYPE_NONE
                && ViewCompat.hasOverlappingRendering(v)
                && modifiesAlpha(anim);
    }

    private void throwException(RuntimeException ex) {
        Log.e(TAG, ex.getMessage());
        Log.e(TAG, "Activity state:");
        LogWriter logw = new LogWriter(TAG);
        PrintWriter pw = new PrintWriter(logw);
        if (mHost != null) {
            try {
                mHost.onDump("  ", null, pw, new String[]{});
            } catch (Exception e) {
                Log.e(TAG, "Failed dumping state", e);
            }
        } else {
            try {
                dump("  ", null, pw, new String[]{});
            } catch (Exception e) {
                Log.e(TAG, "Failed dumping state", e);
            }
        }
        throw ex;
    }

    public MyFragmentTransaction beginTransaction() {
        return new MyBackStackRecord(this);
    }

    public boolean executePendingTransactions() {
        boolean updates = execPendingActions();
        forcePostponedTransactions();
        return updates;
    }

    public void popBackStack() {
        enqueueAction(new PopBackStackState(null, -1, 0), false);
    }

    public boolean popBackStackImmediate() {
        checkStateLoss();
        return popBackStackImmediate(null, -1, 0);
    }

    public void popBackStack(final String name, final int flags) {
        enqueueAction(new PopBackStackState(name, -1, flags), false);
    }

    public boolean popBackStackImmediate(String name, int flags) {
        checkStateLoss();
        return popBackStackImmediate(name, -1, flags);
    }

    public void popBackStack(final int id, final int flags) {
        if (id < 0) {
            throw new IllegalArgumentException("Bad id: " + id);
        }
        enqueueAction(new PopBackStackState(null, id, flags), false);
    }

    public boolean popBackStackImmediate(int id, int flags) {
        checkStateLoss();
        execPendingActions();
        if (id < 0) {
            throw new IllegalArgumentException("Bad id: " + id);
        }
        return popBackStackImmediate(null, id, flags);
    }

    /**
     * Used by all public popBackStackImmediate methods, this executes pending transactions and
     * returns true if the pop action did anything, regardless of what other pending
     * transactions did.
     *
     * @return true if the pop operation did anything or false otherwise.
     */
    private boolean popBackStackImmediate(String name, int id, int flags) {
        execPendingActions();
        ensureExecReady(true);

        boolean executePop = popBackStackState(mTmpRecords, mTmpIsPop, name, id, flags);
        if (executePop) {
            mExecutingActions = true;
            try {
                optimizeAndExecuteOps(mTmpRecords, mTmpIsPop);
            } finally {
                cleanupExec();
            }
        }

        doPendingDeferredStart();
        return executePop;
    }

    public int getBackStackEntryCount() {
        return mBackStack != null ? mBackStack.size() : 0;
    }

    public BackStackEntry getBackStackEntryAt(int index) {
        return mBackStack.get(index);
    }

    public void addOnBackStackChangedListener(OnBackStackChangedListener listener) {
        if (mBackStackChangeListeners == null) {
            mBackStackChangeListeners = new ArrayList<OnBackStackChangedListener>();
        }
        mBackStackChangeListeners.add(listener);
    }

    public void removeOnBackStackChangedListener(OnBackStackChangedListener listener) {
        if (mBackStackChangeListeners != null) {
            mBackStackChangeListeners.remove(listener);
        }
    }

    public void putFragment(Bundle bundle, String key, MyFragment fragment) {
        if (fragment.mIndex < 0) {
            throwException(new IllegalStateException("MyFragment " + fragment
                    + " is not currently in the FragmentManager"));
        }
        bundle.putInt(key, fragment.mIndex);
    }

    public MyFragment getFragment(Bundle bundle, String key) {
        int index = bundle.getInt(key, -1);
        if (index == -1) {
            return null;
        }
        if (index >= mActive.size()) {
            throwException(new IllegalStateException("MyFragment no longer exists for key "
                    + key + ": index " + index));
        }
        MyFragment f = mActive.get(index);
        if (f == null) {
            throwException(new IllegalStateException("MyFragment no longer exists for key "
                    + key + ": index " + index));
        }
        return f;
    }

    public List<MyFragment> getFragments() {
        return mActive;
    }

    public MyFragment.SavedState saveFragmentInstanceState(MyFragment fragment) {
        if (fragment.mIndex < 0) {
            throwException(new IllegalStateException("MyFragment " + fragment
                    + " is not currently in the FragmentManager"));
        }
        if (fragment.mState > MyFragment.INITIALIZING) {
            Bundle result = saveFragmentBasicState(fragment);
            return result != null ? new MyFragment.SavedState(result) : null;
        }
        return null;
    }

    public boolean isDestroyed() {
        return mDestroyed;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("FragmentManager{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(" in ");
        if (mParent != null) {
            DebugUtils.buildShortClassTag(mParent, sb);
        } else {
            DebugUtils.buildShortClassTag(mHost, sb);
        }
        sb.append("}}");
        return sb.toString();
    }

    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        String innerPrefix = prefix + "    ";

        int N;
        if (mActive != null) {
            N = mActive.size();
            if (N > 0) {
                writer.print(prefix);
                writer.print("Active Fragments in ");
                writer.print(Integer.toHexString(System.identityHashCode(this)));
                writer.println(":");
                for (int i = 0; i < N; i++) {
                    MyFragment f = mActive.get(i);
                    writer.print(prefix);
                    writer.print("  #");
                    writer.print(i);
                    writer.print(": ");
                    writer.println(f);
                    if (f != null) {
                        f.dump(innerPrefix, fd, writer, args);
                    }
                }
            }
        }

        if (mAdded != null) {
            N = mAdded.size();
            if (N > 0) {
                writer.print(prefix);
                writer.println("Added Fragments:");
                for (int i = 0; i < N; i++) {
                    MyFragment f = mAdded.get(i);
                    writer.print(prefix);
                    writer.print("  #");
                    writer.print(i);
                    writer.print(": ");
                    writer.println(f.toString());
                }
            }
        }

        if (mCreatedMenus != null) {
            N = mCreatedMenus.size();
            if (N > 0) {
                writer.print(prefix);
                writer.println("Fragments Created Menus:");
                for (int i = 0; i < N; i++) {
                    MyFragment f = mCreatedMenus.get(i);
                    writer.print(prefix);
                    writer.print("  #");
                    writer.print(i);
                    writer.print(": ");
                    writer.println(f.toString());
                }
            }
        }

        if (mBackStack != null) {
            N = mBackStack.size();
            if (N > 0) {
                writer.print(prefix);
                writer.println("Back Stack:");
                for (int i = 0; i < N; i++) {
                    MyBackStackRecord bs = mBackStack.get(i);
                    writer.print(prefix);
                    writer.print("  #");
                    writer.print(i);
                    writer.print(": ");
                    writer.println(bs.toString());
                    bs.dump(innerPrefix, fd, writer, args);
                }
            }
        }

        synchronized (this) {
            if (mBackStackIndices != null) {
                N = mBackStackIndices.size();
                if (N > 0) {
                    writer.print(prefix);
                    writer.println("Back Stack Indices:");
                    for (int i = 0; i < N; i++) {
                        MyBackStackRecord bs = mBackStackIndices.get(i);
                        writer.print(prefix);
                        writer.print("  #");
                        writer.print(i);
                        writer.print(": ");
                        writer.println(bs);
                    }
                }
            }

            if (mAvailBackStackIndices != null && mAvailBackStackIndices.size() > 0) {
                writer.print(prefix);
                writer.print("mAvailBackStackIndices: ");
                writer.println(Arrays.toString(mAvailBackStackIndices.toArray()));
            }
        }

        if (mPendingActions != null) {
            N = mPendingActions.size();
            if (N > 0) {
                writer.print(prefix);
                writer.println("Pending Actions:");
                for (int i = 0; i < N; i++) {
                    OpGenerator r = mPendingActions.get(i);
                    writer.print(prefix);
                    writer.print("  #");
                    writer.print(i);
                    writer.print(": ");
                    writer.println(r);
                }
            }
        }

        writer.print(prefix);
        writer.println("FragmentManager misc state:");
        writer.print(prefix);
        writer.print("  mHost=");
        writer.println(mHost);
        writer.print(prefix);
        writer.print("  mContainer=");
        writer.println(mContainer);
        if (mParent != null) {
            writer.print(prefix);
            writer.print("  mParent=");
            writer.println(mParent);
        }
        writer.print(prefix);
        writer.print("  mCurState=");
        writer.print(mCurState);
        writer.print(" mStateSaved=");
        writer.print(mStateSaved);
        writer.print(" mDestroyed=");
        writer.println(mDestroyed);
        if (mNeedMenuInvalidate) {
            writer.print(prefix);
            writer.print("  mNeedMenuInvalidate=");
            writer.println(mNeedMenuInvalidate);
        }
        if (mNoTransactionsBecause != null) {
            writer.print(prefix);
            writer.print("  mNoTransactionsBecause=");
            writer.println(mNoTransactionsBecause);
        }
        if (mAvailIndices != null && mAvailIndices.size() > 0) {
            writer.print(prefix);
            writer.print("  mAvailIndices: ");
            writer.println(Arrays.toString(mAvailIndices.toArray()));
        }
    }

    static final Interpolator DECELERATE_QUINT = new DecelerateInterpolator(2.5f);
    static final Interpolator DECELERATE_CUBIC = new DecelerateInterpolator(1.5f);
    static final Interpolator ACCELERATE_QUINT = new AccelerateInterpolator(2.5f);
    static final Interpolator ACCELERATE_CUBIC = new AccelerateInterpolator(1.5f);

    static final int ANIM_DUR = 220;

    static Animation makeOpenCloseAnimation(Context context, float startScale,
                                            float endScale, float startAlpha, float endAlpha) {
        AnimationSet set = new AnimationSet(false);
        ScaleAnimation scale = new ScaleAnimation(startScale, endScale, startScale, endScale,
                Animation.RELATIVE_TO_SELF, .5f, Animation.RELATIVE_TO_SELF, .5f);
        scale.setInterpolator(DECELERATE_QUINT);
        scale.setDuration(ANIM_DUR);
        set.addAnimation(scale);
        AlphaAnimation alpha = new AlphaAnimation(startAlpha, endAlpha);
        alpha.setInterpolator(DECELERATE_CUBIC);
        alpha.setDuration(ANIM_DUR);
        set.addAnimation(alpha);
        return set;
    }

    static Animation makeFadeAnimation(Context context, float start, float end) {
        AlphaAnimation anim = new AlphaAnimation(start, end);
        anim.setInterpolator(DECELERATE_CUBIC);
        anim.setDuration(ANIM_DUR);
        return anim;
    }

    Animation loadAnimation(MyFragment fragment, int transit, boolean enter,
                            int transitionStyle) {
        Animation animObj = fragment.onCreateAnimation(transit, enter, fragment.getNextAnim());
        if (animObj != null) {
            return animObj;
        }

        if (fragment.getNextAnim() != 0) {
            Animation anim = AnimationUtils.loadAnimation(mHost.getContext(),
                    fragment.getNextAnim());
            if (anim != null) {
                return anim;
            }
        }

        if (transit == 0) {
            return null;
        }

        int styleIndex = transitToStyleIndex(transit, enter);
        if (styleIndex < 0) {
            return null;
        }

        switch (styleIndex) {
            case ANIM_STYLE_OPEN_ENTER:
                return makeOpenCloseAnimation(mHost.getContext(), 1.125f, 1.0f, 0, 1);
            case ANIM_STYLE_OPEN_EXIT:
                return makeOpenCloseAnimation(mHost.getContext(), 1.0f, .975f, 1, 0);
            case ANIM_STYLE_CLOSE_ENTER:
                return makeOpenCloseAnimation(mHost.getContext(), .975f, 1.0f, 0, 1);
            case ANIM_STYLE_CLOSE_EXIT:
                return makeOpenCloseAnimation(mHost.getContext(), 1.0f, 1.075f, 1, 0);
            case ANIM_STYLE_FADE_ENTER:
                return makeFadeAnimation(mHost.getContext(), 0, 1);
            case ANIM_STYLE_FADE_EXIT:
                return makeFadeAnimation(mHost.getContext(), 1, 0);
        }

        if (transitionStyle == 0 && mHost.onHasWindowAnimations()) {
            transitionStyle = mHost.onGetWindowAnimations();
        }
        if (transitionStyle == 0) {
            return null;
        }

        //TypedArray attrs = mActivity.obtainStyledAttributes(transitionStyle,
        //        com.android.internal.R.styleable.FragmentAnimation);
        //int anim = attrs.getResourceId(styleIndex, 0);
        //attrs.recycle();

        //if (anim == 0) {
        //    return null;
        //}

        //return AnimatorInflater.loadAnimator(mActivity, anim);
        return null;
    }

    public void performPendingDeferredStart(MyFragment f) {
        if (f.mDeferStart) {
            if (mExecutingActions) {
                // Wait until we're done executing our pending transactions
                mHavePendingDeferredStart = true;
                return;
            }
            f.mDeferStart = false;
            moveToState(f, mCurState, 0, 0, false);
        }
    }

    /**
     * Sets the to be animated view on hardware layer during the animation. Note
     * that calling this will replace any existing animation listener on the animation
     * with a new one, as animations do not support more than one listeners. Therefore,
     * animations that already have listeners should do the layer change operations
     * in their existing listeners, rather than calling this function.
     */
    private void setHWLayerAnimListenerIfAlpha(final View v, Animation anim) {
        if (v == null || anim == null) {
            return;
        }
        if (shouldRunOnHWLayer(v, anim)) {
            Animation.AnimationListener originalListener = null;
            try {
                if (sAnimationListenerField == null) {
                    sAnimationListenerField = Animation.class.getDeclaredField("mListener");
                    sAnimationListenerField.setAccessible(true);
                }
                originalListener = (Animation.AnimationListener) sAnimationListenerField.get(anim);
            } catch (NoSuchFieldException e) {
                Log.e(TAG, "No field with the name mListener is found in Animation class", e);
            } catch (IllegalAccessException e) {
                Log.e(TAG, "Cannot access Animation's mListener field", e);
            }
            // If there's already a listener set on the animation, we need wrap the new listener
            // around the existing listener, so that they will both get animation listener
            // callbacks.
            ViewCompat.setLayerType(v, ViewCompat.LAYER_TYPE_HARDWARE, null);
            anim.setAnimationListener(new AnimateOnHWLayerIfNeededListener(v, anim,
                    originalListener));
        }
    }

    boolean isStateAtLeast(int state) {
        return mCurState >= state;
    }

    void moveToState(MyFragment f, int newState, int transit, int transitionStyle,
                     boolean keepActive) {
        // Fragments that are not currently added will sit in the onCreate() state.
        if ((!f.mAdded || f.mDetached) && newState > MyFragment.CREATED) {
            newState = MyFragment.CREATED;
        }
        if (f.mRemoving && newState > f.mState) {
            // While removing a fragment, we can't change it to a higher state.
            newState = f.mState;
        }
        // Defer start if requested; don't allow it to move to STARTED or higher
        // if it's not already started.
        if (f.mDeferStart && f.mState < MyFragment.STARTED && newState > MyFragment.STOPPED) {
            newState = MyFragment.STOPPED;
        }
        if (f.mState < newState) {
            // For fragments that are created from a layout, when restoring from
            // state we don't want to allow them to be created until they are
            // being reloaded from the layout.
            if (f.mFromLayout && !f.mInLayout) {
                return;
            }
            if (f.getAnimatingAway() != null) {
                // The fragment is currently being animated...  but!  Now we
                // want to move our state back up.  Give up on waiting for the
                // animation, move to whatever the final state should be once
                // the animation is done, and then we can proceed from there.
                f.setAnimatingAway(null);
                moveToState(f, f.getStateAfterAnimating(), 0, 0, true);
            }
            switch (f.mState) {
                case MyFragment.INITIALIZING:
                    if (DEBUG) Log.v(TAG, "moveto CREATED: " + f);
                    if (f.mSavedFragmentState != null) {
                        f.mSavedFragmentState.setClassLoader(mHost.getContext().getClassLoader());
                        f.mSavedViewState = f.mSavedFragmentState.getSparseParcelableArray(
                                VIEW_STATE_TAG);
                        f.mTarget = getFragment(f.mSavedFragmentState,
                                TARGET_STATE_TAG);
                        if (f.mTarget != null) {
                            f.mTargetRequestCode = f.mSavedFragmentState.getInt(
                                    TARGET_REQUEST_CODE_STATE_TAG, 0);
                        }
                        f.mUserVisibleHint = f.mSavedFragmentState.getBoolean(
                                USER_VISIBLE_HINT_TAG, true);
                        if (!f.mUserVisibleHint) {
                            f.mDeferStart = true;
                            if (newState > MyFragment.STOPPED) {
                                newState = MyFragment.STOPPED;
                            }
                        }
                    }
                    f.mHost = mHost;
                    f.mParentFragment = mParent;
                    f.mFragmentManager = mParent != null
                            ? mParent.mChildFragmentManager : mHost.getFragmentManagerImp();
                    dispatchOnFragmentPreAttached(f, mHost.getContext(), false);
                    f.mCalled = false;
                    f.onAttach(mHost.getContext());
                    if (!f.mCalled) {
                        throw new AndroidRuntimeException("MyFragment " + f
                                + " did not call through to super.onAttach()");
                    }
                    if (f.mParentFragment == null) {
                        mHost.onAttachFragment(f);
                    } else {
                        f.mParentFragment.onAttachFragment(f);
                    }
                    dispatchOnFragmentAttached(f, mHost.getContext(), false);

                    if (!f.mRetaining) {
                        f.performCreate(f.mSavedFragmentState);
                        dispatchOnFragmentCreated(f, f.mSavedFragmentState, false);
                    } else {
                        f.restoreChildFragmentState(f.mSavedFragmentState);
                        f.mState = MyFragment.CREATED;
                    }
                    f.mRetaining = false;
                    if (f.mFromLayout) {
                        // For fragments that are part of the content view
                        // layout, we need to instantiate the view immediately
                        // and the inflater will take care of adding it.
                        f.mView = f.performCreateView(f.getLayoutInflater(
                                f.mSavedFragmentState), null, f.mSavedFragmentState);
                        if (f.mView != null) {
                            f.mInnerView = f.mView;
                            if (Build.VERSION.SDK_INT >= 11) {
                                ViewCompat.setSaveFromParentEnabled(f.mView, false);
                            } else {
                                //注释
//                                f.mView = NoSaveStateFrameLayout.wrap(f.mView);
                            }
                            if (f.mHidden) f.mView.setVisibility(View.GONE);
                            f.onViewCreated(f.mView, f.mSavedFragmentState);
                            dispatchOnFragmentViewCreated(f, f.mView, f.mSavedFragmentState, false);
                        } else {
                            f.mInnerView = null;
                        }
                    }
                case MyFragment.CREATED:
                    if (newState > MyFragment.CREATED) {
                        if (DEBUG) Log.v(TAG, "moveto ACTIVITY_CREATED: " + f);
                        if (!f.mFromLayout) {
                            ViewGroup container = null;
                            if (f.mContainerId != 0) {
                                if (f.mContainerId == View.NO_ID) {
                                    throwException(new IllegalArgumentException(
                                            "Cannot create fragment "
                                                    + f
                                                    + " for a container view with no id"));
                                }
                                container = (ViewGroup) mContainer.onFindViewById(f.mContainerId);
                                if (container == null && !f.mRestored) {
                                    String resName;
                                    try {
                                        resName = f.getResources().getResourceName(f.mContainerId);
                                    } catch (Resources.NotFoundException e) {
                                        resName = "unknown";
                                    }
                                    throwException(new IllegalArgumentException(
                                            "No view found for id 0x"
                                                    + Integer.toHexString(f.mContainerId) + " ("
                                                    + resName
                                                    + ") for fragment " + f));
                                }
                            }
                            f.mContainer = container;
                            f.mView = f.performCreateView(f.getLayoutInflater(
                                    f.mSavedFragmentState), container, f.mSavedFragmentState);
                            if (f.mView != null) {
                                f.mInnerView = f.mView;
                                if (Build.VERSION.SDK_INT >= 11) {
                                    ViewCompat.setSaveFromParentEnabled(f.mView, false);
                                } else {
                                    //注释
//                                    f.mView = NoSaveStateFrameLayout.wrap(f.mView);
                                }
                                if (container != null) {
                                    container.addView(f.mView);
                                }
                                if (f.mHidden) {
                                    f.mView.setVisibility(View.GONE);
                                }
                                f.onViewCreated(f.mView, f.mSavedFragmentState);
                                dispatchOnFragmentViewCreated(f, f.mView, f.mSavedFragmentState,
                                        false);
                                // Only animate the view if it is visible. This is done after
                                // dispatchOnFragmentViewCreated in case visibility is changed
                                f.mIsNewlyAdded = (f.mView.getVisibility() == View.VISIBLE)
                                        && f.mContainer != null;
                            } else {
                                f.mInnerView = null;
                            }
                        }

                        f.performActivityCreated(f.mSavedFragmentState);
                        dispatchOnFragmentActivityCreated(f, f.mSavedFragmentState, false);
                        if (f.mView != null) {
                            f.restoreViewState(f.mSavedFragmentState);
                        }
                        f.mSavedFragmentState = null;
                    }
                case MyFragment.ACTIVITY_CREATED:
                    if (newState > MyFragment.ACTIVITY_CREATED) {
                        f.mState = MyFragment.STOPPED;
                    }
                case MyFragment.STOPPED:
                    if (newState > MyFragment.STOPPED) {
                        if (DEBUG) Log.v(TAG, "moveto STARTED: " + f);
                        f.performStart();
                        dispatchOnFragmentStarted(f, false);
                    }
                case MyFragment.STARTED:
                    if (newState > MyFragment.STARTED) {
                        if (DEBUG) Log.v(TAG, "moveto RESUMED: " + f);
                        f.performResume();
                        dispatchOnFragmentResumed(f, false);
                        f.mSavedFragmentState = null;
                        f.mSavedViewState = null;
                    }
            }
        } else if (f.mState > newState) {
            switch (f.mState) {
                case MyFragment.RESUMED:
                    if (newState < MyFragment.RESUMED) {
                        if (DEBUG) Log.v(TAG, "movefrom RESUMED: " + f);
                        f.performPause();
                        dispatchOnFragmentPaused(f, false);
                    }
                case MyFragment.STARTED:
                    if (newState < MyFragment.STARTED) {
                        if (DEBUG) Log.v(TAG, "movefrom STARTED: " + f);
                        f.performStop();
                        dispatchOnFragmentStopped(f, false);
                    }
                case MyFragment.STOPPED:
                    if (newState < MyFragment.STOPPED) {
                        if (DEBUG) Log.v(TAG, "movefrom STOPPED: " + f);
                        f.performReallyStop();
                    }
                case MyFragment.ACTIVITY_CREATED:
                    if (newState < MyFragment.ACTIVITY_CREATED) {
                        if (DEBUG) Log.v(TAG, "movefrom ACTIVITY_CREATED: " + f);
                        if (f.mView != null) {
                            // Need to save the current view state if not
                            // done already.
                            if (mHost.onShouldSaveFragmentState(f) && f.mSavedViewState == null) {
                                saveFragmentViewState(f);
                            }
                        }
                        f.performDestroyView();
                        dispatchOnFragmentViewDestroyed(f, false);
                        if (f.mView != null && f.mContainer != null) {
                            Animation anim = null;
                            if (mCurState > MyFragment.INITIALIZING && !mDestroyed
                                    && f.mView.getVisibility() == View.VISIBLE
                                    && f.mPostponedAlpha >= 0) {
                                anim = loadAnimation(f, transit, false,
                                        transitionStyle);
                            }
                            f.mPostponedAlpha = 0;
                            if (anim != null) {
                                final MyFragment fragment = f;
                                f.setAnimatingAway(f.mView);
                                f.setStateAfterAnimating(newState);
                                final View viewToAnimate = f.mView;
                                anim.setAnimationListener(new AnimateOnHWLayerIfNeededListener(
                                        viewToAnimate, anim) {
                                    @Override
                                    public void onAnimationEnd(Animation animation) {
                                        super.onAnimationEnd(animation);
                                        if (fragment.getAnimatingAway() != null) {
                                            fragment.setAnimatingAway(null);
                                            moveToState(fragment, fragment.getStateAfterAnimating(),
                                                    0, 0, false);
                                        }
                                    }
                                });
                                f.mView.startAnimation(anim);
                            }
                            f.mContainer.removeView(f.mView);
                        }
                        f.mContainer = null;
                        f.mView = null;
                        f.mInnerView = null;
                    }
                case MyFragment.CREATED:
                    if (newState < MyFragment.CREATED) {
                        if (mDestroyed) {
                            if (f.getAnimatingAway() != null) {
                                // The fragment's containing activity is
                                // being destroyed, but this fragment is
                                // currently animating away.  Stop the
                                // animation right now -- it is not needed,
                                // and we can't wait any more on destroying
                                // the fragment.
                                View v = f.getAnimatingAway();
                                f.setAnimatingAway(null);
                                v.clearAnimation();
                            }
                        }
                        if (f.getAnimatingAway() != null) {
                            // We are waiting for the fragment's view to finish
                            // animating away.  Just make a note of the state
                            // the fragment now should move to once the animation
                            // is done.
                            f.setStateAfterAnimating(newState);
                            newState = MyFragment.CREATED;
                        } else {
                            if (DEBUG) Log.v(TAG, "movefrom CREATED: " + f);
                            if (!f.mRetaining) {
                                f.performDestroy();
                                dispatchOnFragmentDestroyed(f, false);
                            } else {
                                f.mState = MyFragment.INITIALIZING;
                            }

                            f.performDetach();
                            dispatchOnFragmentDetached(f, false);
                            if (!keepActive) {
                                if (!f.mRetaining) {
                                    makeInactive(f);
                                } else {
                                    f.mHost = null;
                                    f.mParentFragment = null;
                                    f.mFragmentManager = null;
                                }
                            }
                        }
                    }
            }
        }

        if (f.mState != newState) {
            Log.w(TAG, "moveToState: MyFragment state for " + f + " not updated inline; "
                    + "expected state " + newState + " found " + f.mState);
            f.mState = newState;
        }
    }

    void moveToState(MyFragment f) {
        moveToState(f, mCurState, 0, 0, false);
    }

    /**
     * Fragments that have been shown or hidden don't have their visibility changed or
     * animations run during the {@link #showFragment(MyFragment)} or {@link #hideFragment(MyFragment)}
     * calls. After fragments are brought to their final state in
     * {@link #moveFragmentToExpectedState(MyFragment)} the fragments that have been shown or
     * hidden must have their visibility changed and their animations started here.
     *
     * @param fragment The fragment with mHiddenChanged = true that should change its View's
     *                 visibility and start the show or hide animation.
     */
    void completeShowHideFragment(final MyFragment fragment) {
        if (fragment.mView != null) {
            Animation anim = loadAnimation(fragment, fragment.getNextTransition(),
                    !fragment.mHidden, fragment.getNextTransitionStyle());
            if (anim != null) {
                setHWLayerAnimListenerIfAlpha(fragment.mView, anim);
                fragment.mView.startAnimation(anim);
                setHWLayerAnimListenerIfAlpha(fragment.mView, anim);
                anim.start();
            }
            final int visibility = fragment.mHidden && !fragment.isHideReplaced()
                    ? View.GONE
                    : View.VISIBLE;
            fragment.mView.setVisibility(visibility);
            if (fragment.isHideReplaced()) {
                fragment.setHideReplaced(false);
            }
        }
        if (fragment.mAdded && fragment.mHasMenu && fragment.mMenuVisible) {
            mNeedMenuInvalidate = true;
        }
        fragment.mHiddenChanged = false;
        fragment.onHiddenChanged(fragment.mHidden);
    }

    /**
     * Moves a fragment to its expected final state or the fragment manager's state, depending
     * on whether the fragment manager's state is raised properly.
     *
     * @param f The fragment to change.
     */
    void moveFragmentToExpectedState(MyFragment f) {
        if (f == null) {
            return;
        }
        int nextState = mCurState;
        if (f.mRemoving) {
            if (f.isInBackStack()) {
                nextState = Math.min(nextState, MyFragment.CREATED);
            } else {
                nextState = Math.min(nextState, MyFragment.INITIALIZING);
            }
        }
        moveToState(f, nextState, f.getNextTransition(), f.getNextTransitionStyle(), false);

        if (f.mView != null) {
            // Move the view if it is out of order
            MyFragment underFragment = findFragmentUnder(f);
            if (underFragment != null) {
                final View underView = underFragment.mView;
                // make sure this fragment is in the right order.
                final ViewGroup container = f.mContainer;
                int underIndex = container.indexOfChild(underView);
                int viewIndex = container.indexOfChild(f.mView);
                if (viewIndex < underIndex) {
                    container.removeViewAt(viewIndex);
                    container.addView(f.mView, underIndex);
                }
            }
            if (f.mIsNewlyAdded && f.mContainer != null) {
                // Make it visible and run the animations
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                    f.mView.setVisibility(View.VISIBLE);
                } else if (f.mPostponedAlpha > 0f) {
                    f.mView.setAlpha(f.mPostponedAlpha);
                }
                f.mPostponedAlpha = 0f;
                f.mIsNewlyAdded = false;
                // run animations:
                Animation anim = loadAnimation(f, f.getNextTransition(), true,
                        f.getNextTransitionStyle());
                if (anim != null) {
                    setHWLayerAnimListenerIfAlpha(f.mView, anim);
                    f.mView.startAnimation(anim);
                }
            }
        }
        if (f.mHiddenChanged) {
            completeShowHideFragment(f);
        }
    }

    /**
     * Changes the state of the fragment manager to {@code newState}. If the fragment manager
     * changes state or {@code always} is {@code true}, any fragments within it have their
     * states updated as well.
     *
     * @param newState The new state for the fragment manager
     * @param always   If {@code true}, all fragments update their state, even
     *                 if {@code newState} matches the current fragment manager's state.
     */
    void moveToState(int newState, boolean always) {
        if (mHost == null && newState != MyFragment.INITIALIZING) {
            throw new IllegalStateException("No activity");
        }

        if (!always && newState == mCurState) {
            return;
        }

        mCurState = newState;

        if (mActive != null) {
            boolean loadersRunning = false;

            // Must add them in the proper order. mActive fragments may be out of order
            if (mAdded != null) {
                final int numAdded = mAdded.size();
                for (int i = 0; i < numAdded; i++) {
                    MyFragment f = mAdded.get(i);
                    moveFragmentToExpectedState(f);
                    if (f.mLoaderManager != null) {
                        loadersRunning |= f.mLoaderManager.hasRunningLoaders();
                    }
                }
            }

            // Now iterate through all active fragments. These will include those that are removed
            // and detached.
            final int numActive = mActive.size();
            for (int i = 0; i < numActive; i++) {
                MyFragment f = mActive.get(i);
                if (f != null && (f.mRemoving || f.mDetached) && !f.mIsNewlyAdded) {
                    moveFragmentToExpectedState(f);
                    if (f.mLoaderManager != null) {
                        loadersRunning |= f.mLoaderManager.hasRunningLoaders();
                    }
                }
            }

            if (!loadersRunning) {
                startPendingDeferredFragments();
            }

            if (mNeedMenuInvalidate && mHost != null && mCurState == MyFragment.RESUMED) {
                mHost.onSupportInvalidateOptionsMenu();
                mNeedMenuInvalidate = false;
            }
        }
    }

    void startPendingDeferredFragments() {
        if (mActive == null) return;

        for (int i = 0; i < mActive.size(); i++) {
            MyFragment f = mActive.get(i);
            if (f != null) {
                performPendingDeferredStart(f);
            }
        }
    }

    void makeActive(MyFragment f) {
        if (f.mIndex >= 0) {
            return;
        }

        if (mAvailIndices == null || mAvailIndices.size() <= 0) {
            if (mActive == null) {
                mActive = new ArrayList<MyFragment>();
            }
            f.setIndex(mActive.size(), mParent);
            mActive.add(f);

        } else {
            f.setIndex(mAvailIndices.remove(mAvailIndices.size() - 1), mParent);
            mActive.set(f.mIndex, f);
        }
        if (DEBUG) Log.v(TAG, "Allocated fragment index " + f);
    }

    void makeInactive(MyFragment f) {
        if (f.mIndex < 0) {
            return;
        }

        if (DEBUG) Log.v(TAG, "Freeing fragment index " + f);
        mActive.set(f.mIndex, null);
        if (mAvailIndices == null) {
            mAvailIndices = new ArrayList<Integer>();
        }
        mAvailIndices.add(f.mIndex);
        mHost.inactivateFragment(f.mWho);
        f.initState();
    }

    public void addFragment(MyFragment fragment, boolean moveToStateNow) {
        if (mAdded == null) {
            mAdded = new ArrayList<MyFragment>();
        }
        if (DEBUG) Log.v(TAG, "add: " + fragment);
        makeActive(fragment);
        if (!fragment.mDetached) {
            if (mAdded.contains(fragment)) {
                throw new IllegalStateException("MyFragment already added: " + fragment);
            }
            mAdded.add(fragment);
            fragment.mAdded = true;
            fragment.mRemoving = false;
            if (fragment.mView == null) {
                fragment.mHiddenChanged = false;
            }
            if (fragment.mHasMenu && fragment.mMenuVisible) {
                mNeedMenuInvalidate = true;
            }
            if (moveToStateNow) {
                moveToState(fragment);
            }
        }
    }

    public void removeFragment(MyFragment fragment) {
        if (DEBUG) Log.v(TAG, "remove: " + fragment + " nesting=" + fragment.mBackStackNesting);
        final boolean inactive = !fragment.isInBackStack();
        if (!fragment.mDetached || inactive) {
            if (mAdded != null) {
                mAdded.remove(fragment);
            }
            if (fragment.mHasMenu && fragment.mMenuVisible) {
                mNeedMenuInvalidate = true;
            }
            fragment.mAdded = false;
            fragment.mRemoving = true;
        }
    }

    /**
     * Marks a fragment as hidden to be later animated in with
     * {@link #completeShowHideFragment(MyFragment)}.
     *
     * @param fragment The fragment to be shown.
     */
    public void hideFragment(MyFragment fragment) {
        if (DEBUG) Log.v(TAG, "hide: " + fragment);
        if (!fragment.mHidden) {
            fragment.mHidden = true;
            // Toggle hidden changed so that if a fragment goes through show/hide/show
            // it doesn't go through the animation.
            fragment.mHiddenChanged = !fragment.mHiddenChanged;
        }
    }

    /**
     * Marks a fragment as shown to be later animated in with
     * {@link #completeShowHideFragment(MyFragment)}.
     *
     * @param fragment The fragment to be shown.
     */
    public void showFragment(MyFragment fragment) {
        if (DEBUG) Log.v(TAG, "show: " + fragment);
        if (fragment.mHidden) {
            fragment.mHidden = false;
            // Toggle hidden changed so that if a fragment goes through show/hide/show
            // it doesn't go through the animation.
            fragment.mHiddenChanged = !fragment.mHiddenChanged;
        }
    }

    public void detachFragment(MyFragment fragment) {
        if (DEBUG) Log.v(TAG, "detach: " + fragment);
        if (!fragment.mDetached) {
            fragment.mDetached = true;
            if (fragment.mAdded) {
                // We are not already in back stack, so need to remove the fragment.
                if (mAdded != null) {
                    if (DEBUG) Log.v(TAG, "remove from detach: " + fragment);
                    mAdded.remove(fragment);
                }
                if (fragment.mHasMenu && fragment.mMenuVisible) {
                    mNeedMenuInvalidate = true;
                }
                fragment.mAdded = false;
            }
        }
    }

    public void attachFragment(MyFragment fragment) {
        if (DEBUG) Log.v(TAG, "attach: " + fragment);
        if (fragment.mDetached) {
            fragment.mDetached = false;
            if (!fragment.mAdded) {
                if (mAdded == null) {
                    mAdded = new ArrayList<MyFragment>();
                }
                if (mAdded.contains(fragment)) {
                    throw new IllegalStateException("MyFragment already added: " + fragment);
                }
                if (DEBUG) Log.v(TAG, "add from attach: " + fragment);
                mAdded.add(fragment);
                fragment.mAdded = true;
                if (fragment.mHasMenu && fragment.mMenuVisible) {
                    mNeedMenuInvalidate = true;
                }
            }
        }
    }

    public MyFragment findFragmentById(int id) {
        if (mAdded != null) {
            // First look through added fragments.
            for (int i = mAdded.size() - 1; i >= 0; i--) {
                MyFragment f = mAdded.get(i);
                if (f != null && f.mFragmentId == id) {
                    return f;
                }
            }
        }
        if (mActive != null) {
            // Now for any known fragment.
            for (int i = mActive.size() - 1; i >= 0; i--) {
                MyFragment f = mActive.get(i);
                if (f != null && f.mFragmentId == id) {
                    return f;
                }
            }
        }
        return null;
    }

    public MyFragment findFragmentByTag(String tag) {
        if (mAdded != null && tag != null) {
            // First look through added fragments.
            for (int i = mAdded.size() - 1; i >= 0; i--) {
                MyFragment f = mAdded.get(i);
                if (f != null && tag.equals(f.mTag)) {
                    return f;
                }
            }
        }
        if (mActive != null && tag != null) {
            // Now for any known fragment.
            for (int i = mActive.size() - 1; i >= 0; i--) {
                MyFragment f = mActive.get(i);
                if (f != null && tag.equals(f.mTag)) {
                    return f;
                }
            }
        }
        return null;
    }

    public MyFragment findFragmentByWho(String who) {
        if (mActive != null && who != null) {
            for (int i = mActive.size() - 1; i >= 0; i--) {
                MyFragment f = mActive.get(i);
                if (f != null && (f = f.findFragmentByWho(who)) != null) {
                    return f;
                }
            }
        }
        return null;
    }

    private void checkStateLoss() {
        if (mStateSaved) {
            throw new IllegalStateException(
                    "Can not perform this action after onSaveInstanceState");
        }
        if (mNoTransactionsBecause != null) {
            throw new IllegalStateException(
                    "Can not perform this action inside of " + mNoTransactionsBecause);
        }
    }

    /**
     * Adds an action to the queue of pending actions.
     *
     * @param action         the action to add
     * @param allowStateLoss whether to allow loss of state information
     * @throws IllegalStateException if the activity has been destroyed
     */
    public void enqueueAction(OpGenerator action, boolean allowStateLoss) {
        if (!allowStateLoss) {
            checkStateLoss();
        }
        synchronized (this) {
            if (mDestroyed || mHost == null) {
                throw new IllegalStateException("Activity has been destroyed");
            }
            if (mPendingActions == null) {
                mPendingActions = new ArrayList<>();
            }
            mPendingActions.add(action);
            scheduleCommit();
        }
    }

    /**
     * Schedules the execution when one hasn't been scheduled already. This should happen
     * the first time {@link #enqueueAction(MyFragmentManagerImp.OpGenerator, boolean)} is called or when
     * a postponed transaction has been started with
     * {@link MyFragment#startPostponedEnterTransition()}
     */
    private void scheduleCommit() {
        synchronized (this) {
            boolean postponeReady =
                    mPostponedTransactions != null && !mPostponedTransactions.isEmpty();
            boolean pendingReady = mPendingActions != null && mPendingActions.size() == 1;
            if (postponeReady || pendingReady) {
                mHost.getHandler().removeCallbacks(mExecCommit);
                mHost.getHandler().post(mExecCommit);
            }
        }
    }

    public int allocBackStackIndex(MyBackStackRecord bse) {
        synchronized (this) {
            if (mAvailBackStackIndices == null || mAvailBackStackIndices.size() <= 0) {
                if (mBackStackIndices == null) {
                    mBackStackIndices = new ArrayList<MyBackStackRecord>();
                }
                int index = mBackStackIndices.size();
                if (DEBUG) Log.v(TAG, "Setting back stack index " + index + " to " + bse);
                mBackStackIndices.add(bse);
                return index;

            } else {
                int index = mAvailBackStackIndices.remove(mAvailBackStackIndices.size() - 1);
                if (DEBUG) Log.v(TAG, "Adding back stack index " + index + " with " + bse);
                mBackStackIndices.set(index, bse);
                return index;
            }
        }
    }

    public void setBackStackIndex(int index, MyBackStackRecord bse) {
        synchronized (this) {
            if (mBackStackIndices == null) {
                mBackStackIndices = new ArrayList<MyBackStackRecord>();
            }
            int N = mBackStackIndices.size();
            if (index < N) {
                if (DEBUG) Log.v(TAG, "Setting back stack index " + index + " to " + bse);
                mBackStackIndices.set(index, bse);
            } else {
                while (N < index) {
                    mBackStackIndices.add(null);
                    if (mAvailBackStackIndices == null) {
                        mAvailBackStackIndices = new ArrayList<Integer>();
                    }
                    if (DEBUG) Log.v(TAG, "Adding available back stack index " + N);
                    mAvailBackStackIndices.add(N);
                    N++;
                }
                if (DEBUG) Log.v(TAG, "Adding back stack index " + index + " with " + bse);
                mBackStackIndices.add(bse);
            }
        }
    }

    public void freeBackStackIndex(int index) {
        synchronized (this) {
            mBackStackIndices.set(index, null);
            if (mAvailBackStackIndices == null) {
                mAvailBackStackIndices = new ArrayList<Integer>();
            }
            if (DEBUG) Log.v(TAG, "Freeing back stack index " + index);
            mAvailBackStackIndices.add(index);
        }
    }

    /**
     * Broken out from exec*, this prepares for gathering and executing operations.
     *
     * @param allowStateLoss true if state loss should be ignored or false if it should be
     *                       checked.
     */
    private void ensureExecReady(boolean allowStateLoss) {
        if (mExecutingActions) {
            throw new IllegalStateException("FragmentManager is already executing transactions");
        }

        if (Looper.myLooper() != mHost.getHandler().getLooper()) {
            throw new IllegalStateException("Must be called from main thread of fragment host");
        }

        if (!allowStateLoss) {
            checkStateLoss();
        }

        if (mTmpRecords == null) {
            mTmpRecords = new ArrayList<>();
            mTmpIsPop = new ArrayList<>();
        }
        mExecutingActions = true;
        try {
            executePostponedTransaction(null, null);
        } finally {
            mExecutingActions = false;
        }
    }

    public void execSingleAction(OpGenerator action, boolean allowStateLoss) {
        ensureExecReady(allowStateLoss);
        if (action.generateOps(mTmpRecords, mTmpIsPop)) {
            mExecutingActions = true;
            try {
                optimizeAndExecuteOps(mTmpRecords, mTmpIsPop);
            } finally {
                cleanupExec();
            }
        }

        doPendingDeferredStart();
    }

    /**
     * Broken out of exec*, this cleans up the mExecutingActions and the temporary structures
     * used in executing operations.
     */
    private void cleanupExec() {
        mExecutingActions = false;
        mTmpIsPop.clear();
        mTmpRecords.clear();
    }

    /**
     * Only call from main thread!
     */
    public boolean execPendingActions() {
        ensureExecReady(true);

        boolean didSomething = false;
        while (generateOpsForPendingActions(mTmpRecords, mTmpIsPop)) {
            mExecutingActions = true;
            try {
                optimizeAndExecuteOps(mTmpRecords, mTmpIsPop);
            } finally {
                cleanupExec();
            }
            didSomething = true;
        }

        doPendingDeferredStart();

        return didSomething;
    }

    /**
     * Complete the execution of transactions that have previously been postponed, but are
     * now ready.
     */
    private void executePostponedTransaction(ArrayList<MyBackStackRecord> records,
                                             ArrayList<Boolean> isRecordPop) {
        int numPostponed = mPostponedTransactions == null ? 0 : mPostponedTransactions.size();
        for (int i = 0; i < numPostponed; i++) {
            StartEnterTransitionListener listener = mPostponedTransactions.get(i);
            if (records != null && !listener.mIsBack) {
                int index = records.indexOf(listener.mRecord);
                if (index != -1 && isRecordPop.get(index)) {
                    listener.cancelTransaction();
                    continue;
                }
            }
            if (listener.isReady() || (records != null
                    && listener.mRecord.interactsWith(records, 0, records.size()))) {
                mPostponedTransactions.remove(i);
                i--;
                numPostponed--;
                int index;
                if (records != null && !listener.mIsBack
                        && (index = records.indexOf(listener.mRecord)) != -1
                        && isRecordPop.get(index)) {
                    // This is popping a postponed transaction
                    listener.cancelTransaction();
                } else {
                    listener.completeTransaction();
                }
            }
        }
    }

    /**
     * Optimizes MyBackStackRecord operations. This method merges operations of proximate records
     * that allow optimization. See {@link FragmentTransaction#setAllowOptimization(boolean)}.
     * <p>
     * For example, a transaction that adds to the back stack and then another that pops that
     * back stack record will be optimized.
     * <p>
     * Likewise, two transactions committed that are executed at the same time will be optimized
     * as well as two pop operations executed together.
     *
     * @param records     The records pending execution
     * @param isRecordPop The direction that these records are being run.
     */
    private void optimizeAndExecuteOps(ArrayList<MyBackStackRecord> records,
                                       ArrayList<Boolean> isRecordPop) {
        if (records == null || records.isEmpty()) {
            return;
        }

        if (isRecordPop == null || records.size() != isRecordPop.size()) {
            throw new IllegalStateException("Internal error with the back stack records");
        }

        // Force start of any postponed transactions that interact with scheduled transactions:
        executePostponedTransaction(records, isRecordPop);

        final int numRecords = records.size();
        int startIndex = 0;
        for (int recordNum = 0; recordNum < numRecords; recordNum++) {
            final boolean canOptimize = records.get(recordNum).mAllowOptimization;
            if (!canOptimize) {
                // execute all previous transactions
                if (startIndex != recordNum) {
                    executeOpsTogether(records, isRecordPop, startIndex, recordNum);
                }
                // execute all unoptimized pop operations together or one add operation
                int optimizeEnd = recordNum + 1;
                if (isRecordPop.get(recordNum)) {
                    while (optimizeEnd < numRecords
                            && isRecordPop.get(optimizeEnd)
                            && !records.get(optimizeEnd).mAllowOptimization) {
                        optimizeEnd++;
                    }
                }
                executeOpsTogether(records, isRecordPop, recordNum, optimizeEnd);
                startIndex = optimizeEnd;
                recordNum = optimizeEnd - 1;
            }
        }
        if (startIndex != numRecords) {
            executeOpsTogether(records, isRecordPop, startIndex, numRecords);
        }
    }

    /**
     * Optimizes a subset of a list of BackStackRecords, all of which either allow optimization or
     * do not allow optimization.
     *
     * @param records     A list of BackStackRecords that are to be optimized
     * @param isRecordPop The direction that these records are being run.
     * @param startIndex  The index of the first record in <code>records</code> to be optimized
     * @param endIndex    One more than the final record index in <code>records</code> to optimize.
     */
    private void executeOpsTogether(ArrayList<MyBackStackRecord> records,
                                    ArrayList<Boolean> isRecordPop, int startIndex, int endIndex) {
        final boolean allowOptimization = records.get(startIndex).mAllowOptimization;
        boolean addToBackStack = false;
        if (mTmpAddedFragments == null) {
            mTmpAddedFragments = new ArrayList<>();
        } else {
            mTmpAddedFragments.clear();
        }
        if (mAdded != null) {
            mTmpAddedFragments.addAll(mAdded);
        }
        for (int recordNum = startIndex; recordNum < endIndex; recordNum++) {
            final MyBackStackRecord record = records.get(recordNum);
            final boolean isPop = isRecordPop.get(recordNum);
            if (!isPop) {
                record.expandReplaceOps(mTmpAddedFragments);
            } else {
                record.trackAddedFragmentsInPop(mTmpAddedFragments);
            }
            addToBackStack = addToBackStack || record.mAddToBackStack;
        }
        mTmpAddedFragments.clear();

        if (!allowOptimization) {
            //注释
//            MyFragmentTransaction.startTransitions(this, records, isRecordPop, startIndex, endIndex, false);
        }
        executeOps(records, isRecordPop, startIndex, endIndex);

        int postponeIndex = endIndex;
        if (allowOptimization) {
            ArraySet<MyFragment> addedFragments = new ArraySet<>();
            addAddedFragments(addedFragments);
            postponeIndex = postponePostponableTransactions(records, isRecordPop,
                    startIndex, endIndex, addedFragments);
            makeRemovedFragmentsInvisible(addedFragments);
        }

        if (postponeIndex != startIndex && allowOptimization) {
            // need to run something now
            //注释
//            MyFragmentTransaction.startTransitions(this, records, isRecordPop, startIndex, postponeIndex, true);
            moveToState(mCurState, true);
        }

        for (int recordNum = startIndex; recordNum < endIndex; recordNum++) {
            final MyBackStackRecord record = records.get(recordNum);
            final boolean isPop = isRecordPop.get(recordNum);
            if (isPop && record.mIndex >= 0) {
                freeBackStackIndex(record.mIndex);
                record.mIndex = -1;
            }
        }
        if (addToBackStack) {
            reportBackStackChanged();
        }
    }

    /**
     * Any fragments that were removed because they have been postponed should have their views
     * made invisible by setting their alpha to 0 on API >= 11 or setting visibility to INVISIBLE
     * on API < 11.
     *
     * @param fragments The fragments that were added during operation execution. Only the ones
     *                  that are no longer added will have their alpha changed.
     */
    private void makeRemovedFragmentsInvisible(ArraySet<MyFragment> fragments) {
        final int numAdded = fragments.size();
        for (int i = 0; i < numAdded; i++) {
            final MyFragment fragment = fragments.valueAt(i);
            if (!fragment.mAdded) {
                final View view = fragment.getView();
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                    fragment.getView().setVisibility(View.INVISIBLE);
                } else {
                    fragment.mPostponedAlpha = view.getAlpha();
                    view.setAlpha(0f);
                }
            }
        }
    }

    /**
     * Examine all transactions and determine which ones are marked as postponed. Those will
     * have their operations rolled back and moved to the end of the record list (up to endIndex).
     * It will also add the postponed transaction to the queue.
     *
     * @param records     A list of BackStackRecords that should be checked.
     * @param isRecordPop The direction that these records are being run.
     * @param startIndex  The index of the first record in <code>records</code> to be checked
     * @param endIndex    One more than the final record index in <code>records</code> to be checked.
     * @return The index of the first postponed transaction or endIndex if no transaction was
     * postponed.
     */
    private int postponePostponableTransactions(ArrayList<MyBackStackRecord> records,
                                                ArrayList<Boolean> isRecordPop, int startIndex, int endIndex,
                                                ArraySet<MyFragment> added) {
        int postponeIndex = endIndex;
        for (int i = endIndex - 1; i >= startIndex; i--) {
            final MyBackStackRecord record = records.get(i);
            final boolean isPop = isRecordPop.get(i);
            boolean isPostponed = record.isPostponed()
                    && !record.interactsWith(records, i + 1, endIndex);
            if (isPostponed) {
                if (mPostponedTransactions == null) {
                    mPostponedTransactions = new ArrayList<>();
                }
                MyFragmentManagerImp.StartEnterTransitionListener listener =
                        new MyFragmentManagerImp.StartEnterTransitionListener(record, isPop);
                mPostponedTransactions.add(listener);
                record.setOnStartPostponedListener(listener);

                // roll back the transaction
                if (isPop) {
                    record.executeOps();
                } else {
                    record.executePopOps(false);
                }

                // move to the end
                postponeIndex--;
                if (i != postponeIndex) {
                    records.remove(i);
                    records.add(postponeIndex, record);
                }

                // different views may be visible now
                addAddedFragments(added);
            }
        }
        return postponeIndex;
    }

    /**
     * When a postponed transaction is ready to be started, this completes the transaction,
     * removing, hiding, or showing views as well as starting the animations and transitions.
     * <p>
     * {@code runtransitions} is set to false when the transaction postponement was interrupted
     * abnormally -- normally by a new transaction being started that affects the postponed
     * transaction.
     *
     * @param record         The transaction to run
     * @param isPop          true if record is popping or false if it is adding
     * @param runTransitions true if the fragment transition should be run or false otherwise.
     * @param moveToState    true if the state should be changed after executing the operations.
     *                       This is false when the transaction is canceled when a postponed
     *                       transaction is popped.
     */
    private void completeExecute(MyBackStackRecord record, boolean isPop, boolean runTransitions,
                                 boolean moveToState) {
        ArrayList<MyBackStackRecord> records = new ArrayList<>(1);
        ArrayList<Boolean> isRecordPop = new ArrayList<>(1);
        records.add(record);
        isRecordPop.add(isPop);
        executeOps(records, isRecordPop, 0, 1);
        if (runTransitions) {
            //注释
//            FragmentTransition.startTransitions(this, records, isRecordPop, 0, 1, true);
        }
        if (moveToState) {
            moveToState(mCurState, true);
        }

        if (mActive != null) {
            final int numActive = mActive.size();
            for (int i = 0; i < numActive; i++) {
                // Allow added fragments to be removed during the pop since we aren't going
                // to move them to the final state with moveToState(mCurState).
                MyFragment fragment = mActive.get(i);
                if (fragment != null && fragment.mView != null && fragment.mIsNewlyAdded
                        && record.interactsWith(fragment.mContainerId)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB
                            && fragment.mPostponedAlpha > 0) {
                        fragment.mView.setAlpha(fragment.mPostponedAlpha);
                    }
                    if (moveToState) {
                        fragment.mPostponedAlpha = 0;
                    } else {
                        fragment.mPostponedAlpha = -1;
                        fragment.mIsNewlyAdded = false;
                    }
                }
            }
        }
    }

    /**
     * Find a fragment within the fragment's container whose View should be below the passed
     * fragment. {@code null} is returned when the fragment has no View or if there should be
     * no fragment with a View below the given fragment.
     * <p>
     * As an example, if mAdded has two Fragments with Views sharing the same container:
     * FragmentA
     * FragmentB
     * <p>
     * Then, when processing FragmentB, FragmentA will be returned. If, however, FragmentA
     * had no View, null would be returned.
     *
     * @param f The fragment that may be on top of another fragment.
     * @return The fragment with a View under f, if one exists or null if f has no View or
     * there are no fragments with Views in the same container.
     */
    private MyFragment findFragmentUnder(MyFragment f) {
        final ViewGroup container = f.mContainer;
        final View view = f.mView;

        if (container == null || view == null) {
            return null;
        }

        final int fragmentIndex = mAdded.indexOf(f);
        for (int i = fragmentIndex - 1; i >= 0; i--) {
            MyFragment underFragment = mAdded.get(i);
            if (underFragment.mContainer == container && underFragment.mView != null) {
                // Found the fragment under this one
                return underFragment;
            }
        }
        return null;
    }

    /**
     * Run the operations in the BackStackRecords, either to push or pop.
     *
     * @param records     The list of records whose operations should be run.
     * @param isRecordPop The direction that these records are being run.
     * @param startIndex  The index of the first entry in records to run.
     * @param endIndex    One past the index of the final entry in records to run.
     */
    private static void executeOps(ArrayList<MyBackStackRecord> records,
                                   ArrayList<Boolean> isRecordPop, int startIndex, int endIndex) {
        for (int i = startIndex; i < endIndex; i++) {
            final MyBackStackRecord record = records.get(i);
            final boolean isPop = isRecordPop.get(i);
            if (isPop) {
                record.bumpBackStackNesting(-1);
                // Only execute the add operations at the end of
                // all transactions.
                boolean moveToState = i == (endIndex - 1);
                record.executePopOps(moveToState);
            } else {
                record.bumpBackStackNesting(1);
                record.executeOps();
            }
        }
    }

    /**
     * Ensure that fragments that are added are moved to at least the CREATED state.
     * Any newly-added Views are inserted into {@code added} so that the Transaction can be
     * postponed with {@link MyFragment#postponeEnterTransition()}. They will later be made
     * invisible (by setting their alpha to 0) if they have been removed when postponed.
     */
    private void addAddedFragments(ArraySet<MyFragment> added) {
        if (mCurState < MyFragment.CREATED) {
            return;
        }
        // We want to leave the fragment in the started state
        final int state = Math.min(mCurState, MyFragment.STARTED);
        final int numAdded = mAdded == null ? 0 : mAdded.size();
        for (int i = 0; i < numAdded; i++) {
            MyFragment fragment = mAdded.get(i);
            if (fragment.mState < state) {
                moveToState(fragment, state, fragment.getNextAnim(), fragment.getNextTransition(),
                        false);
                if (fragment.mView != null && !fragment.mHidden && fragment.mIsNewlyAdded) {
                    added.add(fragment);
                }
            }
        }
    }

    /**
     * Starts all postponed transactions regardless of whether they are ready or not.
     */
    private void forcePostponedTransactions() {
        if (mPostponedTransactions != null) {
            while (!mPostponedTransactions.isEmpty()) {
                mPostponedTransactions.remove(0).completeTransaction();
            }
        }
    }

    /**
     * Ends the animations of fragments so that they immediately reach the end state.
     * This is used prior to saving the state so that the correct state is saved.
     */
    private void endAnimatingAwayFragments() {
        final int numFragments = mActive == null ? 0 : mActive.size();
        for (int i = 0; i < numFragments; i++) {
            MyFragment fragment = mActive.get(i);
            if (fragment != null && fragment.getAnimatingAway() != null) {
                // Give up waiting for the animation and just end it.
                final int stateAfterAnimating = fragment.getStateAfterAnimating();
                final View animatingAway = fragment.getAnimatingAway();
                fragment.setAnimatingAway(null);
                Animation animation = animatingAway.getAnimation();
                if (animation != null) {
                    animation.cancel();
                }
                moveToState(fragment, stateAfterAnimating, 0, 0, false);
            }
        }
    }

    /**
     * Adds all records in the pending actions to records and whether they are add or pop
     * operations to isPop. After executing, the pending actions will be empty.
     *
     * @param records All pending actions will generate BackStackRecords added to this.
     *                This contains the transactions, in order, to execute.
     * @param isPop   All pending actions will generate booleans to add to this. This contains
     *                an entry for each entry in records to indicate whether or not it is a
     *                pop action.
     */
    private boolean generateOpsForPendingActions(ArrayList<MyBackStackRecord> records,
                                                 ArrayList<Boolean> isPop) {
        int numActions;
        synchronized (this) {
            if (mPendingActions == null || mPendingActions.size() == 0) {
                return false;
            }

            numActions = mPendingActions.size();
            for (int i = 0; i < numActions; i++) {
                mPendingActions.get(i).generateOps(records, isPop);
            }
            mPendingActions.clear();
            mHost.getHandler().removeCallbacks(mExecCommit);
        }
        return numActions > 0;
    }

    void doPendingDeferredStart() {
        if (mHavePendingDeferredStart) {
            boolean loadersRunning = false;
            for (int i = 0; i < mActive.size(); i++) {
                MyFragment f = mActive.get(i);
                if (f != null && f.mLoaderManager != null) {
                    loadersRunning |= f.mLoaderManager.hasRunningLoaders();
                }
            }
            if (!loadersRunning) {
                mHavePendingDeferredStart = false;
                startPendingDeferredFragments();
            }
        }
    }

    void reportBackStackChanged() {
        if (mBackStackChangeListeners != null) {
            for (int i = 0; i < mBackStackChangeListeners.size(); i++) {
                mBackStackChangeListeners.get(i).onBackStackChanged();
            }
        }
    }

    void addBackStackState(MyBackStackRecord state) {
        if (mBackStack == null) {
            mBackStack = new ArrayList<MyBackStackRecord>();
        }
        mBackStack.add(state);
        reportBackStackChanged();
    }

    @SuppressWarnings("unused")
    boolean popBackStackState(ArrayList<MyBackStackRecord> records, ArrayList<Boolean> isRecordPop,
                              String name, int id, int flags) {
        if (mBackStack == null) {
            return false;
        }
        if (name == null && id < 0 && (flags & POP_BACK_STACK_INCLUSIVE) == 0) {
            int last = mBackStack.size() - 1;
            if (last < 0) {
                return false;
            }
            records.add(mBackStack.remove(last));
            isRecordPop.add(true);
        } else {
            int index = -1;
            if (name != null || id >= 0) {
                // If a name or ID is specified, look for that place in
                // the stack.
                index = mBackStack.size() - 1;
                while (index >= 0) {
                    MyBackStackRecord bss = mBackStack.get(index);
                    if (name != null && name.equals(bss.getName())) {
                        break;
                    }
                    if (id >= 0 && id == bss.mIndex) {
                        break;
                    }
                    index--;
                }
                if (index < 0) {
                    return false;
                }
                if ((flags & POP_BACK_STACK_INCLUSIVE) != 0) {
                    index--;
                    // Consume all following entries that match.
                    while (index >= 0) {
                        MyBackStackRecord bss = mBackStack.get(index);
                        if ((name != null && name.equals(bss.getName()))
                                || (id >= 0 && id == bss.mIndex)) {
                            index--;
                            continue;
                        }
                        break;
                    }
                }
            }
            if (index == mBackStack.size() - 1) {
                return false;
            }
            for (int i = mBackStack.size() - 1; i > index; i--) {
                records.add(mBackStack.remove(i));
                isRecordPop.add(true);
            }
        }
        return true;
    }

    FragmentManagerNonConfit retainNonConfig() {
        ArrayList<MyFragment> fragments = null;
        ArrayList<FragmentManagerNonConfit> childFragments = null;
        if (mActive != null) {
            for (int i = 0; i < mActive.size(); i++) {
                MyFragment f = mActive.get(i);
                if (f != null) {
                    if (f.mRetainInstance) {
                        if (fragments == null) {
                            fragments = new ArrayList<MyFragment>();
                        }
                        fragments.add(f);
                        f.mRetaining = true;
                        f.mTargetIndex = f.mTarget != null ? f.mTarget.mIndex : -1;
                        if (DEBUG) Log.v(TAG, "retainNonConfig: keeping retained " + f);
                    }
                    boolean addedChild = false;
                    if (f.mChildFragmentManager != null) {
                        FragmentManagerNonConfit child = f.mChildFragmentManager.retainNonConfig();
                        if (child != null) {
                            if (childFragments == null) {
                                childFragments = new ArrayList<FragmentManagerNonConfit>();
                                for (int j = 0; j < i; j++) {
                                    childFragments.add(null);
                                }
                            }
                            childFragments.add(child);
                            addedChild = true;
                        }
                    }
                    if (childFragments != null && !addedChild) {
                        childFragments.add(null);
                    }
                }
            }
        }
        if (fragments == null && childFragments == null) {
            return null;
        }
        return new FragmentManagerNonConfit(fragments, childFragments);
    }

    void saveFragmentViewState(MyFragment f) {
        if (f.mInnerView == null) {
            return;
        }
        if (mStateArray == null) {
            mStateArray = new SparseArray<Parcelable>();
        } else {
            mStateArray.clear();
        }
        f.mInnerView.saveHierarchyState(mStateArray);
        if (mStateArray.size() > 0) {
            f.mSavedViewState = mStateArray;
            mStateArray = null;
        }
    }

    Bundle saveFragmentBasicState(MyFragment f) {
        Bundle result = null;

        if (mStateBundle == null) {
            mStateBundle = new Bundle();
        }
        f.performSaveInstanceState(mStateBundle);
        dispatchOnFragmentSaveInstanceState(f, mStateBundle, false);
        if (!mStateBundle.isEmpty()) {
            result = mStateBundle;
            mStateBundle = null;
        }

        if (f.mView != null) {
            saveFragmentViewState(f);
        }
        if (f.mSavedViewState != null) {
            if (result == null) {
                result = new Bundle();
            }
            result.putSparseParcelableArray(
                    VIEW_STATE_TAG, f.mSavedViewState);
        }
        if (!f.mUserVisibleHint) {
            if (result == null) {
                result = new Bundle();
            }
            // Only add this if it's not the default value
            result.putBoolean(USER_VISIBLE_HINT_TAG, f.mUserVisibleHint);
        }

        return result;
    }

    Parcelable saveAllState() {
        // Make sure all pending operations have now been executed to get
        // our state update-to-date.
        forcePostponedTransactions();
        endAnimatingAwayFragments();
        execPendingActions();

        if (HONEYCOMB) {
            // As of Honeycomb, we save state after pausing.  Prior to that
            // it is before pausing.  With fragments this is an issue, since
            // there are many things you may do after pausing but before
            // stopping that change the fragment state.  For those older
            // devices, we will not at this point say that we have saved
            // the state, so we will allow them to continue doing fragment
            // transactions.  This retains the same semantics as Honeycomb,
            // though you do have the risk of losing the very most recent state
            // if the process is killed...  we'll live with that.
            mStateSaved = true;
        }

        if (mActive == null || mActive.size() <= 0) {
            return null;
        }

        // First collect all active fragments.
        int N = mActive.size();
        FragmentState[] active = new FragmentState[N];
        boolean haveFragments = false;
        for (int i = 0; i < N; i++) {
            MyFragment f = mActive.get(i);
            if (f != null) {
                if (f.mIndex < 0) {
                    throwException(new IllegalStateException(
                            "Failure saving state: active " + f
                                    + " has cleared index: " + f.mIndex));
                }

                haveFragments = true;

                FragmentState fs = new FragmentState(f);
                active[i] = fs;

                if (f.mState > MyFragment.INITIALIZING && fs.mSavedFragmentState == null) {
                    fs.mSavedFragmentState = saveFragmentBasicState(f);

                    if (f.mTarget != null) {
                        if (f.mTarget.mIndex < 0) {
                            throwException(new IllegalStateException(
                                    "Failure saving state: " + f
                                            + " has target not in fragment manager: " + f.mTarget));
                        }
                        if (fs.mSavedFragmentState == null) {
                            fs.mSavedFragmentState = new Bundle();
                        }
                        putFragment(fs.mSavedFragmentState,
                                MyFragmentManagerImp.TARGET_STATE_TAG, f.mTarget);
                        if (f.mTargetRequestCode != 0) {
                            fs.mSavedFragmentState.putInt(
                                    MyFragmentManagerImp.TARGET_REQUEST_CODE_STATE_TAG,
                                    f.mTargetRequestCode);
                        }
                    }

                } else {
                    fs.mSavedFragmentState = f.mSavedFragmentState;
                }

                if (DEBUG) Log.v(TAG, "Saved state of " + f + ": "
                        + fs.mSavedFragmentState);
            }
        }

        if (!haveFragments) {
            if (DEBUG) Log.v(TAG, "saveAllState: no fragments!");
            return null;
        }

        int[] added = null;
        BackStackState[] backStack = null;

        // Build list of currently added fragments.
        if (mAdded != null) {
            N = mAdded.size();
            if (N > 0) {
                added = new int[N];
                for (int i = 0; i < N; i++) {
                    added[i] = mAdded.get(i).mIndex;
                    if (added[i] < 0) {
                        throwException(new IllegalStateException(
                                "Failure saving state: active " + mAdded.get(i)
                                        + " has cleared index: " + added[i]));
                    }
                    if (DEBUG) Log.v(TAG, "saveAllState: adding fragment #" + i
                            + ": " + mAdded.get(i));
                }
            }
        }

        // Now save back stack.
        if (mBackStack != null) {
            N = mBackStack.size();
            if (N > 0) {
                backStack = new BackStackState[N];
                for (int i = 0; i < N; i++) {
                    backStack[i] = new BackStackState(mBackStack.get(i));
                    if (DEBUG) Log.v(TAG, "saveAllState: adding back stack #" + i
                            + ": " + mBackStack.get(i));
                }
            }
        }

        FragmentManagerState fms = new FragmentManagerState();
        fms.mActive = active;
        fms.mAdded = added;
        fms.mBackStack = backStack;
        return fms;
    }

    void restoreAllState(Parcelable state, FragmentManagerNonConfit nonConfig) {
        // If there is no saved state at all, then there can not be
        // any nonConfig fragments either, so that is that.
        if (state == null) return;
        FragmentManagerState fms = (FragmentManagerState) state;
        if (fms.mActive == null) return;

        List<FragmentManagerNonConfit> childNonConfigs = null;

        // First re-attach any non-config instances we are retaining back
        // to their saved state, so we don't try to instantiate them again.
        if (nonConfig != null) {
            List<MyFragment> nonConfigFragments = nonConfig.getFragments();
            childNonConfigs = nonConfig.getChildNonConfigs();
            final int count = nonConfigFragments != null ? nonConfigFragments.size() : 0;
            for (int i = 0; i < count; i++) {
                MyFragment f = nonConfigFragments.get(i);
                if (DEBUG) Log.v(TAG, "restoreAllState: re-attaching retained " + f);
                FragmentState fs = fms.mActive[f.mIndex];
                fs.mInstance = f;
                f.mSavedViewState = null;
                f.mBackStackNesting = 0;
                f.mInLayout = false;
                f.mAdded = false;
                f.mTarget = null;
                if (fs.mSavedFragmentState != null) {
                    fs.mSavedFragmentState.setClassLoader(mHost.getContext().getClassLoader());
                    f.mSavedViewState = fs.mSavedFragmentState.getSparseParcelableArray(
                            MyFragmentManagerImp.VIEW_STATE_TAG);
                    f.mSavedFragmentState = fs.mSavedFragmentState;
                }
            }
        }

        // Build the full list of active fragments, instantiating them from
        // their saved state.
        mActive = new ArrayList<>(fms.mActive.length);
        if (mAvailIndices != null) {
            mAvailIndices.clear();
        }
        for (int i = 0; i < fms.mActive.length; i++) {
            FragmentState fs = fms.mActive[i];
            if (fs != null) {
                FragmentManagerNonConfit childNonConfig = null;
                if (childNonConfigs != null && i < childNonConfigs.size()) {
                    childNonConfig = childNonConfigs.get(i);
                }
                MyFragment f = fs.instantiate(mHost, mParent, childNonConfig);
                if (DEBUG) Log.v(TAG, "restoreAllState: active #" + i + ": " + f);
                mActive.add(f);
                // Now that the fragment is instantiated (or came from being
                // retained above), clear mInstance in case we end up re-restoring
                // from this FragmentState again.
                fs.mInstance = null;
            } else {
                mActive.add(null);
                if (mAvailIndices == null) {
                    mAvailIndices = new ArrayList<Integer>();
                }
                if (DEBUG) Log.v(TAG, "restoreAllState: avail #" + i);
                mAvailIndices.add(i);
            }
        }

        // Update the target of all retained fragments.
        if (nonConfig != null) {
            List<MyFragment> nonConfigFragments = nonConfig.getFragments();
            final int count = nonConfigFragments != null ? nonConfigFragments.size() : 0;
            for (int i = 0; i < count; i++) {
                MyFragment f = nonConfigFragments.get(i);
                if (f.mTargetIndex >= 0) {
                    if (f.mTargetIndex < mActive.size()) {
                        f.mTarget = mActive.get(f.mTargetIndex);
                    } else {
                        Log.w(TAG, "Re-attaching retained fragment " + f
                                + " target no longer exists: " + f.mTargetIndex);
                        f.mTarget = null;
                    }
                }
            }
        }

        // Build the list of currently added fragments.
        if (fms.mAdded != null) {
            mAdded = new ArrayList<MyFragment>(fms.mAdded.length);
            for (int i = 0; i < fms.mAdded.length; i++) {
                MyFragment f = mActive.get(fms.mAdded[i]);
                if (f == null) {
                    throwException(new IllegalStateException(
                            "No instantiated fragment for index #" + fms.mAdded[i]));
                }
                f.mAdded = true;
                if (DEBUG) Log.v(TAG, "restoreAllState: added #" + i + ": " + f);
                if (mAdded.contains(f)) {
                    throw new IllegalStateException("Already added!");
                }
                mAdded.add(f);
            }
        } else {
            mAdded = null;
        }

        // Build the back stack.
        if (fms.mBackStack != null) {
            mBackStack = new ArrayList<MyBackStackRecord>(fms.mBackStack.length);
            for (int i = 0; i < fms.mBackStack.length; i++) {
                MyBackStackRecord bse = fms.mBackStack[i].instantiate(this);
                if (DEBUG) {
                    Log.v(TAG, "restoreAllState: back stack #" + i
                            + " (index " + bse.mIndex + "): " + bse);
                    LogWriter logw = new LogWriter(TAG);
                    PrintWriter pw = new PrintWriter(logw);
                    bse.dump("  ", pw, false);
                    pw.close();
                }
                mBackStack.add(bse);
                if (bse.mIndex >= 0) {
                    setBackStackIndex(bse.mIndex, bse);
                }
            }
        } else {
            mBackStack = null;
        }
    }

    public void attachController(MyFragmentHostCallBack host,
                                 FragmentContainer container, MyFragment parent) {
        if (mHost != null) throw new IllegalStateException("Already attached");
        mHost = host;
        mContainer = container;
        mParent = parent;
    }

    public void noteStateNotSaved() {
        mStateSaved = false;
    }

    public void dispatchCreate() {
        mStateSaved = false;
        mExecutingActions = true;
        moveToState(MyFragment.CREATED, false);
        mExecutingActions = false;
    }

    public void dispatchActivityCreated() {
        mStateSaved = false;
        mExecutingActions = true;
        moveToState(MyFragment.ACTIVITY_CREATED, false);
        mExecutingActions = false;
    }

    public void dispatchStart() {
        mStateSaved = false;
        mExecutingActions = true;
        moveToState(MyFragment.STARTED, false);
        mExecutingActions = false;
    }

    public void dispatchResume() {
        mStateSaved = false;
        mExecutingActions = true;
        moveToState(MyFragment.RESUMED, false);
        mExecutingActions = false;
    }

    public void dispatchPause() {
        mExecutingActions = true;
        moveToState(MyFragment.STARTED, false);
        mExecutingActions = false;
    }

    public void dispatchStop() {
        // See saveAllState() for the explanation of this.  We do this for
        // all platform versions, to keep our behavior more consistent between
        // them.
        mStateSaved = true;

        mExecutingActions = true;
        moveToState(MyFragment.STOPPED, false);
        mExecutingActions = false;
    }

    public void dispatchReallyStop() {
        mExecutingActions = true;
        moveToState(MyFragment.ACTIVITY_CREATED, false);
        mExecutingActions = false;
    }

    public void dispatchDestroyView() {
        mExecutingActions = true;
        moveToState(MyFragment.CREATED, false);
        mExecutingActions = false;
    }

    public void dispatchDestroy() {
        mDestroyed = true;
        execPendingActions();
        mExecutingActions = true;
        moveToState(MyFragment.INITIALIZING, false);
        mExecutingActions = false;
        mHost = null;
        mContainer = null;
        mParent = null;
    }

    public void dispatchMultiWindowModeChanged(boolean isInMultiWindowMode) {
        if (mAdded == null) {
            return;
        }
        for (int i = mAdded.size() - 1; i >= 0; --i) {
            final MyFragment f = mAdded.get(i);
            if (f != null) {
                f.performMultiWindowModeChanged(isInMultiWindowMode);
            }
        }
    }

    public void dispatchPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        if (mAdded == null) {
            return;
        }
        for (int i = mAdded.size() - 1; i >= 0; --i) {
            final MyFragment f = mAdded.get(i);
            if (f != null) {
                f.performPictureInPictureModeChanged(isInPictureInPictureMode);
            }
        }
    }

    public void dispatchConfigurationChanged(Configuration newConfig) {
        if (mAdded != null) {
            for (int i = 0; i < mAdded.size(); i++) {
                MyFragment f = mAdded.get(i);
                if (f != null) {
                    f.performConfigurationChanged(newConfig);
                }
            }
        }
    }

    public void dispatchLowMemory() {
        if (mAdded != null) {
            for (int i = 0; i < mAdded.size(); i++) {
                MyFragment f = mAdded.get(i);
                if (f != null) {
                    f.performLowMemory();
                }
            }
        }
    }

    public boolean dispatchCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        boolean show = false;
        ArrayList<MyFragment> newMenus = null;
        if (mAdded != null) {
            for (int i = 0; i < mAdded.size(); i++) {
                MyFragment f = mAdded.get(i);
                if (f != null) {
                    if (f.performCreateOptionsMenu(menu, inflater)) {
                        show = true;
                        if (newMenus == null) {
                            newMenus = new ArrayList<MyFragment>();
                        }
                        newMenus.add(f);
                    }
                }
            }
        }

        if (mCreatedMenus != null) {
            for (int i = 0; i < mCreatedMenus.size(); i++) {
                MyFragment f = mCreatedMenus.get(i);
                if (newMenus == null || !newMenus.contains(f)) {
                    f.onDestroyOptionsMenu();
                }
            }
        }

        mCreatedMenus = newMenus;

        return show;
    }

    public boolean dispatchPrepareOptionsMenu(Menu menu) {
        boolean show = false;
        if (mAdded != null) {
            for (int i = 0; i < mAdded.size(); i++) {
                MyFragment f = mAdded.get(i);
                if (f != null) {
                    if (f.performPrepareOptionsMenu(menu)) {
                        show = true;
                    }
                }
            }
        }
        return show;
    }

    public boolean dispatchOptionsItemSelected(MenuItem item) {
        if (mAdded != null) {
            for (int i = 0; i < mAdded.size(); i++) {
                MyFragment f = mAdded.get(i);
                if (f != null) {
                    if (f.performOptionsItemSelected(item)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean dispatchContextItemSelected(MenuItem item) {
        if (mAdded != null) {
            for (int i = 0; i < mAdded.size(); i++) {
                MyFragment f = mAdded.get(i);
                if (f != null) {
                    if (f.performContextItemSelected(item)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void dispatchOptionsMenuClosed(Menu menu) {
        if (mAdded != null) {
            for (int i = 0; i < mAdded.size(); i++) {
                MyFragment f = mAdded.get(i);
                if (f != null) {
                    f.performOptionsMenuClosed(menu);
                }
            }
        }
    }

    public void registerFragmentLifecycleCallbacks(FragmentLifecycleCallbacks cb,
                                                   boolean recursive) {
        if (mLifecycleCallbacks == null) {
            mLifecycleCallbacks = new CopyOnWriteArrayList<>();
        }
        mLifecycleCallbacks.add(new Pair(cb, recursive));
    }

    public void unregisterFragmentLifecycleCallbacks(FragmentLifecycleCallbacks cb) {
        if (mLifecycleCallbacks == null) {
            return;
        }

        synchronized (mLifecycleCallbacks) {
            for (int i = 0, N = mLifecycleCallbacks.size(); i < N; i++) {
                if (mLifecycleCallbacks.get(i).first == cb) {
                    mLifecycleCallbacks.remove(i);
                    break;
                }
            }
        }
    }

    void dispatchOnFragmentPreAttached(MyFragment f, Context context, boolean onlyRecursive) {
        if (mParent != null) {
            MyFragmentManagerImp parentManager = mParent.getFragmentManager();
            parentManager.dispatchOnFragmentPreAttached(f, context, true);
        }
        if (mLifecycleCallbacks == null) {
            return;
        }
        for (Pair<FragmentLifecycleCallbacks, Boolean> p : mLifecycleCallbacks) {
            if (!onlyRecursive || p.second) {
                p.first.onFragmentPreAttached(this, f, context);
            }
        }
    }

    void dispatchOnFragmentAttached(MyFragment f, Context context, boolean onlyRecursive) {
        if (mParent != null) {
            MyFragmentManagerImp parentManager = mParent.getFragmentManager();
            parentManager.dispatchOnFragmentAttached(f, context, true);
        }
        if (mLifecycleCallbacks == null) {
            return;
        }
        for (Pair<FragmentLifecycleCallbacks, Boolean> p : mLifecycleCallbacks) {
            if (!onlyRecursive || p.second) {
                p.first.onFragmentAttached(this, f, context);
            }
        }
    }

    void dispatchOnFragmentCreated(MyFragment f, Bundle savedInstanceState, boolean onlyRecursive) {
        if (mParent != null) {
            MyFragmentManagerImp parentManager = mParent.getFragmentManager();
            parentManager.dispatchOnFragmentCreated(f, savedInstanceState, true);
        }
        if (mLifecycleCallbacks == null) {
            return;
        }
        for (Pair<FragmentLifecycleCallbacks, Boolean> p : mLifecycleCallbacks) {
            if (!onlyRecursive || p.second) {
                p.first.onFragmentCreated(this, f, savedInstanceState);
            }
        }
    }

    void dispatchOnFragmentActivityCreated(MyFragment f, Bundle savedInstanceState,
                                           boolean onlyRecursive) {
        if (mParent != null) {
            MyFragmentManagerImp parentManager = mParent.getFragmentManager();
            parentManager.dispatchOnFragmentActivityCreated(f, savedInstanceState, true);
        }
        if (mLifecycleCallbacks == null) {
            return;
        }
        for (Pair<FragmentLifecycleCallbacks, Boolean> p : mLifecycleCallbacks) {
            if (!onlyRecursive || p.second) {
                p.first.onFragmentActivityCreated(this, f, savedInstanceState);
            }
        }
    }

    void dispatchOnFragmentViewCreated(MyFragment f, View v, Bundle savedInstanceState,
                                       boolean onlyRecursive) {
        if (mParent != null) {
            MyFragmentManagerImp parentManager = mParent.getFragmentManager();
            if (parentManager instanceof MyFragmentManagerImp) {
                ((MyFragmentManagerImp) parentManager)
                        .dispatchOnFragmentViewCreated(f, v, savedInstanceState, true);
            }
        }
        if (mLifecycleCallbacks == null) {
            return;
        }
        for (Pair<FragmentLifecycleCallbacks, Boolean> p : mLifecycleCallbacks) {
            if (!onlyRecursive || p.second) {
                p.first.onFragmentViewCreated(this, f, v, savedInstanceState);
            }
        }
    }

    void dispatchOnFragmentStarted(MyFragment f, boolean onlyRecursive) {
        if (mParent != null) {
            MyFragmentManagerImp parentManager = mParent.getFragmentManager();
            parentManager.dispatchOnFragmentStarted(f, true);
        }
        if (mLifecycleCallbacks == null) {
            return;
        }
        for (Pair<FragmentLifecycleCallbacks, Boolean> p : mLifecycleCallbacks) {
            if (!onlyRecursive || p.second) {
                p.first.onFragmentStarted(this, f);
            }
        }
    }

    void dispatchOnFragmentResumed(MyFragment f, boolean onlyRecursive) {
        if (mParent != null) {
            MyFragmentManagerImp parentManager = mParent.getFragmentManager();
            parentManager.dispatchOnFragmentResumed(f, true);
        }
        if (mLifecycleCallbacks == null) {
            return;
        }
        for (Pair<FragmentLifecycleCallbacks, Boolean> p : mLifecycleCallbacks) {
            if (!onlyRecursive || p.second) {
                p.first.onFragmentResumed(this, f);
            }
        }
    }

    void dispatchOnFragmentPaused(MyFragment f, boolean onlyRecursive) {
        if (mParent != null) {
            MyFragmentManagerImp parentManager = mParent.getFragmentManager();
            parentManager.dispatchOnFragmentPaused(f, true);
        }
        if (mLifecycleCallbacks == null) {
            return;
        }
        for (Pair<FragmentLifecycleCallbacks, Boolean> p : mLifecycleCallbacks) {
            if (!onlyRecursive || p.second) {
                p.first.onFragmentPaused(this, f);
            }
        }
    }

    void dispatchOnFragmentStopped(MyFragment f, boolean onlyRecursive) {
        if (mParent != null) {
            MyFragmentManagerImp parentManager = mParent.getFragmentManager();
            parentManager.dispatchOnFragmentStopped(f, true);
        }

        if (mLifecycleCallbacks == null) {
            return;
        }

        for (Pair<FragmentLifecycleCallbacks, Boolean> p
                : mLifecycleCallbacks) {
            if (!onlyRecursive || p.second) {
                p.first.onFragmentStopped(this, f);
            }
        }
    }

    void dispatchOnFragmentSaveInstanceState(MyFragment f, Bundle outState, boolean onlyRecursive) {
        if (mParent != null) {
            MyFragmentManagerImp parentManager = mParent.getFragmentManager();
            parentManager.dispatchOnFragmentSaveInstanceState(f, outState, true);

        }
        if (mLifecycleCallbacks == null) {
            return;
        }
        for (Pair<FragmentLifecycleCallbacks, Boolean> p : mLifecycleCallbacks) {
            if (!onlyRecursive || p.second) {
                p.first.onFragmentSaveInstanceState(this, f, outState);
            }
        }
    }

    void dispatchOnFragmentViewDestroyed(MyFragment f, boolean onlyRecursive) {
        if (mParent != null) {
            MyFragmentManagerImp parentManager = mParent.getFragmentManager();
            parentManager.dispatchOnFragmentViewDestroyed(f, true);
        }
        if (mLifecycleCallbacks == null) {
            return;
        }
        for (Pair<FragmentLifecycleCallbacks, Boolean> p : mLifecycleCallbacks) {
            if (!onlyRecursive || p.second) {
                p.first.onFragmentViewDestroyed(this, f);
            }
        }
    }

    void dispatchOnFragmentDestroyed(MyFragment f, boolean onlyRecursive) {
        if (mParent != null) {
            MyFragmentManagerImp parentManager = mParent.getFragmentManager();
            parentManager.dispatchOnFragmentDestroyed(f, true);
        }
        if (mLifecycleCallbacks == null) {
            return;
        }
        for (Pair<FragmentLifecycleCallbacks, Boolean> p : mLifecycleCallbacks) {
            if (!onlyRecursive || p.second) {
                p.first.onFragmentDestroyed(this, f);
            }
        }
    }

    void dispatchOnFragmentDetached(MyFragment f, boolean onlyRecursive) {
        if (mParent != null) {
            MyFragmentManagerImp parentManager = mParent.getFragmentManager();
            parentManager.dispatchOnFragmentDetached(f, true);
        }
        if (mLifecycleCallbacks == null) {
            return;
        }
        for (Pair<FragmentLifecycleCallbacks, Boolean> p : mLifecycleCallbacks) {
            if (!onlyRecursive || p.second) {
                p.first.onFragmentDetached(this, f);
            }
        }
    }

    public static int reverseTransit(int transit) {
        int rev = 0;
        switch (transit) {
            case FragmentTransaction.TRANSIT_FRAGMENT_OPEN:
                rev = FragmentTransaction.TRANSIT_FRAGMENT_CLOSE;
                break;
            case FragmentTransaction.TRANSIT_FRAGMENT_CLOSE:
                rev = FragmentTransaction.TRANSIT_FRAGMENT_OPEN;
                break;
            case FragmentTransaction.TRANSIT_FRAGMENT_FADE:
                rev = FragmentTransaction.TRANSIT_FRAGMENT_FADE;
                break;
        }
        return rev;

    }

    public static final int ANIM_STYLE_OPEN_ENTER = 1;
    public static final int ANIM_STYLE_OPEN_EXIT = 2;
    public static final int ANIM_STYLE_CLOSE_ENTER = 3;
    public static final int ANIM_STYLE_CLOSE_EXIT = 4;
    public static final int ANIM_STYLE_FADE_ENTER = 5;
    public static final int ANIM_STYLE_FADE_EXIT = 6;

    public static int transitToStyleIndex(int transit, boolean enter) {
        int animAttr = -1;
        switch (transit) {
            case FragmentTransaction.TRANSIT_FRAGMENT_OPEN:
                animAttr = enter ? ANIM_STYLE_OPEN_ENTER : ANIM_STYLE_OPEN_EXIT;
                break;
            case FragmentTransaction.TRANSIT_FRAGMENT_CLOSE:
                animAttr = enter ? ANIM_STYLE_CLOSE_ENTER : ANIM_STYLE_CLOSE_EXIT;
                break;
            case FragmentTransaction.TRANSIT_FRAGMENT_FADE:
                animAttr = enter ? ANIM_STYLE_FADE_ENTER : ANIM_STYLE_FADE_EXIT;
                break;
        }
        return animAttr;
    }

    @Override
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        if (!"fragment".equals(name)) {
            return null;
        }

        String fname = attrs.getAttributeValue(null, "class");
        TypedArray a = context.obtainStyledAttributes(attrs, FragmentTag.MyFragment);
        if (fname == null) {
            fname = a.getString(FragmentTag.Fragment_name);
        }
        int id = a.getResourceId(FragmentTag.Fragment_id, View.NO_ID);
        String tag = a.getString(FragmentTag.Fragment_tag);
        a.recycle();

        if (!MyFragment.isSupportFragmentClass(mHost.getContext(), fname)) {
            // Invalid support lib fragment; let the device's framework handle it.
            // This will allow android.app.Fragments to do the right thing.
            return null;
        }

        int containerId = parent != null ? parent.getId() : 0;
        if (containerId == View.NO_ID && id == View.NO_ID && tag == null) {
            throw new IllegalArgumentException(attrs.getPositionDescription()
                    + ": Must specify unique android:id, android:tag, or have a parent with an id for " + fname);
        }

        // If we restored from a previous state, we may already have
        // instantiated this fragment from the state and should use
        // that instance instead of making a new one.
        MyFragment fragment = id != View.NO_ID ? findFragmentById(id) : null;
        if (fragment == null && tag != null) {
            fragment = findFragmentByTag(tag);
        }
        if (fragment == null && containerId != View.NO_ID) {
            fragment = findFragmentById(containerId);
        }

        if (DEBUG) Log.v(TAG, "onCreateView: id=0x"
                + Integer.toHexString(id) + " fname=" + fname
                + " existing=" + fragment);
        if (fragment == null) {
            fragment = MyFragment.instantiate(context, fname);
            fragment.mFromLayout = true;
            fragment.mFragmentId = id != 0 ? id : containerId;
            fragment.mContainerId = containerId;
            fragment.mTag = tag;
            fragment.mInLayout = true;
            fragment.mFragmentManager = this;
            fragment.mHost = mHost;
            fragment.onInflate(mHost.getContext(), attrs, fragment.mSavedFragmentState);
            addFragment(fragment, true);

        } else if (fragment.mInLayout) {
            // A fragment already exists and it is not one we restored from
            // previous state.
            throw new IllegalArgumentException(attrs.getPositionDescription()
                    + ": Duplicate id 0x" + Integer.toHexString(id)
                    + ", tag " + tag + ", or parent id 0x" + Integer.toHexString(containerId)
                    + " with another fragment for " + fname);
        } else {
            // This fragment was retained from a previous instance; get it
            // going now.
            fragment.mInLayout = true;
            fragment.mHost = mHost;
            // If this fragment is newly instantiated (either right now, or
            // from last saved state), then give it the attributes to
            // initialize itself.
            if (!fragment.mRetaining) {
                fragment.onInflate(mHost.getContext(), attrs, fragment.mSavedFragmentState);
            }
        }

        // If we haven't finished entering the CREATED state ourselves yet,
        // push the inflated child fragment along.
        if (mCurState < MyFragment.CREATED && fragment.mFromLayout) {
            moveToState(fragment, MyFragment.CREATED, 0, 0, false);
        } else {
            moveToState(fragment);
        }

        if (fragment.mView == null) {
            throw new IllegalStateException("MyFragment " + fname
                    + " did not create a view.");
        }
        if (id != 0) {
            fragment.mView.setId(id);
        }
        if (fragment.mView.getTag() == null) {
            fragment.mView.setTag(tag);
        }
        return fragment.mView;
    }

    LayoutInflaterFactory getLayoutInflaterFactory() {
        return this;
    }

    static class FragmentTag {
        public static final int[] MyFragment = {
                0x01010003, 0x010100d0, 0x010100d1
        };
        public static final int Fragment_id = 1;
        public static final int Fragment_name = 0;
        public static final int Fragment_tag = 2;
    }

    /**
     * An add or pop transaction to be scheduled for the UI thread.
     */
    interface OpGenerator {
        /**
         * Generate transactions to add to {@code records} and whether or not the transaction is
         * an add or pop to {@code isRecordPop}.
         * <p>
         * records and isRecordPop must be added equally so that each transaction in records
         * matches the boolean for whether or not it is a pop in isRecordPop.
         *
         * @param records     A list to add transactions to.
         * @param isRecordPop A list to add whether or not the transactions added to records is
         *                    a pop transaction.
         * @return true if something was added or false otherwise.
         */
        boolean generateOps(ArrayList<MyBackStackRecord> records, ArrayList<Boolean> isRecordPop);
    }

    /**
     * A pop operation OpGenerator. This will be run on the UI thread and will generate the
     * transactions that will be popped if anything can be popped.
     */
    private class PopBackStackState implements OpGenerator {
        final String mName;
        final int mId;
        final int mFlags;

        PopBackStackState(String name, int id, int flags) {
            mName = name;
            mId = id;
            mFlags = flags;
        }

        @Override
        public boolean generateOps(ArrayList<MyBackStackRecord> records,
                                   ArrayList<Boolean> isRecordPop) {
            return popBackStackState(records, isRecordPop, mName, mId, mFlags);
        }
    }

    /**
     * A listener for a postponed transaction. This waits until
     * {@link MyFragment#startPostponedEnterTransition()} is called or a transaction is started
     * that interacts with this one, based on interactions with the fragment container.
     */
    static class StartEnterTransitionListener
            implements MyFragment.OnStartEnterTransitionListener {
        private final boolean mIsBack;
        private final MyBackStackRecord mRecord;
        private int mNumPostponed;

        StartEnterTransitionListener(MyBackStackRecord record, boolean isBack) {
            mIsBack = isBack;
            mRecord = record;
        }

        /**
         * Called from {@link MyFragment#startPostponedEnterTransition()}, this decreases the
         * number of Fragments that are postponed. This may cause the transaction to schedule
         * to finish running and run transitions and animations.
         */
        @Override
        public void onStartEnterTransition() {
            mNumPostponed--;
            if (mNumPostponed != 0) {
                return;
            }
            mRecord.mManager.scheduleCommit();
        }

        /**
         * Called from {@link MyFragment#
         * setOnStartEnterTransitionListener(MyFragment.OnStartEnterTransitionListener)}, this
         * increases the number of fragments that are postponed as part of this transaction.
         */
        @Override
        public void startListening() {
            mNumPostponed++;
        }

        /**
         * @return true if there are no more postponed fragments as part of the transaction.
         */
        public boolean isReady() {
            return mNumPostponed == 0;
        }

        /**
         * Completes the transaction and start the animations and transitions. This may skip
         * the transitions if this is called before all fragments have called
         * {@link MyFragment#startPostponedEnterTransition()}.
         */
        public void completeTransaction() {
            final boolean canceled;
            canceled = mNumPostponed > 0;
            MyFragmentManagerImp manager = mRecord.mManager;
            final int numAdded = manager.mAdded.size();
            for (int i = 0; i < numAdded; i++) {
                final MyFragment fragment = manager.mAdded.get(i);
                fragment.setOnStartEnterTransitionListener(null);
                if (canceled && fragment.isPostponed()) {
                    fragment.startPostponedEnterTransition();
                }
            }
            mRecord.mManager.completeExecute(mRecord, mIsBack, !canceled, true);
        }

        /**
         * Cancels this transaction instead of completing it. That means that the state isn't
         * changed, so the pop results in no change to the state.
         */
        public void cancelTransaction() {
            mRecord.mManager.completeExecute(mRecord, mIsBack, false, false);
        }
    }



}

final class FragmentManagerState implements Parcelable {
    FragmentState[] mActive;
    int[] mAdded;
    BackStackState[] mBackStack;

    public FragmentManagerState() {
    }

    public FragmentManagerState(Parcel in) {
        mActive = in.createTypedArray(FragmentState.CREATOR);
        mAdded = in.createIntArray();
        mBackStack = in.createTypedArray(BackStackState.CREATOR);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedArray(mActive, flags);
        dest.writeIntArray(mAdded);
        dest.writeTypedArray(mBackStack, flags);
    }

    public static final Parcelable.Creator<FragmentManagerState> CREATOR
            = new Parcelable.Creator<FragmentManagerState>() {
        @Override
        public FragmentManagerState createFromParcel(Parcel in) {
            return new FragmentManagerState(in);
        }

        @Override
        public FragmentManagerState[] newArray(int size) {
            return new FragmentManagerState[size];
        }
    };
}