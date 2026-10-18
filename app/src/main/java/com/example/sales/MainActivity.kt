package com.example.sales // 保留你的包名

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.launch
//import androidx.compose.foundation.text.KeyboardOptions
//import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF006C4C),
                    background = Color(0xFFFCFDF8),
                    surface = Color(0xFFFCFDF8)
                )
            ) {
                MainAppScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: SaleViewModel = viewModel()) {
    LaunchedEffect(Unit) { viewModel.initDefaultProducts() }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showBottomSheet by remember { mutableStateOf(false) }

    // 收集所有数据状态
    val salesList by viewModel.salesList.collectAsState()
    val productList by viewModel.productList.collectAsState()
    val todayAmount by viewModel.todayAmount.collectAsState()
    val todayProfit by viewModel.todayProfit.collectAsState()
    val monthProfit by viewModel.monthProfit.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("管家婆", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = { showBottomSheet = true },
                    icon = { Icon(Icons.Filled.Add, contentDescription = "新增") },
                    text = { Text("记一笔") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Icon(Icons.Filled.Home, null) }, label = { Text("明细") })
                NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Icon(Icons.Filled.DateRange, null) }, label = { Text("统计") })
                NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 }, icon = { Icon(Icons.Filled.Settings, null) }, label = { Text("我的") })
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when (selectedTab) {
                0 -> HomeScreen(
                    salesList = salesList,
                    todayAmount = todayAmount ?: 0.0,
                    todayProfit = todayProfit ?: 0.0,
                    monthProfit = monthProfit ?: 0.0,
                    onDeleteConfirm = { id -> viewModel.deleteSale(id) } // 真正的删除动作
                )
                1 -> StatsScreen(viewModel)
                2 -> SettingsScreen()
            }
        }
    }

    if (showBottomSheet) {
        AddSaleBottomSheet(
            products = productList,
            onDismiss = { showBottomSheet = false },
            onSave = { sale ->
                viewModel.addSale(sale)
                showBottomSheet = false
            }
        )
    }
}

