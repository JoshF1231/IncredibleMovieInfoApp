package com.example.incrediblemovieinfoapp.ui.all_movies

import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import com.bumptech.glide.Glide
import androidx.recyclerview.widget.RecyclerView
import com.example.incrediblemovieinfoapp.R
import com.example.incrediblemovieinfoapp.data.models.Movie
import com.example.incrediblemovieinfoapp.databinding.ItemLayoutBinding

class ItemAdapter(private val items: List<Movie>, val callBack: ItemListener) :
    RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    interface ItemListener {
        fun onItemClicked(index: Int)
        fun onItemLongClicked(index: Int)
        fun onButtonClick(index: Int)
    }

    inner class ItemViewHolder(private val binding: ItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root), OnClickListener, OnLongClickListener {
        init {
            binding.root.setOnClickListener(this)
            binding.root.setOnLongClickListener(this)
            binding.ibItemEdit.setOnClickListener {
                callBack.onButtonClick(adapterPosition)
            }
        }

        override fun onClick(p0: View?) {
            callBack.onItemClicked(adapterPosition)
        }

        override fun onLongClick(p0: View?): Boolean {
            callBack.onItemLongClicked(adapterPosition)
            return true
        }

        fun bind(movie: Movie) {
            binding.tvItemMovieTitle.text = movie.title

            Glide.with(binding.root)
                .load(movie.photo.takeIf { !it.isNullOrEmpty() } ?: R.drawable.movie_picture)
                .circleCrop()
                .into(binding.ivItemMovieImage)

            binding.rbItemMovieRating.rating = movie.rate

            val genreIds = movie.genre

            val genreNames = genreIds.joinToString(", ") { genreId ->
                binding.root.context.getString(genreId)
            }
            binding.tvItemMovieGenre.text = genreNames
        }
    }


    fun itemAt(position: Int) = items[position]

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder(
            ItemLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int {
        return items.size
    }
}