package com.example.sales

import androidx.room.Embedded
import androidx.room.Relation

data class SaleWithProduct(
    @Embedded val sale: SaleEntity,

    @Relation(
        parentColumn = "pid",
        entityColumn = "pid"
    )
    val product: ProductEntity?
) {
    // 修改这里：加上 sale. 前缀
    val productName: String get() = product?.name ?: "未知商品"
    val profit: Double get() = sale.amount - sale.cost
}