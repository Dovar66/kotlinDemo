package mvp.presenter

import mvp.contract.RandomContract
import mvp.model.RandomModel
import rx.android.schedulers.AndroidSchedulers
import javax.inject.Inject

/**
 * Created by heweizong on 2017/6/19.
 */
class RandomPresenter @Inject constructor(private val mModel:RandomModel,private val mView:RandomContract.View):RandomContract.Presenter,BasePresenter(){
    override fun getRandom(type: String) {
        addSubscription(mModel.getRandom(type).observeOn(AndroidSchedulers.mainThread()).subscribe({
            res->
            if (!res.error){
                mView.onRandom(res.results[0])
            }
        }))
    }
}