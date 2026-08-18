package com.example.protocol

import android.content.Context
import android.util.Log
import java.io.InputStream

/**
 * Manager to read and extract bundled binary assets (DA Loaders, Payloads, Preloaders)
 * placed in `app/src/main/assets/` when building the APK via GitHub or Android Studio.
 */
class MtkAssetPayloadManager(private val context: Context) {

    companion object {
        private const val TAG = "MtkAssetManager"
        const val PATH_LOADERS = "loaders"
        const val PATH_PAYLOADS = "payloads"
        const val PATH_PRELOADERS = "preloaders"
    }

    /**
     * Reads any binary file from the assets directory and returns its raw ByteArray.
     * @param assetRelativePath Relative path inside assets/ (e.g. "loaders/MTK_DA_V6.bin")
     */
    fun readAssetBinary(assetRelativePath: String): ByteArray? {
        return try {
            context.assets.open(assetRelativePath).use { inputStream ->
                inputStream.readBytes()
            }
        } catch (e: Exception) {
            // Check if user placed it at root or without folder prefix
            val fallbackPath = assetRelativePath.substringAfterLast("/")
            try {
                context.assets.open(fallbackPath).use { inputStream ->
                    inputStream.readBytes()
                }
            } catch (e2: Exception) {
                Log.d(TAG, "Asset file not found in bundle: $assetRelativePath (${e.message})")
                null
            }
        }
    }

    /**
     * Load DA Loader binary (e.g. "MTK_DA_V6.bin", "MTK_DA_V5.bin", "MTK_AllInOne_DA.bin")
     */
    fun loadDaLoader(loaderName: String = "MTK_DA_V6.bin"): ByteArray? {
        val paths = listOf(
            "$PATH_LOADERS/$loaderName",
            "Loader/$loaderName",
            loaderName,
            "$PATH_LOADERS/MTK_DA_V6.bin",
            "$PATH_LOADERS/MTK_DA_V5.bin",
            "MTK_DA_V6.bin",
            "MTK_DA_V5.bin"
        )
        for (p in paths) {
            val data = readAssetBinary(p)
            if (data != null && data.isNotEmpty()) {
                Log.i(TAG, "Successfully loaded DA Loader asset: $p (${data.size} bytes)")
                return data
            }
        }
        return null
    }

    /**
     * Load Kamakiri / BROM exploit payload for a specific Chipset Code (e.g. "0x0766", "mt6765")
     */
    fun loadPayloadForChip(chipIdentifier: String): ByteArray? {
        val clean = chipIdentifier.lowercase().replace("0x", "")
        val candidateNames = listOf(
            "mt${clean}_payload.bin",
            "${clean}_payload.bin",
            "payload_$clean.bin",
            "$PATH_PAYLOADS/mt${clean}_payload.bin",
            "$PATH_PAYLOADS/${clean}_payload.bin"
        )
        for (candidate in candidateNames) {
            val data = readAssetBinary(candidate)
            if (data != null && data.isNotEmpty()) {
                Log.i(TAG, "Found matching payload for $chipIdentifier: $candidate (${data.size} bytes)")
                return data
            }
        }
        return null
    }

    /**
     * Load specific Preloader binary by name (e.g. "preloader_x627_h624.bin")
     */
    fun loadPreloader(preloaderFileName: String): ByteArray? {
        val paths = listOf(
            "$PATH_PRELOADERS/$preloaderFileName",
            "Preloader/$preloaderFileName",
            "Loader/Preloader/$preloaderFileName",
            preloaderFileName
        )
        for (p in paths) {
            val data = readAssetBinary(p)
            if (data != null && data.isNotEmpty()) {
                Log.i(TAG, "Loaded Preloader asset: $p (${data.size} bytes)")
                return data
            }
        }
        return null
    }

    /**
     * Lists all DA loaders present in the bundled assets folder.
     */
    fun listBundledLoaders(): List<String> {
        val result = mutableListOf<String>()
        try {
            val rootFiles = context.assets.list("") ?: emptyArray()
            val loaderFiles = context.assets.list(PATH_LOADERS) ?: emptyArray()
            val loaderFolderFiles = context.assets.list("Loader") ?: emptyArray()

            result.addAll(loaderFiles.map { "$PATH_LOADERS/$it" })
            result.addAll(loaderFolderFiles.filter { it.endsWith(".bin") }.map { "Loader/$it" })
            result.addAll(rootFiles.filter { it.startsWith("MTK_") && it.endsWith(".bin") })
        } catch (e: Exception) {
            Log.e(TAG, "Error listing asset loaders: ${e.message}")
        }
        return result.distinct()
    }

    /**
     * Lists all Preloader binaries bundled in assets.
     */
    fun listBundledPreloaders(): List<String> {
        val result = mutableListOf<String>()
        try {
            val preloaderFiles = context.assets.list(PATH_PRELOADERS) ?: emptyArray()
            val nestedPreloaderFiles = context.assets.list("Loader/Preloader") ?: emptyArray()

            result.addAll(preloaderFiles)
            result.addAll(nestedPreloaderFiles)
        } catch (e: Exception) {
            Log.e(TAG, "Error listing asset preloaders: ${e.message}")
        }
        return result.distinct()
    }
}
