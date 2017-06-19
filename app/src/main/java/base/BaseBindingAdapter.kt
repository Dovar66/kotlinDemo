package base

import android.databinding.ViewDataBinding
import android.support.v7.widget.RecyclerView

/**
 * Created by heweizong on 2017/6/19.
 */
abstract class BaseBindingAdapter<B : ViewDataBinding> : RecyclerView.Adapter<BaseBindingAdapter.DataBoundViewHolder<B>>() {
    var mListener: ((pos: Int) -> Unit)? = null
    override fun onBindViewHolder(holder: DataBoundViewHolder<B>, position: Int) {
        holder.binding.root.setOnClickListener { mListener?.invoke(holder.adapterPosition) }
    }

    class DataBoundViewHolder<T : ViewDataBinding>(val binding: T) : RecyclerView.ViewHolder(binding.root)

    fun setOnItemClickListener(listener: (pos: Int) -> Unit) {
        mListener = listener
    }
}