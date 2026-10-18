package com.example.sales

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sale_records")
data class SaleRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val productName: String,
    val quantity: Int,
    val price: Int,
    val cost: Int
) {
    val totalAmount get() = quantity * price
    val profit get() = totalAmount - cost
}