package com.coffeelab.coffeenotes.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.coffeelab.coffeenotes.data.entity.PurchaseRecord
import com.coffeelab.coffeenotes.util.DateUtils
import com.coffeelab.coffeenotes.viewmodel.PurchaseRecordViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseRecordManagementScreen(
    navController: NavController,
    beanId: Long,
    beanName: String = "",
    viewModel: PurchaseRecordViewModel = viewModel()
) {
    LaunchedEffect(beanId) {
        viewModel.loadForBean(beanId)
    }

    val records by viewModel.records.collectAsState()
    val scope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<PurchaseRecord?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var recordToDelete by remember { mutableStateOf<PurchaseRecord?>(null) }

    val totalWeight = records.sumOf { it.weightGrams }
    val totalSpend = records.sumOf { it.price.toDouble() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("购买记录${if (beanName.isNotEmpty()) " - $beanName" else ""}") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "新增购买记录")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 汇总栏
            if (records.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${records.size}", style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Text("购买次数", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${totalWeight}g", style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Text("总重量", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("¥%.1f".format(totalSpend), style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Text("总花费", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                        if (totalWeight > 0) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("¥%.1f/g".format(totalSpend / totalWeight), style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer)
                                Text("均价", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }
                }
            }

            if (records.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        Spacer(Modifier.height(16.dp))
                        Text("暂无购买记录",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("点击 + 添加第一条记录", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(records, key = { it.id }) { record ->
                        PurchaseRecordCard(
                            record = record,
                            onEdit = { editingRecord = record },
                            onDelete = {
                                recordToDelete = record
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // 新增对话框
    if (showAddDialog) {
        PurchaseRecordDialog(
            title = "新增购买记录",
            record = null,
            onDismiss = { showAddDialog = false },
            onSave = { record ->
                scope.launch {
                    viewModel.insert(record.copy(beanId = beanId))
                    showAddDialog = false
                }
            }
        )
    }

    // 编辑对话框
    if (editingRecord != null) {
        PurchaseRecordDialog(
            title = "编辑购买记录",
            record = editingRecord,
            onDismiss = { editingRecord = null },
            onSave = { record ->
                scope.launch {
                    viewModel.update(record)
                    editingRecord = null
                }
            }
        )
    }

    // 删除确认对话框
    if (showDeleteDialog && recordToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除这条购买记录吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            viewModel.delete(recordToDelete!!)
                            showDeleteDialog = false
                            recordToDelete = null
                        }
                    }
                ) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun PurchaseRecordCard(
    record: PurchaseRecord,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(DateUtils.formatDate(record.date),
                    style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text("重量：${record.weightGrams}g  |  价格：¥${record.price}  |  均价：¥%.2f/g".format(record.unitPrice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PurchaseRecordDialog(
    title: String,
    record: PurchaseRecord?,
    onDismiss: () -> Unit,
    onSave: (PurchaseRecord) -> Unit
) {
    var dateStr by remember { mutableStateOf(record?.let { DateUtils.formatDate(it.date) } ?: DateUtils.formatDate(System.currentTimeMillis())) }
    var weightStr by remember { mutableStateOf(record?.weightGrams?.toString() ?: "") }
    var priceStr by remember { mutableStateOf(record?.price?.toString() ?: "") }
    var roastDateStr by remember { mutableStateOf(record?.roastDate?.let { DateUtils.formatDate(it) } ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = dateStr,
                    onValueChange = { dateStr = it },
                    label = { Text("购买日期") },
                    placeholder = { Text("格式：2026/05/10") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = weightStr,
                    onValueChange = { weightStr = it.filter { c -> c.isDigit() } },
                    label = { Text("重量 (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("总价 (元)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = roastDateStr,
                    onValueChange = { roastDateStr = it },
                    label = { Text("烘焙日期（选填）") },
                    placeholder = { Text("格式：2026/05/10") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                val weight = weightStr.toIntOrNull() ?: 0
                val price = priceStr.toFloatOrNull() ?: 0f
                if (weight > 0 && price > 0) {
                    Text("单价：¥%.2f/g".format(price / weight),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val date = DateUtils.parseDate(dateStr) ?: System.currentTimeMillis()
                    val weight = weightStr.toIntOrNull() ?: 0
                    val price = priceStr.toFloatOrNull() ?: 0f
                    val roastDate = if (roastDateStr.isNotBlank()) DateUtils.parseDate(roastDateStr) else null
                    if (weight > 0 && price > 0) {
                        onSave(PurchaseRecord(
                            id = record?.id ?: 0,
                            beanId = record?.beanId ?: 0,
                            date = date,
                            weightGrams = weight,
                            price = price,
                            unitPrice = price / weight,
                            roastDate = roastDate
                        ))
                    }
                },
                enabled = weightStr.toIntOrNull() != null && priceStr.toFloatOrNull() != null
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
