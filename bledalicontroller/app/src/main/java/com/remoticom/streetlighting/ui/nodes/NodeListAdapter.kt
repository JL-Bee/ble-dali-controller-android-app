package com.remoticom.streetlighting.ui.nodes

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import com.remoticom.streetlighting.data.Node
import com.remoticom.streetlighting.databinding.ItemNodeListBinding

import com.remoticom.streetlighting.ui.nodes.NodeListFragment.OnListFragmentInteractionListener


class NodeListAdapter(
  private val mListener: OnListFragmentInteractionListener?
) :
  androidx.recyclerview.widget.ListAdapter<Node, NodeListAdapter.NodeViewHolder>(
    NodeDiffCallback()
  ) {

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): NodeViewHolder {
    return NodeViewHolder(
      ItemNodeListBinding.inflate(
        LayoutInflater.from(parent.context),
        parent,
        false
      )
    )
  }

  override fun onBindViewHolder(holder: NodeViewHolder, position: Int) {
    val node = getItem(position)

    holder.bind(node)
  }

  inner class NodeViewHolder(private val binding: ItemNodeListBinding) :
    RecyclerView.ViewHolder(binding.root) {

    init {
      binding.setClickListener {
        binding.node?.let {
          mListener?.onNodeSelected(it)
        }
      }
      binding.setNodeButtonClickListener {
        binding.node?.let {
          mListener?.onNodeButtonClicked(it)
        }
      }
    }

    fun bind(item: Node) {
      binding.apply {
        node = item
      }
    }
  }
}

private class NodeDiffCallback : DiffUtil.ItemCallback<Node>() {

  override fun areItemsTheSame(oldItem: Node, newItem: Node): Boolean {
    // Assuming each BT device (in range) has different/unique id
    return oldItem.id == newItem.id
  }

  override fun areContentsTheSame(oldItem: Node, newItem: Node): Boolean {
    // Works because Node is data class (should have equals implementation on content)
    return oldItem == newItem
  }
}
