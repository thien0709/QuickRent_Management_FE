package com.bxt.ui.screen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.bxt.data.api.dto.response.TransportPackageResponse
import com.bxt.data.api.dto.response.TransportPassengerResponse
import com.bxt.data.api.dto.response.TransportServiceResponse
import com.bxt.ui.components.LoadingIndicator
import com.bxt.ui.state.TransportTransactionState
import com.bxt.viewmodel.TransportTransactionDetailViewModel
import java.math.BigDecimal
import java.math.RoundingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransportServiceDetailScreen(
    navController: NavController,
    viewModel: TransportTransactionDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val mySent by viewModel.myPackagesSent.collectAsStateWithLifecycle()
    val myRecv by viewModel.myPackagesReceived.collectAsStateWithLifecycle()

    // địa chỉ đã resolve
    val serviceAddr by viewModel.serviceAddr.collectAsStateWithLifecycle()
    val passengerAddrs by viewModel.passengerAddrs.collectAsStateWithLifecycle()
    val packageAddrs by viewModel.packageAddrs.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.actionResult.collect {
            Toast.makeText(navController.context, it, Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(Unit) {
        viewModel.navigateBackEvent.collect { navController.popBackStack() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chi tiết chuyến #${viewModel.serviceId}") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = { TextButton(onClick = { viewModel.refresh() }) { Text("Làm mới") } }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            when (val state = uiState) {
                is TransportTransactionState.Loading -> LoadingIndicator()
                is TransportTransactionState.Error -> Text("Lỗi: ${state.message}")
                is TransportTransactionState.Success -> {
                    val details = state.details
                    val perms = state.permissions

                    val serviceFrom = serviceAddr?.first
                        ?: coordsLabel(details.service.fromLatitude, details.service.fromLongitude)
                    val serviceTo = serviceAddr?.second
                        ?: coordsLabel(details.service.toLatitude, details.service.toLongitude)

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item(key = "trip_info") {
                            TripInfoCard(details.service, serviceFrom, serviceTo)
                        }

                        if (state.isOwner) {
                            item(key = "owner_actions") {
                                OwnerActions(viewModel, perms)
                            }

                            // Hành khách
                            item(key = "passenger_header") {
                                SectionTitle("Hành khách (${details.passengers.size})")
                            }
                            if (details.passengers.isEmpty()) {
                                item(key = "passenger_empty") { Text("Chưa có hành khách nào.") }
                            } else {
                                items(
                                    details.passengers,
                                    key = { p -> "passenger:${p.id ?: "tmp-${p.hashCode()}"}" }
                                ) { p ->
                                    val addr = passengerAddrs[p.id ?: -1]
                                    val pickup = addr?.first
                                        ?: coordsLabel(p.pickupLatitude, p.pickupLongitude)
                                    val dropoff = addr?.second
                                        ?: coordsLabel(p.dropoffLatitude, p.dropoffLongitude)
                                    PassengerRow(p, pickup, dropoff)
                                }
                            }

                            // Gói hàng toàn chuyến
                            item(key = "pkg_header") {
                                SectionTitle("Gói hàng trong chuyến (${details.packages.size})")
                            }
                            if (details.packages.isEmpty()) {
                                item(key = "pkg_empty") { Text("Chưa có gói hàng nào.") }
                            } else {
                                items(
                                    details.packages,
                                    key = { k -> "pkg_all:${k.id ?: "tmp-${k.hashCode()}"}" }
                                ) { pkg ->
                                    val addr = packageAddrs[pkg.id ?: -1]
                                    val from = addr?.first
                                        ?: coordsLabel(pkg.fromLatitude, pkg.fromLongitude)
                                    val to = addr?.second
                                        ?: coordsLabel(pkg.toLatitude, pkg.toLongitude)
                                    PackageRow(
                                        title = "Gói #${pkg.id ?: "-"}",
                                        fromText = from,
                                        toText = to
                                    )
                                }
                            }
                        } else {
                            // Người tham gia
                            val myAddr = state.myPassenger?.id?.let { passengerAddrs[it] }
                            val pickup = myAddr?.first ?: coordsLabel(
                                state.myPassenger?.pickupLatitude,
                                state.myPassenger?.pickupLongitude
                            )
                            val dropoff = myAddr?.second ?: coordsLabel(
                                state.myPassenger?.dropoffLatitude,
                                state.myPassenger?.dropoffLongitude
                            )
                            item(key = "renter_booking") {
                                RenterBooking(viewModel, state.myPassenger, pickup, dropoff, perms)
                            }

                            item(key = "pkg_sent_header") {
                                SectionTitle("Gói tôi gửi (${mySent.size})")
                            }
                            if (mySent.isEmpty()) {
                                item(key = "pkg_sent_empty") { Text("Không có gói đang gửi trong chuyến này.") }
                            } else {
                                items(
                                    mySent,
                                    key = { p -> "pkg_sent:${p.id ?: "tmp-${p.hashCode()}"}" }
                                ) { p ->
                                    val addr = packageAddrs[p.id ?: -1]
                                    val from = addr?.first
                                        ?: coordsLabel(p.fromLatitude, p.fromLongitude)
                                    val to = addr?.second
                                        ?: coordsLabel(p.toLatitude, p.toLongitude)
                                    PackageRow(
                                        title = "Gửi #${p.id ?: "-"}",
                                        fromText = from,
                                        toText = to
                                    ) {
                                        OutlinedButton(onClick = { p.id?.let(viewModel::cancelMyPackage) }) {
                                            Text("Huỷ")
                                        }
                                    }
                                }
                            }

                            item(key = "pkg_recv_header") {
                                SectionTitle("Gói tôi nhận (${myRecv.size})")
                            }
                            if (myRecv.isEmpty()) {
                                item(key = "pkg_recv_empty") { Text("Không có gói cần nhận trong chuyến này.") }
                            } else {
                                items(
                                    myRecv,
                                    key = { p -> "pkg_recv:${p.id ?: "tmp-${p.hashCode()}"}" }
                                ) { p ->
                                    val addr = packageAddrs[p.id ?: -1]
                                    val from = addr?.first
                                        ?: coordsLabel(p.fromLatitude, p.fromLongitude)
                                    val to = addr?.second
                                        ?: coordsLabel(p.toLatitude, p.toLongitude)
                                    PackageRow(
                                        title = "Nhận #${p.id ?: "-"}",
                                        fromText = from,
                                        toText = to
                                    ) {
                                        OutlinedButton(onClick = { p.id?.let(viewModel::cancelMyPackage) }) {
                                            Text("Huỷ")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/* === UI con === */

@Composable
private fun TripInfoCard(service: TransportServiceResponse, fromText: String, toText: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Thông tin chuyến", style = MaterialTheme.typography.titleMedium)
            InfoRow("Trạng thái", service.status ?: "-")
            InfoRow("Lộ trình", "$fromText → $toText")
            InfoRow("Phí dự kiến", service.deliveryFee?.toString() ?: "-")
            InfoRow("Số chỗ (còn)", (service.availableSeat ?: 0).toString())
        }
    }
}

@Composable
private fun OwnerActions(
    viewModel: TransportTransactionDetailViewModel,
    perms: com.bxt.ui.state.Permissions
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Hành động (Chủ chuyến)", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.confirmTrip() }, enabled = perms.canConfirm, modifier = Modifier.weight(1f)) { Text("Xác nhận") }
                Button(onClick = { viewModel.startTrip() },   enabled = perms.canStart,   modifier = Modifier.weight(1f)) { Text("Bắt đầu") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.completeTrip() }, enabled = perms.canComplete, modifier = Modifier.weight(1f)) { Text("Hoàn thành") }
                OutlinedButton(onClick = { viewModel.cancelTrip() }, enabled = perms.canCancelTrip, modifier = Modifier.weight(1f)) { Text("Huỷ chuyến") }
            }
        }
    }
}

@Composable
private fun PassengerRow(
    p: TransportPassengerResponse,
    pickupText: String,
    dropoffText: String
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("Hành khách #${p.id ?: "-"}", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text("Đón: $pickupText")
            Text("Trả: $dropoffText")
        }
    }
}

@Composable
private fun RenterBooking(
    viewModel: TransportTransactionDetailViewModel,
    myPassenger: TransportPassengerResponse?,
    pickupText: String,
    dropoffText: String,
    perms: com.bxt.ui.state.Permissions
) {
    Text("Đặt chỗ của tôi (Thuê)", style = MaterialTheme.typography.titleLarge)
    if (myPassenger == null) {
        Text("Bạn chưa có đặt chỗ trong chuyến này.")
    } else {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Mã đặt chỗ #${myPassenger.id ?: "-"}", fontWeight = FontWeight.SemiBold)
                Text("Điểm đón: $pickupText")
                Text("Điểm trả: $dropoffText")
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.cancelMyBooking() },
                    enabled = perms.canCancelMyBooking,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Huỷ đặt chỗ") }
            }
        }
    }
}

@Composable
private fun PackageRow(
    title: String,
    fromText: String,
    toText: String,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text("$fromText → $toText", style = MaterialTheme.typography.bodySmall)
            }
            if (trailing != null) Row(content = trailing)
        }
    }
}

@Composable private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge)
}

@Composable private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

/* ===== Helpers ===== */

private fun coordsLabel(lat: BigDecimal?, lng: BigDecimal?): String =
    if (lat == null || lng == null) "Không rõ" else "(${lat.short()}, ${lng.short()})"

private fun BigDecimal.short(): String =
    try { this.setScale(5, RoundingMode.HALF_UP).toPlainString() } catch (_: Throwable) { this.toString() }
