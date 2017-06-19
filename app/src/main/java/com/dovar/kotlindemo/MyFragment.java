package com.dovar.kotlindemo;

import android.app.Activity;
import android.content.ComponentCallbacks;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentContainer;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.content.Loader;
import android.support.v4.util.DebugUtils;
import android.support.v4.util.SimpleArrayMap;
import android.support.v4.util.SparseArrayCompat;
import android.support.v4.view.LayoutInflaterCompat;
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
import android.view.animation.Animation;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.Modifier;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

/**
 * Created by heweizong on 2017/5/19.
 */

public class MyFragment implements ComponentCallbacks {

    private static final SimpleArrayMap<String, Class<?>> sClassMap =
            new SimpleArrayMap<String, Class<?>>();

    static final Object USE_DEFAULT_TRANSITION = new Object();

    static final int INITIALIZING = 0;     // Not yet created.
    static final int CREATED = 1;          // Created.
    static final int ACTIVITY_CREATED = 2; // The activity has finished its creation.
    static final int STOPPED = 3;          // Fully created, not started.
    static final int STARTED = 4;          // Created and started, not resumed.
    static final int RESUMED = 5;          // Created started and resumed.

    int mState = INITIALIZING;

    // When instantiated from saved state, this is the saved state.
    Bundle mSavedFragmentState;
    SparseArray<Parcelable> mSavedViewState;

    // Index into active fragment array.
    int mIndex = -1;

    // Internal unique name for this fragment;
    String mWho;

    // Construction arguments;
    Bundle mArguments;

    // Target fragment.
    MyFragment mTarget;

    // For use when retaining a fragment: this is the index of the last mTarget.
    int mTargetIndex = -1;

    // Target request code.
    int mTargetRequestCode;

    // True if the fragment is in the list of added fragments.
    boolean mAdded;

    // If set this fragment is being removed from its activity.
    boolean mRemoving;

    // Set to true if this fragment was instantiated from a layout file.
    boolean mFromLayout;

    // Set to true when the view has actually been inflated in its layout.
    boolean mInLayout;

    // True if this fragment has been restored from previously saved state.
    boolean mRestored;

    // Number of active back stack entries this fragment is in.
    int mBackStackNesting;

    // The fragment manager we are associated with.  Set as soon as the
    // fragment is used in a transaction; cleared after it has been removed
    // from all transactions.
    MyFragmentManagerImp mFragmentManager;

    // Host this fragment is attached to.
    MyFragmentHostCallBack mHost;

    // Private fragment manager for child fragments inside of this one.
    MyFragmentManagerImp mChildFragmentManager;

    // For use when restoring fragment state and descendant fragments are retained.
    // This state is set by FragmentState.instantiate and cleared in onCreate.
    FragmentManagerNonConfit mChildNonConfig;

    // If this Fragment is contained in another Fragment, this is that container.
    MyFragment mParentFragment;

    // The optional identifier for this fragment -- either the container ID if it
    // was dynamically added to the view hierarchy, or the ID supplied in
    // layout.
    int mFragmentId;

    // When a fragment is being dynamically added to the view hierarchy, this
    // is the identifier of the parent container it is being added to.
    int mContainerId;

    // The optional named tag for this fragment -- usually used to find
    // fragments that are not part of the layout.
    String mTag;

    // Set to true when the app has requested that this fragment be hidden
    // from the user.
    boolean mHidden;

    // Set to true when the app has requested that this fragment be deactivated.
    boolean mDetached;

    // If set this fragment would like its instance retained across
    // configuration changes.
    boolean mRetainInstance;

    // If set this fragment is being retained across the current config change.
    boolean mRetaining;

    // If set this fragment has menu items to contribute.
    boolean mHasMenu;

    // Set to true to allow the fragment's menu to be shown.
    boolean mMenuVisible = true;

    // Used to verify that subclasses call through to super class.
    boolean mCalled;

    // The parent container of the fragment after dynamically added to UI.
    ViewGroup mContainer;

    // The View generated for this fragment.
    View mView;

    // The real inner view that will save/restore state.
    View mInnerView;

    // Whether this fragment should defer starting until after other fragments
    // have been started and their loaders are finished.
    boolean mDeferStart;

    // Hint provided by the app that this fragment is currently visible to the user.
    boolean mUserVisibleHint = true;

    LoaderManagerImpl mLoaderManager;
    boolean mLoadersStarted;
    boolean mCheckedForLoaderManager;

    // The animation and transition information for the fragment. This will be null
    // unless the elements are explicitly accessed and should remain null for Fragments
    // without Views.
    AnimationInfo mAnimationInfo;

    // True if the View was added, and its animation has yet to be run. This could
    // also indicate that the fragment view hasn't been made visible, even if there is no
    // animation for this fragment.
    boolean mIsNewlyAdded;

    // True if mHidden has been changed and the animation should be scheduled.
    boolean mHiddenChanged;

    // The alpha of the view when the view was added and then postponed. If the value is less
    // than zero, this means that the view's add was canceled and should not participate in
    // removal animations.
    float mPostponedAlpha;

    /**
     * State information that has been retrieved from a fragment instance
     * through {@link FragmentManager#saveFragmentInstanceState(Fragment)
     * FragmentManager.saveFragmentInstanceState}.
     */
    public static class SavedState implements Parcelable {
        final Bundle mState;

        SavedState(Bundle state) {
            mState = state;
        }

        SavedState(Parcel in, ClassLoader loader) {
            mState = in.readBundle();
            if (loader != null && mState != null) {
                mState.setClassLoader(loader);
            }
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeBundle(mState);
        }

        public static final Parcelable.Creator<SavedState> CREATOR
                = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in, null);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

//    /**
//     * Thrown by {@link Fragment#instantiate(Context, String, Bundle)} when
//     * there is an instantiation failure.
//     */
//    static public class InstantiationException extends RuntimeException {
//        public InstantiationException(String msg, Exception cause) {
//            super(msg, cause);
//        }
//    }

    /**
     * Default constructor.  <strong>Every</strong> fragment must have an
     * empty constructor, so it can be instantiated when restoring its
     * activity's state.  It is strongly recommended that subclasses do not
     * have other constructors with parameters, since these constructors
     * will not be called when the fragment is re-instantiated; instead,
     * arguments can be supplied by the caller with {@link #setArguments}
     * and later retrieved by the Fragment with {@link #getArguments}.
     *
     * <p>Applications should generally not implement a constructor. Prefer
     * {@link #onAttach(Context)} instead. It is the first place application code can run where
     * the fragment is ready to be used - the point where the fragment is actually associated with
     * its context. Some applications may also want to implement {@link #onInflate} to retrieve
     * attributes from a layout resource, although note this happens when the fragment is attached.
     */
    public MyFragment() {
    }

    /**
     * Like {@link #instantiate(Context, String, Bundle)} but with a null
     * argument Bundle.
     */
    public static MyFragment instantiate(Context context, String fname) {
        return instantiate(context, fname, null);
    }

    /**
     * Create a new instance of a Fragment with the given class name.  This is
     * the same as calling its empty constructor.
     *
     * @param context The calling context being used to instantiate the fragment.
     * This is currently just used to get its ClassLoader.
     * @param fname The class name of the fragment to instantiate.
     * @param args Bundle of arguments to supply to the fragment, which it
     * can retrieve with {@link #getArguments()}.  May be null.
     * @return Returns a new fragment instance.
     * @throws Fragment.InstantiationException If there is a failure in instantiating
     * the given fragment class.  This is a runtime exception; it is not
     * normally expected to happen.
     */
    public static MyFragment instantiate(Context context, String fname, @Nullable Bundle args) {
        try {
            Class<?> clazz = sClassMap.get(fname);
            if (clazz == null) {
                // Class not found in the cache, see if it's real, and try to add it
                clazz = context.getClassLoader().loadClass(fname);
                sClassMap.put(fname, clazz);
            }
            MyFragment f = (MyFragment)clazz.newInstance();
            if (args != null) {
                args.setClassLoader(f.getClass().getClassLoader());
                f.mArguments = args;
            }
            return f;
        } catch (ClassNotFoundException e) {
            throw new Fragment.InstantiationException("Unable to instantiate fragment " + fname
                    + ": make sure class name exists, is public, and has an"
                    + " empty constructor that is public", e);
        } catch (java.lang.InstantiationException e) {
            throw new Fragment.InstantiationException("Unable to instantiate fragment " + fname
                    + ": make sure class name exists, is public, and has an"
                    + " empty constructor that is public", e);
        } catch (IllegalAccessException e) {
            throw new Fragment.InstantiationException("Unable to instantiate fragment " + fname
                    + ": make sure class name exists, is public, and has an"
                    + " empty constructor that is public", e);
        }
    }

