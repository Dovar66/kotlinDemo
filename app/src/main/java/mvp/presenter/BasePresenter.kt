package mvp.presenter

import rx.Subscription
import rx.subscriptions.CompositeSubscription

/**
 * Created by heweizong on 2017/6/19.
 */
open class BasePresenter {
    var compositeSubscription=CompositeSubscription()

    protected fun addSubscription(subscription: Subscription){
        compositeSubscription.add(subscription)
    }

    fun unSubscribe(){
        if (compositeSubscription.hasSubscriptions()){
            compositeSubscription.unsubscribe()
        }
    }
}