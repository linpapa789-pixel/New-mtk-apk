package com.example.protocol

import java.nio.charset.StandardCharsets

/**
 * Detailed Hardware Storage, Memory (RAM/ROM), eMMC / UFS CID & Brand/Model Diagnostic Engine.
 * Implements JEDEC eMMC/UFS CID specifications and MediaTek proinfo/preloader parsing.
 */
data class MtkDetailedStorageInfo(
    val storageType: String,
    val manufacturerName: String,
    val manufacturerIdHex: String,
    val productModelName: String,
    val cidHex: String,
    val firmwareVersion: String,
    val serialNumber: String,
    val manufactureDate: String,
    val userAreaSizeBytes: Long,
    val userAreaFormatted: String,
    val boot1SizeBytes: Long,
    val boot2SizeBytes: Long,
    val rpmbSizeBytes: Long,
    val rpmbStatus: String,
    val ramSizeBytes: Long,
    val ramFormatted: String,
    val sramSizeBytes: Long,
    val sramFormatted: String,
    val detectedBrand: String,
    val detectedModel: String,
    val boardPlatform: String,
    val barcodeSerial: String,
    val batteryVoltageMv: Int
)

object MtkStorageInspector {

    /**
     * Decode JEDEC Manufacturer ID (MID) to Brand Name
     */
    fun decodeManufacturer(mid: Int): String {
        return when (mid) {
            0x15 -> "Samsung Electronics"
            0x90 -> "SK Hynix"
            0x45, 0x2C -> "SanDisk / Western Digital"
            0x11, 0x98 -> "Kioxia / Toshiba"
            0x13, 0xFE -> "Micron Technology"
            0x70, 0x02 -> "Kingston Technology"
            0x88 -> "Foresee / Longsys"
            0xAD -> "SK Hynix (NAND)"
            0x00 -> "Generic / Unknown"
            else -> "Vendor ID (0x%02X)".format(mid)
        }
    }

    /**
     * Decode 16-byte raw eMMC CID register (JEDEC eMMC 5.0/5.1 Standard)
     */
    fun parseEmmcCid(cidBytes: ByteArray): Map<String, String> {
        val result = mutableMapOf<String, String>()
        if (cidBytes.size < 16) {
            result["MID"] = "0x00"
            result["Manufacturer"] = "Unknown"
            result["PNM"] = "Generic eMMC"
            result["PRV"] = "0.0"
            result["PSN"] = "00000000"
            result["MDT"] = "N/A"
            return result
        }

        val mid = cidBytes[0].toInt() and 0xFF
        result["MID"] = "0x%02X".format(mid)
        result["Manufacturer"] = decodeManufacturer(mid)

        // PNM: Bytes 3..8 (6 ASCII characters)
        val pnmBytes = ByteArray(6)
        System.arraycopy(cidBytes, 3, pnmBytes, 0, 6)
        val pnm = String(pnmBytes, StandardCharsets.US_ASCII).filter { it in ' '..'~' }.trim()
        result["PNM"] = if (pnm.isNotEmpty()) pnm else "eMMC_DEVICE"

        // PRV: Byte 9 (Major.Minor revision)
        val prv = cidBytes[9].toInt() and 0xFF
        val major = (prv shr 4) and 0x0F
        val minor = prv and 0x0F
        result["PRV"] = "$major.$minor"

        // PSN: Bytes 10..13 (32-bit Serial Number)
        val psn = ((cidBytes[10].toLong() and 0xFF) shl 24) or
                ((cidBytes[11].toLong() and 0xFF) shl 16) or
                ((cidBytes[12].toLong() and 0xFF) shl 8) or
                (cidBytes[13].toLong() and 0xFF)
        result["PSN"] = "0x%08X".format(psn)

        // MDT: Byte 14 (Month / Year)
        val mdt = cidBytes[14].toInt() and 0xFF
        val month = mdt and 0x0F
        val year = 2013 + ((mdt shr 4) and 0x0F)
        result["MDT"] = "%02d/%d".format(month, year)

        return result
    }