    /**
     * Determine if the given fragment name is a support library fragment class.
     *
     * @param context Context used to determine the correct ClassLoader to use
     * @param fname Class name of the fragment to test
     * @return true if <code>fname</code> is <code>android.support.v4.app.Fragment</code>
     *         or a subclass, false otherwise.
     */
    static boolean isSupportFragmentClass(Context context, String fname) {
        try {
            Class<?> clazz = sClassMap.get(fname);
            if (clazz == null) {
                // Class not found in the cache, see if it's real, and try to add it
                clazz = context.getClassLoader().loadClass(fname);
                sClassMap.put(fname, clazz);
            }
            return Fragment.class.isAssignableFrom(clazz);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    final void restoreViewState(Bundle savedInstanceState) {
        if (mSavedViewState != null) {
            mInnerView.restoreHierarchyState(mSavedViewState);
            mSavedViewState = null;
        }
        mCalled = false;
        onViewStateRestored(savedInstanceState);
        if (!mCalled) {
            throw new AndroidRuntimeException("Fragment " + this
                    + " did not call through to super.onViewStateRestored()");
        }
    }

    final void setIndex(int index, MyFragment parent) {
        mIndex = index;
        if (parent != null) {
            mWho = parent.mWho + ":" + mIndex;
        } else {
            mWho = "android:fragment:" + mIndex;
        }
    }

    final boolean isInBackStack() {
        return mBackStackNesting > 0;
    }

    /**
     * Subclasses can not override equals().
     */
    @Override final public boolean equals(Object o) {
        return super.equals(o);
    }

    /**
     * Subclasses can not override hashCode().
     */
    @Override final public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        DebugUtils.buildShortClassTag(this, sb);
        if (mIndex >= 0) {
            sb.append(" #");
            sb.append(mIndex);
        }
        if (mFragmentId != 0) {
            sb.append(" id=0x");
            sb.append(Integer.toHexString(mFragmentId));
        }
        if (mTag != null) {
            sb.append(" ");
            sb.append(mTag);
        }
        sb.append('}');
        return sb.toString();
    }

    /**
     * Return the identifier this fragment is known by.  This is either
     * the android:id value supplied in a layout or the container view ID
     * supplied when adding the fragment.
     */
    final public int getId() {
        return mFragmentId;
    }

    /**
     * Get the tag name of the fragment, if specified.
     */
    final public String getTag() {
        return mTag;
    }

    /**
     * Supply the construction arguments for this fragment.  This can only
     * be called before the fragment has been attached to its activity; that
     * is, you should call it immediately after constructing the fragment.  The
     * arguments supplied here will be retained across fragment destroy and
     * creation.
     */
    public void setArguments(Bundle args) {
        if (mIndex >= 0) {
            throw new IllegalStateException("Fragment already active");
        }
        mArguments = args;
    }

    /**
     * Return the arguments supplied when the fragment was instantiated,
     * if any.
     */
    final public Bundle getArguments() {
        return mArguments;
    }

    /**
     * Set the initial saved state that this Fragment should restore itself
     * from when first being constructed, as returned by
     * {@link FragmentManager#saveFragmentInstanceState(Fragment)
     * FragmentManager.saveFragmentInstanceState}.
     *
     * @param state The state the fragment should be restored from.
     */
    public void setInitialSavedState(SavedState state) {
        if (mIndex >= 0) {
            throw new IllegalStateException("Fragment already active");
        }
        mSavedFragmentState = state != null && state.mState != null
                ? state.mState : null;
    }

    /**
     * Optional target for this fragment.  This may be used, for example,
     * if this fragment is being started by another, and when done wants to
     * give a result back to the first.  The target set here is retained
     * across instances via {@link FragmentManager#putFragment
     * FragmentManager.putFragment()}.
     *
     * @param fragment The fragment that is the target of this one.
     * @param requestCode Optional request code, for convenience if you
     * are going to call back with {@link #onActivityResult(int, int, Intent)}.
     */
    public void setTargetFragment(MyFragment fragment, int requestCode) {
        mTarget = fragment;
        mTargetRequestCode = requestCode;
    }

    /**
     * Return the target fragment set by {@link #setTargetFragment}.
     */
    final public MyFragment getTargetFragment() {
        return mTarget;
    }

    /**
     * Return the target request code set by {@link #setTargetFragment}.
     */
    final public int getTargetRequestCode() {
        return mTargetRequestCode;
    }

    /**
     * Return the {@link Context} this fragment is currently associated with.
     */
    public Context getContext() {
        return mHost == null ? null : mHost.getContext();
    }

    /**
     * Return the {@link FragmentActivity} this fragment is currently associated with.
     * May return {@code null} if the fragment is associated with a {@link Context}
     * instead.
     */
    final public FragmentActivity getActivity() {
        return mHost == null ? null : (FragmentActivity) mHost.getActivity();
    }

    /**
     * Return the host object of this fragment. May return {@code null} if the fragment
     * isn't currently being hosted.
     */
    final public Object getHost() {
        return mHost == null ? null : mHost.onGetHost();
    }

    /**
     * Return <code>getActivity().getResources()</code>.
     */
    final public Resources getResources() {
        if (mHost == null) {
            throw new IllegalStateException("Fragment " + this + " not attached to Activity");
        }
        return mHost.getContext().getResources();
    }

    /**
     * Return a localized, styled CharSequence from the application's package's
     * default string table.
     *
     * @param resId Resource id for the CharSequence text
     */
    public final CharSequence getText(@StringRes int resId) {
        return getResources().getText(resId);
    }

    /**
     * Return a localized string from the application's package's
     * default string table.
     *
     * @param resId Resource id for the string
     */
    public final String getString(@StringRes int resId) {
        return getResources().getString(resId);
    }

    /**
     * Return a localized formatted string from the application's package's
     * default string table, substituting the format arguments as defined in
     * {@link java.util.Formatter} and {@link java.lang.String#format}.
     *
     * @param resId Resource id for the format string
     * @param formatArgs The format arguments that will be used for substitution.
     */

    public final String getString(@StringRes int resId, Object... formatArgs) {
        return getResources().getString(resId, formatArgs);
    }

    /**
     * Return the FragmentManager for interacting with fragments associated
     * with this fragment's activity.  Note that this will be non-null slightly
     * before {@link #getActivity()}, during the time from when the fragment is
     * placed in a {@link FragmentTransaction} until it is committed and
     * attached to its activity.
     *
     * <p>If this Fragment is a child of another Fragment, the FragmentManager
     * returned here will be the parent's {@link #getChildFragmentManager()}.
     */
    final public MyFragmentManagerImp getFragmentManager() {
        return mFragmentManager;
    }

    /**
     * Return a private FragmentManager for placing and managing Fragments
     * inside of this Fragment.
     */
    final public MyFragmentManagerImp getChildFragmentManager() {
        if (mChildFragmentManager == null) {
            instantiateChildFragmentManager();
            if (mState >= RESUMED) {
                mChildFragmentManager.dispatchResume();
            } else if (mState >= STARTED) {
                mChildFragmentManager.dispatchStart();
            } else if (mState >= ACTIVITY_CREATED) {
                mChildFragmentManager.dispatchActivityCreated();
            } else if (mState >= CREATED) {
                mChildFragmentManager.dispatchCreate();
            }
        }
        return mChildFragmentManager;
    }

    /**
     * Return this fragment's child FragmentManager one has been previously created,
     * otherwise null.
     */
    MyFragmentManagerImp peekChildFragmentManager() {
        return mChildFragmentManager;
    }

    /**
     * Returns the parent Fragment containing this Fragment.  If this Fragment
     * is attached directly to an Activity, returns null.
     */
    final public MyFragment getParentFragment() {
        return mParentFragment;
    }

    /**
     * Return true if the fragment is currently added to its activity.
     */
    final public boolean isAdded() {
        return mHost != null && mAdded;
    }

    /**
     * Return true if the fragment has been explicitly detached from the UI.
     * That is, {@link FragmentTransaction#detach(Fragment)
     * FragmentTransaction.detach(Fragment)} has been used on it.
     */
    final public boolean isDetached() {
        return mDetached;
    }

    /**
     * Return true if this fragment is currently being removed from its
     * activity.  This is  <em>not</em> whether its activity is finishing, but
     * rather whether it is in the process of being removed from its activity.
     */
    final public boolean isRemoving() {
        return mRemoving;
    }

    /**
     * Return true if the layout is included as part of an activity view
     * hierarchy via the &lt;fragment&gt; tag.  This will always be true when
     * fragments are created through the &lt;fragment&gt; tag, <em>except</em>
     * in the case where an old fragment is restored from a previous state and
     * it does not appear in the layout of the current state.
     */
    final public boolean isInLayout() {
        return mInLayout;
    }

    /**
     * Return true if the fragment is in the resumed state.  This is true
     * for the duration of {@link #onResume()} and {@link #onPause()} as well.
     */
    final public boolean isResumed() {
        return mState >= RESUMED;
    }

    /**
     * Return true if the fragment is currently visible to the user.  This means
     * it: (1) has been added, (2) has its view attached to the window, and
     * (3) is not hidden.
     */
    final public boolean isVisible() {
        return isAdded() && !isHidden() && mView != null
                && mView.getWindowToken() != null && mView.getVisibility() == View.VISIBLE;
    }

    /**
     * Return true if the fragment has been hidden.  By default fragments
     * are shown.  You can find out about changes to this state with
     * {@link #onHiddenChanged}.  Note that the hidden state is orthogonal
     * to other states -- that is, to be visible to the user, a fragment
     * must be both started and not hidden.
     */
    final public boolean isHidden() {
        return mHidden;
    }

    /** @hide */
    @RestrictTo(LIBRARY_GROUP)
    final public boolean hasOptionsMenu() {
        return mHasMenu;
    }

    /** @hide */
    @RestrictTo(LIBRARY_GROUP)
    final public boolean isMenuVisible() {
        return mMenuVisible;
    }

    /**
     * Called when the hidden state (as returned by {@link #isHidden()} of
     * the fragment has changed.  Fragments start out not hidden; this will
     * be called whenever the fragment changes state from that.
     * @param hidden True if the fragment is now hidden, false otherwise.
     */
    public void onHiddenChanged(boolean hidden) {
    }

    /**
     * Control whether a fragment instance is retained across Activity
     * re-creation (such as from a configuration change).  This can only
     * be used with fragments not in the back stack.  If set, the fragment
     * lifecycle will be slightly different when an activity is recreated:
     * <ul>
     * <li> {@link #onDestroy()} will not be called (but {@link #onDetach()} still
     * will be, because the fragment is being detached from its current activity).
     * <li> {@link #onCreate(Bundle)} will not be called since the fragment
     * is not being re-created.
     * <li> {@link #onAttach(Activity)} and {@link #onActivityCreated(Bundle)} <b>will</b>
     * still be called.
     * </ul>
     */
    public void setRetainInstance(boolean retain) {
        mRetainInstance = retain;
    }

    final public boolean getRetainInstance() {
        return mRetainInstance;
    }

    /**
     * Report that this fragment would like to participate in populating
     * the options menu by receiving a call to {@link #onCreateOptionsMenu}
     * and related methods.
     *
     * @param hasMenu If true, the fragment has menu items to contribute.
     */
    public void setHasOptionsMenu(boolean hasMenu) {
        if (mHasMenu != hasMenu) {
            mHasMenu = hasMenu;
            if (isAdded() && !isHidden()) {
                mHost.onSupportInvalidateOptionsMenu();
            }
        }
    }

    /**
     * Set a hint for whether this fragment's menu should be visible.  This
     * is useful if you know that a fragment has been placed in your view
     * hierarchy so that the user can not currently seen it, so any menu items
     * it has should also not be shown.
     *
     * @param menuVisible The default is true, meaning the fragment's menu will
     * be shown as usual.  If false, the user will not see the menu.
     */
    public void setMenuVisibility(boolean menuVisible) {
        if (mMenuVisible != menuVisible) {
            mMenuVisible = menuVisible;
            if (mHasMenu && isAdded() && !isHidden()) {
                mHost.onSupportInvalidateOptionsMenu();
            }
        }
    }

    /**
     * Set a hint to the system about whether this fragment's UI is currently visible
     * to the user. This hint defaults to true and is persistent across fragment instance
     * state save and restore.
     *
     * <p>An app may set this to false to indicate that the fragment's UI is
     * scrolled out of visibility or is otherwise not directly visible to the user.
     * This may be used by the system to prioritize operations such as fragment lifecycle updates
     * or loader ordering behavior.</p>
     *
     * <p><strong>Note:</strong> This method may be called outside of the fragment lifecycle.
     * and thus has no ordering guarantees with regard to fragment lifecycle method calls.</p>
     *
     * @param isVisibleToUser true if this fragment's UI is currently visible to the user (default),
     *                        false if it is not.
     */
    public void setUserVisibleHint(boolean isVisibleToUser) {
        if (!mUserVisibleHint && isVisibleToUser && mState < STARTED
                && mFragmentManager != null && isAdded()) {
            mFragmentManager.performPendingDeferredStart(this);
        }
        mUserVisibleHint = isVisibleToUser;
        mDeferStart = mState < STARTED && !isVisibleToUser;
    }

    /**
     * @return The current value of the user-visible hint on this fragment.
     * @see #setUserVisibleHint(boolean)
     */
    public boolean getUserVisibleHint() {
        return mUserVisibleHint;
    }

    /**
     * Return the LoaderManager for this fragment, creating it if needed.
     */
    public LoaderManager getLoaderManager() {
        if (mLoaderManager != null) {
            return mLoaderManager;
        }
        if (mHost == null) {
            throw new IllegalStateException("Fragment " + this + " not attached to Activity");
        }
        mCheckedForLoaderManager = true;
        mLoaderManager = mHost.getLoaderManager(mWho, mLoadersStarted, true);
        return mLoaderManager;
    }

    /**
     * Call {@link Activity#startActivity(Intent)} from the fragment's
     * containing Activity.
     */
    public void startActivity(Intent intent) {
        startActivity(intent, null);
    }

    /**
     * Call {@link Activity#startActivity(Intent, Bundle)} from the fragment's
     * containing Activity.
     */
    public void startActivity(Intent intent, @Nullable Bundle options) {
        if (mHost == null) {
            throw new IllegalStateException("Fragment " + this + " not attached to Activity");
        }
        mHost.onStartActivityFromFragment(this /*fragment*/, intent, -1, options);
    }

    /**
     * Call {@link Activity#startActivityForResult(Intent, int)} from the fragment's
     * containing Activity.
     */
    public void startActivityForResult(Intent intent, int requestCode) {
        startActivityForResult(intent, requestCode, null);
    }

    /**
     * Call {@link Activity#startActivityForResult(Intent, int, Bundle)} from the fragment's
     * containing Activity.
     */
    public void startActivityForResult(Intent intent, int requestCode, @Nullable Bundle options) {
        if (mHost == null) {
            throw new IllegalStateException("Fragment " + this + " not attached to Activity");
        }
        mHost.onStartActivityFromFragment(this /*fragment*/, intent, requestCode, options);
    }

    /**
     * Call {@link Activity#startIntentSenderForResult(IntentSender, int, Intent, int, int, int,
     * Bundle)} from the fragment's containing Activity.
     */
    public void startIntentSenderForResult(IntentSender intent, int requestCode,
                                           @Nullable Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags,
                                           Bundle options) throws IntentSender.SendIntentException {
        if (mHost == null) {
            throw new IllegalStateException("Fragment " + this + " not attached to Activity");
        }
        mHost.onStartIntentSenderFromFragment(this, intent, requestCode, fillInIntent, flagsMask,
                flagsValues, extraFlags, options);
    }

    /**
     * Receive the result from a previous call to
     * {@link #startActivityForResult(Intent, int)}.  This follows the
     * related Activity API as described there in
     * {@link Activity#onActivityResult(int, int, Intent)}.
     *
     * @param requestCode The integer request code originally supplied to
     *                    startActivityForResult(), allowing you to identify who this
     *                    result came from.
     * @param resultCode The integer result code returned by the child activity
     *                   through its setResult().
     * @param data An Intent, which can return result data to the caller
     *               (various data can be attached to Intent "extras").
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    }

    /**
     * Requests permissions to be granted to this application. These permissions
     * must be requested in your manifest, they should not be granted to your app,
     * and they should have protection level {@link android.content.pm.PermissionInfo
     * #PROTECTION_DANGEROUS dangerous}, regardless whether they are declared by
     * the platform or a third-party app.
     * <p>
     * Normal permissions {@link android.content.pm.PermissionInfo#PROTECTION_NORMAL}
     * are granted at install time if requested in the manifest. Signature permissions
     * {@link android.content.pm.PermissionInfo#PROTECTION_SIGNATURE} are granted at
     * install time if requested in the manifest and the signature of your app matches
     * the signature of the app declaring the permissions.
     * </p>
     * <p>
     * If your app does not have the requested permissions the user will be presented
     * with UI for accepting them. After the user has accepted or rejected the
     * requested permissions you will receive a callback on {@link
     * #onRequestPermissionsResult(int, String[], int[])} reporting whether the
     * permissions were granted or not.
     * </p>
     * <p>
     * Note that requesting a permission does not guarantee it will be granted and
     * your app should be able to run without having this permission.
     * </p>
     * <p>
     * This method may start an activity allowing the user to choose which permissions
     * to grant and which to reject. Hence, you should be prepared that your activity
     * may be paused and resumed. Further, granting some permissions may require
     * a restart of you application. In such a case, the system will recreate the
     * activity stack before delivering the result to {@link
     * #onRequestPermissionsResult(int, String[], int[])}.
     * </p>
     * <p>
     * When checking whether you have a permission you should use {@link
     * android.content.Context#checkSelfPermission(String)}.
     * </p>
     * <p>
     * Calling this API for permissions already granted to your app would show UI
     * to the user to decided whether the app can still hold these permissions. This
     * can be useful if the way your app uses the data guarded by the permissions
     * changes significantly.
     * </p>
     * <p>
     * A sample permissions request looks like this:
     * </p>
     * <code><pre><p>
     * private void showContacts() {
     *     if (getActivity().checkSelfPermission(Manifest.permission.READ_CONTACTS)
     *             != PackageManager.PERMISSION_GRANTED) {
     *         requestPermissions(new String[]{Manifest.permission.READ_CONTACTS},
     *                 PERMISSIONS_REQUEST_READ_CONTACTS);
     *     } else {
     *         doShowContacts();
     *     }
     * }
     *
     * {@literal @}Override
     * public void onRequestPermissionsResult(int requestCode, String[] permissions,
     *         int[] grantResults) {
     *     if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS
     *             && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
     *         doShowContacts();
     *     }
     * }
     * </code></pre></p>
     *
     * @param permissions The requested permissions.
     * @param requestCode Application specific request code to match with a result
     *    reported to {@link #onRequestPermissionsResult(int, String[], int[])}.
     *
     * @see #onRequestPermissionsResult(int, String[], int[])
     * @see android.content.Context#checkSelfPermission(String)
     */
    public final void requestPermissions(@NonNull String[] permissions, int requestCode) {
        if (mHost == null) {
            throw new IllegalStateException("Fragment " + this + " not attached to Activity");
        }
        mHost.onRequestPermissionsFromFragment(this, permissions, requestCode);
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on {@link #requestPermissions(String[], int)}.
     * <p>
     * <strong>Note:</strong> It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     * </p>
     *
     * @param requestCode The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *     which is either {@link android.content.pm.PackageManager#PERMISSION_GRANTED}
     *     or {@link android.content.pm.PackageManager#PERMISSION_DENIED}. Never null.
     *
     * @see #requestPermissions(String[], int)
     */
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        /* callback - do nothing */
    }

    /**
     * Gets whether you should show UI with rationale for requesting a permission.
     * You should do this only if you do not have the permission and the context in
     * which the permission is requested does not clearly communicate to the user
     * what would be the benefit from granting this permission.
     * <p>
     * For example, if you write a camera app, requesting the camera permission
     * would be expected by the user and no rationale for why it is requested is
     * needed. If however, the app needs location for tagging photos then a non-tech
     * savvy user may wonder how location is related to taking photos. In this case
     * you may choose to show UI with rationale of requesting this permission.
     * </p>
     *
     * @param permission A permission your app wants to request.
     * @return Whether you can show permission rationale UI.
     *
     * @see Context#checkSelfPermission(String)
     * @see #requestPermissions(String[], int)
     * @see #onRequestPermissionsResult(int, String[], int[])
     */
    public boolean shouldShowRequestPermissionRationale(@NonNull String permission) {
        if (mHost != null) {
            return mHost.onShouldShowRequestPermissionRationale(permission);
        }
        return false;
    }

    /**
     * Hack so that DialogFragment can make its Dialog before creating
     * its views, and the view construction can use the dialog's context for
     * inflation.  Maybe this should become a public API. Note sure.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public LayoutInflater getLayoutInflater(Bundle savedInstanceState) {
        LayoutInflater result = mHost.onGetLayoutInflater();
        getChildFragmentManager(); // Init if needed; use raw implementation below.
        LayoutInflaterCompat.setFactory(result, mChildFragmentManager.getLayoutInflaterFactory());
        return result;
    }

    /**
     * Called when a fragment is being created as part of a view layout
     * inflation, typically from setting the content view of an activity.  This
     * may be called immediately after the fragment is created from a <fragment>
     * tag in a layout file.  Note this is <em>before</em> the fragment's
     * {@link #onAttach(Activity)} has been called; all you should do here is
     * parse the attributes and save them away.
     *
     * <p>This is called every time the fragment is inflated, even if it is
     * being inflated into a new instance with saved state.  It typically makes
     * sense to re-parse the parameters each time, to allow them to change with
     * different configurations.</p>
     *
     * <p>Here is a typical implementation of a fragment that can take parameters
     * both through attributes supplied here as well from {@link #getArguments()}:</p>
     *
     * {@sample frameworks/support/samples/Support4Demos/src/com/example/android/supportv4/app/FragmentArgumentsSupport.java
     *      fragment}
     *
     * <p>Note that parsing the XML attributes uses a "styleable" resource.  The
     * declaration for the styleable used here is:</p>
     *
     * {@sample frameworks/support/samples/Support4Demos/res/values/attrs.xml fragment_arguments}
     *
     * <p>The fragment can then be declared within its activity's content layout
     * through a tag like this:</p>
     *
     * {@sample frameworks/support/samples/Support4Demos/res/layout/fragment_arguments_support.xml from_attributes}
     *
     * <p>This fragment can also be created dynamically from arguments given
     * at runtime in the arguments Bundle; here is an example of doing so at
     * creation of the containing activity:</p>
     *
     * {@sample frameworks/support/samples/Support4Demos/src/com/example/android/supportv4/app/FragmentArgumentsSupport.java
     *      create}
     *
     * @param context The Activity that is inflating this fragment.
     * @param attrs The attributes at the tag where the fragment is
     * being created.
     * @param savedInstanceState If the fragment is being re-created from
     * a previous saved state, this is the state.
     */
    @CallSuper
    public void onInflate(Context context, AttributeSet attrs, Bundle savedInstanceState) {
        mCalled = true;
        final Activity hostActivity = mHost == null ? null : mHost.getActivity();
        if (hostActivity != null) {
            mCalled = false;
            onInflate(hostActivity, attrs, savedInstanceState);
        }
    }

    /**
     * Called when a fragment is being created as part of a view layout
     * inflation, typically from setting the content view of an activity.
     *
     * @deprecated See {@link #onInflate(Context, AttributeSet, Bundle)}.
     */
    @Deprecated
    @CallSuper
    public void onInflate(Activity activity, AttributeSet attrs, Bundle savedInstanceState) {
        mCalled = true;
    }

    /**
     * Called when a fragment is attached as a child of this fragment.
     *
     * <p>This is called after the attached fragment's <code>onAttach</code> and before
     * the attached fragment's <code>onCreate</code> if the fragment has not yet had a previous
     * call to <code>onCreate</code>.</p>
     *
     * @param childFragment child fragment being attached
     */
    public void onAttachFragment(MyFragment childFragment) {
    }

    /**
     * Called when a fragment is first attached to its context.
     * {@link #onCreate(Bundle)} will be called after this.
     */
    @CallSuper
    public void onAttach(Context context) {
        mCalled = true;
        final Activity hostActivity = mHost == null ? null : mHost.getActivity();
        if (hostActivity != null) {
            mCalled = false;
            onAttach(hostActivity);
        }
    }

    /**
     * Called when a fragment is first attached to its activity.
     * {@link #onCreate(Bundle)} will be called after this.
     *
     * @deprecated See {@link #onAttach(Context)}.
     */
    @Deprecated
    @CallSuper
    public void onAttach(Activity activity) {
        mCalled = true;
    }

    /**
     * Called when a fragment loads an animation.
     */
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        return null;
    }

    /**
     * Called to do initial creation of a fragment.  This is called after
     * {@link #onAttach(Activity)} and before
     * {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     *
     * <p>Note that this can be called while the fragment's activity is
     * still in the process of being created.  As such, you can not rely
     * on things like the activity's content view hierarchy being initialized
     * at this point.  If you want to do work once the activity itself is
     * created, see {@link #onActivityCreated(Bundle)}.
     *
     * <p>Any restored child fragments will be created before the base
     * <code>Fragment.onCreate</code> method returns.</p>
     *
     * @param savedInstanceState If the fragment is being re-created from
     * a previous saved state, this is the state.
     */
    @CallSuper
    public void onCreate(@Nullable Bundle savedInstanceState) {
        mCalled = true;
        restoreChildFragmentState(savedInstanceState);
        if (mChildFragmentManager != null
                && !mChildFragmentManager.isStateAtLeast(CREATED)) {
            mChildFragmentManager.dispatchCreate();
        }
    }

    /**
     * Restore the state of the child FragmentManager. Called by either
     * {@link #onCreate(Bundle)} for non-retained instance fragments or by
     * {@link MyFragmentManagerImp#moveToState(MyFragment, int, int, int, boolean)}
     * for retained instance fragments.
     *
     * <p><strong>Postcondition:</strong> if there were child fragments to restore,
     * the child FragmentManager will be instantiated and brought to the {@link #CREATED} state.
     * </p>
     *
     * @param savedInstanceState the savedInstanceState potentially containing fragment info
     */
    void restoreChildFragmentState(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            Parcelable p = savedInstanceState.getParcelable("android:support:fragments");
            if (p != null) {
                if (mChildFragmentManager == null) {
                    instantiateChildFragmentManager();
                }
                mChildFragmentManager.restoreAllState(p, mChildNonConfig);
                mChildNonConfig = null;
                mChildFragmentManager.dispatchCreate();
            }
        }
    }

    /**
     * Called to have the fragment instantiate its user interface view.
     * This is optional, and non-graphical fragments can return null (which
     * is the default implementation).  This will be called between
     * {@link #onCreate(Bundle)} and {@link #onActivityCreated(Bundle)}.
     *
     * <p>If you return a View from here, you will later be called in
     * {@link #onDestroyView} when the view is being released.
     *
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     *
     * @return Return the View for the fragment's UI, or null.
     */
    @Nullable
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return null;
    }

    /**
     * Called immediately after {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}
     * has returned, but before any saved state has been restored in to the view.
     * This gives subclasses a chance to initialize themselves once
     * they know their view hierarchy has been completely created.  The fragment's
     * view hierarchy is not however attached to its parent at this point.
     * @param view The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    }

    /**
     * Get the root view for the fragment's layout (the one returned by {@link #onCreateView}),
     * if provided.
     *
     * @return The fragment's root view, or null if it has no layout.
     */
    @Nullable
    public View getView() {
        return mView;
    }

    /**
     * Called when the fragment's activity has been created and this
     * fragment's view hierarchy instantiated.  It can be used to do final
     * initialization once these pieces are in place, such as retrieving
     * views or restoring state.  It is also useful for fragments that use
     * {@link #setRetainInstance(boolean)} to retain their instance,
     * as this callback tells the fragment when it is fully associated with
     * the new activity instance.  This is called after {@link #onCreateView}
     * and before {@link #onViewStateRestored(Bundle)}.
     *
     * @param savedInstanceState If the fragment is being re-created from
     * a previous saved state, this is the state.
     */
    @CallSuper
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        mCalled = true;
    }

