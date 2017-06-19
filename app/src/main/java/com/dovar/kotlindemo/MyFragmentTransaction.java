package com.dovar.kotlindemo;

import android.support.annotation.AnimRes;
import android.support.annotation.IdRes;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.StringRes;
import android.support.annotation.StyleRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.View;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

/**
 * Created by heweizong on 2017/5/19.
 */

public abstract class MyFragmentTransaction {
    /**
     * Calls {@link #add(int, MyFragment, String)} with a 0 containerViewId.
     */
    public abstract MyFragmentTransaction add(MyFragment fragment, String tag);

    /**
     * Calls {@link #add(int, MyFragment, String)} with a null tag.
     */
    public abstract MyFragmentTransaction add(@IdRes int containerViewId, MyFragment fragment);

    /**
     * Add a fragment to the activity state.  This fragment may optionally
     * also have its view (if {@link Fragment#onCreateView Fragment.onCreateView}
     * returns non-null) into a container view of the activity.
     *
     * @param containerViewId Optional identifier of the container this fragment is
     * to be placed in.  If 0, it will not be placed in a container.
     * @param fragment The fragment to be added.  This fragment must not already
     * be added to the activity.
     * @param tag Optional tag name for the fragment, to later retrieve the
     * fragment with {@link FragmentManager#findFragmentByTag(String)
     * FragmentManager.findFragmentByTag(String)}.
     *
     * @return Returns the same MyFragmentTransaction instance.
     */
    public abstract MyFragmentTransaction add(@IdRes int containerViewId, MyFragment fragment,
                                            @Nullable String tag);

    /**
     * Calls {@link #replace(int, MyFragment, String)} with a null tag.
     */
    public abstract MyFragmentTransaction replace(@IdRes int containerViewId, MyFragment fragment);

    /**
     * Replace an existing fragment that was added to a container.  This is
     * essentially the same as calling {@link #remove(MyFragment)} for all
     * currently added fragments that were added with the same containerViewId
     * and then {@link #add(int, MyFragment, String)} with the same arguments
     * given here.
     *
     * @param containerViewId Identifier of the container whose fragment(s) are
     * to be replaced.
     * @param fragment The new fragment to place in the container.
     * @param tag Optional tag name for the fragment, to later retrieve the
     * fragment with {@link FragmentManager#findFragmentByTag(String)
     * FragmentManager.findFragmentByTag(String)}.
     *
     * @return Returns the same MyFragmentTransaction instance.
     */
    public abstract MyFragmentTransaction replace(@IdRes int containerViewId, MyFragment fragment,
                                                @Nullable String tag);

    /**
     * Remove an existing fragment.  If it was added to a container, its view
     * is also removed from that container.
     *
     * @param fragment The fragment to be removed.
     *
     * @return Returns the same MyFragmentTransaction instance.
     */
    public abstract MyFragmentTransaction remove(MyFragment fragment);

    /**
     * Hides an existing fragment.  This is only relevant for fragments whose
     * views have been added to a container, as this will cause the view to
     * be hidden.
     *
     * @param fragment The fragment to be hidden.
     *
     * @return Returns the same MyFragmentTransaction instance.
     */
    public abstract MyFragmentTransaction hide(MyFragment fragment);

    /**
     * Shows a previously hidden fragment.  This is only relevant for fragments whose
     * views have been added to a container, as this will cause the view to
     * be shown.
     *
     * @param fragment The fragment to be shown.
     *
     * @return Returns the same MyFragmentTransaction instance.
     */
    public abstract MyFragmentTransaction show(MyFragment fragment);

    /**
     * Detach the given fragment from the UI.  This is the same state as
     * when it is put on the back stack: the fragment is removed from
     * the UI, however its state is still being actively managed by the
     * fragment manager.  When going into this state its view hierarchy
     * is destroyed.
     *
     * @param fragment The fragment to be detached.
     *
     * @return Returns the same MyFragmentTransaction instance.
     */
    public abstract MyFragmentTransaction detach(MyFragment fragment);

    /**
     * Re-attach a fragment after it had previously been detached from
     * the UI with {@link #detach(MyFragment)}.  This
     * causes its view hierarchy to be re-created, attached to the UI,
     * and displayed.
     *
     * @param fragment The fragment to be attached.
     *
     * @return Returns the same MyFragmentTransaction instance.
     */
    public abstract MyFragmentTransaction attach(MyFragment fragment);

    /**
     * @return <code>true</code> if this transaction contains no operations,
     * <code>false</code> otherwise.
     */
    public abstract boolean isEmpty();

