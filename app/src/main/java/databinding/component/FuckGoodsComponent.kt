package databinding.component

import dagger.Module
import dagger.Subcomponent
import mvp.contract.FuckGoodsContract
import ui.fragment.AndroidFragment
import ui.fragment.IOSFragment

/**
 * Created by heweizong on 2017/6/20.
 */
@Subcomponent(modules = arrayOf(FuckGoodsModule::class))
interface FuckGoodsComponent{
    fun inject(fragment:AndroidFragment)

    fun inject(fragment:IOSFragment)
}

@Module
class FuckGoodsModule(private val mView:FuckGoodsContract.View){
    fun getView()=mView
}