    /**
     * Called when all saved state has been restored into the view hierarchy
     * of the fragment.  This can be used to do initialization based on saved
     * state that you are letting the view hierarchy track itself, such as
     * whether check box widgets are currently checked.  This is called
     * after {@link #onActivityCreated(Bundle)} and before
     * {@link #onStart()}.
     *
     * @param savedInstanceState If the fragment is being re-created from
     * a previous saved state, this is the state.
     */
    @CallSuper
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        mCalled = true;
    }

    /**
     * Called when the Fragment is visible to the user.  This is generally
     * tied to {@link Activity#onStart() Activity.onStart} of the containing
     * Activity's lifecycle.
     */
    @CallSuper
    public void onStart() {
        mCalled = true;

        if (!mLoadersStarted) {
            mLoadersStarted = true;
            if (!mCheckedForLoaderManager) {
                mCheckedForLoaderManager = true;
                mLoaderManager = mHost.getLoaderManager(mWho, mLoadersStarted, false);
            }
            if (mLoaderManager != null) {
                mLoaderManager.doStart();
            }
        }
    }

    /**
     * Called when the fragment is visible to the user and actively running.
     * This is generally
     * tied to {@link Activity#onResume() Activity.onResume} of the containing
     * Activity's lifecycle.
     */
    @CallSuper
    public void onResume() {
        mCalled = true;
    }

    /**
     * Called to ask the fragment to save its current dynamic state, so it
     * can later be reconstructed in a new instance of its process is
     * restarted.  If a new instance of the fragment later needs to be
     * created, the data you place in the Bundle here will be available
     * in the Bundle given to {@link #onCreate(Bundle)},
     * {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}, and
     * {@link #onActivityCreated(Bundle)}.
     *
     * <p>This corresponds to {@link Activity#onSaveInstanceState(Bundle)
     * Activity.onSaveInstanceState(Bundle)} and most of the discussion there
     * applies here as well.  Note however: <em>this method may be called
     * at any time before {@link #onDestroy()}</em>.  There are many situations
     * where a fragment may be mostly torn down (such as when placed on the
     * back stack with no UI showing), but its state will not be saved until
     * its owning activity actually needs to save its state.
     *
     * @param outState Bundle in which to place your saved state.
     */
    public void onSaveInstanceState(Bundle outState) {
    }

    /**
     * Called when the Fragment's activity changes from fullscreen mode to multi-window mode and
     * visa-versa. This is generally tied to {@link Activity#onMultiWindowModeChanged} of the
     * containing Activity.
     *
     * @param isInMultiWindowMode True if the activity is in multi-window mode.
     */
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode) {
    }

    /**
     * Called by the system when the activity changes to and from picture-in-picture mode. This is
     * generally tied to {@link Activity#onPictureInPictureModeChanged} of the containing Activity.
     *
     * @param isInPictureInPictureMode True if the activity is in picture-in-picture mode.
     */
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
    }

    @Override
    @CallSuper
    public void onConfigurationChanged(Configuration newConfig) {
        mCalled = true;
    }

    /**
     * Called when the Fragment is no longer resumed.  This is generally
     * tied to {@link Activity#onPause() Activity.onPause} of the containing
     * Activity's lifecycle.
     */
    @CallSuper
    public void onPause() {
        mCalled = true;
    }

    /**
     * Called when the Fragment is no longer started.  This is generally
     * tied to {@link Activity#onStop() Activity.onStop} of the containing
     * Activity's lifecycle.
     */
    @CallSuper
    public void onStop() {
        mCalled = true;
    }

    @Override
    @CallSuper
    public void onLowMemory() {
        mCalled = true;
    }

    /**
     * Called when the view previously created by {@link #onCreateView} has
     * been detached from the fragment.  The next time the fragment needs
     * to be displayed, a new view will be created.  This is called
     * after {@link #onStop()} and before {@link #onDestroy()}.  It is called
     * <em>regardless</em> of whether {@link #onCreateView} returned a
     * non-null view.  Internally it is called after the view's state has
     * been saved but before it has been removed from its parent.
     */
    @CallSuper
    public void onDestroyView() {
        mCalled = true;
    }

    /**
     * Called when the fragment is no longer in use.  This is called
     * after {@link #onStop()} and before {@link #onDetach()}.
     */
    @CallSuper
    public void onDestroy() {
        mCalled = true;
        //Log.v("foo", "onDestroy: mCheckedForLoaderManager=" + mCheckedForLoaderManager
        //        + " mLoaderManager=" + mLoaderManager);
        if (!mCheckedForLoaderManager) {
            mCheckedForLoaderManager = true;
            mLoaderManager = mHost.getLoaderManager(mWho, mLoadersStarted, false);
        }
        if (mLoaderManager != null) {
            mLoaderManager.doDestroy();
        }
    }

    /**
     * Called by the fragment manager once this fragment has been removed,
     * so that we don't have any left-over state if the application decides
     * to re-use the instance.  This only clears state that the framework
     * internally manages, not things the application sets.
     */
    void initState() {
        mIndex = -1;
        mWho = null;
        mAdded = false;
        mRemoving = false;
        mFromLayout = false;
        mInLayout = false;
        mRestored = false;
        mBackStackNesting = 0;
        mFragmentManager = null;
        mChildFragmentManager = null;
        mHost = null;
        mFragmentId = 0;
        mContainerId = 0;
        mTag = null;
        mHidden = false;
        mDetached = false;
        mRetaining = false;
        mLoaderManager = null;
        mLoadersStarted = false;
        mCheckedForLoaderManager = false;
    }

    /**
     * Called when the fragment is no longer attached to its activity.  This
     * is called after {@link #onDestroy()}.
     */
    @CallSuper
    public void onDetach() {
        mCalled = true;
    }

    /**
     * Initialize the contents of the Fragment host's standard options menu.  You
     * should place your menu items in to <var>menu</var>.  For this method
     * to be called, you must have first called {@link #setHasOptionsMenu}.  See
     * {@link Activity#onCreateOptionsMenu(Menu) Activity.onCreateOptionsMenu}
     * for more information.
     *
     * @param menu The options menu in which you place your items.
     *
     * @see #setHasOptionsMenu
     * @see #onPrepareOptionsMenu
     * @see #onOptionsItemSelected
     */
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    }

    /**
     * Prepare the Fragment host's standard options menu to be displayed.  This is
     * called right before the menu is shown, every time it is shown.  You can
     * use this method to efficiently enable/disable items or otherwise
     * dynamically modify the contents.  See
     * {@link Activity#onPrepareOptionsMenu(Menu) Activity.onPrepareOptionsMenu}
     * for more information.
     *
     * @param menu The options menu as last shown or first initialized by
     *             onCreateOptionsMenu().
     *
     * @see #setHasOptionsMenu
     * @see #onCreateOptionsMenu
     */
    public void onPrepareOptionsMenu(Menu menu) {
    }

    /**
     * Called when this fragment's option menu items are no longer being
     * included in the overall options menu.  Receiving this call means that
     * the menu needed to be rebuilt, but this fragment's items were not
     * included in the newly built menu (its {@link #onCreateOptionsMenu(Menu, MenuInflater)}
     * was not called).
     */
    public void onDestroyOptionsMenu() {
    }

    /**
     * This hook is called whenever an item in your options menu is selected.
     * The default implementation simply returns false to have the normal
     * processing happen (calling the item's Runnable or sending a message to
     * its Handler as appropriate).  You can use this method for any items
     * for which you would like to do processing without those other
     * facilities.
     *
     * <p>Derived classes should call through to the base class for it to
     * perform the default menu handling.
     *
     * @param item The menu item that was selected.
     *
     * @return boolean Return false to allow normal menu processing to
     *         proceed, true to consume it here.
     *
     * @see #onCreateOptionsMenu
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        return false;
    }

    /**
     * This hook is called whenever the options menu is being closed (either by the user canceling
     * the menu with the back/menu button, or when an item is selected).
     *
     * @param menu The options menu as last shown or first initialized by
     *             onCreateOptionsMenu().
     */
    public void onOptionsMenuClosed(Menu menu) {
    }

    /**
     * This hook is called whenever an item in a context menu is selected. The
     * default implementation simply returns false to have the normal processing
     * happen (calling the item's Runnable or sending a message to its Handler
     * as appropriate). You can use this method for any items for which you
     * would like to do processing without those other facilities.
     * <p>
     * Use {@link MenuItem#getMenuInfo()} to get extra information set by the
     * View that added this menu item.
     * <p>
     * Derived classes should call through to the base class for it to perform
     * the default menu handling.
     *
     * @param item The context menu item that was selected.
     * @return boolean Return false to allow normal context menu processing to
     *         proceed, true to consume it here.
     */
    public boolean onContextItemSelected(MenuItem item) {
        return false;
    }

    /**
     * When custom transitions are used with Fragments, the enter transition callback
     * is called when this Fragment is attached or detached when not popping the back stack.
     *
     * @param callback Used to manipulate the shared element transitions on this Fragment
     *                 when added not as a pop from the back stack.
     */
    public void setEnterSharedElementCallback(SharedElementCallback callback) {
        ensureAnimationInfo().mEnterTransitionCallback = callback;
    }

    /**
     * When custom transitions are used with Fragments, the exit transition callback
     * is called when this Fragment is attached or detached when popping the back stack.
     *
     * @param callback Used to manipulate the shared element transitions on this Fragment
     *                 when added as a pop from the back stack.
     */
    public void setExitSharedElementCallback(SharedElementCallback callback) {
        ensureAnimationInfo().mExitTransitionCallback = callback;
    }

    /**
     * Sets the Transition that will be used to move Views into the initial scene. The entering
     * Views will be those that are regular Views or ViewGroups that have
     * {@link ViewGroup#isTransitionGroup} return true. Typical Transitions will extend
     * {@link android.transition.Visibility} as entering is governed by changing visibility from
     * {@link View#INVISIBLE} to {@link View#VISIBLE}. If <code>transition</code> is null,
     * entering Views will remain unaffected.
     *
     * @param transition The Transition to use to move Views into the initial Scene.
     */
    public void setEnterTransition(Object transition) {
        ensureAnimationInfo().mEnterTransition = transition;
    }

    /**
     * Returns the Transition that will be used to move Views into the initial scene. The entering
     * Views will be those that are regular Views or ViewGroups that have
     * {@link ViewGroup#isTransitionGroup} return true. Typical Transitions will extend
     * {@link android.transition.Visibility} as entering is governed by changing visibility from
     * {@link View#INVISIBLE} to {@link View#VISIBLE}.
     *
     * @return the Transition to use to move Views into the initial Scene.
     */
    public Object getEnterTransition() {
        if (mAnimationInfo == null) {
            return null;
        }
        return mAnimationInfo.mEnterTransition;
    }

    /**
     * Sets the Transition that will be used to move Views out of the scene when the Fragment is
     * preparing to be removed, hidden, or detached because of popping the back stack. The exiting
     * Views will be those that are regular Views or ViewGroups that have
     * {@link ViewGroup#isTransitionGroup} return true. Typical Transitions will extend
     * {@link android.transition.Visibility} as entering is governed by changing visibility from
     * {@link View#VISIBLE} to {@link View#INVISIBLE}. If <code>transition</code> is null,
     * entering Views will remain unaffected. If nothing is set, the default will be to
     * use the same value as set in {@link #setEnterTransition(Object)}.
     *
     * @param transition The Transition to use to move Views out of the Scene when the Fragment
     *                   is preparing to close. <code>transition</code> must be an
     *                   android.transition.Transition.
     */
    public void setReturnTransition(Object transition) {
        ensureAnimationInfo().mReturnTransition = transition;
    }

    /**
     * Returns the Transition that will be used to move Views out of the scene when the Fragment is
     * preparing to be removed, hidden, or detached because of popping the back stack. The exiting
     * Views will be those that are regular Views or ViewGroups that have
     * {@link ViewGroup#isTransitionGroup} return true. Typical Transitions will extend
     * {@link android.transition.Visibility} as entering is governed by changing visibility from
     * {@link View#VISIBLE} to {@link View#INVISIBLE}. If <code>transition</code> is null,
     * entering Views will remain unaffected.
     *
     * @return the Transition to use to move Views out of the Scene when the Fragment
     *         is preparing to close.
     */
    public Object getReturnTransition() {
        if (mAnimationInfo == null) {
            return null;
        }
        return mAnimationInfo.mReturnTransition == USE_DEFAULT_TRANSITION ? getEnterTransition()
                : mAnimationInfo.mReturnTransition;
    }

    /**
     * Sets the Transition that will be used to move Views out of the scene when the
     * fragment is removed, hidden, or detached when not popping the back stack.
     * The exiting Views will be those that are regular Views or ViewGroups that
     * have {@link ViewGroup#isTransitionGroup} return true. Typical Transitions will extend
     * {@link android.transition.Visibility} as exiting is governed by changing visibility
     * from {@link View#VISIBLE} to {@link View#INVISIBLE}. If transition is null, the views will
     * remain unaffected.
     *
     * @param transition The Transition to use to move Views out of the Scene when the Fragment
     *                   is being closed not due to popping the back stack. <code>transition</code>
     *                   must be an android.transition.Transition.
     */
    public void setExitTransition(Object transition) {
        ensureAnimationInfo().mExitTransition = transition;
    }

    /**
     * Returns the Transition that will be used to move Views out of the scene when the
     * fragment is removed, hidden, or detached when not popping the back stack.
     * The exiting Views will be those that are regular Views or ViewGroups that
     * have {@link ViewGroup#isTransitionGroup} return true. Typical Transitions will extend
     * {@link android.transition.Visibility} as exiting is governed by changing visibility
     * from {@link View#VISIBLE} to {@link View#INVISIBLE}. If transition is null, the views will
     * remain unaffected.
     *
     * @return the Transition to use to move Views out of the Scene when the Fragment
     *         is being closed not due to popping the back stack.
     */
    public Object getExitTransition() {
        if (mAnimationInfo == null) {
            return null;
        }
        return mAnimationInfo.mExitTransition;
    }

    /**
     * Sets the Transition that will be used to move Views in to the scene when returning due
     * to popping a back stack. The entering Views will be those that are regular Views
     * or ViewGroups that have {@link ViewGroup#isTransitionGroup} return true. Typical Transitions
     * will extend {@link android.transition.Visibility} as exiting is governed by changing
     * visibility from {@link View#VISIBLE} to {@link View#INVISIBLE}. If transition is null,
     * the views will remain unaffected. If nothing is set, the default will be to use the same
     * transition as {@link #setExitTransition(Object)}.
     *
     * @param transition The Transition to use to move Views into the scene when reentering from a
     *                   previously-started Activity. <code>transition</code>
     *                   must be an android.transition.Transition.
     */
    public void setReenterTransition(Object transition) {
        ensureAnimationInfo().mReenterTransition = transition;
    }

    /**
     * Returns the Transition that will be used to move Views in to the scene when returning due
     * to popping a back stack. The entering Views will be those that are regular Views
     * or ViewGroups that have {@link ViewGroup#isTransitionGroup} return true. Typical Transitions
     * will extend {@link android.transition.Visibility} as exiting is governed by changing
     * visibility from {@link View#VISIBLE} to {@link View#INVISIBLE}. If transition is null,
     * the views will remain unaffected. If nothing is set, the default will be to use the same
     * transition as {@link #setExitTransition(Object)}.
     *
     * @return the Transition to use to move Views into the scene when reentering from a
     *                   previously-started Activity.
     */
    public Object getReenterTransition() {
        if (mAnimationInfo == null) {
            return null;
        }
        return mAnimationInfo.mReenterTransition == USE_DEFAULT_TRANSITION ? getExitTransition()
                : mAnimationInfo.mReenterTransition;
    }

    /**
     * Sets the Transition that will be used for shared elements transferred into the content
     * Scene. Typical Transitions will affect size and location, such as
     * {@link android.transition.ChangeBounds}. A null
     * value will cause transferred shared elements to blink to the final position.
     *
     * @param transition The Transition to use for shared elements transferred into the content
     *                   Scene.  <code>transition</code> must be an android.transition.Transition.
     */
    public void setSharedElementEnterTransition(Object transition) {
        ensureAnimationInfo().mSharedElementEnterTransition = transition;
    }

    /**
     * Returns the Transition that will be used for shared elements transferred into the content
     * Scene. Typical Transitions will affect size and location, such as
     * {@link android.transition.ChangeBounds}. A null
     * value will cause transferred shared elements to blink to the final position.
     *
     * @return The Transition to use for shared elements transferred into the content
     *                   Scene.
     */
    public Object getSharedElementEnterTransition() {
        if (mAnimationInfo == null) {
            return null;
        }
        return mAnimationInfo.mSharedElementEnterTransition;
    }

    /**
     * Sets the Transition that will be used for shared elements transferred back during a
     * pop of the back stack. This Transition acts in the leaving Fragment.
     * Typical Transitions will affect size and location, such as
     * {@link android.transition.ChangeBounds}. A null
     * value will cause transferred shared elements to blink to the final position.
     * If no value is set, the default will be to use the same value as
     * {@link #setSharedElementEnterTransition(Object)}.
     *
     * @param transition The Transition to use for shared elements transferred out of the content
     *                   Scene. <code>transition</code> must be an android.transition.Transition.
     */
    public void setSharedElementReturnTransition(Object transition) {
        ensureAnimationInfo().mSharedElementReturnTransition = transition;
    }

    /**
     * Return the Transition that will be used for shared elements transferred back during a
     * pop of the back stack. This Transition acts in the leaving Fragment.
     * Typical Transitions will affect size and location, such as
     * {@link android.transition.ChangeBounds}. A null
     * value will cause transferred shared elements to blink to the final position.
     * If no value is set, the default will be to use the same value as
     * {@link #setSharedElementEnterTransition(Object)}.
     *
     * @return The Transition to use for shared elements transferred out of the content
     *                   Scene.
     */
    public Object getSharedElementReturnTransition() {
        if (mAnimationInfo == null) {
            return null;
        }
        return mAnimationInfo.mSharedElementReturnTransition == USE_DEFAULT_TRANSITION
                ? getSharedElementEnterTransition()
                : mAnimationInfo.mSharedElementReturnTransition;
    }

    /**
     * Sets whether the the exit transition and enter transition overlap or not.
     * When true, the enter transition will start as soon as possible. When false, the
     * enter transition will wait until the exit transition completes before starting.
     *
     * @param allow true to start the enter transition when possible or false to
     *              wait until the exiting transition completes.
     */
    public void setAllowEnterTransitionOverlap(boolean allow) {
        ensureAnimationInfo().mAllowEnterTransitionOverlap = allow;
    }

    /**
     * Returns whether the the exit transition and enter transition overlap or not.
     * When true, the enter transition will start as soon as possible. When false, the
     * enter transition will wait until the exit transition completes before starting.
     *
     * @return true when the enter transition should start as soon as possible or false to
     * when it should wait until the exiting transition completes.
     */
    public boolean getAllowEnterTransitionOverlap() {
        return (mAnimationInfo == null || mAnimationInfo.mAllowEnterTransitionOverlap == null)
                ? true : mAnimationInfo.mAllowEnterTransitionOverlap;
    }

    /**
     * Sets whether the the return transition and reenter transition overlap or not.
     * When true, the reenter transition will start as soon as possible. When false, the
     * reenter transition will wait until the return transition completes before starting.
     *
     * @param allow true to start the reenter transition when possible or false to wait until the
     *              return transition completes.
     */
    public void setAllowReturnTransitionOverlap(boolean allow) {
        ensureAnimationInfo().mAllowReturnTransitionOverlap = allow;
    }

    /**
     * Returns whether the the return transition and reenter transition overlap or not.
     * When true, the reenter transition will start as soon as possible. When false, the
     * reenter transition will wait until the return transition completes before starting.
     *
     * @return true to start the reenter transition when possible or false to wait until the
     *         return transition completes.
     */
    public boolean getAllowReturnTransitionOverlap() {
        return (mAnimationInfo == null || mAnimationInfo.mAllowReturnTransitionOverlap == null)
                ? true : mAnimationInfo.mAllowReturnTransitionOverlap;
    }

    /**
     * Postpone the entering Fragment transition until {@link #startPostponedEnterTransition()}
     * or {@link FragmentManager#executePendingTransactions()} has been called.
     * <p>
     * This method gives the Fragment the ability to delay Fragment animations
     * until all data is loaded. Until then, the added, shown, and
     * attached Fragments will be INVISIBLE and removed, hidden, and detached Fragments won't
     * be have their Views removed. The transaction runs when all postponed added Fragments in the
     * transaction have called {@link #startPostponedEnterTransition()}.
     * <p>
     * This method should be called before being added to the FragmentTransaction or
     * in {@link #onCreate(Bundle), {@link #onAttach(Context)}, or
     * {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}}.
     * {@link #startPostponedEnterTransition()} must be called to allow the Fragment to
     * start the transitions.
     * <p>
     * When a FragmentTransaction is started that may affect a postponed FragmentTransaction,
     * based on which containers are in their operations, the postponed FragmentTransaction
     * will have its start triggered. The early triggering may result in faulty or nonexistent
     * animations in the postponed transaction. FragmentTransactions that operate only on
     * independent containers will not interfere with each other's postponement.
     * <p>
     * Calling postponeEnterTransition on Fragments with a null View will not postpone the
     * transition. Likewise, postponement only works if FragmentTransaction optimizations are
     * enabled.
     *
     * @see Activity#postponeEnterTransition()
     * @see FragmentTransaction#setAllowOptimization(boolean)
     */
    public void postponeEnterTransition() {
        ensureAnimationInfo().mEnterTransitionPostponed = true;
    }

    /**
     * Begin postponed transitions after {@link #postponeEnterTransition()} was called.
     * If postponeEnterTransition() was called, you must call startPostponedEnterTransition()
     * or {@link FragmentManager#executePendingTransactions()} to complete the FragmentTransaction.
     * If postponement was interrupted with {@link FragmentManager#executePendingTransactions()},
     * before {@code startPostponedEnterTransition()}, animations may not run or may execute
     * improperly.
     *
     * @see Activity#startPostponedEnterTransition()
     */
    public void startPostponedEnterTransition() {
        if (mFragmentManager == null || mFragmentManager.mHost == null) {
            ensureAnimationInfo().mEnterTransitionPostponed = false;
        } else if (Looper.myLooper() != mFragmentManager.mHost.getHandler().getLooper()) {
            mFragmentManager.mHost.getHandler().postAtFrontOfQueue(new Runnable() {
                @Override
                public void run() {
                    callStartTransitionListener();
                }
            });
        } else {
            callStartTransitionListener();
        }
    }

    /**
     * Calls the start transition listener. This must be called on the UI thread.
     */
    private void callStartTransitionListener() {
        final OnStartEnterTransitionListener listener;
        if (mAnimationInfo == null) {
            listener = null;
        } else {
            mAnimationInfo.mEnterTransitionPostponed = false;
            listener = mAnimationInfo.mStartEnterTransitionListener;
            mAnimationInfo.mStartEnterTransitionListener = null;
        }
        if (listener != null) {
            listener.onStartEnterTransition();
        }
    }

    /**
     * Print the Fragments's state into the given stream.
     *
     * @param prefix Text to print at the front of each line.
     * @param fd The raw file descriptor that the dump is being sent to.
     * @param writer The PrintWriter to which you should dump your state.  This will be
     * closed for you after you return.
     * @param args additional arguments to the dump request.
     */
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        writer.print(prefix); writer.print("mFragmentId=#");
        writer.print(Integer.toHexString(mFragmentId));
        writer.print(" mContainerId=#");
        writer.print(Integer.toHexString(mContainerId));
        writer.print(" mTag="); writer.println(mTag);
        writer.print(prefix); writer.print("mState="); writer.print(mState);
        writer.print(" mIndex="); writer.print(mIndex);
        writer.print(" mWho="); writer.print(mWho);
        writer.print(" mBackStackNesting="); writer.println(mBackStackNesting);
        writer.print(prefix); writer.print("mAdded="); writer.print(mAdded);
        writer.print(" mRemoving="); writer.print(mRemoving);
        writer.print(" mFromLayout="); writer.print(mFromLayout);
        writer.print(" mInLayout="); writer.println(mInLayout);
        writer.print(prefix); writer.print("mHidden="); writer.print(mHidden);
        writer.print(" mDetached="); writer.print(mDetached);
        writer.print(" mMenuVisible="); writer.print(mMenuVisible);
        writer.print(" mHasMenu="); writer.println(mHasMenu);
        writer.print(prefix); writer.print("mRetainInstance="); writer.print(mRetainInstance);
        writer.print(" mRetaining="); writer.print(mRetaining);
        writer.print(" mUserVisibleHint="); writer.println(mUserVisibleHint);
        if (mFragmentManager != null) {
            writer.print(prefix); writer.print("mFragmentManager=");
            writer.println(mFragmentManager);
        }
        if (mHost != null) {
            writer.print(prefix); writer.print("mHost=");
            writer.println(mHost);
        }
        if (mParentFragment != null) {
            writer.print(prefix); writer.print("mParentFragment=");
            writer.println(mParentFragment);
        }
        if (mArguments != null) {
            writer.print(prefix); writer.print("mArguments="); writer.println(mArguments);
        }
        if (mSavedFragmentState != null) {
            writer.print(prefix); writer.print("mSavedFragmentState=");
            writer.println(mSavedFragmentState);
        }
        if (mSavedViewState != null) {
            writer.print(prefix); writer.print("mSavedViewState=");
            writer.println(mSavedViewState);
        }
        if (mTarget != null) {
            writer.print(prefix); writer.print("mTarget="); writer.print(mTarget);
            writer.print(" mTargetRequestCode=");
            writer.println(mTargetRequestCode);
        }
        if (getNextAnim() != 0) {
            writer.print(prefix); writer.print("mNextAnim="); writer.println(getNextAnim());
        }
        if (mContainer != null) {
            writer.print(prefix); writer.print("mContainer="); writer.println(mContainer);
        }
        if (mView != null) {
            writer.print(prefix); writer.print("mView="); writer.println(mView);
        }
        if (mInnerView != null) {
            writer.print(prefix); writer.print("mInnerView="); writer.println(mView);
        }
        if (getAnimatingAway() != null) {
            writer.print(prefix);
            writer.print("mAnimatingAway=");
            writer.println(getAnimatingAway());
            writer.print(prefix);
            writer.print("mStateAfterAnimating=");
            writer.println(getStateAfterAnimating());
        }
        if (mLoaderManager != null) {
            writer.print(prefix); writer.println("Loader Manager:");
            mLoaderManager.dump(prefix + "  ", fd, writer, args);
        }
        if (mChildFragmentManager != null) {
            writer.print(prefix); writer.println("Child " + mChildFragmentManager + ":");
            mChildFragmentManager.dump(prefix + "  ", fd, writer, args);
        }
    }

    MyFragment findFragmentByWho(String who) {
        if (who.equals(mWho)) {
            return this;
        }
        if (mChildFragmentManager != null) {
            return mChildFragmentManager.findFragmentByWho(who);
        }
        return null;
    }

    void instantiateChildFragmentManager() {
        if (mHost == null) {
            throw new IllegalStateException("Fragment has not been attached yet.");
        }
        mChildFragmentManager = new MyFragmentManagerImp();
        mChildFragmentManager.attachController(mHost, new FragmentContainer() {
            @Override
            @Nullable
            public View onFindViewById(int id) {
                if (mView == null) {
                    throw new IllegalStateException("Fragment does not have a view");
                }
                return mView.findViewById(id);
            }

            @Override
            public boolean onHasView() {
                return (mView != null);
            }
        }, this);
    }

    void performCreate(Bundle savedInstanceState) {
        if (mChildFragmentManager != null) {
            mChildFragmentManager.noteStateNotSaved();
        }
        mState = CREATED;
        mCalled = false;
        onCreate(savedInstanceState);
        if (!mCalled) {
            throw new AndroidRuntimeException("Fragment " + this
                    + " did not call through to super.onCreate()");
        }
    }

    View performCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
        if (mChildFragmentManager != null) {
            mChildFragmentManager.noteStateNotSaved();
        }
        return onCreateView(inflater, container, savedInstanceState);
    }

    void performActivityCreated(Bundle savedInstanceState) {
        if (mChildFragmentManager != null) {
            mChildFragmentManager.noteStateNotSaved();
        }
        mState = ACTIVITY_CREATED;
        mCalled = false;
        onActivityCreated(savedInstanceState);
        if (!mCalled) {
            throw new AndroidRuntimeException("Fragment " + this
                    + " did not call through to super.onActivityCreated()");
        }
        if (mChildFragmentManager != null) {
            mChildFragmentManager.dispatchActivityCreated();
        }
    }

    void performStart() {
        if (mChildFragmentManager != null) {
            mChildFragmentManager.noteStateNotSaved();
            mChildFragmentManager.execPendingActions();
        }
        mState = STARTED;
        mCalled = false;
        onStart();
        if (!mCalled) {
            throw new AndroidRuntimeException("Fragment " + this
                    + " did not call through to super.onStart()");
        }
        if (mChildFragmentManager != null) {
            mChildFragmentManager.dispatchStart();
        }
        if (mLoaderManager != null) {
            mLoaderManager.doReportStart();
        }
    }

    void performResume() {
        if (mChildFragmentManager != null) {
            mChildFragmentManager.noteStateNotSaved();
            mChildFragmentManager.execPendingActions();
        }
        mState = RESUMED;
        mCalled = false;
        onResume();
        if (!mCalled) {
            throw new AndroidRuntimeException("Fragment " + this
                    + " did not call through to super.onResume()");
        }
        if (mChildFragmentManager != null) {
            mChildFragmentManager.dispatchResume();
            mChildFragmentManager.execPendingActions();
        }
    }

    void performMultiWindowModeChanged(boolean isInMultiWindowMode) {
        onMultiWindowModeChanged(isInMultiWindowMode);
        if (mChildFragmentManager != null) {
            mChildFragmentManager.dispatchMultiWindowModeChanged(isInMultiWindowMode);
        }
    }

    void performPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        onPictureInPictureModeChanged(isInPictureInPictureMode);
        if (mChildFragmentManager != null) {
            mChildFragmentManager.dispatchPictureInPictureModeChanged(isInPictureInPictureMode);
        }
    }

    void performConfigurationChanged(Configuration newConfig) {
        onConfigurationChanged(newConfig);
        if (mChildFragmentManager != null) {
            mChildFragmentManager.dispatchConfigurationChanged(newConfig);
        }
    }

    void performLowMemory() {
        onLowMemory();
        if (mChildFragmentManager != null) {
            mChildFragmentManager.dispatchLowMemory();
        }
    }

    /*
    void performTrimMemory(int level) {
        onTrimMemory(level);
        if (mChildFragmentManager != null) {
            mChildFragmentManager.dispatchTrimMemory(level);
        }
    }
    */

    boolean performCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        boolean show = false;
        if (!mHidden) {
            if (mHasMenu && mMenuVisible) {
                show = true;
                onCreateOptionsMenu(menu, inflater);
            }
            if (mChildFragmentManager != null) {
                show |= mChildFragmentManager.dispatchCreateOptionsMenu(menu, inflater);
            }
        }
        return show;
    }

    boolean performPrepareOptionsMenu(Menu menu) {
        boolean show = false;
        if (!mHidden) {
            if (mHasMenu && mMenuVisible) {
                show = true;
                onPrepareOptionsMenu(menu);
            }
            if (mChildFragmentManager != null) {
                show |= mChildFragmentManager.dispatchPrepareOptionsMenu(menu);
            }
        }
        return show;
    }

    boolean performOptionsItemSelected(MenuItem item) {
        if (!mHidden) {
            if (mHasMenu && mMenuVisible) {
                if (onOptionsItemSelected(item)) {
                    return true;
                }
            }
            if (mChildFragmentManager != null) {
                if (mChildFragmentManager.dispatchOptionsItemSelected(item)) {
                    return true;
                }
            }
        }
        return false;
    }

    boolean performContextItemSelected(MenuItem item) {
        if (!mHidden) {
            if (onContextItemSelected(item)) {
                return true;
            }
            if (mChildFragmentManager != null) {
                if (mChildFragmentManager.dispatchContextItemSelected(item)) {
                    return true;
                }
            }
        }
        return false;
    }

    void performOptionsMenuClosed(Menu menu) {
        if (!mHidden) {
            if (mHasMenu && mMenuVisible) {
                onOptionsMenuClosed(menu);
            }
            if (mChildFragmentManager != null) {
                mChildFragmentManager.dispatchOptionsMenuClosed(menu);
            }
        }
    }

    void performSaveInstanceState(Bundle outState) {
        onSaveInstanceState(outState);
        if (mChildFragmentManager != null) {
            Parcelable p = mChildFragmentManager.saveAllState();
            if (p != null) {
                outState.putParcelable("android:support:fragments", p);
            }
        }
    }

    void performPause() {
        if (mChildFragmentManager != null) {
            mChildFragmentManager.dispatchPause();
        }
        mState = STARTED;
        mCalled = false;
        onPause();
        if (!mCalled) {
            throw new AndroidRuntimeException("Fragment " + this
                    + " did not call through to super.onPause()");
        }
    }

    void performStop() {
        if (mChildFragmentManager != null) {
            mChildFragmentManager.dispatchStop();
        }
        mState = STOPPED;
        mCalled = false;
        onStop();
        if (!mCalled) {
            throw new AndroidRuntimeException("Fragment " + this
                    + " did not call through to super.onStop()");
        }
    }

    void performReallyStop() {
        if (mChildFragmentManager != null) {
            mChildFragmentManager.dispatchReallyStop();
        }
        mState = ACTIVITY_CREATED;
        if (mLoadersStarted) {
            mLoadersStarted = false;
            if (!mCheckedForLoaderManager) {
                mCheckedForLoaderManager = true;
                mLoaderManager = mHost.getLoaderManager(mWho, mLoadersStarted, false);
            }
            if (mLoaderManager != null) {
                if (mHost.getRetainLoaders()) {
                    mLoaderManager.doRetain();
                } else {
                    mLoaderManager.doStop();
                }
            }
        }
    }

    void performDestroyView() {
        if (mChildFragmentManager != null) {
            mChildFragmentManager.dispatchDestroyView();
        }
        mState = CREATED;
        mCalled = false;
        onDestroyView();
        if (!mCalled) {
            throw new AndroidRuntimeException("Fragment " + this
                    + " did not call through to super.onDestroyView()");
        }
        if (mLoaderManager != null) {
            mLoaderManager.doReportNextStart();
        }
    }

    void performDestroy() {
        if (mChildFragmentManager != null) {
            mChildFragmentManager.dispatchDestroy();
        }
        mState = INITIALIZING;
        mCalled = false;
        onDestroy();
        if (!mCalled) {
            throw new AndroidRuntimeException("Fragment " + this
                    + " did not call through to super.onDestroy()");
        }
        mChildFragmentManager = null;
    }

    void performDetach() {
        mCalled = false;
        onDetach();
        if (!mCalled) {
            throw new AndroidRuntimeException("Fragment " + this
                    + " did not call through to super.onDetach()");
        }

        // Destroy the child FragmentManager if we still have it here.
        // We won't unless we're retaining our instance and if we do,
        // our child FragmentManager instance state will have already been saved.
        if (mChildFragmentManager != null) {
            if (!mRetaining) {
                throw new IllegalStateException("Child FragmentManager of " + this + " was not "
                        + " destroyed and this fragment is not retaining instance");
            }
            mChildFragmentManager.dispatchDestroy();
            mChildFragmentManager = null;
        }
    }

    void setOnStartEnterTransitionListener(OnStartEnterTransitionListener listener) {
        ensureAnimationInfo();
        if (listener == mAnimationInfo.mStartEnterTransitionListener) {
            return;
        }
        if (listener != null && mAnimationInfo.mStartEnterTransitionListener != null) {
            throw new IllegalStateException("Trying to set a replacement "
                    + "startPostponedEnterTransition on " + this);
        }
        if (mAnimationInfo.mEnterTransitionPostponed) {
            mAnimationInfo.mStartEnterTransitionListener = listener;
        }
        if (listener != null) {
            listener.startListening();
        }
    }

    private AnimationInfo ensureAnimationInfo() {
        if (mAnimationInfo == null) {
            mAnimationInfo = new AnimationInfo();
        }
        return mAnimationInfo;
    }

    int getNextAnim() {
        if (mAnimationInfo == null) {
            return 0;
        }
        return mAnimationInfo.mNextAnim;
    }

    void setNextAnim(int animResourceId) {
        if (mAnimationInfo == null && animResourceId == 0) {
            return; // no change!
        }
        ensureAnimationInfo().mNextAnim = animResourceId;
    }

    int getNextTransition() {
        if (mAnimationInfo == null) {
            return 0;
        }
        return mAnimationInfo.mNextTransition;
    }

    void setNextTransition(int nextTransition, int nextTransitionStyle) {
        if (mAnimationInfo == null && nextTransition == 0 && nextTransitionStyle == 0) {
            return; // no change!
        }
        ensureAnimationInfo();
        mAnimationInfo.mNextTransition = nextTransition;
        mAnimationInfo.mNextTransitionStyle = nextTransitionStyle;
    }

    int getNextTransitionStyle() {
        if (mAnimationInfo == null) {
            return 0;
        }
        return mAnimationInfo.mNextTransitionStyle;
    }

    SharedElementCallback getEnterTransitionCallback() {
        if (mAnimationInfo == null) {
            return null;
        }
        return mAnimationInfo.mEnterTransitionCallback;
    }

    SharedElementCallback getExitTransitionCallback() {
        if (mAnimationInfo == null) {
            return null;
        }
        return mAnimationInfo.mExitTransitionCallback;
    }

    View getAnimatingAway() {
        if (mAnimationInfo == null) {
            return null;
        }
        return mAnimationInfo.mAnimatingAway;
    }

    void setAnimatingAway(View view) {
        ensureAnimationInfo().mAnimatingAway = view;
    }

    int getStateAfterAnimating() {
        if (mAnimationInfo == null) {
            return 0;
        }
        return mAnimationInfo.mStateAfterAnimating;
    }

    void setStateAfterAnimating(int state) {
        ensureAnimationInfo().mStateAfterAnimating = state;
    }

    boolean isPostponed() {
        if (mAnimationInfo == null) {
            return false;
        }
        return mAnimationInfo.mEnterTransitionPostponed;
    }

    boolean isHideReplaced() {
        if (mAnimationInfo == null) {
            return false;
        }
        return mAnimationInfo.mIsHideReplaced;
    }

    void setHideReplaced(boolean replaced) {
        ensureAnimationInfo().mIsHideReplaced = replaced;
    }

    /**
     * Used internally to be notified when {@link #startPostponedEnterTransition()} has
     * been called. This listener will only be called once and then be removed from the
     * listeners.
     */
    interface OnStartEnterTransitionListener {
        void onStartEnterTransition();
        void startListening();
    }

    /**
     * Contains all the animation and transition information for a fragment. This will only
     * be instantiated for Fragments that have Views.
     */
    static class AnimationInfo {
        // Non-null if the fragment's view hierarchy is currently animating away,
        // meaning we need to wait a bit on completely destroying it.  This is the
        // view that is animating.
        View mAnimatingAway;

        // If mAnimatingAway != null, this is the state we should move to once the
        // animation is done.
        int mStateAfterAnimating;

        // If app has requested a specific animation, this is the one to use.
        int mNextAnim;

        // If app has requested a specific transition, this is the one to use.
        int mNextTransition;

        // If app has requested a specific transition style, this is the one to use.
        int mNextTransitionStyle;

        private Object mEnterTransition = null;
        private Object mReturnTransition = USE_DEFAULT_TRANSITION;
        private Object mExitTransition = null;
        private Object mReenterTransition = USE_DEFAULT_TRANSITION;
        private Object mSharedElementEnterTransition = null;
        private Object mSharedElementReturnTransition = USE_DEFAULT_TRANSITION;
        private Boolean mAllowReturnTransitionOverlap;
        private Boolean mAllowEnterTransitionOverlap;

        SharedElementCallback mEnterTransitionCallback = null;
        SharedElementCallback mExitTransitionCallback = null;

        // True when postponeEnterTransition has been called and startPostponeEnterTransition
        // hasn't been called yet.
        boolean mEnterTransitionPostponed;

        // Listener to wait for startPostponeEnterTransition. After being called, it will
        // be set to null
        OnStartEnterTransitionListener mStartEnterTransitionListener;

        // True if the View was hidden, but the transition is handling the hide
        boolean mIsHideReplaced;
    }
}

