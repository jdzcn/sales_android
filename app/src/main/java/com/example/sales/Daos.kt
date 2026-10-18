package com.example.sales


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Insert
    suspend fun insertProduct(product: ProductEntity)

    @Query("SELECT * FROM product")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT COUNT(*) FROM product")
    suspend fun getProductCount(): Int
}

@Dao
interface SaleDao {
    // 核心：关联查询销售记录和商品
    @Transaction
    @Query("SELECT * FROM sale ORDER BY date DESC, id DESC  LIMIT 20")
    fun getSalesWithProduct(): Flow<List<SaleWithProduct>>

    @Insert
    suspend fun insertSale(sale: SaleEntity)

    @Query("DELETE FROM sale WHERE id = :id")
    suspend fun deleteSale(id: Int)

    // 1. 查询指定日期的总销售额
    @Query("SELECT SUM(amount) FROM sale WHERE date = :today")
    fun getTodayAmount(today: String): Flow<Double?>

    // 2. 查询指定日期的总毛利 (总金额 - 总成本)
    @Query("SELECT SUM(amount - cost) FROM sale WHERE date = :today")
    fun getTodayProfit(today: String): Flow<Double?>

    // 3. 查询前缀为该月的总毛利 (例如传入 "2026-10%")
    @Query("SELECT SUM(amount - cost) FROM sale WHERE date LIKE :monthPrefix")
    fun getMonthProfit(monthPrefix: String): Flow<Double?>
    // 查询指定时间段内的所有明细（用于前端聚合）
    @Transaction
    @Query("SELECT * FROM sale WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun getSalesByDateRange(startDate: String, endDate: String): Flow<List<SaleWithProduct>>

}