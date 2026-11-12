package fr.lpoirier.radios.support

import fr.lpoirier.radios.R
import androidx.annotation.StringRes
import android.app.Activity
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Montants de don proposés. Les identifiants doivent exister dans la Play Console (produits intégrés). */
enum class Tip(val productId: String, val emoji: String, @StringRes val label: Int) {
    COFFEE("soutien_cafe", "☕", R.string.tip_coffee),
    CROISSANT("soutien_croissant", "🥐", R.string.tip_croissant),
    MEAL("soutien_repas", "🍽️", R.string.tip_meal),
}

data class SupportState(
    /** Prix formatés par Google (devise du pays), par don disponible. Vide si le Store est injoignable. */
    val prices: Map<Tip, String> = emptyMap(),
    val connecting: Boolean = true,
    /** L'utilisateur a déjà fait au moins un don (mémorisé). */
    val isSupporter: Boolean = false,
    /** Message ponctuel à afficher (remerciement, erreur), en ressource texte. */
    @StringRes val message: Int? = null,
)

private val Context.supportStore by preferencesDataStore("support")

/**
 * Dons via Google Play Billing : produits **consommables**, on peut donner plusieurs fois.
 * Chaque achat est consommé aussitôt, ce qui vaut confirmation auprès de Google
 * (sans confirmation sous 3 jours, Google rembourse automatiquement).
 *
 * Autonome (aucune dépendance au reste de l'app) : ce fichier peut être copié tel quel
 * dans un autre projet, en changeant le package et en reprenant les textes `support_*`/`tip_*`.
 */
class SupportManager(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val supporterKey = booleanPreferencesKey("supporter")

    private val _state = MutableStateFlow(SupportState())
    val state: StateFlow<SupportState> = _state.asStateFlow()

    private val products = mutableMapOf<Tip, ProductDetails>()

    private val client: BillingClient = BillingClient.newBuilder(appContext)
        .setListener { result, purchases -> onPurchasesUpdated(result, purchases) }
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    init {
        scope.launch {
            appContext.supportStore.data.map { it[supporterKey] == true }.collect { supporter ->
                _state.update { it.copy(isSupporter = supporter) }
            }
        }
    }

    /** Connexion au Store (à appeler à l'ouverture de l'écran « Soutenir »). */
    fun connect() {
        if (client.isReady) {
            loadProducts()
            return
        }
        _state.update { it.copy(connecting = true) }
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    loadProducts()
                    consumePendingPurchases()
                } else {
                    _state.update { it.copy(connecting = false) }
                }
            }

            override fun onBillingServiceDisconnected() = Unit // reconnexion automatique
        })
    }

    fun launch(activity: Activity, tip: Tip) {
        val details = products[tip] ?: return
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).build()),
            )
            .build()
        client.launchBillingFlow(activity, params)
    }

    fun clearMessage() = _state.update { it.copy(message = null) }

    private fun loadProducts() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                Tip.entries.map {
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(it.productId)
                        .setProductType(ProductType.INAPP)
                        .build()
                },
            )
            .build()
        client.queryProductDetailsAsync(params) { _, result ->
            val found = result.productDetailsList.orEmpty()
            scope.launch {
                products.clear()
                found.forEach { details ->
                    Tip.entries.firstOrNull { it.productId == details.productId }?.let { products[it] = details }
                }
                _state.update { state ->
                    state.copy(
                        connecting = false,
                        prices = products.mapNotNull { (tip, d) ->
                            d.oneTimePurchaseOfferDetails?.formattedPrice?.let { tip to it }
                        }.toMap(),
                    )
                }
            }
        }
    }

    private fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> purchases.orEmpty().forEach(::handlePurchase)
            BillingClient.BillingResponseCode.USER_CANCELED -> Unit
            else -> _state.update { it.copy(message = R.string.support_msg_failed) }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> consume(purchase, thank = true)
            Purchase.PurchaseState.PENDING -> _state.update {
                it.copy(message = R.string.support_msg_pending)
            }
            else -> Unit
        }
    }

    /** Achats payés mais pas encore consommés (app fermée pendant le paiement, paiement différé…). */
    private fun consumePendingPurchases() {
        val params = QueryPurchasesParams.newBuilder().setProductType(ProductType.INAPP).build()
        client.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryPurchasesAsync
            purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                .forEach { consume(it, thank = false) }
        }
    }

    private fun consume(purchase: Purchase, thank: Boolean) {
        val params = ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        client.consumeAsync(params) { result, _ ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@consumeAsync
            scope.launch {
                appContext.supportStore.edit { it[supporterKey] = true }
                if (thank) _state.update { it.copy(message = R.string.support_msg_thanks) }
            }
        }
    }
}