final class FragmentState implements Parcelable {
    final String mClassName;
    final int mIndex;
    final boolean mFromLayout;
    final int mFragmentId;
    final int mContainerId;
    final String mTag;
    final boolean mRetainInstance;
    final boolean mDetached;
    final Bundle mArguments;
    final boolean mHidden;

    Bundle mSavedFragmentState;

    MyFragment mInstance;

    public FragmentState(MyFragment frag) {
        mClassName = frag.getClass().getName();
        mIndex = frag.mIndex;
        mFromLayout = frag.mFromLayout;
        mFragmentId = frag.mFragmentId;
        mContainerId = frag.mContainerId;
        mTag = frag.mTag;
        mRetainInstance = frag.mRetainInstance;
        mDetached = frag.mDetached;
        mArguments = frag.mArguments;
        mHidden = frag.mHidden;
    }

    public FragmentState(Parcel in) {
        mClassName = in.readString();
        mIndex = in.readInt();
        mFromLayout = in.readInt() != 0;
        mFragmentId = in.readInt();
        mContainerId = in.readInt();
        mTag = in.readString();
        mRetainInstance = in.readInt() != 0;
        mDetached = in.readInt() != 0;
        mArguments = in.readBundle();
        mHidden = in.readInt() != 0;
        mSavedFragmentState = in.readBundle();
    }

