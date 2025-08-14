package com.bxt.viewmodel

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bxt.data.api.dto.request.ItemRequest
import com.bxt.data.local.DataStoreManager
import com.bxt.data.repository.ItemRepository
import com.bxt.di.ApiResult
import com.bxt.ui.state.AddItemState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

@HiltViewModel
class AddItemViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val dataStore: DataStoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<AddItemState>(AddItemState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _userId = MutableStateFlow<Long?>(null)
    val userId: StateFlow<Long?> = _userId.asStateFlow()

    init {
        // Collect userId from DataStore
        viewModelScope.launch {
            dataStore.userId.collect { id ->
                _userId.value = id
            }
        }
    }

    fun addItem(
        context: Context,
        req: ItemRequest, // req object gốc
        imageUris: List<Uri>
    ) {
        viewModelScope.launch {
            if (req.title.isBlank()) {
                _uiState.value = AddItemState.Error("Thiếu tiêu đề")
                return@launch
            }
            _uiState.value = AddItemState.Submitting

            // 1) KHÔNG CẦN TẠO JSON PART THỦ CÔNG NỮA
            // Dòng này đã được xóa:
            // val reqPart = gson.toJson(req).toRequestBody("application/json".toMediaType())

            // 2) Chuẩn bị file -> MultipartBody.Part("images")
            val tmpFiles = mutableListOf<File>()
            val imageParts = mutableListOf<MultipartBody.Part>()
            val total = imageUris.size
            var prepared = 0
            val warnings = mutableListOf<String>()

            withContext(Dispatchers.IO) { // Sử dụng Dispatchers.IO chuẩn
                _uiState.value = AddItemState.Uploading(prepared, total)
                for (uri in imageUris) {
                    val f = copyUriToCache(context, uri)
                    if (f == null) {
                        warnings += "Không đọc được ảnh: ${safeName(context, uri)}"
                        prepared += 1
                        _uiState.value = AddItemState.Uploading(prepared, total)
                        continue
                    }
                    tmpFiles += f

                    val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                    val body = f.asRequestBody(mime.toMediaType())
                    imageParts += MultipartBody.Part.createFormData(
                        name = "images",
                        filename = f.name,
                        body = body
                    )

                    prepared += 1
                    _uiState.value = AddItemState.Uploading(prepared, total)
                }
            }

            // 3) Gọi repo với đối tượng 'req' gốc
            when (val res = itemRepository.addItem(req, imageParts)) { // <-- THAY ĐỔI Ở ĐÂY
                is ApiResult.Success -> {
                    _uiState.value = AddItemState.Success(
                        data = res.data,
                        warning = warnings.takeIf { it.isNotEmpty() }?.joinToString("; ")
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = AddItemState.Error(res.error.message ?: "Không thể thêm sản phẩm")
                }
            }

            // 4) Dọn file tạm
            withContext(Dispatchers.IO) { tmpFiles.forEach { runCatching { it.delete() } } }
        }
    }

    // ============ Helpers (Không thay đổi) ============
    private fun copyUriToCache(context: Context, uri: Uri): File? = runCatching {
        val name = queryDisplayName(context, uri) ?: "IMG_${System.currentTimeMillis()}.tmp"
        val suffix = name.substringAfterLast('.', "tmp")
        val prefixRaw = name.substringBeforeLast('.', name)
        val prefix = if (prefixRaw.length >= 3) prefixRaw else "IMG"
        val tmp = File.createTempFile(prefix, ".$suffix", context.cacheDir)
        context.contentResolver.openInputStream(uri)?.use { input ->
            tmp.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        tmp
    }.getOrNull()

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return if (uri.scheme == "content") {
            var name: String? = null
            val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
            val cursor: Cursor? = context.contentResolver.query(uri, projection, null, null, null)
            cursor?.use {
                val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (it.moveToFirst() && idx >= 0) name = it.getString(idx)
            }
            name
        } else uri.lastPathSegment
    }

    private fun safeName(context: Context, uri: Uri): String =
        queryDisplayName(context, uri) ?: uri.toString()
}