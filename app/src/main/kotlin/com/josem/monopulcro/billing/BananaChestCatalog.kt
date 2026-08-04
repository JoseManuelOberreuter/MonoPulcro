package com.josem.monopulcro.billing

/**
 * Catálogo local de cofres IAP. Los precios viven en Play Console;
 * aquí solo product IDs y bananas otorgadas tras compra consumible.
 */
object BananaChestCatalog {

    data class Chest(
        val productId: String,
        val displayName: String,
        val bananas: Int,
        val subtitle: String,
    )

    val ALL: List<Chest> = listOf(
        Chest(
            productId = "bananas_chest_small",
            displayName = "Cofre pequeño",
            bananas = 50,
            subtitle = "Un puñado de bananas",
        ),
        Chest(
            productId = "bananas_chest_medium",
            displayName = "Cofre mediano",
            bananas = 150,
            subtitle = "Buen stock para la tienda",
        ),
        Chest(
            productId = "bananas_chest_xlarge",
            displayName = "Cofre grande",
            bananas = 400,
            subtitle = "Casi el catálogo completo",
        ),
    )

    val PRODUCT_IDS: List<String> = ALL.map { it.productId }

    fun find(productId: String): Chest? = ALL.find { it.productId == productId }

    fun bananasFor(productId: String): Int? = find(productId)?.bananas
}
