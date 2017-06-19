package com.dovar.kotlindemo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentContainer;
import android.support.v4.app.LoaderManager;
import android.support.v4.util.SimpleArrayMap;
import android.view.LayoutInflater;
import android.view.View;

import java.io.FileDescriptor;
import java.io.PrintWriter;

/**
 * Created by heweizong on 2017/5/19.
 */

public abstract class MyFragmentHostCallBack<E> extends FragmentContainer {
    private final Activity mActivity;
    final Context mContext;
    private final Handler mHandler;
    final int mWindowAnimations;
    final MyFragmentManagerImp mFragmentManager = new MyFragmentManagerImp();
    /**
     * The loader managers for individual fragments [i.e. Fragment#getLoaderManager()]
     */
    private SimpleArrayMap<String, LoaderManager> mAllLoaderManagers;
    /**
     * Whether or not fragment loaders should retain their state
     */
    private boolean mRetainLoaders;
    /**
     * The loader manger for the fragment host [i.e. Activity#getLoaderManager()]
     */
    private LoaderManagerImpl mLoaderManager;
    private boolean mCheckedForLoaderManager;
    /**
     * Whether or not the fragment host loader manager was started
     */
    private boolean mLoadersStarted;

    public MyFragmentHostCallBack(Context context,
                                  Handler handler,
                                  int windowAnimations) {
        this(context instanceof Activity ? (Activity) context : null, context,
                handler,
                windowAnimations);
    }

    MyFragmentHostCallBack(FragmentActivity activity) {
        this(activity, activity /*context*/
                , null//activity.mHandler
                , 0 /*windowAnimations*/);
    }

    MyFragmentHostCallBack(Activity activity, Context context,
                           Handler handler,
                           int windowAnimations) {
        mActivity = activity;
        mContext = context;
        mHandler = handler;
        mWindowAnimations = windowAnimations;
    }

