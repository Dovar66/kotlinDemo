package mvp.model

import api.GankApi
import com.wingsofts.gankclient.bean.FuckGoods
import com.wingsofts.gankclient.bean.JsonResult
import mvp.contract.FuckGoodsContract
import rx.Observable
import ui.fragment.AndroidFragment
import ui.fragment.IOSFragment
import kotlin.reflect.jvm.internal.impl.javax.inject.Inject

/**
 * Created by heweizong on 2017/6/19.
 */
class FuckGoodsModel @Inject constructor(private val api: GankApi) : FuckGoodsContract.Model {
    override fun getData(page: Int, type: String): Observable<JsonResult<List<FuckGoods>>> {
        when (type) {
            AndroidFragment.ANDROID -> return api.getAndroidData(page)
            IOSFragment.IOS ->return api.getiOSData(page)
            else ->return api.getAndroidData(page)
        }
    }

}