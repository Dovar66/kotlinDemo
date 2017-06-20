package adapter

import base.BaseBindingAdapter
import com.wingsofts.gankclient.bean.FuckGoods

/**
 * Created by heweizong on 2017/6/19.
 */
class FuckGoodsAdapter (private val mList:List<FuckGoods>):BaseBindingAdapter<ItemFuckgoodsBinding>{
    override fun getItemCount(): Int {
        return mList.size
    }

//    override fun onBindViewHolder(holder: DataBoundViewHolder<>?, position: Int) {
//        super.onBindViewHolder(holder, position)
//        holder.binding.fuckgoods=mList[position]
//        holder.binding.executePendingBindings()
//    }

}