package com.android.developer.prof.reda.productsadder

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isEmpty
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.android.developer.prof.reda.productsadder.databinding.ActivityMainBinding
import com.google.android.material.chip.Chip
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.util.Collections
import java.util.UUID

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

        binding.addProductBtn.setOnClickListener {
            if (productValidation()){
                saveProduct {
                    Log.d("Save Product", it.toString())
                }
            }
        }
    }

    private fun saveProduct(state: (Boolean)-> Unit){
        val imagesBytesArrays = getImagesBytesArrays()
        val sizes = sizeList
        val colors = colorList
        val productName = binding.productNameEditText.text?.trim().toString()
        val images = mutableListOf<String>()
        val productCategory = binding.productCategoryEditText.text?.trim().toString()
        val productDescription = binding.productDescriptionEditText.text?.trim().toString()
        val productPrice = binding.productPriceEditText.text?.trim().toString()
        val productOffer = binding.productOfferEditText.text?.trim().toString()

        lifecycleScope.launch {
            showLoading()
            try {
                async {
                    imagesBytesArrays.forEach {
                        val id = UUID.randomUUID().toString()
                        launch {
                            val imagesStorage = firebaseStorage.child("products/images/$id")
                            val result = imagesStorage.putBytes(it).await()
                            val downloadUrl = result.storage.downloadUrl.await().toString()
                            images.add(downloadUrl)
                        }
                    }
                }.await()
            }catch (e: Exception){
                hideLoading()
                state(false)
                e.printStackTrace()
            }

            val product = Product(
                UUID.randomUUID().toString(),
                productName,
                productCategory,
                productPrice.toFloat(),
                if (productOffer.isEmpty()) null else productOffer.toFloat(),
                productDescription,
                colors,
                sizes,
                images
            )

            firesotre.collection("products").add(product)
                .addOnSuccessListener {
                    state(true)

                    binding.productNameEditText.text?.clear()
                    binding.productCategoryEditText.text?.clear()
                    binding.productPriceEditText.text?.clear()
                    binding.productOfferEditText.text?.clear()
                    binding.productDescriptionEditText.text?.clear()
                    imgList.clear()
                    colorList.clear()
                    sizeList.clear()
                    binding.addImagesProductRv.adapter = null
                    binding.addSizeChipGroup.clearCheck()
                    binding.addColorChipGroup.clearCheck()
                    hideLoading()
                }
                .addOnFailureListener{
                    Log.d("Save Product", it.message.toString())
                    state(false)
                    hideLoading()
                }
        }
    }

    private fun showLoading(){
        binding.progressBarFrame.visibility = View.VISIBLE
        binding.progressCircular.visibility = View.VISIBLE
    }
    private fun hideLoading(){
        binding.progressBarFrame.visibility = View.GONE
        binding.progressCircular.visibility = View.GONE
    }

    private fun productValidation(): Boolean{
        if(imgList.isEmpty()){
            return false
        }
        if (colorList.isEmpty()){
            return false
        }
        if (sizeList.isEmpty()){
            return false
        }
        if (TextUtils.isEmpty(binding.productNameEditText.text)){
            binding.productNameOutlinedTextLayout.error = "Product Name cannot be empty"
            binding.productNameOutlinedTextLayout.isErrorEnabled = true
            return false
        }
        if (TextUtils.isEmpty(binding.productCategoryEditText.text)){
            binding.productCategoryOutlinedTextLayout.error = "Product category cannot be empty"
            binding.productCategoryOutlinedTextLayout.isErrorEnabled = true
            return false
        }
        if (TextUtils.isEmpty(binding.productNameEditText.text)){
            binding.productPriceOutlinedTextLayout.error = "Product price cannot be empty"
            binding.productNameOutlinedTextLayout.isErrorEnabled = true
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