// ================== 主页 ==================
@Composable
fun HomeScreen(
    salesList: List<SaleWithProduct>,
    todayAmount: Double,
    todayProfit: Double,
    monthProfit: Double,
    onDeleteConfirm: (Int) -> Unit
) {
    // 控制删除确认弹窗的状态
    var recordToDelete by remember { mutableStateOf<SaleWithProduct?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, bottom = 100.dp)
    ) {
        item {
            SummaryCard(todayAmount, todayProfit, monthProfit)
            Spacer(modifier = Modifier.height(24.dp))
            Text("最近交易", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        items(salesList) { record ->
            TransactionCard(
                record = record,
                onClick = { recordToDelete = record } // 点击只触发弹窗
            )
        }
    }

    // 确认删除弹窗 (Material 3 标准 AlertDialog)
    if (recordToDelete != null) {
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除【${recordToDelete?.productName}】的这笔记录吗？删除后不可恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        recordToDelete?.let { onDeleteConfirm(it.sale.id) }
                        recordToDelete = null // 关闭弹窗
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }
}

// 统计卡片（真实数据）
@Composable
fun SummaryCard(todayAmount: Double, todayProfit: Double, monthProfit: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        // 使用 Row 等分成 3 列
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 第一列：本日合计
            Column(modifier = Modifier.weight(1f)) {
                Text("本日合计", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "¥ ${String.format("%.2f", todayAmount)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // 第二列：本日毛利
            Column(modifier = Modifier.weight(1f)) {
                Text("本日毛利", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "¥ ${String.format("%.2f", todayProfit)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // 第三列：本月毛利
            Column(modifier = Modifier.weight(1f)) {
                Text("本月毛利", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "¥ ${String.format("%.2f", monthProfit)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
// 列表卡片 (不再直接删除，通过回调通知弹窗)
@Composable
fun TransactionCard(record: SaleWithProduct, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clickable { onClick() }, // 点击触发弹窗
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = record.productName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = record.sale.date, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "¥ ${record.sale.amount}", style = MaterialTheme.typography.titleMedium)
                Text(text = "利润: ${String.format("%.2f", record.profit)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// ================== 底部表单 (保持不变) ==================


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSaleBottomSheet(products: List<ProductEntity>, onDismiss: () -> Unit, onSave: (SaleEntity) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf<ProductEntity?>(null) }

    // 将数量、售价、成本改为 TextFieldValue 以支持选中文本
    var quantityState by remember { mutableStateOf(TextFieldValue("1")) }
    var priceState by remember { mutableStateOf(TextFieldValue("")) }
    var costState by remember { mutableStateOf(TextFieldValue("")) }

    // 当选择商品时，自动填充标准售价和成本
    LaunchedEffect(selectedProduct) {
        selectedProduct?.let {
            priceState = TextFieldValue(it.price.toString())
            costState = TextFieldValue(it.cost.toString())
        }
    }

    // 实时计算小计金额
    val quantity = quantityState.text.toDoubleOrNull() ?: 0.0
    val currentPrice = priceState.text.toDoubleOrNull() ?: 0.0
    val amount = quantity * currentPrice

    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(16.dp).padding(bottom = 32.dp)) {
            Text("新增销售记录", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            // 商品下拉选择框 (保持不变)
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(
                    value = selectedProduct?.name ?: "请选择商品",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("商品") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    products.forEach { product ->
                        DropdownMenuItem(
                            text = { Text(product.name) },
                            onClick = {
                                selectedProduct = product
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // 数量输入框
                OutlinedTextField(
                    value = quantityState,
                    onValueChange = { quantityState = it },
                    label = { Text("数量") },
                    modifier = Modifier.weight(1f).onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            // 获得焦点时，全选文本
                            quantityState = quantityState.copy(selection = TextRange(0, quantityState.text.length))
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                // 售价输入框
                OutlinedTextField(
                    value = priceState,
                    onValueChange = { priceState = it },
                    label = { Text("售价") },
                    modifier = Modifier.weight(1f).onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            priceState = priceState.copy(selection = TextRange(0, priceState.text.length))
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                // 成本输入框
                OutlinedTextField(
                    value = costState,
                    onValueChange = { costState = it },
                    label = { Text("成本") },
                    modifier = Modifier.weight(1f).onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            costState = costState.copy(selection = TextRange(0, costState.text.length))
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("小计金额: ¥${String.format("%.2f", amount)}", fontSize = 18.sp, color = Color.Red, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (selectedProduct != null && quantityState.text.isNotBlank()) {
                        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        val finalQuantity = quantityState.text.toIntOrNull() ?: 1
                        val finalPrice = priceState.text.toDoubleOrNull() ?: 0.0
                        val finalCost = (costState.text.toDoubleOrNull() ?: 0.0) * finalQuantity

                        onSave(
                            SaleEntity(
                                date = dateStr,
                                pid = selectedProduct!!.pid,
                                price = finalPrice,
                                number = finalQuantity,
                                amount = amount,
                                cost = finalCost,
                                remark = ""
                            )
                        )
                    }
                },
                enabled = selectedProduct != null,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("保存", fontSize = 16.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: SaleViewModel) {
    val statsList by viewModel.statsList.collectAsState()
    val startDate by viewModel.startDate.collectAsState()
    val endDate by viewModel.endDate.collectAsState()

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    // 新增：记录当前选中的 Tab (0:月, 1:商品, 2:日)
    var selectedStatsTab by remember { mutableIntStateOf(1) } // 默认停在"商品"，因为最常用

    Column(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
        // 1. 顶部时间选择器
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = { showStartPicker = true }) { Text(startDate) }
            Text("至", fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = { showEndPicker = true }) { Text(endDate) }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. 新增：M3 风格的 Tab 切换栏
        TabRow(
            selectedTabIndex = selectedStatsTab,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            Tab(selected = selectedStatsTab == 0, onClick = { selectedStatsTab = 0 }, text = { Text("月汇总") })
            Tab(selected = selectedStatsTab == 1, onClick = { selectedStatsTab = 1 }, text = { Text("商品汇总") })
            Tab(selected = selectedStatsTab == 2, onClick = { selectedStatsTab = 2 }, text = { Text("日汇总") })
        }

        // 3. 列表渲染区（根据选中的 Tab 动态变化）
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, bottom = 100.dp)
        ) {
            if (selectedStatsTab == 0) {
                // ====== 月汇总 ======
                val monthlyData = statsList.groupBy { it.sale.date.substring(0, 7) }.mapValues { entry ->
                    val amount = entry.value.sumOf { it.sale.amount }
                    val profit = entry.value.sumOf { it.profit }
                    Pair(amount, profit)
                }
                items(monthlyData.toList()) { (month, data) ->
                    SummaryRow(title = month, amount = data.first, profit = data.second)
                }

            } else if (selectedStatsTab == 1) {
                // ====== 商品汇总 ======
                val productData = statsList.groupBy { it.productName }.mapValues { entry ->
                    val num = entry.value.sumOf { it.sale.number }
                    val amount = entry.value.sumOf { it.sale.amount }
                    val profit = entry.value.sumOf { it.profit }
                    Triple(num, amount, profit)
                }.toList().sortedByDescending { it.second.third } // 按毛利降序

                items(productData) { (name, data) ->
                    ProductSummaryRow(name = name, number = data.first, amount = data.second, profit = data.third)
                }

            } else {
                // ====== 日汇总 ======
                val dailyData = statsList.groupBy { it.sale.date.substring(0, 10) }.mapValues { entry ->
                    val count = entry.value.size
                    val amount = entry.value.sumOf { it.sale.amount }
                    val profit = entry.value.sumOf { it.profit }
                    Triple(count, amount, profit)
                }
                items(dailyData.toList()) { (date, data) ->
                    DailySummaryRow(date = date, count = data.first, amount = data.second, profit = data.third)
                }
            }
        }
    }

    // --- 日期选择器逻辑（保持你原来的不变）---
    if (showStartPicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showStartPicker = false
                    datePickerState.selectedDateMillis?.let {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        viewModel.updateDateRange(sdf.format(Date(it)), endDate)
                    }
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showStartPicker = false }) { Text("取消") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showEndPicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showEndPicker = false
                    datePickerState.selectedDateMillis?.let {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        viewModel.updateDateRange(startDate, sdf.format(Date(it)))
                    }
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showEndPicker = false }) { Text("取消") } }
        ) { DatePicker(state = datePickerState) }
    }
}
// === 辅助 UI 组件 ===
@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 12.dp)
    )
}

@Composable
fun SummaryRow(title: String, amount: Double, profit: Double) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, fontWeight = FontWeight.Bold)
        Text("销售: ¥${String.format("%.2f", amount)}")
        Text("毛利: ¥${String.format("%.2f", profit)}", color = MaterialTheme.colorScheme.primary)
    }
    Divider(color = Color.LightGray.copy(alpha = 0.5f))
}

@Composable
fun ProductSummaryRow(name: String, number: Int, amount: Double, profit: Double) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(name, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
        Text("${number}件", modifier = Modifier.width(50.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
        Text("¥${String.format("%.2f", amount)}", modifier = Modifier.width(80.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
        Text("毛利:¥${String.format("%.2f", profit)}", modifier = Modifier.width(100.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End, color = MaterialTheme.colorScheme.primary)
    }
    Divider(color = Color.LightGray.copy(alpha = 0.5f))
}

@Composable
fun DailySummaryRow(date: String, count: Int, amount: Double, profit: Double) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(date, fontWeight = FontWeight.Bold)
        Text("$count 笔")
        Text("销售: ¥${String.format("%.2f", amount)}")
        Text("毛利: ¥${String.format("%.2f", profit)}", color = MaterialTheme.colorScheme.primary)
    }
    Divider(color = Color.LightGray.copy(alpha = 0.5f))
}



@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 导出文件启动器
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/x-sqlite3")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch { BackupManager.exportDatabase(context, uri) }
        }
    }

    // 导入文件启动器
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch { BackupManager.importDatabase(context, uri) }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text(
            text = "数据管理",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // 导出卡片
        SettingsCard(
            title = "备份数据库",
            subtitle = "将当前所有数据导出为 .db 文件",
            icon = Icons.Default.Save,
            onClick = {
                // 默认文件名带上时间戳，防止覆盖
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                exportLauncher.launch("salesa_$timeStamp.db")
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 导入卡片
        SettingsCard(
            title = "恢复数据",
            subtitle = "从本地 .db 文件覆盖恢复数据",
            icon = Icons.Default.Add,
            onClick = {
                importLauncher.launch(arrayOf("*/*")) // 不限后缀，方便各种文件管理器选择
            }
        )

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "提示：恢复数据后，请将App从后台彻底划掉并重新打开，数据才会刷新。",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// 辅助组件：设置项卡片
@Composable
fun SettingsCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.LightGray)
        }
    }
}