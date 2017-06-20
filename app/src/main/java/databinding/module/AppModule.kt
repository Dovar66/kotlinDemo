package databinding.module

import android.content.Context
import dagger.Module
import dagger.Provides

/**
 * Created by heweizong on 2017/6/20.
 */
@Module
class AppModule(private val context:Context) {
    @Provides fun provideContext()=context
}