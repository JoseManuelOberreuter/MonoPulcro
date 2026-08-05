package com.josem.monopulcro.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class BananaChestOfferUi(
    val productId: String,
    val displayName: String,
    val bananas: Int,
    val subtitle: String,
    val formattedPrice: String?,
)

data class BillingUiState(
    val isReady: Boolean = false,
    val isConnecting: Boolean = false,
    val offers: List<BananaChestOfferUi> = BananaChestCatalog.ALL.map {
        BananaChestOfferUi(
            productId = it.productId,
            displayName = it.displayName,
            bananas = it.bananas,
            subtitle = it.subtitle,
            formattedPrice = null,
        )
    },
    val purchasingProductId: String? = null,
    val lastError: String? = null,
)

/**
 * Google Play Billing para cofres de bananas consumibles.
 * Grant de bananas lo hace el caller vía [onPurchaseConsumed]; aquí solo
 * consulta detalles, lanza el flujo, consume y recupera compras pendientes.
 */
class BillingManager(
    context: Context,
    /** Devuelve bananas otorgadas, o null si ya estaba procesada / producto desconocido. */
    private val onPurchaseReady: (productId: String, purchaseToken: String) -> Int?,
) : PurchasesUpdatedListener {

    private val appContext = context.applicationContext

    private val _state = MutableStateFlow(BillingUiState())
    val state: StateFlow<BillingUiState> = _state.asStateFlow()

    private var productDetailsById: Map<String, ProductDetails> = emptyMap()
    private var connectionAttemptInFlight = false

    private val billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build(),
        )
        .build()

    fun start() {
        if (billingClient.isReady) {
            queryProductDetails()
            queryAndProcessPendingPurchases()
            return
        }
        if (connectionAttemptInFlight) return

        connectionAttemptInFlight = true
        _state.update { it.copy(isConnecting = true) }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                connectionAttemptInFlight = false
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    _state.update {
                        it.copy(isReady = true, isConnecting = false, lastError = null)
                    }
                    queryProductDetails()
                    queryAndProcessPendingPurchases()
                } else {
                    Log.w(
                        TAG,
                        "Billing setup failed: code=${result.responseCode} msg=${result.debugMessage}",
                    )
                    _state.update {
                        it.copy(isReady = false, isConnecting = false)
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                connectionAttemptInFlight = false
                _state.update { it.copy(isReady = false) }
            }
        })
    }

    fun launchPurchase(activity: Activity, productId: String) {
        if (!billingClient.isReady) {
            start()
            if (!billingClient.isReady) {
                _state.update {
                    it.copy(
                        lastError = if (it.isConnecting) {
                            "Conectando con Play. Espera un momento e inténtalo de nuevo."
                        } else {
                            billingMessage(BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE)
                        },
                    )
                }
            }
            return
        }

        val details = productDetailsById[productId]
        if (details == null) {
            _state.update {
                it.copy(lastError = "Producto no disponible. ¿Está activo en Play Console?")
            }
            queryProductDetails()
            return
        }

        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()

        _state.update {
            it.copy(purchasingProductId = productId, lastError = null)
        }
        val result = billingClient.launchBillingFlow(activity, flowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _state.update {
                it.copy(
                    purchasingProductId = null,
                    lastError = billingMessage(result.responseCode),
                )
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(lastError = null) }
    }

    fun destroy() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases.isNullOrEmpty()) {
                    _state.update { it.copy(purchasingProductId = null) }
                    return
                }
                purchases.forEach { processPurchase(it) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _state.update {
                    it.copy(purchasingProductId = null, lastError = null)
                }
            }
            else -> {
                _state.update {
                    it.copy(
                        purchasingProductId = null,
                        lastError = billingMessage(result.responseCode),
                    )
                }
            }
        }
    }

    private fun queryProductDetails() {
        val products = BananaChestCatalog.PRODUCT_IDS.map { id ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(id)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(products)
            .build()

        billingClient.queryProductDetailsAsync(params) { result, detailsList ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.w(TAG, "queryProductDetails failed: ${result.debugMessage}")
                return@queryProductDetailsAsync
            }
            productDetailsById = detailsList.associateBy { it.productId }
            _state.update { current ->
                current.copy(
                    offers = BananaChestCatalog.ALL.map { chest ->
                        val price = productDetailsById[chest.productId]
                            ?.oneTimePurchaseOfferDetails
                            ?.formattedPrice
                        BananaChestOfferUi(
                            productId = chest.productId,
                            displayName = chest.displayName,
                            bananas = chest.bananas,
                            subtitle = chest.subtitle,
                            formattedPrice = price,
                        )
                    }
                )
            }
        }
    }

    private fun queryAndProcessPendingPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryPurchasesAsync
            purchases.forEach { processPurchase(it) }
        }
    }

    private fun processPurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return

        val productId = purchase.products.firstOrNull { it in BananaChestCatalog.PRODUCT_IDS }
            ?: return

        // Idempotent grant, then consume so the pack can be bought again.
        val bananasGranted = onPurchaseReady(productId, purchase.purchaseToken)
        consumePurchase(purchase, productId, bananasGranted != null)
    }

    private fun consumePurchase(purchase: Purchase, productId: String, newlyGranted: Boolean) {
        val params = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.consumeAsync(params) { result, _ ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                _state.update {
                    it.copy(
                        purchasingProductId = null,
                        lastError = null,
                    )
                }
                if (!newlyGranted) {
                    Log.d(TAG, "Purchase $productId already granted; consumed anyway")
                }
            } else {
                Log.w(TAG, "consume failed: ${result.debugMessage}")
                _state.update {
                    it.copy(
                        purchasingProductId = null,
                        lastError = if (newlyGranted) null else "No se pudo completar la compra",
                    )
                }
            }
        }
    }

    private fun billingMessage(code: Int): String = when (code) {
        BillingClient.BillingResponseCode.BILLING_UNAVAILABLE ->
            "Facturación no disponible en este dispositivo"
        BillingClient.BillingResponseCode.ITEM_UNAVAILABLE ->
            "Producto no disponible"
        BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE ->
            "Servicio de Play no disponible"
        else -> "Error de compra ($code)"
    }

    companion object {
        private const val TAG = "BillingManager"
    }
}
