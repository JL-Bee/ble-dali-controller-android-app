package com.remoticom.streetlighting.ui.nodes.info

data class NodeInfoListItem(
  val label: String,
  val value: String,
  val type: NodeInfoListItemType? = null,
  val editable: Boolean = false,
  val status: NodeInfoListItemStatus = NodeInfoListItemStatus.Regular
) {

}

enum class NodeInfoListItemType {
  Owner,
  AssetName
}

enum class NodeInfoListItemStatus {
  Regular,
  Success,
  Error
}