    public MyFragment instantiate(MyFragmentHostCallBack host, MyFragment parent,
                                  FragmentManagerNonConfit childNonConfig) {
        if (mInstance == null) {
            final Context context = host.getContext();
            if (mArguments != null) {
                mArguments.setClassLoader(context.getClassLoader());
            }

            mInstance = MyFragment.instantiate(context, mClassName, mArguments);

            if (mSavedFragmentState != null) {
                mSavedFragmentState.setClassLoader(context.getClassLoader());
                mInstance.mSavedFragmentState = mSavedFragmentState;
            }
            mInstance.setIndex(mIndex, parent);
            mInstance.mFromLayout = mFromLayout;
            mInstance.mRestored = true;
            mInstance.mFragmentId = mFragmentId;
            mInstance.mContainerId = mContainerId;
            mInstance.mTag = mTag;
            mInstance.mRetainInstance = mRetainInstance;
            mInstance.mDetached = mDetached;
            mInstance.mHidden = mHidden;
            mInstance.mFragmentManager = host.mFragmentManager;

            if (MyFragmentManagerImp.DEBUG) Log.v(MyFragmentManagerImp.TAG,
                    "Instantiated fragment " + mInstance);
        }
        mInstance.mChildNonConfig = childNonConfig;
        return mInstance;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mClassName);
        dest.writeInt(mIndex);
        dest.writeInt(mFromLayout ? 1 : 0);
        dest.writeInt(mFragmentId);
        dest.writeInt(mContainerId);
        dest.writeString(mTag);
        dest.writeInt(mRetainInstance ? 1 : 0);
        dest.writeInt(mDetached ? 1 : 0);
        dest.writeBundle(mArguments);
        dest.writeInt(mHidden? 1 : 0);
        dest.writeBundle(mSavedFragmentState);
    }

    public static final Parcelable.Creator<FragmentState> CREATOR
            = new Parcelable.Creator<FragmentState>() {
        @Override
        public FragmentState createFromParcel(Parcel in) {
            return new FragmentState(in);
        }

        @Override
        public FragmentState[] newArray(int size) {
            return new FragmentState[size];
        }
    };
}

