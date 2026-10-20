package com.example.sales


import android.content.Context
import android.net.Uri
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import android.content.Intent
import androidx.core.content.FileProvider
object BackupManager {

    // 导出数据库
    suspend fun exportDatabase(context: Context, uri: Uri) {
        withContext(Dispatchers.IO) {
            try {
                // 1. 获取 App 内部数据库文件
                val dbFile = context.getDatabasePath("sales.db")
                if (!dbFile.exists()) {
                    showToast(context, "数据库文件不存在")
                    return@withContext
                }

                // 2. 写入到用户选择的 Uri 中
                val inputStream = FileInputStream(dbFile)
                val outputStream = context.contentResolver.openOutputStream(uri)

                inputStream.copyTo(outputStream!!)

                inputStream.close()
                outputStream.close()

                showToast(context, "导出成功！")
            } catch (e: Exception) {
                showToast(context, "导出失败: ${e.message}")
            }
        }
    }

    // 导入数据库
    suspend fun importDatabase(context: Context, uri: Uri) {
        withContext(Dispatchers.IO) {
            try {
                // 1. 必须强行关闭当前数据库，解除文件占用
                AppDatabase.getDatabase(context).close()

                val dbFile = context.getDatabasePath("sales.db")

                // 2. 把用户选的文件复制过来，覆盖原数据库
                val inputStream = context.contentResolver.openInputStream(uri)
                val outputStream = FileOutputStream(dbFile)

                inputStream?.copyTo(outputStream)

                inputStream?.close()
                outputStream.close()

                // 3. 清理可能残留的临时文件 (非常重要，否则会导致数据读取错误)
                val walFile = File(dbFile.path + "-wal")
                if (walFile.exists()) walFile.delete()
                val shmFile = File(dbFile.path + "-shm")
                if (shmFile.exists()) shmFile.delete()

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "导入成功！请彻底关闭App并重新打开以刷新数据。", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                showToast(context, "导入失败: ${e.message}")
            }
        }
    }

    private fun showToast(context: Context, message: String) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }


    // 在 BackupManager 里添加：
    fun shareDatabase(context: Context) {
        try {
            // 1. 获取真实的数据库路径
            val dbPath = AppDatabase.getDatabase(context).openHelper.writableDatabase.path
            val dbFile = File(dbPath)

            if (!dbFile.exists()) {
                Toast.makeText(context, "找不到数据库", Toast.LENGTH_SHORT).show()
                return
            }

            // 2. 复制一份到 App 的缓存目录 (因为直接分享沙盒文件微信没权限读)
            val cacheDir = File(context.cacheDir, "share_db")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            // 起个带日期的名字：管家婆_20261019.db
            val dateStr = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(java.util.Date())
            val shareFile = File(cacheDir, "sales_$dateStr.db")

            dbFile.copyTo(shareFile, overwrite = true)

            // 3. 生成 URI (使用 FileProvider，这是现代 Android 分享文件的唯一合法方式)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider", // 注意这个后缀，下面第二步要配
                shareFile
            )

            // 4. 唤起微信/QQ 等分享面板
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/x-sqlite3"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // 授予接收方临时读取权限
            }

            context.startActivity(Intent.createChooser(shareIntent, "分享数据库到..."))

        } catch (e: Exception) {
            Toast.makeText(context, "分享失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }


}