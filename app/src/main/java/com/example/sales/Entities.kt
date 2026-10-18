package com.example.sales


import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// 1. 商品表 (对应你的 product)
@Entity(tableName = "product")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val pid: Int = 0,
    val name: String,
    val price: Int,
    val cost: Int
)

// 2. 销售表 (对应你的 sale)
@Entity(
    tableName = "sale",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["pid"],
            childColumns = ["pid"],
            onDelete = ForeignKey.CASCADE // 删商品时，连带删除相关销售记录
        )
    ],
    indices = [Index("pid")] // 给外键加索引，提升查询速度
)
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // 存格式："yyyy-MM-dd"
    val pid: Int,
    val price: Double,
    val number: Int,
    val amount: Double,
    val cost: Double,
    val remark: String?
)