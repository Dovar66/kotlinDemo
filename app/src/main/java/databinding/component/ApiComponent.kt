package databinding.component

import dagger.Component
import databinding.module.ApiModule

/**
 * Created by heweizong on 2017/6/20.
 */
@Component(modules = arrayOf(ApiModule::class))
interface ApiComponent {
    fun inject(app: App)

    fun plus(module:FuckGoodsModule):FuckGoodsComponent

    fun plus(module:RandomModule):RandomComponent
}