package com.denise.castro.adicionaprodutos

import android.app.Activity
import android.content.Intent
import android.content.Intent.ACTION_GET_CONTENT
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.denise.castro.adicionaprodutos.databinding.ActivityMainBinding
import com.denise.castro.adicionaprodutos.product.Product
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.skydoves.colorpickerview.ColorEnvelope
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

class MainActivity : AppCompatActivity() {

        private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
        val selectedColors = mutableListOf<Int>()
        var selectedImages = mutableListOf<Uri>()
        val firestore = Firebase.firestore

        private val storage = Firebase.storage.reference

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(binding.root)

            binding.edPrice.addTextChangedListener(EditTextWatcher(binding.edPrice))

            binding.buttonColorPicker.setOnClickListener {
                ColorPickerDialog
                    .Builder(this)
                    .setTitle("Cor do Produto")
                    .setPositiveButton("Selecionar", object : ColorEnvelopeListener {

                        override fun onColorSelected(envelope: ColorEnvelope?, fromUser: Boolean) {
                            envelope?.let {
                                selectedColors.add(it.color)
                                updateColors()
                            }
                        }
                    }).setNegativeButton("Cancelar") { colorPicker, _ ->
                        colorPicker.dismiss()
                    }.show()
            }

            val selectImagesActivityResult =
                registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        val intent = result.data

                        if (intent?.clipData != null) {
                            val count = intent.clipData?.itemCount ?: 0
                            (0 until count).forEach {
                                val imagesUri = intent.clipData?.getItemAt(it)?.uri
                                imagesUri?.let { selectedImages.add(it) }
                            }
                        } else {
                            val imageUri = intent?.data
                            imageUri?.let { selectedImages.add(it) }
                        }
                        updateImages()
                    }
                }

            binding.buttonImagesPicker.setOnClickListener {
                val intent = Intent(ACTION_GET_CONTENT)
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                intent.type = "image/*"
                selectImagesActivityResult.launch(intent)
            }
        }

        override fun onCreateOptionsMenu(menu: Menu?): Boolean {
            menuInflater.inflate(R.menu.toolbar_menu, menu)
            return true
        }

        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            if (item.itemId == R.id.saveProduct) {
                val productValidation = validateInformation()
                if (!productValidation) {
                    Snackbar.make(binding.root, "Cheque suas entradas.", Snackbar.LENGTH_LONG).show()
                    return false
                }
                saveProducts() {}
            }
            return super.onOptionsItemSelected(item)
        }

        private fun validateInformation(): Boolean {
            if (selectedImages.isEmpty())
                return false
            if (binding.edName.text.toString().trim().isEmpty())
                return false
            if (binding.edCategory.text.toString().trim().isEmpty())
                return false
            if (binding.edPrice.text.toString().trim().isEmpty())
                return false
            return true
        }

        private fun saveProducts(state: (Boolean) -> Unit) {
            val sizes = getSizesList(binding.edSizes.text.toString().trim())
            val imagesByteArrays = getImagesByteArrays() //7
            val name = binding.edName.text.toString().trim()
            val images = mutableListOf<String>()
            val category = binding.edCategory.text.toString().trim()
            val productDescription = binding.edDescription.text.toString().trim()
            val price = binding.edPrice.addTextChangedListener(EditTextWatcher(binding.edPrice))
            val offerPercentage = binding.offerPercentage.text.toString().trim()

            lifecycleScope.launch {
                showLoading()
                try {
                    async {
                        imagesByteArrays.forEach {
                            val id = UUID.randomUUID().toString()
                            launch {
                                val imagesStorage = storage.child("products/images/$id")
                                val result = imagesStorage.putBytes(it).await()
                                val downloadUrl = result.storage.downloadUrl.await().toString()
                                images.add(downloadUrl)
                            }
                        }
                    }.await()
                } catch (e: java.lang.Exception) {
                    hideLoading()
                    state(false)
                }

                val product = Product(
                    UUID.randomUUID().toString(),
                    name,
                    category,
                    price.toString(),
                    if (offerPercentage.isEmpty()) null else offerPercentage.toFloat(),
                    if (productDescription.isEmpty()) null else productDescription,
                    selectedColors,
                    sizes,
                    images
                )

                firestore.collection("Products").add(product).addOnSuccessListener {
                    state(true)
                    hideLoading()
                    Snackbar.make(binding.root, "Produto cadastrado com sucesso!", Snackbar.LENGTH_SHORT).show()
                }.addOnFailureListener {
                    state(false)
                    hideLoading()
                    Snackbar.make(binding.root, "Erro ao cadastrar o produto!", Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        private fun hideLoading() { binding.progressbar.visibility = View.INVISIBLE }

        private fun showLoading() { binding.progressbar.visibility = View.VISIBLE }

        private fun getImagesByteArrays(): List<ByteArray> {
            val imagesByteArray = mutableListOf<ByteArray>()
            selectedImages.forEach {
                val stream = ByteArrayOutputStream()
                val imageBmp = MediaStore.Images.Media.getBitmap(contentResolver, it)
                if (imageBmp.compress(Bitmap.CompressFormat.JPEG, 85, stream)) {
                    val imageAsByteArray = stream.toByteArray()
                    imagesByteArray.add(imageAsByteArray)
                }
            }
            return imagesByteArray
        }

        private fun getSizesList(sizes: String): List<String>? {
            if (sizes.isEmpty())
                return null
            val sizesList = sizes.split(",").map { it.trim() }
            return sizesList
        }

        private fun updateColors() {
            var colors = ""
            selectedColors.forEach {
                colors = "$colors ${Integer.toHexString(it)}, "
            }
            binding.imgSelectedColors.setBackgroundColor(selectedColors.last())
        }

        private fun updateImages() {
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedImages[0])
            binding.imgSelectedImages.setImageBitmap(bitmap)
        }
    }

    class EditTextWatcher(val edPrice: EditText) : TextWatcher {

        companion object {
            private const val replaceRegex: String = "[R$.,\u00A0]"
            private const val replaceFinal: String = "R$\u00A0"
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(editable: Editable?) {
            try {
                val stringEditable = editable.toString()
                if (stringEditable.isEmpty()) return
                edPrice.removeTextChangedListener(this)
                val cleanString = stringEditable.replace(replaceRegex.toRegex(), "")

                val parsed = BigDecimal(cleanString)
                    .setScale(2, BigDecimal.ROUND_FLOOR)
                    .divide(BigDecimal(100), BigDecimal.ROUND_FLOOR)

                val decimalFormat =
                    NumberFormat.getCurrencyInstance(Locale("pt", "BR")) as DecimalFormat
                val formatted = decimalFormat.format(parsed)
                val stringFinal = formatted.replace(replaceFinal, "")
                edPrice.setText(stringFinal)
                edPrice.setSelection(stringFinal.length)
                edPrice.addTextChangedListener(this)

            } catch (e: Exception) {
                Log.e("Error", e.message.toString())
            }
        }
}

