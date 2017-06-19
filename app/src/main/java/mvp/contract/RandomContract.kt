package mvp.contract

import com.wingsofts.gankclient.bean.FuckGoods
import com.wingsofts.gankclient.bean.JsonResult
import rx.Observable

/**
 * Created by heweizong on 2017/6/19.
 */
interface RandomContract {
    interface View {
        fun onRandom(goods: FuckGoods)
    }

    interface Model {
        fun getRandom(type: String): Observable<JsonResult<List<FuckGoods>>>
    }

    interface Presenter{
        fun getRandom(type:String)
    }
}