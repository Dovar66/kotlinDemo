
import android.app.Application
import databinding.component.ApiComponent
import kotlin.reflect.jvm.internal.impl.javax.inject.Inject

/**
 * Created by heweizong on 2017/6/20.
 */
class App :Application(){

    companion object{
        lateinit var instance:App
    }
    init {
        instance=this
    }

    @Inject lateinit var apiComponent:ApiComponent
    override fun onCreate() {
        super.onCreate()

    }
}