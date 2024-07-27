package com.mobdeve.s11.manlangit.bernardo.flavorscout

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.RatingBar
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class RestaurantPreviewAdapter(
    private var restaurantPreviews: List<Restaurant>,
    private val onItemClickListener: OnItemClickListener
) : RecyclerView.Adapter<RestaurantPreviewAdapter.ViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val restoNameTv: TextView = itemView.findViewById(R.id.restoNameTv)
        val restoPreviewIv: ImageView = itemView.findViewById(R.id.restoPreviewIv)
        val distanceTv: TextView = itemView.findViewById(R.id.distanceTv)
        val ratingNumberTv: TextView = itemView.findViewById(R.id.ratingNumTv)
        val ratingBar: RatingBar = itemView.findViewById(R.id.priceBar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.restaurant_preview_layout, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val preview = restaurantPreviews[position]

        with(holder) {
            restoNameTv.text = preview.name

            Glide.with(itemView.context)
                .load(preview.imageURI)
                .centerCrop()
                .into(restoPreviewIv)
            distanceTv.text = String.format("%.1f ", preview.distance)
            ratingBar.rating = preview.priceRange.toFloat()
            ratingNumberTv.text = preview.rating.toString()

            itemView.setOnClickListener {
                onItemClickListener.onItemClick(position)
            }
        }
    }

    override fun getItemCount(): Int {
        return restaurantPreviews.size
    }

    fun updateList(newList: List<Restaurant>) {
        val diffResult = DiffUtil.calculateDiff(RestaurantPreviewDiffCallback(restaurantPreviews, newList))
        restaurantPreviews = newList
        diffResult.dispatchUpdatesTo(this)
    }
}

class RestaurantPreviewDiffCallback(
    private val oldList: List<Restaurant>,
    private val newList: List<Restaurant>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}