    /**
     * Bit mask that is set for all enter transitions.
     */
    public static final int TRANSIT_ENTER_MASK = 0x1000;

    /**
     * Bit mask that is set for all exit transitions.
     */
    public static final int TRANSIT_EXIT_MASK = 0x2000;

    /** @hide */
    @RestrictTo(LIBRARY_GROUP)
    @IntDef({TRANSIT_NONE, TRANSIT_FRAGMENT_OPEN, TRANSIT_FRAGMENT_CLOSE, TRANSIT_FRAGMENT_FADE})
    @Retention(RetentionPolicy.SOURCE)
    private @interface Transit {}

    /** Not set up for a transition. */
    public static final int TRANSIT_UNSET = -1;
    /** No animation for transition. */
    public static final int TRANSIT_NONE = 0;
    /** Fragment is being added onto the stack */
    public static final int TRANSIT_FRAGMENT_OPEN = 1 | TRANSIT_ENTER_MASK;
    /** Fragment is being removed from the stack */
    public static final int TRANSIT_FRAGMENT_CLOSE = 2 | TRANSIT_EXIT_MASK;
    /** Fragment should simply fade in or out; that is, no strong navigation associated
     * with it except that it is appearing or disappearing for some reason. */
    public static final int TRANSIT_FRAGMENT_FADE = 3 | TRANSIT_ENTER_MASK;

    /**
     * Set specific animation resources to run for the fragments that are
     * entering and exiting in this transaction. These animations will not be
     * played when popping the back stack.
     */
    public abstract MyFragmentTransaction setCustomAnimations(@AnimRes int enter,
                                                            @AnimRes int exit);

    /**
     * Set specific animation resources to run for the fragments that are
     * entering and exiting in this transaction. The <code>popEnter</code>
     * and <code>popExit</code> animations will be played for enter/exit
     * operations specifically when popping the back stack.
     */
    public abstract MyFragmentTransaction setCustomAnimations(@AnimRes int enter,
                                                            @AnimRes int exit, @AnimRes int popEnter, @AnimRes int popExit);

    /**
     * Used with custom Transitions to map a View from a removed or hidden
     * Fragment to a View from a shown or added Fragment.
     * <var>sharedElement</var> must have a unique transitionName in the View hierarchy.
     *
     * @param sharedElement A View in a disappearing Fragment to match with a View in an
     *                      appearing Fragment.
     * @param name The transitionName for a View in an appearing Fragment to match to the shared
     *             element.
     * @see Fragment#setSharedElementReturnTransition(Object)
     * @see Fragment#setSharedElementEnterTransition(Object)
     */
    public abstract MyFragmentTransaction addSharedElement(View sharedElement, String name);

    /**
     * Select a standard transition animation for this transaction.  May be
     * one of {@link #TRANSIT_NONE}, {@link #TRANSIT_FRAGMENT_OPEN},
     * {@link #TRANSIT_FRAGMENT_CLOSE}, or {@link #TRANSIT_FRAGMENT_FADE}.
     */
    public abstract MyFragmentTransaction setTransition(@Transit int transit);

    /**
     * Set a custom style resource that will be used for resolving transit
     * animations.
     */
    public abstract MyFragmentTransaction setTransitionStyle(@StyleRes int styleRes);

    /**
     * Add this transaction to the back stack.  This means that the transaction
     * will be remembered after it is committed, and will reverse its operation
     * when later popped off the stack.
     *
     * @param name An optional name for this back stack state, or null.
     */
    public abstract MyFragmentTransaction addToBackStack(@Nullable String name);

    /**
     * Returns true if this MyFragmentTransaction is allowed to be added to the back
     * stack. If this method would return false, {@link #addToBackStack(String)}
     * will throw {@link IllegalStateException}.
     *
     * @return True if {@link #addToBackStack(String)} is permitted on this transaction.
     */
    public abstract boolean isAddToBackStackAllowed();

    /**
     * Disallow calls to {@link #addToBackStack(String)}. Any future calls to
     * addToBackStack will throw {@link IllegalStateException}. If addToBackStack
     * has already been called, this method will throw IllegalStateException.
     */
    public abstract MyFragmentTransaction disallowAddToBackStack();

    /**
     * Set the full title to show as a bread crumb when this transaction
     * is on the back stack.
     *
     * @param res A string resource containing the title.
     */
    public abstract MyFragmentTransaction setBreadCrumbTitle(@StringRes int res);

    /**
     * Like {@link #setBreadCrumbTitle(int)} but taking a raw string; this
     * method is <em>not</em> recommended, as the string can not be changed
     * later if the locale changes.
     */
    public abstract MyFragmentTransaction setBreadCrumbTitle(CharSequence text);

