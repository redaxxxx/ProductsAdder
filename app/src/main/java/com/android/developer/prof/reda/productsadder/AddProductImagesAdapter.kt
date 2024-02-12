package com.android.developer.prof.reda.productsadder

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.android.developer.prof.reda.productsadder.databinding.AddImagesItemBinding
import com.bumptech.glide.Glide

class AddProductImagesAdapter(): ListAdapter<Uri, AddProductImagesAdapter.ProductViewHolder>(DiffCallback){

    class ProductViewHolder(private val binding: AddImagesItemBinding): ViewHolder(binding.root) {
        fun bind(imgUrl: Uri){
            if (imgUrl.toString().contains("http://")){
                Glide.with(binding.addImageView.context)
                    .asBitmap()
                    .load(imgUrl.buildUpon().scheme("http").build())
                    .into(binding.addImageView)
            }else{
                binding.addImageView.setImageURI(imgUrl)
            }
        }

    }

    companion object DiffCallback: DiffUtil.ItemCallback<Uri>(){
        override fun areItemsTheSame(oldItem: Uri, newItem: Uri): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: Uri, newItem: Uri): Boolean {
            return oldItem == newItem
        }

    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        return ProductViewHolder(AddImagesItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        ))
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val imgUrl = getItem(position)
        holder.bind(imgUrl)



    }

    class OnClickListener(val clickListener: (imgUrl: Uri) -> Unit){
        fun onDelete(imgUrl: Uri) = clickListener(imgUrl)
    }


}