package ui.fragment

import adapter.FuckGoodsAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import base.BaseBindingFragment
import com.wingsofts.gankclient.bean.FuckGoods
import mvp.contract.FuckGoodsContract
import javax.inject.Inject
import kotlin.reflect.jvm.internal.impl.javax.inject.Inject

/**
 * Created by heweizong on 2017/6/19.
 */
class AndroidFragment:BaseBindingFragment<>,FuckGoodsContract.View {
    companion object{
        val ANDROID="android"
        fun newInstance():AndroidFragment{
            val fragment=AndroidFragment()
            val bundle=Bundle()
            fragment.arguments=bundle
            return fragment
        }
    }
    var mList=ArrayList<FuckGoods>()
    lateinit var mAdapter:FuckGoodsAdapter
    var mPage=1

    @Inject fun createDataBinding(inflater: LayoutInflater?, container:ViewGroup?, savedInstanceState:Bundle?):
}