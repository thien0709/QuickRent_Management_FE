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
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch



data class FullRentalDetails(
    val request: RentalRequestResponse,
    val transaction: RentalTransactionResponse?,
    val item: ItemResponse,
    val images: List<TransactionImageResponse>,
    val isOwner: Boolean,
    val isRenter: Boolean,
    val currentStep: RentalStep
)

enum class RentalStep(val displayName: String, val description: String) {
    PENDING("Chờ xác nhận", "Chờ chủ sở hữu xác nhận yêu cầu thuê."),
    CONFIRMED("Chờ chuẩn bị hàng", "Chủ sở hữu đã xác nhận. Vui lòng chờ họ đăng ảnh bàn giao sản phẩm."),
    READY_FOR_PICKUP("Sẵn sàng bàn giao", "Sản phẩm đã sẵn sàng. Người thuê cần đến nhận và xác nhận đã nhận hàng."),
    RENTING("Đang thuê", "Sản phẩm đang trong thời gian thuê."),
    AWAITING_RETURN("Chờ trả hàng", "Đã hết hạn thuê. Người thuê cần đăng ảnh tình trạng sản phẩm trước khi trả."),
    RETURNED("Đã trả hàng", "Người thuê đã đăng ảnh trả hàng. Chờ chủ sở hữu nhận lại và xác nhận."),
    COMPLETED("Hoàn thành", "Giao dịch đã kết thúc thành công."),
    CANCELLED("Đã hủy", "Yêu cầu thuê đã được hủy."),
    REJECTED("Bị từ chối", "Chủ sở hữu đã từ chối yêu cầu thuê."),
    UNKNOWN("Không xác định", "Trạng thái không xác định.")
}

data class Capabilities(
    val ownerCanConfirmOrReject: Boolean = false,
    val ownerCanUploadPickupImages: Boolean = false,
    val ownerCanComplete: Boolean = false,

    val renterCanChoosePickupOrDelivery: Boolean = false,
    val renterCanUploadReturnImages: Boolean = false,

    val userCanCancel: Boolean = false
)

sealed interface DetailState {
    object Loading : DetailState
    data class Success(val details: FullRentalDetails, val caps: Capabilities) : DetailState
    data class Error(val message: String) : DetailState
}

