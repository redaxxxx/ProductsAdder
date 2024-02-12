package com.android.developer.prof.reda.productsadder

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.TypedValue
import android.view.View
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isEmpty
import androidx.databinding.DataBindingUtil
import com.android.developer.prof.reda.productsadder.databinding.ActivityMainBinding
import com.google.android.material.chip.Chip
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.ByteArrayOutputStream
import java.util.Collections

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var imgList = mutableListOf<Uri>()
    private var sizeList = mutableListOf<String>()
    private var colorList = mutableListOf<String>()

    private val firesotre = Firebase.firestore
    private val firebaseStorage = Firebase.storage.reference

    private val getImages: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents(),
            ActivityResultCallback<List<Uri>>() {
                imgList.addAll(it)
                if (imgList.size > 3) {
                    imgList = imgList.subList(0, 3) as ArrayList<Uri>
                }

                val imagesAdapter = AddProductImagesAdapter()
                imagesAdapter.submitList(imgList)
                binding.addImagesProductRv.adapter = imagesAdapter
            })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        binding.addProductImagesBtn.setOnClickListener {
            getImages.launch("image/*")
        }

        setSizeChips()
        setColorChips()
    }

    private fun saveProduct(){

    }

    private fun showLoading(){
        binding.progressCircular.visibility = View.VISIBLE
    }
    private fun hideLoading(){
        binding.progressCircular.visibility = View.GONE
    }

    private fun productValidation(): Boolean{
        if(imgList.isEmpty()){
            return false
        }
        if (binding.productNameOutlinedTextLayout.isEmpty()){
            binding.productNameOutlinedTextLayout.error = "Product Name cannot be empty"
            return false
        }
        if (binding.productCategoryOutlinedTextLayout.isEmpty()){
            binding.productCategoryOutlinedTextLayout.error = "Product category cannot be empty"
            return false
        }
        if (binding.productDescriptionOutlinedTextLayout.isEmpty()){
            binding.productDescriptionOutlinedTextLayout.error = "Product description cannot be empty"
            return false
        }

        return true
    }

    private fun getImagesBytesArrays(): List<ByteArray>{
        val imagesByteArray = mutableListOf<ByteArray>()
        imgList.forEach {
            val stream = ByteArrayOutputStream()
            val imgBitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)
            if (imgBitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)) {
                val imageAsByteArray = stream.toByteArray()
                imagesByteArray.add(imageAsByteArray)
            }
        }
        return imagesByteArray
    }

    private fun setSizeChips() {
        val sizes = Collections.emptyList<String>()
        binding.addSizeChipGroup.removeAllViews()
        var ind = 1

        for (entry: Map.Entry<String, String> in sizesMap.entries) {
            val chip = Chip(this)
            chip.id = ind
            chip.tag = entry.value
            chip.text = entry.value
            chip.isCheckable = true

            if (sizes.contains(entry.value)) {
                chip.isChecked = true
//                val tag = chip.tag
                sizeList.add(chip.tag.toString())
            }

            chip.setOnCheckedChangeListener { buttonView, isChecked ->
//                val tag = Integer.parseInt(buttonView.tag.toString())
                if (!isChecked) {
                    sizeList.remove(buttonView.tag)
                } else {
                    sizeList.add(buttonView.tag.toString())
                }
            }
            binding.addSizeChipGroup.addView(chip)
            ind++
        }
        binding.addSizeChipGroup.invalidate()
    }

    private fun setColorChips(){
        val colors = Collections.emptyList<String>()
        binding.addColorChipGroup.removeAllViews()
        var ind = 1

        for (entry: Map.Entry<String,String> in colorsMap){
            val chip = Chip(this)
            chip.id = ind
            chip.tag = entry.key
            chip.chipStrokeColor = ColorStateList.valueOf(Color.BLACK)
            chip.chipStrokeWidth = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                3F,
                this.resources.displayMetrics
            )
            chip.chipBackgroundColor = ColorStateList.valueOf(Color.parseColor(entry.value))
            chip.isCheckable = true

            if (colors.contains(entry.key)){
                chip.isChecked = true
                colorList.add(chip.tag.toString())
            }

            chip.setOnCheckedChangeListener { buttonView, isChecked ->
                val tag = buttonView.tag.toString()
                if (!isChecked){
                    colorList.remove(tag)
                    Log.d("MainActivity", "size of colorList is ${colorList.size}")
                }else{
                    colorList.add(tag)
                    Log.d("MainActivity", "size of colorList is ${colorList.size}")
                }
            }
            binding.addColorChipGroup.addView(chip)
            ind++
        }
        binding.addColorChipGroup.invalidate()
    }
}