package com.bxt.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.data.api.dto.request.ItemRequest
import com.bxt.data.api.dto.response.ItemResponse
import com.bxt.data.repository.ItemRepository
import com.bxt.data.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchItemViewModel @Inject constructor(
    private val repo: ItemRepository,
    private val locationRepo: LocationRepository
) : ViewModel() {

    // ---- Filters ----
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _minPrice = MutableStateFlow<Double?>(null)
    private val _maxPrice = MutableStateFlow<Double?>(null)

    private val _nearCenter = MutableStateFlow<Pair<Double, Double>?>(null)
    private val _radiusKm = MutableStateFlow(5.0)

    // ---- Paging state ----
    private val _items = MutableStateFlow<List<ItemResponse>>(emptyList())
    val items: StateFlow<List<ItemResponse>> = _items.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _loadingMore = MutableStateFlow(false)
    val loadingMore: StateFlow<Boolean> = _loadingMore.asStateFlow()

    private val _end = MutableStateFlow(false)
    val endReached: StateFlow<Boolean> = _end.asStateFlow()

    private var page = 0

    // ---- Address cache for cards ----
    private val _addresses = MutableStateFlow<Map<Long, String>>(emptyMap())
    val addresses: StateFlow<Map<Long, String>> = _addresses.asStateFlow()
    private val addrCache = mutableMapOf<String, String>()

    init {
        viewModelScope.launch {
            combine(
                _query.debounce(250).map { it.trim() },
                _minPrice, _maxPrice, _nearCenter, _radiusKm
            ) { q, min, max, center, r -> Params(q.ifBlank { null }, min, max, center, r) }
                .distinctUntilChanged()
                .collectLatest { startNew(it) }
        }
    }

    // ---- Public API ----
    fun setQuery(text: String) { _query.value = text }
    fun setPrice(min: Double?, max: Double?) { _minPrice.value = min; _maxPrice.value = max }
    fun setNearMe(enabled: Boolean, center: Pair<Double, Double>?, radiusKm: Double) {
        _nearCenter.value = if (enabled) center else null
        _radiusKm.value = radiusKm
    }

    fun refresh() {
        val p = Params(_query.value.ifBlank { null }, _minPrice.value, _maxPrice.value, _nearCenter.value, _radiusKm.value)
        if (_refreshing.value || _loading.value) return
        viewModelScope.launch {
            _refreshing.value = true
            fetch(reset = true, p)
            _refreshing.value = false
        }
    }

    fun loadMore() {
        val p = Params(_query.value.ifBlank { null }, _minPrice.value, _maxPrice.value, _nearCenter.value, _radiusKm.value)
        if (_loadingMore.value || _end.value || _loading.value) return
        viewModelScope.launch {
            _loadingMore.value = true
            fetch(reset = false, p)
            _loadingMore.value = false
        }
    }

    // ---- Core ----
    private fun startNew(p: Params) {
        page = 0
        _end.value = false
        _items.value = emptyList()
        viewModelScope.launch {
            _loading.value = true
            fetch(reset = true, p)
            _loading.value = false
        }
    }

    private suspend fun fetch(reset: Boolean, p: Params) {
        val req = ItemRequest(title = p.title) // server search theo tên
        val resp = if (p.center != null)
            repo.searchItems(req, page, p.center.first, p.center.second, p.radiusKm)
        else
            repo.searchItems(req, page)

        // Lọc giá client-side (gọn & an toàn nếu server chưa hỗ trợ)
        val batch = resp.content.orEmpty().filter { item ->
            val price = item.rentalPricePerHour?.toDouble()
            val okMin = p.min == null || (price != null && price >= p.min)
            val okMax = p.max == null || (price != null && price <= p.max)
            okMin && okMax
        }

        if (batch.isEmpty()) { _end.value = true; return }

        _items.value = if (reset) batch else _items.value + batch
        page += 1

        prefetchAddresses(batch)
    }

    private fun prefetchAddresses(list: List<ItemResponse>) {
        list.forEach { item ->
            val id = item.id ?: return@forEach
            if (_addresses.value.containsKey(id)) return@forEach
            val lat = item.lat?.toDouble() ?: return@forEach
            val lng = item.lng?.toDouble() ?: return@forEach

            viewModelScope.launch(Dispatchers.IO) {
                val key = String.format(Locale.US, "%.6f,%.6f", lat, lng)
                val cached = addrCache[key]
                val text = cached ?: locationRepo.getAddressFromLatLng(lat, lng).getOrNull()
                if (text != null) {
                    addrCache[key] = text
                    _addresses.update { it + (id to text) }
                }
            }
        }
    }

    private data class Params(
        val title: String?,
        val min: Double?, val max: Double?,
        val center: Pair<Double, Double>?, val radiusKm: Double
    )
}