    /**
     * Print internal state into the given stream.
     *
     * @param prefix Desired prefix to prepend at each line of output.
     * @param fd     The raw file descriptor that the dump is being sent to.
     * @param writer The PrintWriter to which you should dump your state. This will be closed
     *               for you after you return.
     * @param args   additional arguments to the dump request.
     */
    public void onDump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
    }

    /**
     * Return {@code true} if the fragment's state needs to be saved.
     */
    public boolean onShouldSaveFragmentState(MyFragment fragment) {
        return true;
    }

    /**
     * Return a {@link LayoutInflater}.
     * See {@link Activity#getLayoutInflater()}.
     */
    public LayoutInflater onGetLayoutInflater() {
        return (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    /**
     * Return the object that's currently hosting the fragment. If a {@link MyFragment}
     * is hosted by a {@link FragmentActivity}, the object returned here should be
     * the same object returned from {@link MyFragment#getActivity()}.
     */
    @Nullable
    public abstract E onGetHost();

    /**
     * Invalidates the activity's options menu.
     * See {@link FragmentActivity#supportInvalidateOptionsMenu()}
     */
    public void onSupportInvalidateOptionsMenu() {
    }

    /**
     * Starts a new {@link Activity} from the given fragment.
     * See {@link FragmentActivity#startActivityForResult(Intent, int)}.
     */
    public void onStartActivityFromFragment(MyFragment fragment, Intent intent, int requestCode) {
        onStartActivityFromFragment(fragment, intent, requestCode, null);
    }

    /**
     * Starts a new {@link Activity} from the given fragment.
     * See {@link FragmentActivity#startActivityForResult(Intent, int, Bundle)}.
     */
    public void onStartActivityFromFragment(
            MyFragment fragment, Intent intent, int requestCode, @Nullable Bundle options) {
        if (requestCode != -1) {
            throw new IllegalStateException(
                    "Starting activity with a requestCode requires a FragmentActivity host");
        }
        mContext.startActivity(intent);
    }

    /**
     * Starts a new {@link IntentSender} from the given fragment.
     * See {@link Activity#startIntentSender(IntentSender, Intent, int, int, int, Bundle)}.
     */
    public void onStartIntentSenderFromFragment(MyFragment fragment, IntentSender intent,
                                                int requestCode, @Nullable Intent fillInIntent, int flagsMask, int flagsValues,
                                                int extraFlags, Bundle options) throws IntentSender.SendIntentException {
        if (requestCode != -1) {
            throw new IllegalStateException(
                    "Starting intent sender with a requestCode requires a FragmentActivity host");
        }
        ActivityCompat.startIntentSenderForResult(mActivity, intent, requestCode, fillInIntent,
                flagsMask, flagsValues, extraFlags, options);
    }

    /**
     * Requests permissions from the given fragment.
     * See {@link FragmentActivity#requestPermissions(String[], int)}
     */
    public void onRequestPermissionsFromFragment(@NonNull MyFragment fragment,
                                                 @NonNull String[] permissions, int requestCode) {
    }

    /**
     * Checks whether to show permission rationale UI from a fragment.
     * See {@link FragmentActivity#shouldShowRequestPermissionRationale(String)}
     */
    public boolean onShouldShowRequestPermissionRationale(@NonNull String permission) {
        return false;
    }

    /**
     * Return {@code true} if there are window animations.
     */
    public boolean onHasWindowAnimations() {
        return true;
    }

    /**
     * Return the window animations.
     */
    public int onGetWindowAnimations() {
        return mWindowAnimations;
    }

    @Nullable
    @Override
    public View onFindViewById(int id) {
        return null;
    }

    @Override
    public boolean onHasView() {
        return true;
    }

    Activity getActivity() {
        return mActivity;
    }

    Context getContext() {
        return mContext;
    }

    Handler getHandler() {
        return mHandler;
    }

    MyFragmentManagerImp getFragmentManagerImp() {
        return mFragmentManager;
    }

    LoaderManagerImpl getLoaderManagerImpl() {
        if (mLoaderManager != null) {
            return mLoaderManager;
        }
        mCheckedForLoaderManager = true;
        mLoaderManager = getLoaderManager("(root)", mLoadersStarted, true /*create*/);
        return mLoaderManager;
    }

    void inactivateFragment(String who) {
        //Log.v(TAG, "invalidateSupportFragment: who=" + who);
        if (mAllLoaderManagers != null) {
            LoaderManagerImpl lm = (LoaderManagerImpl) mAllLoaderManagers.get(who);
            if (lm != null && !lm.mRetaining) {
                lm.doDestroy();
                mAllLoaderManagers.remove(who);
            }
        }
    }

    void onAttachFragment(MyFragment fragment) {
    }

    boolean getRetainLoaders() {
        return mRetainLoaders;
    }

    void doLoaderStart() {
        if (mLoadersStarted) {
            return;
        }
        mLoadersStarted = true;

        if (mLoaderManager != null) {
            mLoaderManager.doStart();
        } else if (!mCheckedForLoaderManager) {
            mLoaderManager = getLoaderManager("(root)", mLoadersStarted, false);
            // the returned loader manager may be a new one, so we have to start it
            if ((mLoaderManager != null) && (!mLoaderManager.mStarted)) {
                mLoaderManager.doStart();
            }
        }
        mCheckedForLoaderManager = true;
    }

    // retain -- whether to stop the loader or retain it
    void doLoaderStop(boolean retain) {
        mRetainLoaders = retain;

        if (mLoaderManager == null) {
            return;
        }

        if (!mLoadersStarted) {
            return;
        }
        mLoadersStarted = false;

        if (retain) {
            mLoaderManager.doRetain();
        } else {
            mLoaderManager.doStop();
        }
    }

    void doLoaderRetain() {
        if (mLoaderManager == null) {
            return;
        }
        mLoaderManager.doRetain();
    }

    void doLoaderDestroy() {
        if (mLoaderManager == null) {
            return;
        }
        mLoaderManager.doDestroy();
    }

    void reportLoaderStart() {
        if (mAllLoaderManagers != null) {
            final int N = mAllLoaderManagers.size();
            LoaderManagerImpl loaders[] = new LoaderManagerImpl[N];
            for (int i = N - 1; i >= 0; i--) {
                loaders[i] = (LoaderManagerImpl) mAllLoaderManagers.valueAt(i);
            }
            for (int i = 0; i < N; i++) {
                LoaderManagerImpl lm = loaders[i];
                lm.finishRetain();
                lm.doReportStart();
            }
        }
    }

    LoaderManagerImpl getLoaderManager(String who, boolean started, boolean create) {
        if (mAllLoaderManagers == null) {
            mAllLoaderManagers = new SimpleArrayMap<String, LoaderManager>();
        }
        LoaderManagerImpl lm = (LoaderManagerImpl) mAllLoaderManagers.get(who);
        if (lm == null && create) {
            lm = new LoaderManagerImpl(who, this, started);
            mAllLoaderManagers.put(who, lm);
        } else if (started && lm != null && !lm.mStarted) {
            lm.doStart();
        }
        return lm;
    }

    SimpleArrayMap<String, LoaderManager> retainLoaderNonConfig() {
        boolean retainLoaders = false;
        if (mAllLoaderManagers != null) {
            // Restart any loader managers that were already stopped so that they
            // will be ready to retain
            final int N = mAllLoaderManagers.size();
            LoaderManagerImpl loaders[] = new LoaderManagerImpl[N];
            for (int i = N - 1; i >= 0; i--) {
                loaders[i] = (LoaderManagerImpl) mAllLoaderManagers.valueAt(i);
            }
            final boolean doRetainLoaders = getRetainLoaders();
            for (int i = 0; i < N; i++) {
                LoaderManagerImpl lm = loaders[i];
                if (!lm.mRetaining && doRetainLoaders) {
                    if (!lm.mStarted) {
                        lm.doStart();
                    }
                    lm.doRetain();
                }
                if (lm.mRetaining) {
                    retainLoaders = true;
                } else {
                    lm.doDestroy();
                    mAllLoaderManagers.remove(lm.mWho);
                }
            }
        }

        if (retainLoaders) {
            return mAllLoaderManagers;
        }
        return null;
    }

    void restoreLoaderNonConfig(SimpleArrayMap<String, LoaderManager> loaderManagers) {
        if (loaderManagers != null) {
            final int numLoaderManagers = loaderManagers.size();
            for (int i = 0; i < numLoaderManagers; i++) {
                ((LoaderManagerImpl) loaderManagers.valueAt(i)).updateHostController(this);
            }
        }
        mAllLoaderManagers = loaderManagers;
    }

    void dumpLoaders(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        writer.print(prefix);
        writer.print("mLoadersStarted=");
        writer.println(mLoadersStarted);
        if (mLoaderManager != null) {
            writer.print(prefix);
            writer.print("Loader Manager ");
            writer.print(Integer.toHexString(System.identityHashCode(mLoaderManager)));
            writer.println(":");
            mLoaderManager.dump(prefix + "  ", fd, writer, args);
        }
    }
}
