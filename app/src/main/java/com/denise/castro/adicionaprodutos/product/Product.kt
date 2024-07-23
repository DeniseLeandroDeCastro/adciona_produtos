package com.denise.castro.adicionaprodutos.product

data class Product(
    val id: String,
    val name: String,
    val category: String,
    val price: String,
    val offerPercentage: Float? = null,
    val description: String? = null,
    val colors: List<Int>? = null,
    val sizes: List<String>? = null,
    val images: List<String>
)