data class DetailUiState(
    val state: DetailState = DetailState.Loading,
    val isUploading: Boolean = false,
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

    private val _ui = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _ui.asStateFlow()

    init { loadDetails() }

    fun loadDetails() {
        viewModelScope.launch {
            _ui.update { it.copy(state = DetailState.Loading) }

            val requestResult = requestRepo.getRentalRequestById(requestId)
            if (requestResult !is ApiResult.Success) {
                val errorMsg = (requestResult as? ApiResult.Error)?.error?.message ?: "Lỗi tải yêu cầu thuê."
                _ui.update { it.copy(state = DetailState.Error(errorMsg)) }
                return@launch
            }
            val request = requestResult.data

            try {
                val currentUserId = dataStore.userId.first()
                val itemDeferred = async { itemRepo.getItemInfo(request.itemId!!) }
                val transactionDeferred = async { transactionRepo.getTransactionByRequestId(requestId) }

                val item = (itemDeferred.await() as? ApiResult.Success)?.data
                    ?: throw IllegalStateException("Không thể tải thông tin sản phẩm.")
                val transaction = (transactionDeferred.await() as? ApiResult.Success)?.data

                val images = transaction?.id?.let {
                    (transactionRepo.getImagesByTransactionId(it) as? ApiResult.Success)?.data
                } ?: emptyList()

                val isOwner = item.ownerId == currentUserId
                val isRenter = request.renterId == currentUserId

                val currentStep = determineCurrentStep(request, images)
                val capabilities = computeCapabilities(currentStep, isOwner, isRenter, images)

                val details = FullRentalDetails(request, transaction, item, images, isOwner, isRenter, currentStep)
                _ui.update { it.copy(state = DetailState.Success(details, capabilities)) }

            } catch (e: Exception) {
                _ui.update { it.copy(state = DetailState.Error("Lỗi không xác định: ${e.message}")) }
            }
        }
    }

    private fun determineCurrentStep(request: RentalRequestResponse, images: List<TransactionImageResponse>): RentalStep {
        val status = request.status?.uppercase() ?: return RentalStep.UNKNOWN

        return when (status) {
            "PENDING" -> RentalStep.PENDING
            "CONFIRMED" -> RentalStep.CONFIRMED
            "READY_FOR_PICKUP" -> RentalStep.READY_FOR_PICKUP
            "RENTING" -> {
                val now = Instant.now()
                val hasReturnImages = images.any { it.imageType.equals("RETURN", true) }
                if (request.rentalEndTime != null && now.isAfter(request.rentalEndTime) && !hasReturnImages) {
                    RentalStep.AWAITING_RETURN
                } else {
                    RentalStep.RENTING
                }
            }
            "RETURNED" -> RentalStep.RETURNED
            "COMPLETED" -> RentalStep.COMPLETED
            "CANCELLED" -> RentalStep.CANCELLED
            "REJECTED" -> RentalStep.REJECTED
            else -> RentalStep.UNKNOWN
        }
    }

    private fun computeCapabilities(
        step: RentalStep,
        isOwner: Boolean,
        isRenter: Boolean,
        images: List<TransactionImageResponse>
    ): Capabilities {
        val hasReturnImages = images.any { it.imageType.equals("RETURN", true) }
        val renterCanCancel = isRenter && step == RentalStep.PENDING
        val ownerCanCancel = isOwner && step in listOf(RentalStep.PENDING, RentalStep.CONFIRMED)

        return Capabilities(
            ownerCanConfirmOrReject = isOwner && step == RentalStep.PENDING,
            ownerCanUploadPickupImages = isOwner && step == RentalStep.CONFIRMED,
            ownerCanComplete = isOwner && step == RentalStep.RETURNED,

            renterCanChoosePickupOrDelivery = isRenter && step == RentalStep.READY_FOR_PICKUP,

            renterCanUploadReturnImages = isRenter && !hasReturnImages && (step == RentalStep.RENTING || step == RentalStep.AWAITING_RETURN),
            userCanCancel = renterCanCancel || ownerCanCancel
        )
    }


    fun ownerConfirmRequest() = performAction { requestRepo.confirmRequest(requestId) }
    fun ownerRejectRequest() = performAction { requestRepo.rejectRequest(requestId) }
    fun userCancelRequest() = performAction { requestRepo.cancelRequest(requestId) }
    fun ownerCompleteRequest() = performAction { requestRepo.completeRequest(requestId) }
    fun renterConfirmPickup() = performAction { requestRepo.startRental(requestId) }

    fun uploadImages(context: Context, uris: List<Uri>, imageType: String) {
        val transactionId = (_ui.value.state as? DetailState.Success)?.details?.transaction?.id ?: return
        viewModelScope.launch {
            _ui.update { it.copy(isUploading = true) }
            val parts = uris.mapNotNull { FileUtils.uriToMultipart(context, it, "images") }
            if (parts.isEmpty()) {
                _ui.update { it.copy(isUploading = false, error = "Không thể xử lý file ảnh.") }
                return@launch
            }
            when (val result = transactionRepo.uploadTransactionImages(transactionId, imageType, parts)) {
                is ApiResult.Success -> loadDetails()
                is ApiResult.Error -> _ui.update { it.copy(error = result.error.message) }
            }
            _ui.update { it.copy(isUploading = false) }
        }
    }

    private fun performAction(apiCall: suspend () -> ApiResult<*>) {
        viewModelScope.launch {
            _ui.update { it.copy(isActionInProgress = true) }
            when (apiCall()) {
                is ApiResult.Success -> {
                    _ui.update { it.copy(actionSuccess = true) }
                    loadDetails()
                }
                is ApiResult.Error -> _ui.update { it.copy(error = (it as ApiResult.Error).error.message) }
            }
            _ui.update { it.copy(isActionInProgress = false) }
        }
    }

    fun clearError() = _ui.update { it.copy(error = null) }
    fun clearActionSuccess() = _ui.update { it.copy(actionSuccess = false) }
}