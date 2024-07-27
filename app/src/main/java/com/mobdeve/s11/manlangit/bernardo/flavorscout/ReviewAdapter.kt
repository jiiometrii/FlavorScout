package com.mobdeve.s11.manlangit.bernardo.flavorscout

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.libraries.places.api.model.Review

class ReviewAdapter(private val reviews: List<Review>) :
    RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {

    class ReviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val usernameTv : TextView = itemView.findViewById(R.id.usernameTv) // Use actual ID from your review item layout
        val reviewTv: TextView = itemView.findViewById(R.id.ratingMessageTv) // Use actual ID
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.reviews_layout, parent, false) // Use your review item layout
        return ReviewViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val currentReview = reviews[position]
        holder.usernameTv.text = currentReview.authorAttribution.name
        holder.reviewTv.text = currentReview.text
    }

    override fun getItemCount(): Int = reviews.size
}