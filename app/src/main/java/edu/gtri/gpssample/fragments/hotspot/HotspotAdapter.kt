/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.fragments.hotspot

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.databinding.library.baseAdapters.BR
import androidx.recyclerview.widget.RecyclerView
import edu.gtri.gpssample.R
import edu.gtri.gpssample.viewmodels.NetworkConnectionViewModel
import kotlin.collections.ArrayList

class HotspotAdapter : RecyclerView.Adapter<HotspotAdapter.BindableViewHolder>()
{
    override fun getItemCount() = connections!!.size

    private var mContext: Context? = null
    private var allHolders = ArrayList<BindableViewHolder>()

    private var connections: List<NetworkConnectionViewModel> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BindableViewHolder
    {
        this.mContext = parent.context


        val binding: ViewDataBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.list_item_network,
            parent,
            false)


//        val viewHolder = BindableViewHolder(binding)
//        allHolders.add(viewHolder)
        return BindableViewHolder(binding)

    }

    fun updateConnections( connections: List<NetworkConnectionViewModel>? )
    {
       // val diffResult = DiffUtil.calculateDiff(DiffUtilCallback(this.connections, connections ?: emptyList()), false)
        this.connections = connections ?: emptyList()
        connections?.let{
            for(item in it)
            {
            }
        }

        notifyDataSetChanged()
      //  diffResult.dispatchUpdatesTo(this)
    }

    override fun onBindViewHolder(holder: BindableViewHolder, position: Int)
    {
        holder.itemView.isSelected = false
        holder.bind(connections[position])

    }

    class BindableViewHolder(private val binding: ViewDataBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(networkConnectionViewModel: NetworkConnectionViewModel) {
            binding.setVariable(BR.networkConnectionViewModel, networkConnectionViewModel)
        }
    }
}

//class DiffUtilCallback(
//    val old: List<NetworkConnectionViewModel>,
//    val new: List<NetworkConnectionViewModel>
//) : DiffUtil.Callback() {
//    override fun getOldListSize(): Int {
//        return old.size
//    }
//
//    override fun getNewListSize(): Int {
//        return new.size
//    }
//
//    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
//        val oldItem = old[oldItemPosition]
//        val newItem = new[newItemPosition]
//        return newItem.areItemsTheSame(oldItem)
//    }
//
//    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
//        val oldItem = old[oldItemPosition]
//        val newItem = new[newItemPosition]
//        return newItem.areContentsTheSame(oldItem)
//    }
//}

@BindingAdapter("connectionViewModels")
fun bindConnectionViewModels(recyclerView: RecyclerView, itemViewModels: List<NetworkConnectionViewModel>?) {
    val adapter = getOrCreateAdapter(recyclerView)
    adapter.updateConnections(itemViewModels)
}

private fun getOrCreateAdapter(recyclerView: RecyclerView): HotspotAdapter {
    return if (recyclerView.adapter != null && recyclerView.adapter is HotspotAdapter) {
        recyclerView.adapter as HotspotAdapter
    } else {
        val bindableRecyclerAdapter = HotspotAdapter()
        recyclerView.adapter = bindableRecyclerAdapter
        bindableRecyclerAdapter
    }
}