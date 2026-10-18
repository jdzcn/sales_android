package com.example.sales

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SaleViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val saleDao = db.saleDao()
    private val productDao = db.productDao()

    // UI 监听的真实数据源
    val salesList = saleDao.getSalesWithProduct().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // 供 UI 下拉框使用的商品列表
    val productList = productDao.getAllProducts().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // 初始化预设几个商品（如果你数据库是空的）
    fun initDefaultProducts() {
        viewModelScope.launch {
            if (productDao.getProductCount() == 0) {
                productDao.insertProduct(ProductEntity(name = "颜色釉", price = 69, cost = 15))
                productDao.insertProduct(ProductEntity(name = "手绘", price = 69, cost = 15))
                productDao.insertProduct(ProductEntity(name = "书法杯", price = 39, cost = 10))
            }
        }
    }

    fun addSale(sale: SaleEntity) {
        viewModelScope.launch {
            saleDao.insertSale(sale)
        }
    }

    fun deleteSale(id: Int) {
        viewModelScope.launch {
            saleDao.deleteSale(id)
        }
    }
    // 获取当前真实日期
    private val todayDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
    private val monthPrefix = todayDate.substring(0, 7) + "%"

    // 监听今日合计
    val todayAmount = saleDao.getTodayAmount(todayDate).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    // 监听今日毛利
    val todayProfit = saleDao.getTodayProfit(todayDate).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    // 监听本月毛利
    val monthProfit = saleDao.getMonthProfit(monthPrefix).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    // 默认时间范围（本月第一天到今天）
    private val _startDate = MutableStateFlow(todayDate.substring(0, 8) + "01")
    val startDate = _startDate.asStateFlow()

    private val _endDate = MutableStateFlow(todayDate)
    val endDate = _endDate.asStateFlow()

    // 响应时间变化，自动重新查询
    val statsList = combine(_startDate, _endDate) { start, end ->
        Pair(start, end)
    }.flatMapLatest { (start, end) ->
        saleDao.getSalesByDateRange(start, end)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // 更新时间的函数
    fun updateDateRange(start: String, end: String) {
        _startDate.value = start
        _endDate.value = end
    }

}