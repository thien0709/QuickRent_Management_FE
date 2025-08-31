package com.bxt.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.data.api.dto.response.ItemResponse
import com.bxt.data.api.dto.response.RentalRequestResponse
import com.bxt.data.api.dto.response.RentalTransactionResponse
import com.bxt.data.api.dto.response.TransactionImageResponse
import com.bxt.data.local.DataStoreManager
import com.bxt.data.repository.ItemRepository
import com.bxt.data.repository.RentalRequestRepository
import com.bxt.data.repository.RentalTransactionRepository
import com.bxt.di.ApiResult
import com.bxt.util.FileUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.math.BigDecimal

/** Vị trí nhận hàng để mở map */
data class PickupLocation(val lat: BigDecimal, val lng: BigDecimal, val label: String = "Điểm nhận hàng")

/** Dữ liệu đầy đủ cho UI */
data class FullTransactionDetails(
    val transaction: RentalTransactionResponse,
    val request: RentalRequestResponse,
    val item: ItemResponse,
    val images: List<TransactionImageResponse>,
    val isOwner: Boolean,
    val isRenter: Boolean,
    val pickupLocation: PickupLocation?,
    val currentStep: RentalStep
)

/** Các bước trong quy trình thuê - được sắp xếp theo thứ tự logic */
enum class RentalStep(val displayName: String, val description: String) {
    REQUEST_CREATED("Yêu cầu được tạo", "Chờ chủ cho thuê xác nhận"),
    OWNER_CONFIRMED("Chủ đã xác nhận", "Chờ chủ upload ảnh tình trạng sản phẩm"),
    PICKUP_IMAGES_UPLOADED("Đã có ảnh bàn giao", "Chờ người thuê thanh toán"),
    PAYMENT_COMPLETED("Đã thanh toán", "Chờ người thuê xác nhận nhận hàng"),
    ITEM_PICKED_UP("Đã nhận hàng", "Đang trong thời gian thuê"),
    RENTAL_ACTIVE("Đang cho thuê", "Sản phẩm đang được sử dụng"),
    RENTAL_DUE("Đến hạn trả", "Người thuê cần upload ảnh và trả hàng"),
    RETURN_IMAGES_UPLOADED("Đã có ảnh trả hàng", "Chờ chủ xác nhận và hoàn cọc"),
    COMPLETED("Hoàn thành", "Giao dịch đã kết thúc")
}

/** Quy tắc hiển thị / thao tác theo từng bước */
data class Capabilities(
    val ownerCanConfirmRequest: Boolean,
    val ownerCanUploadPickup: Boolean,
    val ownerCanConfirmCashPaid: Boolean,
    val ownerCanComplete: Boolean,
    val renterCanMakePayment: Boolean,
    val renterCanOpenPickupMap: Boolean,
    val renterCanConfirmPickup: Boolean,
    val renterCanUploadReturn: Boolean,
    val showBankingPendingHint: Boolean,
    val showCurrentStep: Boolean,
    val canProgress: Boolean = true,
    val progressBlockedReason: String? = null
)

sealed interface TransactionDetailState {
    object Loading : TransactionDetailState
    data class Success(val details: FullTransactionDetails, val caps: Capabilities) : TransactionDetailState
    data class Error(val message: String) : TransactionDetailState
}

