package com.dovar.kotlindemo

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.widget.Toast
import base.BaseBindingActivity
import com.wingsofts.gankclient.bean.FuckGoods
import mvp.contract.RandomContract
import mvp.presenter.RandomPresenter
import ui.fragment.AndroidFragment
import ui.fragment.IOSFragment
import java.net.URLEncoder
import javax.inject.Inject

class Main2Activity : BaseBindingActivity<>(), RandomContract.View {

    lateinit var mFragments:MutableList<Fragment>
    @Inject lateinit var mPresenter:RandomPresenter
    override fun initView() {
        initFragments()
    }

    override fun createDataBing(savedInstanceState: Bundle?): {
        return DataBindingUtil.setContentView(this, R.layout.activity_main2)
    }

    override fun onRandom(goods: FuckGoods) {
        val url=URLEncoder.encode(goods.url)
        Toast.makeText(this,"手气不错",Toast.LENGTH_SHORT).show()
    }



    fun initFragments(){
        mFragments=ArrayList()
        mFragments.add(AndroidFragment.newInstance())
        mFragments.add(IOSFragment.newInstance())
    }
}