    /**
     * Set the short title to show as a bread crumb when this transaction
     * is on the back stack.
     *
     * @param res A string resource containing the title.
     */
    public abstract MyFragmentTransaction setBreadCrumbShortTitle(@StringRes int res);

    /**
     * Like {@link #setBreadCrumbShortTitle(int)} but taking a raw string; this
     * method is <em>not</em> recommended, as the string can not be changed
     * later if the locale changes.
     */
    public abstract MyFragmentTransaction setBreadCrumbShortTitle(CharSequence text);

    /**
     * Sets whether or not to allow optimizing operations within and across
     * transactions. Optimizing fragment transaction's operations can eliminate
     * operations that cancel. For example, if two transactions are executed
     * together, one that adds a fragment A and the next replaces it with fragment B,
     * the operations will cancel and only fragment B will be added. That means that
     * fragment A may not go through the creation/destruction lifecycle.
     * <p>
     * The side effect of optimization is that fragments may have state changes
     * out of the expected order. For example, one transaction adds fragment A,
     * a second adds fragment B, then a third removes fragment A. Without optimization,
     * fragment B could expect that while it is being created, fragment A will also
     * exist because fragment A will be removed after fragment B was added.
     * With optimization, fragment B cannot expect fragment A to exist when
     * it has been created because fragment A's add/remove will be optimized out.
     * <p>
     * The default is {@code false}.
     *
     * @param allowOptimization {@code true} to enable optimizing operations
     *                          or {@code false} to disable optimizing
     *                          operations on this transaction.
     */
    public abstract MyFragmentTransaction setAllowOptimization(boolean allowOptimization);

    /**
     * Schedules a commit of this transaction.  The commit does
     * not happen immediately; it will be scheduled as work on the main thread
     * to be done the next time that thread is ready.
     *
     * <p class="note">A transaction can only be committed with this method
     * prior to its containing activity saving its state.  If the commit is
     * attempted after that point, an exception will be thrown.  This is
     * because the state after the commit can be lost if the activity needs to
     * be restored from its state.  See {@link #commitAllowingStateLoss()} for
     * situations where it may be okay to lose the commit.</p>
     *
     * @return Returns the identifier of this transaction's back stack entry,
     * if {@link #addToBackStack(String)} had been called.  Otherwise, returns
     * a negative number.
     */
    public abstract int commit();

    /**
     * Like {@link #commit} but allows the commit to be executed after an
     * activity's state is saved.  This is dangerous because the commit can
     * be lost if the activity needs to later be restored from its state, so
     * this should only be used for cases where it is okay for the UI state
     * to change unexpectedly on the user.
     */
    public abstract int commitAllowingStateLoss();

    /**
     * Commits this transaction synchronously. Any added fragments will be
     * initialized and brought completely to the lifecycle state of their host
     * and any removed fragments will be torn down accordingly before this
     * call returns. Committing a transaction in this way allows fragments
     * to be added as dedicated, encapsulated components that monitor the
     * lifecycle state of their host while providing firmer ordering guarantees
     * around when those fragments are fully initialized and ready. Fragments
     * that manage views will have those views created and attached.
     *
     * <p>Calling <code>commitNow</code> is preferable to calling
     * {@link #commit()} followed by {@link FragmentManager#executePendingTransactions()}
     * as the latter will have the side effect of attempting to commit <em>all</em>
     * currently pending transactions whether that is the desired behavior
     * or not.</p>
     *
     * <p>Transactions committed in this way may not be added to the
     * FragmentManager's back stack, as doing so would break other expected
     * ordering guarantees for other asynchronously committed transactions.
     * This method will throw {@link IllegalStateException} if the transaction
     * previously requested to be added to the back stack with
     * {@link #addToBackStack(String)}.</p>
     *
     * <p class="note">A transaction can only be committed with this method
     * prior to its containing activity saving its state.  If the commit is
     * attempted after that point, an exception will be thrown.  This is
     * because the state after the commit can be lost if the activity needs to
     * be restored from its state.  See {@link #commitAllowingStateLoss()} for
     * situations where it may be okay to lose the commit.</p>
     */
    public abstract void commitNow();

    /**
     * Like {@link #commitNow} but allows the commit to be executed after an
     * activity's state is saved.  This is dangerous because the commit can
     * be lost if the activity needs to later be restored from its state, so
     * this should only be used for cases where it is okay for the UI state
     * to change unexpectedly on the user.
     */
    public abstract void commitNowAllowingStateLoss();
}