data class TransactionDetailUiState(
    val state: TransactionDetailState = TransactionDetailState.Loading,
    val isUploading: Boolean = false,
    val uploadSuccess: Boolean = false,
    val isActionInProgress: Boolean = false,
    val actionSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    private val transactionRepo: RentalTransactionRepository,
    private val requestRepo: RentalRequestRepository,
    private val itemRepo: ItemRepository,
    private val dataStore: DataStoreManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val requestId: Long = savedStateHandle.get<Long>("rentalRequestId")
        ?: error("rentalRequestId is required")

    private val _ui = MutableStateFlow(TransactionDetailUiState())
    val uiState: StateFlow<TransactionDetailUiState> = _ui.asStateFlow()

    private var transactionId: Long? = null
    private var pollingJob: Job? = null

    init { loadAll() }

    fun loadAll() {
        viewModelScope.launch {
            _ui.update { it.copy(state = TransactionDetailState.Loading) }

            try {
                // 1) Load rental request
                val reqRes = requestRepo.getRentalRequestById(requestId)
                if (reqRes is ApiResult.Error) {
                    _ui.update { it.copy(state = TransactionDetailState.Error(reqRes.error.message)) }
                    return@launch
                }
                val req = (reqRes as ApiResult.Success).data

                // 2) Load transaction
                val txnRes = transactionRepo.getTransactionByRequestId(requestId)
                if (txnRes is ApiResult.Error) {
                    _ui.update { it.copy(state = TransactionDetailState.Error(txnRes.error.message)) }
                    return@launch
                }
                val txn = (txnRes as ApiResult.Success).data
                val txnId = txn.id ?: run {
                    _ui.update { it.copy(state = TransactionDetailState.Error("Thiếu transactionId")) }
                    return@launch
                }
                transactionId = txnId

                // 3) Load parallel data
                val itemDeferred = async { itemRepo.getItemInfo(req.itemId!!) }
                val imgsDeferred = async { transactionRepo.getImagesByTransactionId(txnId) }
                val uidDeferred = async { dataStore.userId.first() }

                val itemRes = itemDeferred.await()
                val imgsRes = imgsDeferred.await()
                val currentUserId = uidDeferred.await()

                if (itemRes is ApiResult.Error || imgsRes is ApiResult.Error || currentUserId == null) {
                    val errorMsg = buildString {
                        if (itemRes is ApiResult.Error) append("Lỗi item: ${itemRes.error.message}. ")
                        if (imgsRes is ApiResult.Error) append("Lỗi images: ${imgsRes.error.message}. ")
                        if (currentUserId == null) append("Không lấy được userId hiện tại.")
                    }.trim()
                    _ui.update { it.copy(state = TransactionDetailState.Error(errorMsg)) }
                    return@launch
                }

                val item = (itemRes as ApiResult.Success).data
                val images = (imgsRes as ApiResult.Success).data

                // 4) Determine user roles
                val ownerId = item.ownerId
                val isOwner = (ownerId != null && ownerId == currentUserId)
                val isRenter = (req.renterId != null && req.renterId == currentUserId)

                // 5) Pickup location
                val pickupLoc = if (req.latFrom != null && req.lngFrom != null) {
                    PickupLocation(req.latFrom, req.lngFrom, "Điểm nhận hàng")
                } else null

                // 6) Determine current step with improved logic
                val currentStep = determineCurrentStepImproved(req, txn, images)

                val details = FullTransactionDetails(
                    transaction = txn,
                    request = req,
                    item = item,
                    images = images,
                    isOwner = isOwner,
                    isRenter = isRenter,
                    pickupLocation = pickupLoc,
                    currentStep = currentStep
                )

                val caps = computeCapabilitiesImproved(details)

                _ui.update { it.copy(state = TransactionDetailState.Success(details, caps)) }
                maybeStartPaymentPolling()

            } catch (e: Exception) {
                _ui.update { it.copy(state = TransactionDetailState.Error("Lỗi không xác định: ${e.message}")) }
            }
        }
    }

    private fun determineCurrentStepImproved(
        request: RentalRequestResponse,
        transaction: RentalTransactionResponse,
        images: List<TransactionImageResponse>
    ): RentalStep {
        val reqStatus = request.status?.uppercase()
        val payStatus = transaction.paymentStatus?.uppercase()
        val hasPickupImages = images.any { it.imageType.equals("PICKUP", true) }
        val hasReturnImages = images.any { it.imageType.equals("RETURN", true) }

        println("DEBUG - determineCurrentStep:")
        println("reqStatus: $reqStatus, payStatus: $payStatus")
        println("hasPickupImages: $hasPickupImages, hasReturnImages: $hasReturnImages")

        val now = Instant.now()
        val rentalStart = request.rentalStartTime
        val rentalEnd = request.rentalEndTime

        val isRentalStarted = rentalStart?.let { now.isAfter(it) } ?: false
        val isRentalEnded = rentalEnd?.let { now.isAfter(it) } ?: false

        // ĐƠN GIẢN HÓA logic xác định bước
        return when {
            reqStatus == "COMPLETED" -> RentalStep.COMPLETED
            hasReturnImages -> RentalStep.RETURN_IMAGES_UPLOADED
            isRentalEnded && payStatus == "PAID" -> RentalStep.RENTAL_DUE
            payStatus == "PAID" && hasPickupImages && isRentalStarted && !isRentalEnded -> RentalStep.RENTAL_ACTIVE
            payStatus == "PAID" && hasPickupImages && !isRentalStarted -> RentalStep.ITEM_PICKED_UP
            payStatus == "PAID" && !hasPickupImages -> RentalStep.PAYMENT_COMPLETED

            // QUAN TRỌNG: Điều kiện cho thanh toán tiền mặt
            hasPickupImages && payStatus != "PAID" && reqStatus == "CONFIRMED" -> {
                println("Setting step to PICKUP_IMAGES_UPLOADED for cash payment")
                RentalStep.PICKUP_IMAGES_UPLOADED
            }

            reqStatus == "CONFIRMED" && !hasPickupImages -> RentalStep.OWNER_CONFIRMED
            else -> RentalStep.REQUEST_CREATED
        }
    }
    /** Cải thiện logic tính toán capabilities */
    /** Cải thiện logic tính toán capabilities - ĐƠN GIẢN HÓA */
    private fun computeCapabilitiesImproved(d: FullTransactionDetails): Capabilities {
        val payMethod = d.transaction.paymentMethod?.uppercase()
        val payStatus = d.transaction.paymentStatus?.uppercase()
        val reqStatus = d.request.status?.uppercase()
        val now = Instant.now()
        val rentalStart = d.request.rentalStartTime

        val isBeforeRentalStart = rentalStart?.let { now.isBefore(it) } ?: true

        // DEBUG: In ra các giá trị để kiểm tra
        println("DEBUG - computeCapabilities:")
        println("isOwner: ${d.isOwner}, isRenter: ${d.isRenter}")
        println("payMethod: $payMethod, payStatus: $payStatus, reqStatus: $reqStatus")
        println("currentStep: ${d.currentStep}")
        println("hasPickupImages: ${d.images.any { it.imageType.equals("PICKUP", true) }}")

        // ĐƠN GIẢN HÓA điều kiện cho owner xác nhận tiền mặt
        val ownerCanConfirmCash = d.isOwner &&
                payMethod == "CASH" &&
                payStatus != "PAID" &&
                reqStatus == "CONFIRMED" &&
                d.images.any { it.imageType.equals("PICKUP", true) }

        println("ownerCanConfirmCash: $ownerCanConfirmCash")

        // Kiểm tra các điều kiện block
        val (canProgress, blockReason) = when {
            d.currentStep == RentalStep.PAYMENT_COMPLETED && isBeforeRentalStart ->
                false to "Chưa đến thời gian thuê (${formatInstant(rentalStart)})"
            else -> true to null
        }

        return Capabilities(
            ownerCanConfirmRequest = d.isOwner && d.currentStep == RentalStep.REQUEST_CREATED,

            ownerCanUploadPickup = d.isOwner && d.currentStep == RentalStep.OWNER_CONFIRMED,

            // SỬA QUAN TRỌNG: Sử dụng điều kiện đơn giản
            ownerCanConfirmCashPaid = ownerCanConfirmCash,

            ownerCanComplete = d.isOwner && d.currentStep == RentalStep.RETURN_IMAGES_UPLOADED,

            renterCanMakePayment = d.isRenter &&
                    d.currentStep == RentalStep.PICKUP_IMAGES_UPLOADED &&
                    payStatus != "PAID",

            renterCanOpenPickupMap = d.isRenter &&
                    d.currentStep in listOf(RentalStep.PAYMENT_COMPLETED, RentalStep.ITEM_PICKED_UP, RentalStep.RENTAL_ACTIVE) &&
                    d.pickupLocation != null,

            renterCanConfirmPickup = d.isRenter &&
                    d.currentStep == RentalStep.PAYMENT_COMPLETED,

            renterCanUploadReturn = d.isRenter &&
                    d.currentStep in listOf(RentalStep.RENTAL_ACTIVE, RentalStep.RENTAL_DUE),

            showBankingPendingHint = (payMethod == "BANKING" && payStatus == "PENDING"),

            showCurrentStep = true,

            canProgress = canProgress,

            progressBlockedReason = blockReason
        )
    }
    /** Owner: xác nhận yêu cầu thuê với validation cải thiện */
    fun ownerConfirmRequest() {
        val s = _ui.value.state as? TransactionDetailState.Success ?: return

        // Validate conditions
        if (!s.details.isOwner) {
            setError("Chỉ chủ cho thuê mới có thể xác nhận.")
            return
        }

        if (s.details.currentStep != RentalStep.REQUEST_CREATED) {
            setError("Yêu cầu đã được xác nhận rồi.")
            return
        }

        performAction("Xác nhận yêu cầu") {
            requestRepo.updateRequestStatus(requestId, "CONFIRMED")
        }
    }

    /** Owner: xác nhận đã nhận tiền mặt */
    /** Owner: xác nhận đã nhận tiền mặt - ĐƠN GIẢN HÓA */
    fun ownerConfirmCashReceived() {
        val s = _ui.value.state as? TransactionDetailState.Success ?: return

        println("DEBUG - ownerConfirmCashReceived called")
        println("isOwner: ${s.details.isOwner}")

        if (!s.details.isOwner) {
            setError("Chỉ chủ cho thuê mới xác nhận thanh toán.")
            return
        }

        val method = s.details.transaction.paymentMethod?.uppercase()
        println("paymentMethod: $method")

        if (method != "CASH") {
            setError("Chỉ áp dụng cho thanh toán bằng tiền mặt.")
            return
        }

        val status = s.details.transaction.paymentStatus?.uppercase()
        println("paymentStatus: $status")

        if (status == "PAID") {
            setError("Giao dịch đã được thanh toán rồi.")
            return
        }

        val reqStatus = s.details.request.status?.uppercase()
        println("requestStatus: $reqStatus")

        if (reqStatus != "CONFIRMED") {
            setError("Yêu cầu chưa được xác nhận.")
            return
        }

        val hasPickupImages = s.details.images.any { it.imageType.equals("PICKUP", true) }
        println("hasPickupImages: $hasPickupImages")

        if (!hasPickupImages) {
            setError("Cần upload ảnh bàn giao trước khi xác nhận thanh toán.")
            return
        }

        // Sử dụng API confirmPickup có sẵn trong code cũ với status "PAID"
        val txnId = transactionId ?: return
        println("transactionId: $txnId")

        performAction("Xác nhận thanh toán") {
            println("Calling confirmPickup API with PAID status")
            transactionRepo.confirmPickup(txnId, "PAID")
        }
    }

    /** Owner: hoàn tất giao dịch */
    fun ownerComplete() {
        val s = _ui.value.state as? TransactionDetailState.Success ?: return

        if (!s.details.isOwner) {
            setError("Chỉ chủ cho thuê mới hoàn tất.")
            return
        }

        if (s.details.currentStep != RentalStep.RETURN_IMAGES_UPLOADED) {
            setError("Chưa có ảnh trả hàng để xác nhận.")
            return
        }

        performAction("Hoàn tất giao dịch") {
            requestRepo.updateRequestStatus(requestId, "COMPLETED")
        }
    }

    /** Renter: xác nhận đã nhận hàng */
    fun renterConfirmPickup() {
        val s = _ui.value.state as? TransactionDetailState.Success ?: return

        if (!s.details.isRenter) {
            setError("Chỉ người thuê mới có thể xác nhận nhận hàng.")
            return
        }

        if (s.details.currentStep != RentalStep.PAYMENT_COMPLETED) {
            setError("Chưa đến bước nhận hàng.")
            return
        }

        val now = Instant.now()
        val rentalStart = s.details.request.rentalStartTime
        if (rentalStart != null && now.isBefore(rentalStart)) {
            setError("Chưa đến thời gian thuê. Vui lòng đợi đến ${formatInstant(rentalStart)}")
            return
        }

        // Trong code cũ, hàm này chỉ reload dữ liệu mà không gọi API
        // Chúng ta sẽ giữ nguyên behavior này nhưng cải thiện logic
        performAction("Xác nhận nhận hàng") {
            // Chỉ reload dữ liệu như code cũ
            ApiResult.Success(Unit)
        }
    }

    fun uploadImages(context: Context, uris: List<Uri>, imageType: String) {
        val s = _ui.value.state as? TransactionDetailState.Success ?: return
        val txnId = transactionId ?: return

        // Validate theo flow cải thiện
        val canUpload = when (imageType.uppercase()) {
            "PICKUP" -> s.details.isOwner && s.details.currentStep == RentalStep.OWNER_CONFIRMED
            "RETURN" -> s.details.isRenter && s.details.currentStep in listOf(
                RentalStep.RENTAL_ACTIVE,
                RentalStep.RENTAL_DUE
            )
            else -> false
        }

        if (!canUpload) {
            setError("Không thể upload ảnh ở bước hiện tại.")
            return
        }

        viewModelScope.launch {
            _ui.update { it.copy(isUploading = true, uploadSuccess = false, error = null) }

            val parts = uris.mapNotNull { FileUtils.uriToMultipart(context, it, "images") }
            if (parts.isEmpty()) {
                _ui.update { it.copy(isUploading = false, error = "Không thể xử lý file ảnh.") }
                return@launch
            }

            when (val res = transactionRepo.uploadTransactionImages(txnId, imageType, parts)) {
                is ApiResult.Success -> {
                    loadAll() // Reload toàn bộ state
                    _ui.update { it.copy(isUploading = false, uploadSuccess = true) }
                }
                is ApiResult.Error -> {
                    _ui.update { it.copy(isUploading = false, error = res.error.message) }
                }
            }
        }
    }

    // ---------- Helper methods ----------

    /** Generic method để thực hiện action với loading state */
    private fun performAction(actionName: String, apiCall: suspend () -> ApiResult<*>) {
        viewModelScope.launch {
            _ui.update { it.copy(isActionInProgress = true, actionSuccess = false, error = null) }

            when (val result = apiCall()) {
                is ApiResult.Success -> {
                    loadAll() // Reload toàn bộ state
                    _ui.update { it.copy(isActionInProgress = false, actionSuccess = true) }
                }
                is ApiResult.Error -> {
                    _ui.update { it.copy(isActionInProgress = false, error = result.error.message) }
                }
            }
        }
    }

    private fun setError(message: String) {
        _ui.update { it.copy(error = message) }
    }

    fun clearError() = _ui.update { it.copy(error = null) }
    fun clearUploadSuccess() = _ui.update { it.copy(uploadSuccess = false) }
    fun clearActionSuccess() = _ui.update { it.copy(actionSuccess = false) }

    // ---------- Polling với cải thiện ----------

    private fun shouldPoll(t: RentalTransactionResponse?): Boolean {
        val method = t?.paymentMethod?.uppercase()
        val status = t?.paymentStatus?.uppercase()
        return (method == "BANKING" && status == "PENDING")
    }

    private fun maybeStartPaymentPolling() {
        val s = _ui.value.state as? TransactionDetailState.Success ?: return
        if (!shouldPoll(s.details.transaction) || pollingJob?.isActive == true) return

        pollingJob = viewModelScope.launch {
            while (isActive) {
                val currentState = _ui.value.state as? TransactionDetailState.Success
                if (currentState == null || !shouldPoll(currentState.details.transaction)) break

                delay(5000) // Poll every 5 seconds
                refreshCompleteState() // Refresh toàn bộ state thay vì chỉ transaction
            }
            pollingJob = null
        }
    }

    /** Refresh toàn bộ state thay vì chỉ transaction */
    private suspend fun refreshCompleteState() {
        try {
            // Reload toàn bộ dữ liệu để đảm bảo consistency
            val currentState = _ui.value.state as? TransactionDetailState.Success ?: return

            // Reload transaction và images
            val txnRes = transactionRepo.getTransactionByRequestId(requestId)
            val imagesRes = transactionId?.let { transactionRepo.getImagesByTransactionId(it) }

            if (txnRes is ApiResult.Success) {
                val newTxn = txnRes.data
                val newImages = if (imagesRes is ApiResult.Success) imagesRes.data else currentState.details.images

                val newStep = determineCurrentStepImproved(currentState.details.request, newTxn, newImages)
                val newDetails = currentState.details.copy(
                    transaction = newTxn,
                    images = newImages,
                    currentStep = newStep
                )
                val newCaps = computeCapabilitiesImproved(newDetails)

                _ui.update { it.copy(state = TransactionDetailState.Success(newDetails, newCaps)) }
            }
        } catch (e: Exception) {
            // Log error but don't update UI to avoid disrupting user experience
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }

    private fun formatInstant(instant: Instant?): String {
        if (instant == null) return "-"
        return runCatching {
            DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy")
                .withLocale(Locale("vi", "VN"))
                .withZone(ZoneId.systemDefault())
                .format(instant)
        }.getOrDefault("-")
    }
}