package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FlipRepository
import com.example.data.MarketplaceEngine
import com.example.data.RssScanResult
import com.example.model.AcquisitionType
import com.example.model.CityMarket
import com.example.model.DealMode
import com.example.model.FlipItem
import com.example.model.FlipStatus
import com.example.model.LiveListing
import com.example.model.MarketplacePlatform
import com.example.model.ProfitCalcState
import com.example.model.ResaleGuideCategory
import com.example.model.SavedScout
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class FlipNavTab {
    SCOUT,
    GUIDES,
    CALCULATOR,
    PIPELINE
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed interface LiveFeedUiState {
    data object Idle : LiveFeedUiState
    data object Loading : LiveFeedUiState
    data class Success(val listings: List<LiveListing>, val feedUrl: String) : LiveFeedUiState
    data class Unavailable(val message: String, val browserUrl: String) : LiveFeedUiState
}

data class ScoutFilterState(
    val keywords: String = "solid wood dresser",
    val category: String = "Furniture",
    val dealMode: DealMode = DealMode.FREE_ONLY,
    val maxCashDollars: Int = 75,
    val selectedCity: CityMarket = MarketplaceEngine.CITY_MARKETS.first(),
    val customCitySlug: String = "",
    val radiusMiles: Int = 25,
    val targetResaleDollars: Int = 180
) {
    val effectiveCitySlug: String
        get() = customCitySlug.trim().lowercase().replace(Regex("[^a-z0-9]"), "")
            .ifEmpty { selectedCity.slug }

    val effectiveCityName: String
        get() = if (customCitySlug.trim().isNotEmpty()) {
            customCitySlug.trim()
        } else {
            selectedCity.name
        }

    val effectiveMaxPrice: Int
        get() = if (dealMode == DealMode.FREE_ONLY) 0 else maxCashDollars.coerceAtLeast(1)
}

class FlipScoutViewModel(
    private val repository: FlipRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _currentTab = MutableStateFlow(FlipNavTab.SCOUT)
    val currentTab: StateFlow<FlipNavTab> = _currentTab.asStateFlow()

    private val _scoutFilter = MutableStateFlow(ScoutFilterState())
    val scoutFilter: StateFlow<ScoutFilterState> = _scoutFilter.asStateFlow()

    private val _liveFeedState = MutableStateFlow<LiveFeedUiState>(LiveFeedUiState.Idle)
    val liveFeedState: StateFlow<LiveFeedUiState> = _liveFeedState.asStateFlow()

    private val _calcState = MutableStateFlow(ProfitCalcState())
    val calcState: StateFlow<ProfitCalcState> = _calcState.asStateFlow()

    private val _guideFilterFreeOnly = MutableStateFlow<Boolean?>(null)
    val guideFilterFreeOnly: StateFlow<Boolean?> = _guideFilterFreeOnly.asStateFlow()

    private val _pipelineStatusFilter = MutableStateFlow<FlipStatus?>(null)
    val pipelineStatusFilter: StateFlow<FlipStatus?> = _pipelineStatusFilter.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    val savedScouts: StateFlow<UiState<List<SavedScout>>> = repository.observeScouts(currentUserId)
        .map<List<SavedScout>, UiState<List<SavedScout>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing saved scouts", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load saved scouts"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val flipItems: StateFlow<UiState<List<FlipItem>>> = repository.observeFlips(currentUserId)
        .map<List<FlipItem>, UiState<List<FlipItem>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing flip items", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load flip pipeline"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    fun setTab(tab: FlipNavTab) {
        _currentTab.value = tab
    }

    fun updateKeywords(keywords: String) {
        _scoutFilter.update { it.copy(keywords = keywords) }
    }

    fun updateCategory(category: String) {
        _scoutFilter.update { it.copy(category = category) }
    }

    fun updateDealMode(mode: DealMode) {
        _scoutFilter.update { it.copy(dealMode = mode) }
    }

    fun updateMaxCash(dollars: Int) {
        _scoutFilter.update { it.copy(maxCashDollars = dollars.coerceIn(1, 50000)) }
    }

    fun selectCity(city: CityMarket) {
        _scoutFilter.update { it.copy(selectedCity = city, customCitySlug = "") }
    }

    fun updateCustomCitySlug(slug: String) {
        _scoutFilter.update { it.copy(customCitySlug = slug) }
    }

    fun updateTargetResale(dollars: Int) {
        _scoutFilter.update { it.copy(targetResaleDollars = dollars.coerceIn(10, 100000)) }
    }

    fun applyGuideToScout(guide: ResaleGuideCategory) {
        _scoutFilter.update {
            it.copy(
                keywords = guide.searchKeywords,
                dealMode = if (guide.defaultFreeSearch) DealMode.FREE_ONLY else DealMode.CASH_BARGAIN,
                maxCashDollars = if (guide.suggestedMaxCash > 0) guide.suggestedMaxCash else 75,
                category = when (guide.id) {
                    "furniture" -> "Furniture"
                    "tools" -> "Power Tools"
                    "appliances" -> "Appliances"
                    "patio" -> "Patio & Yard"
                    "electronics" -> "Electronics"
                    "bikes" -> "Bikes & Fitness"
                    "baby" -> "Baby & Kids"
                    else -> "All"
                }
            )
        }
        _currentTab.value = FlipNavTab.SCOUT
        _snackbarEvents.tryEmit("Loaded '${guide.name}' into Scout")
    }

    fun applySavedScout(scout: SavedScout) {
        val matchedCity = MarketplaceEngine.CITY_MARKETS.firstOrNull { it.slug == scout.citySlug }
        _scoutFilter.update {
            it.copy(
                keywords = scout.keywords,
                category = scout.category,
                dealMode = scout.parsedDealMode,
                maxCashDollars = if (scout.maxPrice > 0L) scout.maxPrice.toInt() else 75,
                selectedCity = matchedCity ?: it.selectedCity,
                customCitySlug = if (matchedCity == null) scout.citySlug else "",
                radiusMiles = scout.radiusMiles.toInt(),
                targetResaleDollars = scout.targetResale.toInt()
            )
        }
        _snackbarEvents.tryEmit("Loaded scout: ${scout.title}")
    }

    fun saveCurrentScout(customTitle: String) {
        val filter = _scoutFilter.value
        val title = customTitle.trim().ifEmpty {
            val prefix = if (filter.dealMode == DealMode.FREE_ONLY) "Free" else "Under $${filter.effectiveMaxPrice}"
            val topic = filter.keywords.trim().ifEmpty { filter.category }
            "$prefix $topic"
        }.take(80)

        viewModelScope.launch {
            val result = repository.createScout(
                SavedScout(
                    title = title,
                    keywords = filter.keywords,
                    category = filter.category,
                    dealMode = filter.dealMode.wireValue,
                    maxPrice = filter.effectiveMaxPrice.toLong(),
                    citySlug = filter.effectiveCitySlug,
                    cityName = filter.effectiveCityName,
                    radiusMiles = filter.radiusMiles.toLong(),
                    targetResale = filter.targetResaleDollars.toLong()
                )
            )
            result.onSuccess {
                _snackbarEvents.tryEmit("Saved scout '$title' to cloud")
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.localizedMessage ?: "Could not save scout")
            }
        }
    }

    fun deleteScout(scoutId: String) {
        viewModelScope.launch {
            repository.deleteScout(scoutId)
                .onSuccess { _snackbarEvents.tryEmit("Scout removed") }
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Could not remove scout") }
        }
    }

    fun scanLiveCraigslistFeed() {
        val filter = _scoutFilter.value
        _liveFeedState.value = LiveFeedUiState.Loading
        viewModelScope.launch {
            when (
                val res = MarketplaceEngine.fetchCraigslistRss(
                    citySlug = filter.effectiveCitySlug,
                    keywords = filter.keywords,
                    dealMode = filter.dealMode,
                    maxPriceDollars = filter.effectiveMaxPrice
                )
            ) {
                is RssScanResult.Success -> {
                    _liveFeedState.value = LiveFeedUiState.Success(res.listings, res.feedUrl)
                }
                is RssScanResult.Unavailable -> {
                    _liveFeedState.value = LiveFeedUiState.Unavailable(res.message, res.browserUrl)
                }
            }
        }
    }

    fun trackLiveListing(listing: LiveListing) {
        val filter = _scoutFilter.value
        val isFree = listing.parsedPriceDollars == null && filter.dealMode == DealMode.FREE_ONLY ||
            listing.parsedPriceDollars == 0
        val buyCents = if (isFree) 0L else ((listing.parsedPriceDollars ?: filter.effectiveMaxPrice) * 100L)
        val estResaleCents = (filter.targetResaleDollars.coerceAtLeast(50) * 100L)

        viewModelScope.launch {
            repository.createFlip(
                FlipItem(
                    title = listing.title.take(100),
                    sourcePlatform = MarketplacePlatform.CRAIGSLIST.label,
                    category = filter.category,
                    acquisitionType = if (isFree) AcquisitionType.FREE.wireValue else AcquisitionType.CASH.wireValue,
                    buyPriceCents = buyCents,
                    repairCostCents = 0L,
                    estResaleCents = estResaleCents,
                    soldPriceCents = 0L,
                    status = FlipStatus.SCOUTING.wireValue,
                    listingUrl = listing.url.take(500),
                    notes = "Found in ${listing.locationText}"
                )
            ).onSuccess {
                _snackbarEvents.tryEmit("Added '${listing.title.take(28)}' to My Flips")
            }.onFailure {
                _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to track listing")
            }
        }
    }

    fun setGuideFilter(freeOnly: Boolean?) {
        _guideFilterFreeOnly.value = freeOnly
    }

    fun setPipelineFilter(status: FlipStatus?) {
        _pipelineStatusFilter.value = status
    }

    fun updateCalculator(
        isFreePickup: Boolean? = null,
        cashBuyDollars: Int? = null,
        gasAndRepairDollars: Int? = null,
        estResaleDollars: Int? = null,
        feePresetName: String? = null,
        platformFeePercent: Double? = null,
        fixedFeeDollars: Double? = null
    ) {
        _calcState.update { current ->
            current.copy(
                isFreePickup = isFreePickup ?: current.isFreePickup,
                cashBuyDollars = cashBuyDollars ?: current.cashBuyDollars,
                gasAndRepairDollars = gasAndRepairDollars ?: current.gasAndRepairDollars,
                estResaleDollars = estResaleDollars ?: current.estResaleDollars,
                feePresetName = feePresetName ?: current.feePresetName,
                platformFeePercent = platformFeePercent ?: current.platformFeePercent,
                fixedFeeDollars = fixedFeeDollars ?: current.fixedFeeDollars
            )
        }
    }

    fun saveCalculatorDealToPipeline(
        title: String,
        sourcePlatform: String,
        category: String,
        notes: String
    ) {
        val state = _calcState.value
        val acq = if (state.isFreePickup) AcquisitionType.FREE else AcquisitionType.CASH
        val buyCents = if (state.isFreePickup) 0L else (state.cashBuyDollars * 100L)
        viewModelScope.launch {
            repository.createFlip(
                FlipItem(
                    title = title.trim().ifEmpty { "Marketplace Deal" }.take(100),
                    sourcePlatform = sourcePlatform,
                    category = category,
                    acquisitionType = acq.wireValue,
                    buyPriceCents = buyCents,
                    repairCostCents = (state.gasAndRepairDollars * 100L),
                    estResaleCents = (state.estResaleDollars * 100L),
                    soldPriceCents = 0L,
                    status = FlipStatus.SCOUTING.wireValue,
                    listingUrl = "",
                    notes = notes.trim().take(500)
                )
            ).onSuccess {
                _snackbarEvents.tryEmit("Saved deal to My Flips")
                _currentTab.value = FlipNavTab.PIPELINE
            }.onFailure {
                _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to save deal")
            }
        }
    }

    fun createFlip(item: FlipItem) {
        viewModelScope.launch {
            repository.createFlip(item)
                .onSuccess { _snackbarEvents.tryEmit("Added '${item.title}' to pipeline") }
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to add item") }
        }
    }

    fun advanceFlipStatus(item: FlipItem, newStatus: FlipStatus, soldPriceDollars: Int? = null) {
        viewModelScope.launch {
            val soldCents = soldPriceDollars?.let { it.coerceAtLeast(0) * 100L }
            repository.updateFlipStatus(item.id, newStatus, soldCents)
                .onSuccess {
                    val label = if (newStatus == FlipStatus.SOLD) "Marked SOLD!" else "Moved to ${newStatus.label}"
                    _snackbarEvents.tryEmit(label)
                }
                .onFailure {
                    _snackbarEvents.tryEmit(it.localizedMessage ?: "Could not update status")
                }
        }
    }

    fun deleteFlip(flipId: String) {
        viewModelScope.launch {
            repository.deleteFlip(flipId)
                .onSuccess { _snackbarEvents.tryEmit("Flip removed") }
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Could not delete item") }
        }
    }

    private companion object {
        const val TAG = "FlipScoutVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
