package mvp.model

import api.GankApi
import com.wingsofts.gankclient.bean.FuckGoods
import com.wingsofts.gankclient.bean.JsonResult
import mvp.contract.RandomContract
import rx.Observable
import kotlin.reflect.jvm.internal.impl.javax.inject.Inject

/**
 * Created by heweizong on 2017/6/19.
 */
class RandomModel @Inject constructor(private val api: GankApi) : RandomContract.Model {
    override fun getRandom(type: String): Observable<JsonResult<List<FuckGoods>>> {
        return api.getRandom(type);
    }

}