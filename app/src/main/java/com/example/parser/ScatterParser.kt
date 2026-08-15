package com.example.parser

import com.example.model.PartitionEntry

object ScatterParser {

    /**
     * Parses standard MediaTek scatter format (v1.1.0 to v2.0.0+)
     */
    fun parseScatter(content: String): Pair<String, List<PartitionEntry>> {
        var platform = "MT6765"
        val partitions = mutableListOf<PartitionEntry>()

        val lines = content.lines()
        var currentPartitionIndex = 0
        var currentPartName = ""
        var currentFileName = ""
        var currentLinearAddr = "0x0"
        var currentPhysicalAddr = "0x0"
        var currentSize = "0x0"
        var currentRegion = "EMMC_USER"
        var currentIsDownload = true
        var inPartitionBlock = false

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) continue

            if (line.contains("platform:", ignoreCase = true) || line.contains("project:", ignoreCase = true)) {
                val parts = line.split(":")
                if (parts.size >= 2) {
                    platform = parts[1].trim()
                }
                continue
            }

            if (line.startsWith("- partition_index:", ignoreCase = true) || line.startsWith("partition_index:", ignoreCase = true)) {
                if (inPartitionBlock && currentPartName.isNotEmpty()) {
                    val sizeBytes = parseHexOrDec(currentSize)
                    val isNv = isNvramPartition(currentPartName)
                    partitions.add(
                        PartitionEntry(
                            partitionIndex = currentPartitionIndex,
                            partitionName = currentPartName,
                            fileName = currentFileName.ifEmpty { "NONE" },
                            linearStartAddrHex = currentLinearAddr,
                            physicalStartAddrHex = currentPhysicalAddr,
                            partitionSizeHex = currentSize,
                            sizeBytes = sizeBytes,
                            region = currentRegion,
                            isDownload = currentIsDownload,
                            isProtectedNv = isNv
                        )
                    )
                }

                inPartitionBlock = true
                val idxStr = line.substringAfter(":").trim()
                currentPartitionIndex = idxStr.toIntOrNull() ?: (partitions.size)
                currentPartName = ""
                currentFileName = ""
                currentLinearAddr = "0x0"
                currentPhysicalAddr = "0x0"
                currentSize = "0x0"
                currentRegion = "EMMC_USER"
                currentIsDownload = true
                continue
            }

            if (inPartitionBlock) {
                when {
                    line.startsWith("partition_name:", ignoreCase = true) -> {
                        currentPartName = line.substringAfter(":").trim()
                    }
                    line.startsWith("file_name:", ignoreCase = true) -> {
                        currentFileName = line.substringAfter(":").trim()
                    }
                    line.startsWith("linear_start_addr:", ignoreCase = true) -> {
                        currentLinearAddr = line.substringAfter(":").trim()
                    }
                    line.startsWith("physical_start_addr:", ignoreCase = true) -> {
                        currentPhysicalAddr = line.substringAfter(":").trim()
                    }
                    line.startsWith("partition_size:", ignoreCase = true) -> {
                        currentSize = line.substringAfter(":").trim()
                    }
                    line.startsWith("region:", ignoreCase = true) -> {
                        currentRegion = line.substringAfter(":").trim()
                    }
                    line.startsWith("is_download:", ignoreCase = true) -> {
                        currentIsDownload = line.substringAfter(":").trim().equals("true", ignoreCase = true)
                    }
                }
            }
        }

        // Add last partition block if present
        if (inPartitionBlock && currentPartName.isNotEmpty()) {
            val sizeBytes = parseHexOrDec(currentSize)
            val isNv = isNvramPartition(currentPartName)
            partitions.add(
                PartitionEntry(
                    partitionIndex = currentPartitionIndex,
                    partitionName = currentPartName,
                    fileName = currentFileName.ifEmpty { "NONE" },
                    linearStartAddrHex = currentLinearAddr,
                    physicalStartAddrHex = currentPhysicalAddr,
                    partitionSizeHex = currentSize,
                    sizeBytes = sizeBytes,
                    region = currentRegion,
                    isDownload = currentIsDownload,
                    isProtectedNv = isNv
                )
            )
        }

        // If parsed empty (e.g. unformatted custom text), fallback to standard preset
        if (partitions.isEmpty()) {
            return getDefaultPreset("MT6765")
        }

        return Pair(platform, partitions)
    }

    private fun parseHexOrDec(str: String): Long {
        return try {
            val clean = str.trim()
            if (clean.startsWith("0x", ignoreCase = true)) {
                java.lang.Long.decode(clean)
            } else {
                clean.toLong()
            }
        } catch (_: Exception) {
            0L
        }
    }

    private fun isNvramPartition(name: String): Boolean {
        val lower = name.lowercase()
        return lower in listOf("nvram", "nvdata", "protect1", "protect2", "protect_f", "protect_s", "secro", "nvcfg", "proinfo")
    }

    fun getDefaultPreset(platform: String): Pair<String, List<PartitionEntry>> {
        val list = listOf(
            PartitionEntry(0, "preloader", "preloader.bin", "0x0", "0x0", "0x40000", 262144, "EMMC_BOOT_1", true, false),
            PartitionEntry(1, "pgpt", "pgpt.bin", "0x0", "0x0", "0x80000", 524288, "EMMC_USER", true, false),
            PartitionEntry(2, "nvram", "nvram.bin", "0x80000", "0x80000", "0x500000", 5242880, "EMMC_USER", true, true),
            PartitionEntry(3, "protect1", "protect1.bin", "0x580000", "0x580000", "0xA00000", 10485760, "EMMC_USER", true, true),
            PartitionEntry(4, "protect2", "protect2.bin", "0xF80000", "0xF80000", "0xA00000", 10485760, "EMMC_USER", true, true),
            PartitionEntry(5, "secro", "secro.bin", "0x1980000", "0x1980000", "0x600000", 6291456, "EMMC_USER", true, true),
            PartitionEntry(6, "nvcfg", "nvcfg.bin", "0x1F80000", "0x1F80000", "0x800000", 8388608, "EMMC_USER", true, true),
            PartitionEntry(7, "nvdata", "nvdata.bin", "0x2780000", "0x2780000", "0x2000000", 33554432, "EMMC_USER", true, true),
            PartitionEntry(8, "boot", "boot.img", "0x4780000", "0x4780000", "0x2000000", 33554432, "EMMC_USER", true, false),
            PartitionEntry(9, "dtbo", "dtbo.img", "0x6780000", "0x6780000", "0x800000", 8388608, "EMMC_USER", true, false),
            PartitionEntry(10, "vbmeta", "vbmeta.img", "0x6F80000", "0x6F80000", "0x800000", 8388608, "EMMC_USER", true, false),
            PartitionEntry(11, "recovery", "recovery.img", "0x7780000", "0x7780000", "0x2000000", 33554432, "EMMC_USER", true, false),
            PartitionEntry(12, "super", "super.img", "0x9780000", "0x9780000", "0x120000000", 4831838208, "EMMC_USER", true, false),
            PartitionEntry(13, "userdata", "NONE", "0x129780000", "0x129780000", "0x0", 0, "EMMC_USER", false, false)
        )
        return Pair(platform, list)
    }
}