class LoaderManagerImpl extends LoaderManager {
    static final String TAG = "LoaderManager";
    static boolean DEBUG = false;

    // These are the currently active loaders.  A loader is here
    // from the time its load is started until it has been explicitly
    // stopped or restarted by the application.
    final SparseArrayCompat<LoaderInfo> mLoaders = new SparseArrayCompat<LoaderInfo>();

    // These are previously run loaders.  This list is maintained internally
    // to avoid destroying a loader while an application is still using it.
    // It allows an application to restart a loader, but continue using its
    // previously run loader until the new loader's data is available.
    final SparseArrayCompat<LoaderInfo> mInactiveLoaders = new SparseArrayCompat<LoaderInfo>();

    final String mWho;

    boolean mStarted;
    boolean mRetaining;
    boolean mRetainingStarted;

    boolean mCreatingLoader;
    MyFragmentHostCallBack mHost;

    final class LoaderInfo implements Loader.OnLoadCompleteListener<Object>,
            Loader.OnLoadCanceledListener<Object> {
        final int mId;
        final Bundle mArgs;
        LoaderManager.LoaderCallbacks<Object> mCallbacks;
        Loader<Object> mLoader;
        boolean mHaveData;
        boolean mDeliveredData;
        Object mData;
        @SuppressWarnings("hiding")
        boolean mStarted;
        @SuppressWarnings("hiding")
        boolean mRetaining;
        @SuppressWarnings("hiding")
        boolean mRetainingStarted;
        boolean mReportNextStart;
        boolean mDestroyed;
        boolean mListenerRegistered;

        LoaderInfo mPendingLoader;

        public LoaderInfo(int id, Bundle args, LoaderManager.LoaderCallbacks<Object> callbacks) {
            mId = id;
            mArgs = args;
            mCallbacks = callbacks;
        }

        void start() {
            if (mRetaining && mRetainingStarted) {
                // Our owner is started, but we were being retained from a
                // previous instance in the started state...  so there is really
                // nothing to do here, since the loaders are still started.
                mStarted = true;
                return;
            }

            if (mStarted) {
                // If loader already started, don't restart.
                return;
            }

            mStarted = true;

            if (DEBUG) Log.v(TAG, "  Starting: " + this);
            if (mLoader == null && mCallbacks != null) {
                mLoader = mCallbacks.onCreateLoader(mId, mArgs);
            }
            if (mLoader != null) {
                if (mLoader.getClass().isMemberClass()
                        && !Modifier.isStatic(mLoader.getClass().getModifiers())) {
                    throw new IllegalArgumentException(
                            "Object returned from onCreateLoader must not be a non-static inner member class: "
                                    + mLoader);
                }
                if (!mListenerRegistered) {
                    mLoader.registerListener(mId, this);
                    mLoader.registerOnLoadCanceledListener(this);
                    mListenerRegistered = true;
                }
                mLoader.startLoading();
            }
        }

        void retain() {
            if (DEBUG) Log.v(TAG, "  Retaining: " + this);
            mRetaining = true;
            mRetainingStarted = mStarted;
            mStarted = false;
            mCallbacks = null;
        }

        void finishRetain() {
            if (mRetaining) {
                if (DEBUG) Log.v(TAG, "  Finished Retaining: " + this);
                mRetaining = false;
                if (mStarted != mRetainingStarted) {
                    if (!mStarted) {
                        // This loader was retained in a started state, but
                        // at the end of retaining everything our owner is
                        // no longer started...  so make it stop.
                        stop();
                    }
                }
            }

            if (mStarted && mHaveData && !mReportNextStart) {
                // This loader has retained its data, either completely across
                // a configuration change or just whatever the last data set
                // was after being restarted from a stop, and now at the point of
                // finishing the retain we find we remain started, have
                // our data, and the owner has a new callback...  so
                // let's deliver the data now.
                callOnLoadFinished(mLoader, mData);
            }
        }

        void reportStart() {
            if (mStarted) {
                if (mReportNextStart) {
                    mReportNextStart = false;
                    if (mHaveData && !mRetaining) {
                        callOnLoadFinished(mLoader, mData);
                    }
                }
            }
        }

        void stop() {
            if (DEBUG) Log.v(TAG, "  Stopping: " + this);
            mStarted = false;
            if (!mRetaining) {
                if (mLoader != null && mListenerRegistered) {
                    // Let the loader know we're done with it
                    mListenerRegistered = false;
                    mLoader.unregisterListener(this);
                    mLoader.unregisterOnLoadCanceledListener(this);
                    mLoader.stopLoading();
                }
            }
        }

        boolean cancel() {
            if (DEBUG) Log.v(TAG, "  Canceling: " + this);
            if (mStarted && mLoader != null && mListenerRegistered) {
                final boolean cancelLoadResult = mLoader.cancelLoad();
                if (!cancelLoadResult) {
                    onLoadCanceled(mLoader);
                }
                return cancelLoadResult;
            }
            return false;
        }

        void destroy() {
            if (DEBUG) Log.v(TAG, "  Destroying: " + this);
            mDestroyed = true;
            boolean needReset = mDeliveredData;
            mDeliveredData = false;
            if (mCallbacks != null && mLoader != null && mHaveData && needReset) {
                if (DEBUG) Log.v(TAG, "  Resetting: " + this);
                String lastBecause = null;
                if (mHost != null) {
                    lastBecause = mHost.mFragmentManager.mNoTransactionsBecause;
                    mHost.mFragmentManager.mNoTransactionsBecause = "onLoaderReset";
                }
                try {
                    mCallbacks.onLoaderReset(mLoader);
                } finally {
                    if (mHost != null) {
                        mHost.mFragmentManager.mNoTransactionsBecause = lastBecause;
                    }
                }
            }
            mCallbacks = null;
            mData = null;
            mHaveData = false;
            if (mLoader != null) {
                if (mListenerRegistered) {
                    mListenerRegistered = false;
                    mLoader.unregisterListener(this);
                    mLoader.unregisterOnLoadCanceledListener(this);
                }
                mLoader.reset();
            }
            if (mPendingLoader != null) {
                mPendingLoader.destroy();
            }
        }

        @Override
        public void onLoadCanceled(Loader<Object> loader) {
            if (DEBUG) Log.v(TAG, "onLoadCanceled: " + this);

            if (mDestroyed) {
                if (DEBUG) Log.v(TAG, "  Ignoring load canceled -- destroyed");
                return;
            }

            if (mLoaders.get(mId) != this) {
                // This cancellation message is not coming from the current active loader.
                // We don't care about it.
                if (DEBUG) Log.v(TAG, "  Ignoring load canceled -- not active");
                return;
            }

            LoaderInfo pending = mPendingLoader;
            if (pending != null) {
                // There is a new request pending and we were just
                // waiting for the old one to cancel or complete before starting
                // it.  So now it is time, switch over to the new loader.
                if (DEBUG) Log.v(TAG, "  Switching to pending loader: " + pending);
                mPendingLoader = null;
                mLoaders.put(mId, null);
                destroy();
                installLoader(pending);
            }
        }

        @Override
        public void onLoadComplete(Loader<Object> loader, Object data) {
            if (DEBUG) Log.v(TAG, "onLoadComplete: " + this);

            if (mDestroyed) {
                if (DEBUG) Log.v(TAG, "  Ignoring load complete -- destroyed");
                return;
            }

            if (mLoaders.get(mId) != this) {
                // This data is not coming from the current active loader.
                // We don't care about it.
                if (DEBUG) Log.v(TAG, "  Ignoring load complete -- not active");
                return;
            }

            LoaderInfo pending = mPendingLoader;
            if (pending != null) {
                // There is a new request pending and we were just
                // waiting for the old one to complete before starting
                // it.  So now it is time, switch over to the new loader.
                if (DEBUG) Log.v(TAG, "  Switching to pending loader: " + pending);
                mPendingLoader = null;
                mLoaders.put(mId, null);
                destroy();
                installLoader(pending);
                return;
            }

            // Notify of the new data so the app can switch out the old data before
            // we try to destroy it.
            if (mData != data || !mHaveData) {
                mData = data;
                mHaveData = true;
                if (mStarted) {
                    callOnLoadFinished(loader, data);
                }
            }

            //if (DEBUG) Log.v(TAG, "  onLoadFinished returned: " + this);

            // We have now given the application the new loader with its
            // loaded data, so it should have stopped using the previous
            // loader.  If there is a previous loader on the inactive list,
            // clean it up.
            LoaderInfo info = mInactiveLoaders.get(mId);
            if (info != null && info != this) {
                info.mDeliveredData = false;
                info.destroy();
                mInactiveLoaders.remove(mId);
            }

            if (mHost != null && !hasRunningLoaders()) {
                mHost.mFragmentManager.startPendingDeferredFragments();
            }
        }

        void callOnLoadFinished(Loader<Object> loader, Object data) {
            if (mCallbacks != null) {
                String lastBecause = null;
                if (mHost != null) {
                    lastBecause = mHost.mFragmentManager.mNoTransactionsBecause;
                    mHost.mFragmentManager.mNoTransactionsBecause = "onLoadFinished";
                }
                try {
                    if (DEBUG) Log.v(TAG, "  onLoadFinished in " + loader + ": "
                            + loader.dataToString(data));
                    mCallbacks.onLoadFinished(loader, data);
                } finally {
                    if (mHost != null) {
                        mHost.mFragmentManager.mNoTransactionsBecause = lastBecause;
                    }
                }
                mDeliveredData = true;
            }
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(64);
            sb.append("LoaderInfo{");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append(" #");
            sb.append(mId);
            sb.append(" : ");
            DebugUtils.buildShortClassTag(mLoader, sb);
            sb.append("}}");
            return sb.toString();
        }

        public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
            writer.print(prefix); writer.print("mId="); writer.print(mId);
            writer.print(" mArgs="); writer.println(mArgs);
            writer.print(prefix); writer.print("mCallbacks="); writer.println(mCallbacks);
            writer.print(prefix); writer.print("mLoader="); writer.println(mLoader);
            if (mLoader != null) {
                mLoader.dump(prefix + "  ", fd, writer, args);
            }
            if (mHaveData || mDeliveredData) {
                writer.print(prefix); writer.print("mHaveData="); writer.print(mHaveData);
                writer.print("  mDeliveredData="); writer.println(mDeliveredData);
                writer.print(prefix); writer.print("mData="); writer.println(mData);
            }
            writer.print(prefix); writer.print("mStarted="); writer.print(mStarted);
            writer.print(" mReportNextStart="); writer.print(mReportNextStart);
            writer.print(" mDestroyed="); writer.println(mDestroyed);
            writer.print(prefix); writer.print("mRetaining="); writer.print(mRetaining);
            writer.print(" mRetainingStarted="); writer.print(mRetainingStarted);
            writer.print(" mListenerRegistered="); writer.println(mListenerRegistered);
            if (mPendingLoader != null) {
                writer.print(prefix); writer.println("Pending Loader ");
                writer.print(mPendingLoader); writer.println(":");
                mPendingLoader.dump(prefix + "  ", fd, writer, args);
            }
        }
    }

    LoaderManagerImpl(String who, MyFragmentHostCallBack host, boolean started) {
        mWho = who;
        mHost = host;
        mStarted = started;
    }

    void updateHostController(MyFragmentHostCallBack host) {
        mHost = host;
    }

    private LoaderInfo createLoader(int id, Bundle args, LoaderManager.LoaderCallbacks<Object> callback) {
        LoaderInfo info = new LoaderInfo(id, args,  callback);
        Loader<Object> loader = callback.onCreateLoader(id, args);
        info.mLoader = loader;
        return info;
    }

    private LoaderInfo createAndInstallLoader(int id, Bundle args, LoaderManager.LoaderCallbacks<Object> callback) {
        try {
            mCreatingLoader = true;
            LoaderInfo info = createLoader(id, args, callback);
            installLoader(info);
            return info;
        } finally {
            mCreatingLoader = false;
        }
    }

    void installLoader(LoaderInfo info) {
        mLoaders.put(info.mId, info);
        if (mStarted) {
            // The activity will start all existing loaders in it's onStart(),
            // so only start them here if we're past that point of the activity's
            // life cycle
            info.start();
        }
    }

    /**
     * Call to initialize a particular ID with a Loader.  If this ID already
     * has a Loader associated with it, it is left unchanged and any previous
     * callbacks replaced with the newly provided ones.  If there is not currently
     * a Loader for the ID, a new one is created and started.
     *
     * <p>This function should generally be used when a component is initializing,
     * to ensure that a Loader it relies on is created.  This allows it to re-use
     * an existing Loader's data if there already is one, so that for example
     * when an {@link Activity} is re-created after a configuration change it
     * does not need to re-create its loaders.
     *
     * <p>Note that in the case where an existing Loader is re-used, the
     * <var>args</var> given here <em>will be ignored</em> because you will
     * continue using the previous Loader.
     *
     * @param id A unique (to this LoaderManager instance) identifier under
     * which to manage the new Loader.
     * @param args Optional arguments that will be propagated to
     * {@link android.support.v4.app.LoaderManager.LoaderCallbacks#onCreateLoader(int, Bundle) LoaderCallbacks.onCreateLoader()}.
     * @param callback Interface implementing management of this Loader.  Required.
     * Its onCreateLoader() method will be called while inside of the function to
     * instantiate the Loader object.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <D> Loader<D> initLoader(int id, Bundle args, LoaderManager.LoaderCallbacks<D> callback) {
        if (mCreatingLoader) {
            throw new IllegalStateException("Called while creating a loader");
        }

        LoaderInfo info = mLoaders.get(id);

        if (DEBUG) Log.v(TAG, "initLoader in " + this + ": args=" + args);

        if (info == null) {
            // Loader doesn't already exist; create.
            info = createAndInstallLoader(id, args,  (LoaderManager.LoaderCallbacks<Object>)callback);
            if (DEBUG) Log.v(TAG, "  Created new loader " + info);
        } else {
            if (DEBUG) Log.v(TAG, "  Re-using existing loader " + info);
            info.mCallbacks = (LoaderManager.LoaderCallbacks<Object>)callback;
        }

        if (info.mHaveData && mStarted) {
            // If the loader has already generated its data, report it now.
            info.callOnLoadFinished(info.mLoader, info.mData);
        }

        return (Loader<D>)info.mLoader;
    }

    /**
     * Call to re-create the Loader associated with a particular ID.  If there
     * is currently a Loader associated with this ID, it will be
     * canceled/stopped/destroyed as appropriate.  A new Loader with the given
     * arguments will be created and its data delivered to you once available.
     *
     * <p>This function does some throttling of Loaders.  If too many Loaders
     * have been created for the given ID but not yet generated their data,
     * new calls to this function will create and return a new Loader but not
     * actually start it until some previous loaders have completed.
     *
     * <p>After calling this function, any previous Loaders associated with
     * this ID will be considered invalid, and you will receive no further
     * data updates from them.
     *
     * @param id A unique (to this LoaderManager instance) identifier under
     * which to manage the new Loader.
     * @param args Optional arguments that will be propagated to
     * {@link android.support.v4.app.LoaderManager.LoaderCallbacks#onCreateLoader(int, Bundle) LoaderCallbacks.onCreateLoader()}.
     * @param callback Interface implementing management of this Loader.  Required.
     * Its onCreateLoader() method will be called while inside of the function to
     * instantiate the Loader object.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <D> Loader<D> restartLoader(int id, Bundle args, LoaderManager.LoaderCallbacks<D> callback) {
        if (mCreatingLoader) {
            throw new IllegalStateException("Called while creating a loader");
        }

        LoaderInfo info = mLoaders.get(id);
        if (DEBUG) Log.v(TAG, "restartLoader in " + this + ": args=" + args);
        if (info != null) {
            LoaderInfo inactive = mInactiveLoaders.get(id);
            if (inactive != null) {
                if (info.mHaveData) {
                    // This loader now has data...  we are probably being
                    // called from within onLoadComplete, where we haven't
                    // yet destroyed the last inactive loader.  So just do
                    // that now.
                    if (DEBUG) Log.v(TAG, "  Removing last inactive loader: " + info);
                    inactive.mDeliveredData = false;
                    inactive.destroy();
                    info.mLoader.abandon();
                    mInactiveLoaders.put(id, info);
                } else {
                    // We already have an inactive loader for this ID that we are
                    // waiting for! Try to cancel; if this returns true then the task is still
                    // running and we have more work to do.
                    if (!info.cancel()) {
                        // The current Loader has not been started or was successfully canceled,
                        // we thus have no reason to keep it around. Remove it and a new
                        // LoaderInfo will be created below.
                        if (DEBUG) Log.v(TAG, "  Current loader is stopped; replacing");
                        mLoaders.put(id, null);
                        info.destroy();
                    } else {
                        // Now we have three active loaders... we'll queue
                        // up this request to be processed once one of the other loaders
                        // finishes.
                        if (DEBUG) Log.v(TAG,
                                "  Current loader is running; configuring pending loader");
                        if (info.mPendingLoader != null) {
                            if (DEBUG) Log.v(TAG, "  Removing pending loader: " + info.mPendingLoader);
                            info.mPendingLoader.destroy();
                            info.mPendingLoader = null;
                        }
                        if (DEBUG) Log.v(TAG, "  Enqueuing as new pending loader");
                        info.mPendingLoader = createLoader(id, args,
                                (LoaderManager.LoaderCallbacks<Object>)callback);
                        return (Loader<D>)info.mPendingLoader.mLoader;
                    }
                }
            } else {
                // Keep track of the previous instance of this loader so we can destroy
                // it when the new one completes.
                if (DEBUG) Log.v(TAG, "  Making last loader inactive: " + info);
                info.mLoader.abandon();
                mInactiveLoaders.put(id, info);
            }
        }

        info = createAndInstallLoader(id, args,  (LoaderManager.LoaderCallbacks<Object>)callback);
        return (Loader<D>)info.mLoader;
    }

    /**
     * Rip down, tear apart, shred to pieces a current Loader ID.  After returning
     * from this function, any Loader objects associated with this ID are
     * destroyed.  Any data associated with them is destroyed.  You better not
     * be using it when you do this.
     * @param id Identifier of the Loader to be destroyed.
     */
    @Override
    public void destroyLoader(int id) {
        if (mCreatingLoader) {
            throw new IllegalStateException("Called while creating a loader");
        }

        if (DEBUG) Log.v(TAG, "destroyLoader in " + this + " of " + id);
        int idx = mLoaders.indexOfKey(id);
        if (idx >= 0) {
            LoaderInfo info = mLoaders.valueAt(idx);
            mLoaders.removeAt(idx);
            info.destroy();
        }
        idx = mInactiveLoaders.indexOfKey(id);
        if (idx >= 0) {
           LoaderInfo info = mInactiveLoaders.valueAt(idx);
            mInactiveLoaders.removeAt(idx);
            info.destroy();
        }
        if (mHost != null && !hasRunningLoaders()) {
            mHost.mFragmentManager.startPendingDeferredFragments();
        }
    }

    /**
     * Return the most recent Loader object associated with the
     * given ID.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <D> Loader<D> getLoader(int id) {
        if (mCreatingLoader) {
            throw new IllegalStateException("Called while creating a loader");
        }

        LoaderInfo loaderInfo = mLoaders.get(id);
        if (loaderInfo != null) {
            if (loaderInfo.mPendingLoader != null) {
                return (Loader<D>)loaderInfo.mPendingLoader.mLoader;
            }
            return (Loader<D>)loaderInfo.mLoader;
        }
        return null;
    }

    void doStart() {
        if (DEBUG) Log.v(TAG, "Starting in " + this);
        if (mStarted) {
            RuntimeException e = new RuntimeException("here");
            e.fillInStackTrace();
            Log.w(TAG, "Called doStart when already started: " + this, e);
            return;
        }

        mStarted = true;

        // Call out to sub classes so they can start their loaders
        // Let the existing loaders know that we want to be notified when a load is complete
        for (int i = mLoaders.size()-1; i >= 0; i--) {
            mLoaders.valueAt(i).start();
        }
    }

    void doStop() {
        if (DEBUG) Log.v(TAG, "Stopping in " + this);
        if (!mStarted) {
            RuntimeException e = new RuntimeException("here");
            e.fillInStackTrace();
            Log.w(TAG, "Called doStop when not started: " + this, e);
            return;
        }

        for (int i = mLoaders.size()-1; i >= 0; i--) {
            mLoaders.valueAt(i).stop();
        }
        mStarted = false;
    }

    void doRetain() {
        if (DEBUG) Log.v(TAG, "Retaining in " + this);
        if (!mStarted) {
            RuntimeException e = new RuntimeException("here");
            e.fillInStackTrace();
            Log.w(TAG, "Called doRetain when not started: " + this, e);
            return;
        }

        mRetaining = true;
        mStarted = false;
        for (int i = mLoaders.size()-1; i >= 0; i--) {
            mLoaders.valueAt(i).retain();
        }
    }

    void finishRetain() {
        if (mRetaining) {
            if (DEBUG) Log.v(TAG, "Finished Retaining in " + this);

            mRetaining = false;
            for (int i = mLoaders.size()-1; i >= 0; i--) {
                mLoaders.valueAt(i).finishRetain();
            }
        }
    }

    void doReportNextStart() {
        for (int i = mLoaders.size()-1; i >= 0; i--) {
            mLoaders.valueAt(i).mReportNextStart = true;
        }
    }

    void doReportStart() {
        for (int i = mLoaders.size()-1; i >= 0; i--) {
            mLoaders.valueAt(i).reportStart();
        }
    }

    void doDestroy() {
        if (!mRetaining) {
            if (DEBUG) Log.v(TAG, "Destroying Active in " + this);
            for (int i = mLoaders.size()-1; i >= 0; i--) {
                mLoaders.valueAt(i).destroy();
            }
            mLoaders.clear();
        }

        if (DEBUG) Log.v(TAG, "Destroying Inactive in " + this);
        for (int i = mInactiveLoaders.size()-1; i >= 0; i--) {
            mInactiveLoaders.valueAt(i).destroy();
        }
        mInactiveLoaders.clear();
        mHost = null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("LoaderManager{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(" in ");
        DebugUtils.buildShortClassTag(mHost, sb);
        sb.append("}}");
        return sb.toString();
    }

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        if (mLoaders.size() > 0) {
            writer.print(prefix); writer.println("Active Loaders:");
            String innerPrefix = prefix + "    ";
            for (int i=0; i < mLoaders.size(); i++) {
                LoaderInfo li = mLoaders.valueAt(i);
                writer.print(prefix); writer.print("  #"); writer.print(mLoaders.keyAt(i));
                writer.print(": "); writer.println(li.toString());
                li.dump(innerPrefix, fd, writer, args);
            }
        }
        if (mInactiveLoaders.size() > 0) {
            writer.print(prefix); writer.println("Inactive Loaders:");
            String innerPrefix = prefix + "    ";
            for (int i=0; i < mInactiveLoaders.size(); i++) {
                LoaderInfo li = mInactiveLoaders.valueAt(i);
                writer.print(prefix); writer.print("  #"); writer.print(mInactiveLoaders.keyAt(i));
                writer.print(": "); writer.println(li.toString());
                li.dump(innerPrefix, fd, writer, args);
            }
        }
    }

    @Override
    public boolean hasRunningLoaders() {
        boolean loadersRunning = false;
        final int count = mLoaders.size();
        for (int i = 0; i < count; i++) {
            final LoaderInfo li = mLoaders.valueAt(i);
            loadersRunning |= li.mStarted && !li.mDeliveredData;
        }
        return loadersRunning;
    }
}