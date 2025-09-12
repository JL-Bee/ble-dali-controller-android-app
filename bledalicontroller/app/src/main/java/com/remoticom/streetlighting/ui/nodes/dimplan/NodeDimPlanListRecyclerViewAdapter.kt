package com.remoticom.streetlighting.ui.nodes.dimplan

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.remoticom.streetlighting.databinding.FragmentNodeDimPlanListItemBinding

class NodeDimPlanListRecyclerViewAdapter(
  private val values: List<NodeDimPlanListItem>,
  private val listener: NodeDimPlanListFragment.OnListFragmentInteractionListener?
) : RecyclerView.Adapter<NodeDimPlanListRecyclerViewAdapter.ViewHolder>() {

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): ViewHolder {
    val binding = FragmentNodeDimPlanListItemBinding.inflate(
      LayoutInflater.from(parent.context),
      parent,
      false
    )

    return ViewHolder(binding)
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val item = values[position]
    holder.bind(item)
  }

  override fun getItemCount(): Int = values.size

  inner class ViewHolder(private val binding: FragmentNodeDimPlanListItemBinding) : RecyclerView.ViewHolder(binding.root) {

    init {
      binding.setClickListener {
        binding.info?.let {
          listener?.onListFragmentInteraction(it)
        }
      }
    }

    fun bind(item: NodeDimPlanListItem) {
      binding.apply {
        info = item
      }
    }
  }
}
