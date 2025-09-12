package com.remoticom.streetlighting.ui.nodes.info

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.remoticom.streetlighting.databinding.FragmentNodeInfoListItemBinding


import com.remoticom.streetlighting.ui.nodes.info.NodeInfoListFragment.OnListFragmentInteractionListener

class NodeInfoListRecyclerViewAdapter(
  private val mValues: List<NodeInfoListItem>,
  private val mListener: OnListFragmentInteractionListener?
) : RecyclerView.Adapter<NodeInfoListRecyclerViewAdapter.ViewHolder>() {

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): ViewHolder {
    val binding = FragmentNodeInfoListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)

    return ViewHolder(binding)
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val item = mValues[position]

    holder.bind(item)
  }

  override fun getItemCount(): Int = mValues.size

  inner class ViewHolder(private val binding: FragmentNodeInfoListItemBinding) : RecyclerView.ViewHolder(binding.root) {

    init {
      binding.setClickListener {
        binding.info?.let {
          mListener?.onListFragmentInteraction(it)
        }
      }
    }

    fun bind(item: NodeInfoListItem) {
      binding.apply {
        info = item
      }
    }
  }
}
