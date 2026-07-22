package com.yansproject.app.ui.invoice

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yansproject.app.data.IdrAccountingEngine
import com.yansproject.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentRecordBottomSheet(
    invoiceNumber: String,
    isCustomProject: Boolean,
    remainingBalance: Double,
    onDismiss: () -> Unit,
    onPaymentRecorded: (amount: Double, method: String, triggerWa: Boolean, customDateMillis: Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amountInput by remember { mutableStateOf(remainingBalance.toInt().toString()) }
    var selectedMethod by remember { mutableStateOf("TUNAI") }
    var triggerWhatsApp by remember { mutableStateOf(true) }
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    val paymentMethods = listOf("TUNAI", "TRANSFER BANK", "QRIS")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceDarkTealSurface,
        scrimColor = Color.Black.copy(alpha = 0.6f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = DividerDarkCyanGray) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "REKAM TRANSAKSI PEMBAYARAN",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = AccentAgedGold,
                        fontWeight = FontWeight.Bold
                    )
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = TextNonActive
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sub-info display
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SecondaryShadowBlackTeal)
                    .padding(12.dp)
            ) {
                Text(
                    text = "No. Tagihan: $invoiceNumber",
                    color = TextOnCarbon,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Sisa Pembayaran:", color = TextNonActive, fontSize = 12.sp)
                    Text(
                        text = IdrAccountingEngine.formatRupiahNoCents(remainingBalance),
                        color = AccentAgedGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive Clickable Calendar Preview
            CompactClickableCalendarPreview(
                selectedDateMillis = selectedDateMillis,
                onDateSelected = { selectedDateMillis = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Amount Input Field
            OutlinedTextField(
                value = amountInput,
                onValueChange = { input ->
                    if (input.all { it.isDigit() }) {
                        amountInput = input
                    }
                },
                label = { Text("Jumlah Pembayaran (Rp)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = HighlightSoftCyan,
                    unfocusedBorderColor = DividerDarkCyanGray,
                    focusedLabelColor = HighlightSoftCyan,
                    cursorColor = HighlightSoftCyan
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Payment Method Selector Label
            Text(
                text = "METODE PEMBAYARAN",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextNonActive,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Row containing method selectors
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                paymentMethods.forEach { method ->
                    val isSelected = selectedMethod == method
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) PrimaryDarkTeal else SecondaryShadowBlackTeal)
                            .clickable { selectedMethod = method }
                            .border(
                                width = 1.dp,
                                color = if (isSelected) HighlightSoftCyan else DividerDarkCyanGray,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = method,
                            color = if (isSelected) AccentAgedGold else TextIsiSoftGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // WhatsApp Notification Trigger Checkbox
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { triggerWhatsApp = !triggerWhatsApp }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = triggerWhatsApp,
                    onCheckedChange = { triggerWhatsApp = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = HighlightSoftCyan,
                        checkmarkColor = SecondaryShadowBlackTeal,
                        uncheckedColor = TextNonActive
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Kirim Resi Pembayaran Ke WhatsApp Klien",
                        color = TextOnCarbon,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Otomatis memicu n8n webhook secara asinkron.",
                        color = TextNonActive,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Submit Payment Button
            Button(
                onClick = {
                    val amt = amountInput.toDoubleOrNull() ?: 0.0
                    if (amt > 0.0) {
                        onPaymentRecorded(amt, selectedMethod, triggerWhatsApp, selectedDateMillis)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HighlightSoftCyan,
                    contentColor = SecondaryShadowBlackTeal
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "KONFIRMASI TERIMA BAYAR",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

/**
 * Interactive Calendar Preview Component allowing direct date clicking.
 */
@Composable
fun CompactClickableCalendarPreview(
    selectedDateMillis: Long,
    onDateSelected: (Long) -> Unit
) {
    var currentDisplayMonthCalendar by remember {
        mutableStateOf(Calendar.getInstance().apply { timeInMillis = selectedDateMillis })
    }

    val sdfMonthYear = remember { SimpleDateFormat("MMMM yyyy", Locale("id", "ID")) }
    val sdfSelectedDay = remember { SimpleDateFormat("EEEE, dd MMM yyyy", Locale("id", "ID")) }

    val daysOfWeek = listOf("Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab")

    val daysInMonth = remember(currentDisplayMonthCalendar.get(Calendar.MONTH), currentDisplayMonthCalendar.get(Calendar.YEAR)) {
        val tempCal = currentDisplayMonthCalendar.clone() as Calendar
        tempCal.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) - 1 // 0 = Sunday
        val maxDays = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val list = mutableListOf<CalendarDayItem?>()
        for (i in 0 until firstDayOfWeek) {
            list.add(null)
        }
        for (day in 1..maxDays) {
            val dayCal = tempCal.clone() as Calendar
            dayCal.set(Calendar.DAY_OF_MONTH, day)
            list.add(CalendarDayItem(day, dayCal.timeInMillis))
        }
        list
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SecondaryShadowBlackTeal)
            .border(1.dp, DividerDarkCyanGray, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TANGGAL PEMBAYARAN",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = AccentAgedGold,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = sdfSelectedDay.format(Date(selectedDateMillis)),
                color = HighlightSoftCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Month navigation header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    val newCal = currentDisplayMonthCalendar.clone() as Calendar
                    newCal.add(Calendar.MONTH, -1)
                    currentDisplayMonthCalendar = newCal
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Bulan Lalu", tint = AccentAgedGold)
            }

            Text(
                text = sdfMonthYear.format(currentDisplayMonthCalendar.time),
                color = TextOnCarbon,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = {
                    val newCal = currentDisplayMonthCalendar.clone() as Calendar
                    newCal.add(Calendar.MONTH, 1)
                    currentDisplayMonthCalendar = newCal
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Bulan Depan", tint = AccentAgedGold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Days of week header
        Row(modifier = Modifier.fillMaxWidth()) {
            daysOfWeek.forEach { dayName ->
                Text(
                    text = dayName,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 10.sp,
                    color = TextNonActive,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Grid of dates
        val rows = daysInMonth.chunked(7)
        rows.forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (i in 0..6) {
                    val item = week.getOrNull(i)
                    if (item != null) {
                        val itemCal = Calendar.getInstance().apply { timeInMillis = item.timeMillis }
                        val selectedCal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }

                        val isSelected = itemCal.get(Calendar.YEAR) == selectedCal.get(Calendar.YEAR) &&
                                itemCal.get(Calendar.DAY_OF_YEAR) == selectedCal.get(Calendar.DAY_OF_YEAR)

                        val isToday = itemCal.get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.YEAR) &&
                                itemCal.get(Calendar.DAY_OF_YEAR) == Calendar.getInstance().get(Calendar.DAY_OF_YEAR)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSelected -> HighlightSoftCyan
                                        isToday -> PrimaryDarkTeal
                                        else -> Color.Transparent
                                    }
                                )
                                .border(
                                    width = if (isToday && !isSelected) 1.dp else 0.dp,
                                    color = if (isToday && !isSelected) AccentAgedGold else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable {
                                    onDateSelected(item.timeMillis)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.dayNumber.toString(),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                color = when {
                                    isSelected -> SecondaryShadowBlackTeal
                                    isToday -> AccentAgedGold
                                    else -> TextOnCarbon
                                }
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private data class CalendarDayItem(val dayNumber: Int, val timeMillis: Long)

