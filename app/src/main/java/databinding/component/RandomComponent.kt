package databinding.component

import com.dovar.kotlindemo.Main2Activity
import dagger.Module
import dagger.Subcomponent
import mvp.contract.RandomContract

/**
 * Created by heweizong on 2017/6/20.
 */
@Module
class RandomModule(private val mView:RandomContract.View){
    fun getView()=mView
}

@Subcomponent(modules = arrayOf(RandomModule::class))
interface RandomComponent{
    fun inject(activity:Main2Activity)
}