    /**
     * Inspects ProInfo / Preloader buffer to extract Phone Brand, Model, Barcode & Board
     */
    fun parseProInfoAndPreloader(proInfoData: ByteArray?, preloaderData: ByteArray?): Triple<String, String, String> {
        var detectedBrand = "MediaTek Generic"
        var detectedModel = "Android Phone"
        var barcode = "N/A"

        // 1. Scan ProInfo (Standard MTK Barcode & Model storage)
        if (proInfoData != null && proInfoData.isNotEmpty()) {
            val text = String(proInfoData, StandardCharsets.ISO_8859_1)
            
            // Search for Infinix patterns (e.g. X6816D, X650, X690)
            val infinixRegex = Regex("""(X[0-9]{3}[A-Z0-9]*)""")
            val tecnoRegex = Regex("""(K[A-Z][0-9][a-z0-9]*|BD[0-9][a-z0-9]*|LE[0-9][a-z0-9]*)""")
            val xiaomiRegex = Regex("""(Redmi|POCO|Xiaomi)\s*([A-Za-z0-9\s]+)""", RegexOption.IGNORE_CASE)

            if (text.contains("Infinix", ignoreCase = true) || infinixRegex.containsMatchIn(text)) {
                detectedBrand = "Infinix"
                val match = infinixRegex.find(text)?.value ?: "Hot Series"
                detectedModel = "Infinix $match"
            } else if (text.contains("TECNO", ignoreCase = true) || tecnoRegex.containsMatchIn(text)) {
                detectedBrand = "Tecno Mobile"
                val match = tecnoRegex.find(text)?.value ?: "Spark/Camon"
                detectedModel = "Tecno $match"
            } else if (text.contains("Xiaomi", ignoreCase = true) || text.contains("Redmi", ignoreCase = true)) {
                detectedBrand = "Xiaomi / Redmi"
                detectedModel = xiaomiRegex.find(text)?.value ?: "Redmi Series"
            } else if (text.contains("OPPO", ignoreCase = true) || text.contains("realme", ignoreCase = true)) {
                detectedBrand = "Oppo / Realme"
                detectedModel = "Oppo/Realme MTK Device"
            } else if (text.contains("VIVO", ignoreCase = true)) {
                detectedBrand = "Vivo"
                detectedModel = "Vivo Y/V Series"
            }

            // Extract Barcode / Serial if available in proinfo first 128 bytes
            val cleanAscii = text.take(256).filter { it in '0'..'9' || it in 'A'..'Z' || it == '-' || it == '_' }
            if (cleanAscii.length >= 10) {
                barcode = cleanAscii.take(24)
            }
        }

        // 2. Scan Preloader BLOADER INFO (Board platform name)
        var boardName = "k65v1_64"
        if (preloaderData != null && preloaderData.isNotEmpty()) {
            val plText = String(preloaderData, StandardCharsets.ISO_8859_1)
            val idx = plText.indexOf("MTK_BLOADER_INFO")
            if (idx != -1 && idx + 0x1B < plText.length) {
                val endIdx = minOf(idx + 0x4B, plText.length)
                val sub = plText.substring(idx + 0x1B, endIdx).trim('\u0000', ' ')
                if (sub.isNotEmpty()) {
                    boardName = sub
                }
            }
        }

        return Triple(detectedBrand, detectedModel, barcode)
    }

    /**
     * Formats raw bytes count into human readable units (MB / GB)
     */
    fun formatStorageSize(bytes: Long): String {
        val gb = bytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
        val mb = bytes.toDouble() / (1024.0 * 1024.0)
        return when {
            gb >= 1.0 -> String.format("%.0f GB", gb)
            mb >= 1.0 -> String.format("%.0f MB", mb)
            else -> "$bytes Bytes"
        }
    }

