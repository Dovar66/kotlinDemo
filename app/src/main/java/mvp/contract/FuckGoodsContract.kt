package mvp.contract

import com.wingsofts.gankclient.bean.FuckGoods
import com.wingsofts.gankclient.bean.JsonResult
import rx.Observable

/**
 * Created by heweizong on 2017/6/19.
 */
interface FuckGoodsContract {
    interface View{
        fun setData(results:List<FuckGoods>)
    }

    interface Model{
        fun getData(page:Int,type:String):Observable<JsonResult<List<FuckGoods>>>
    }

    interface Presenter{
        fun getData(page: Int,type: String)
    }
}