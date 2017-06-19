package base

import android.databinding.ViewDataBinding
import android.os.Bundle
import android.support.v7.app.AppCompatActivity

/**
 * Created by heweizong on 2017/6/19.
 */
abstract class BaseBindingActivity<B: ViewDataBinding>:AppCompatActivity() {
    lateinit var mBinding:B//延迟初始化
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    abstract fun initView()
    abstract fun createDataBing(savedInstanceState: Bundle?):B
}