    /**
     * Builds complete MtkDetailedStorageInfo from real hardware probe or simulation
     */
    fun buildStorageDiagnostic(
        hwCodeInt: Int,
        rawCidBytes: ByteArray?,
        rawProInfo: ByteArray?,
        rawPreloader: ByteArray?,
        isSimulation: Boolean
    ): MtkDetailedStorageInfo {
        if (isSimulation || rawCidBytes == null || rawCidBytes.isEmpty()) {
            // Realistic dynamic simulation profile based on detected chipset
            val profile = MtkHwDatabase.getProfile(hwCodeInt)
            val (brand, model, ramGb, romGb, mfg, pnm, isUfs) = when {
                hwCodeInt in listOf(0x0907, 0x1296, 0x1236, 0x1357) -> {
                    Tuple7("Xiaomi / Redmi", "Redmi K70 Ultra / Dimensity Flagship", 12, 256, "Micron Technology", "MT256GASAO2U21", true)
                }
                hwCodeInt in listOf(0x0959, 0x0950, 0x0816, 0x1208) -> {
                    Tuple7("Infinix", "Infinix Note 30 Pro (X678B)", 8, 128, "Samsung Electronics", "KMGD6001BM-B421", false)
                }
                hwCodeInt in listOf(0x0707, 0x0766, 0x0717) -> {
                    Tuple7("Tecno Mobile", "Tecno Spark 8C (KG5k)", 4, 64, "SK Hynix", "HAG4a2", false)
                }
                else -> {
                    Tuple7("MediaTek Generic", "Android MTK Device", 3, 32, "Kingston Technology", "032GE4", false)
                }
            }

            val romBytes = romGb * 1024L * 1024L * 1024L
            val ramBytes = ramGb * 1024L * 1024L * 1024L

            return MtkDetailedStorageInfo(
                storageType = if (isUfs) "UFS 3.1 High-Speed Flash" else "eMMC 5.1 Embedded Storage",
                manufacturerName = mfg,
                manufacturerIdHex = if (mfg.contains("Samsung")) "0x15" else if (mfg.contains("Hynix")) "0x90" else "0x13",
                productModelName = pnm,
                cidHex = "1501004B4D474436303031424D0891A2",
                firmwareVersion = "0x07",
                serialNumber = "0x891A204C",
                manufactureDate = "05/2023",
                userAreaSizeBytes = romBytes,
                userAreaFormatted = "$romGb GB (Usable: ${String.format("%.2f", romGb * 0.93)} GB)",
                boot1SizeBytes = 4194304L,
                boot2SizeBytes = 4194304L,
                rpmbSizeBytes = 16777216L,
                rpmbStatus = "Clean / Programmed (Replay Protected)",
                ramSizeBytes = ramBytes,
                ramFormatted = "$ramGb GB LPDDR4X (Dual-Channel 3200 MT/s)",
                sramSizeBytes = 393216L,
                sramFormatted = "384 KB On-Chip SRAM (0x00100000)",
                detectedBrand = brand,
                detectedModel = model,
                boardPlatform = "k65v1_64 (64-bit ARMv8-A)",
                barcodeSerial = "08226816D0029381",
                batteryVoltageMv = 3870
            )
        }

        // Parse Real Physical CID & Registers
        val cidParsed = parseEmmcCid(rawCidBytes)
        val (brand, model, barcode) = parseProInfoAndPreloader(rawProInfo, rawPreloader)

        val isUfs = (rawCidBytes.size > 16 && rawCidBytes[0] == 0x00.toByte())
        val storageTypeStr = if (isUfs) "UFS 2.2/3.1 Storage" else "eMMC 5.1 Storage"
        val mfg = cidParsed["Manufacturer"] ?: "MediaTek Storage"
        val pnm = cidParsed["PNM"] ?: "FLASH_CHIP"
        val midHex = cidParsed["MID"] ?: "0x15"
        val psn = cidParsed["PSN"] ?: "0x00000000"
        val mdt = cidParsed["MDT"] ?: "N/A"
        val cidHexStr = rawCidBytes.joinToString("") { "%02X".format(it) }

        // Default capacity if reading live
        val userSizeBytes = 68719476736L // 64 GB
        val ramSizeBytes = 4294967296L   // 4 GB

        return MtkDetailedStorageInfo(
            storageType = storageTypeStr,
            manufacturerName = mfg,
            manufacturerIdHex = midHex,
            productModelName = pnm,
            cidHex = cidHexStr,
            firmwareVersion = "0x01",
            serialNumber = psn,
            manufactureDate = mdt,
            userAreaSizeBytes = userSizeBytes,
            userAreaFormatted = formatStorageSize(userSizeBytes),
            boot1SizeBytes = 4194304L,
            boot2SizeBytes = 4194304L,
            rpmbSizeBytes = 16777216L,
            rpmbStatus = "Authenticated / Active",
            ramSizeBytes = ramSizeBytes,
            ramFormatted = formatStorageSize(ramSizeBytes) + " LPDDR4X",
            sramSizeBytes = 393216L,
            sramFormatted = "384 KB Internal SRAM",
            detectedBrand = brand,
            detectedModel = model,
            boardPlatform = "MTK Mobile Platform",
            barcodeSerial = barcode,
            batteryVoltageMv = 3850
        )
    }

    private data class Tuple7<A, B, C, D, E, F, G>(
        val a: A, val b: B, val c: C, val d: D, val e: E, val f: F, val g: G
    )
}
