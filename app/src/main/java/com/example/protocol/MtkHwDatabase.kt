package com.example.protocol

/**
 * Complete MediaTek SoC hardware specifications, register maps & security vectors.
 * Ported directly from mtkclient's brom_config.py and mtk_config.py.
 */
data class MtkChipProfile(
    val hwCode: Int,
    val name: String,
    val description: String,
    val watchdogAddr: Long = 0x10007000L,
    val watchdogValue: Long = 0x22000064L,
    val uartAddr: Long = 0x11002000L,
    val bromPayloadAddr: Long = 0x100A00L,
    val daPayloadAddr: Long = 0x201000L,
    val plPayloadAddr: Long = 0x40200000L,
    val sejBase: Long? = 0x1000A000L,
    val gcpuBase: Long? = null,
    val cqdmaBase: Long? = null,
    val dxccBase: Long? = null,
    val efuseAddr: Long? = null,
    val meidAddr: Long? = null,
    val socidAddr: Long? = null,
    val var1: Int = 0x0A,
    val daMode: String = "XFLASH",
    val is64Bit: Boolean = false,
    val isIot: Boolean = false
)

object MtkHwDatabase {

    val CHIP_PROFILES: Map<Int, MtkChipProfile> = mapOf(
        // Legacy 32-bit & IoT SoCs
        0x2601 to MtkChipProfile(
            hwCode = 0x2601,
            name = "MT2601",
            description = "Smartwatch Wearable SoC",
            watchdogAddr = 0x10007000L,
            uartAddr = 0x11005000L,
            daPayloadAddr = 0x2008000L,
            plPayloadAddr = 0x81E00000L,
            sejBase = 0x1000A000L,
            daMode = "LEGACY",
            isIot = true
        ),
        0x6261 to MtkChipProfile(
            hwCode = 0x6261,
            name = "MT6261 / MT2503",
            description = "Feature Phone / IoT SoC",
            watchdogAddr = 0xA0030000L,
            watchdogValue = 0x2200L,
            uartAddr = 0xA0080000L,
            sejBase = 0xA0110000L,
            var1 = 0x28,
            daMode = "LEGACY",
            isIot = true
        ),
        0x6572 to MtkChipProfile(
            hwCode = 0x6572,
            name = "MT6572",
            description = "Dual-Core 3G Smartphone SoC",
            watchdogAddr = 0x10007000L,
            uartAddr = 0x11005000L,
            bromPayloadAddr = 0x10036A0L,
            daPayloadAddr = 0x2008000L,
            plPayloadAddr = 0x81E00000L,
            efuseAddr = 0x10009000L,
            daMode = "LEGACY"
        ),
        0x6580 to MtkChipProfile(
            hwCode = 0x6580,
            name = "MT6580",
            description = "Quad-Core 3G Smartphone SoC",
            watchdogAddr = 0x10007000L,
            uartAddr = 0x11005000L,
            cqdmaBase = 0x1020AC00L,
            efuseAddr = 0x10009000L,
            var1 = 0xAC,
            daMode = "LEGACY"
        ),
        0x6582 to MtkChipProfile(
            hwCode = 0x6582,
            name = "MT6582 / MT6574 / MT8382",
            description = "Quad-Core 3G Smartphone/Tablet SoC",
            watchdogAddr = 0x10007000L,
            uartAddr = 0x11002000L,
            gcpuBase = 0x1101B000L,
            efuseAddr = 0x10206000L,
            daMode = "LEGACY"
        ),
        0x6592 to MtkChipProfile(
            hwCode = 0x6592,
            name = "MT6592 / MT8392",
            description = "Octa-Core 3G Smartphone SoC",
            watchdogAddr = 0x10007000L,
            uartAddr = 0x11002000L,
            gcpuBase = 0x10210000L,
            cqdmaBase = 0x10212000L,
            efuseAddr = 0x10206000L,
            daMode = "LEGACY"
        ),
        0x6595 to MtkChipProfile(
            hwCode = 0x6595,
            name = "MT6595",
            description = "Octa-Core 4G LTE Smartphone SoC",
            watchdogAddr = 0x10007000L,
            uartAddr = 0x11002000L,
            efuseAddr = 0x10206000L,
            daMode = "LEGACY"
        ),

        // MT67xx 4G LTE 64-bit SoCs
        0x0321 to MtkChipProfile(
            hwCode = 0x0321,
            name = "MT6735 / MT8735A",
            description = "Quad-Core 64-bit LTE SoC",
            watchdogAddr = 0x10212000L,
            watchdogValue = 0x22000000L,
            gcpuBase = 0x10216000L,
            cqdmaBase = 0x10217C00L,
            efuseAddr = 0x11C50000L,
            var1 = 0x28,
            daMode = "LEGACY",
            is64Bit = true
        ),
        0x0335 to MtkChipProfile(
            hwCode = 0x0335,
            name = "MT6737M / MT6735G",
            description = "Quad-Core 64-bit LTE SoC",
            watchdogAddr = 0x10212000L,
            watchdogValue = 0x22000000L,
            gcpuBase = 0x10216000L,
            cqdmaBase = 0x10217C00L,
            efuseAddr = 0x10206000L,
            var1 = 0x28,
            daMode = "LEGACY",
            is64Bit = true
        ),
        0x0699 to MtkChipProfile(
            hwCode = 0x0699,
            name = "MT6739 / MT6731 / MT8765",
            description = "Quad-Core 64-bit Entry 4G SoC",
            watchdogAddr = 0x10007000L,
            gcpuBase = 0x10050000L,
            dxccBase = 0x10210000L,
            cqdmaBase = 0x10212000L,
            efuseAddr = 0x11C00000L,
            var1 = 0xB4,
            daMode = "XFLASH",
            is64Bit = true
        ),
        0x0601 to MtkChipProfile(
            hwCode = 0x0601,
            name = "MT6750 / MT6750T",
            description = "Octa-Core 4G LTE SoC",
            watchdogAddr = 0x10007000L,
            gcpuBase = 0x10210000L,
            cqdmaBase = 0x10212C00L,
            efuseAddr = 0x10206000L,
            daMode = "XFLASH",
            is64Bit = true
        ),
        0x6752 to MtkChipProfile(
            hwCode = 0x6752,
            name = "MT6752",
            description = "Octa-Core 64-bit LTE SoC",
            watchdogAddr = 0x10007000L,
            gcpuBase = 0x10210000L,
            cqdmaBase = 0x10212C00L,
            efuseAddr = 0x10206000L,
            var1 = 0x28,
            daMode = "LEGACY",
            is64Bit = true
        ),
        0x0337 to MtkChipProfile(
            hwCode = 0x0337,
            name = "MT6753",
            description = "Octa-Core CDMA/LTE SoC",
            watchdogAddr = 0x10212000L,
            watchdogValue = 0x22000000L,
            gcpuBase = 0x10216000L,
            cqdmaBase = 0x10217C00L,
            var1 = 0x28,
            daMode = "LEGACY",
            is64Bit = true
        ),
        0x0326 to MtkChipProfile(
            hwCode = 0x0326,
            name = "MT6755 (Helio P10 / P15 / P18)",
            description = "Octa-Core Helio P10 SoC",
            watchdogAddr = 0x10007000L,
            gcpuBase = 0x10210000L,
            cqdmaBase = 0x10212C00L,
            efuseAddr = 0x10206000L,
            daMode = "XFLASH",
            is64Bit = true
        ),
        0x0551 to MtkChipProfile(
            hwCode = 0x0551,
            name = "MT6757 (Helio P20 / P25)",
            description = "Octa-Core Helio P20 SoC",
            watchdogAddr = 0x10007000L,
            gcpuBase = 0x10210000L,
            cqdmaBase = 0x10212C00L,
            efuseAddr = 0x10206000L,
            daMode = "XFLASH",
            is64Bit = true
        ),
        0x0688 to MtkChipProfile(
            hwCode = 0x0688,
            name = "MT6758 (Helio P30)",
            description = "Octa-Core Helio P30 SoC",
            watchdogAddr = 0x10211000L,
            watchdogValue = 0x22000064L,
            gcpuBase = 0x10050000L,
            dxccBase = 0x11240000L,
            cqdmaBase = 0x10200000L,
            efuseAddr = 0x10450000L,
            daMode = "XFLASH",
            is64Bit = true
        ),
        0x0717 to MtkChipProfile(
            hwCode = 0x0717,
            name = "MT6761 (Helio A22 / A20 / A25)",
            description = "Quad-Core Helio A22 SoC",
            watchdogAddr = 0x10007000L,
            gcpuBase = 0x10050000L,
            dxccBase = 0x10210000L,
            cqdmaBase = 0x10212000L,
            efuseAddr = 0x11C50000L,
            var1 = 0x25,
            daMode = "XFLASH",
            is64Bit = true
        ),
        0x0766 to MtkChipProfile(
            hwCode = 0x0766,
            name = "MT6765 / MT6762 (Helio P35 / G25 / G35 / P22)",
            description = "Octa-Core Helio P35/G35 SoC",
            watchdogAddr = 0x10007000L,
            gcpuBase = 0x10050000L,
            dxccBase = 0x10210000L,
            cqdmaBase = 0x10212000L,
            efuseAddr = 0x11C50000L,
            var1 = 0x25,
            daMode = "XFLASH",
            is64Bit = true
        ),
        0x0707 to MtkChipProfile(
            hwCode = 0x0707,
            name = "MT6768 / MT6769 (Helio G85 / G80 / P65)",
            description = "Octa-Core Helio G85 Gaming SoC",
            watchdogAddr = 0x10007000L,
            gcpuBase = 0x10050000L,
            dxccBase = 0x10210000L,
            cqdmaBase = 0x10212000L,
            efuseAddr = 0x11CE0000L,
            var1 = 0x25,
            daMode = "XFLASH",
            is64Bit = true
        ),
        0x0788 to MtkChipProfile(
            hwCode = 0x0788,
            name = "MT6771 (Helio P60 / P70 / G80)",
            description = "Octa-Core Helio P60/P70 AI SoC",
            watchdogAddr = 0x10007000L,
            gcpuBase = 0x10050000L,
            dxccBase = 0x10210000L,
            cqdmaBase = 0x10212000L,
            efuseAddr = 0x11F10000L,
            daMode = "XFLASH",
            is64Bit = true
        ),
        0x0725 to MtkChipProfile(
            hwCode = 0x0725,
            name = "MT6779 (Helio P90 / P95)",
            description = "Octa-Core Helio P90 APU SoC",
            watchdogAddr = 0x10007000L,
            gcpuBase = 0x10050000L,
            dxccBase = 0x10210000L,
            cqdmaBase = 0x10212000L,
            efuseAddr = 0x11C10000L,
            daMode = "XFLASH",
            is64Bit = true
        ),
        0x1066 to MtkChipProfile(
            hwCode = 0x1066,
            name = "MT6781 (Helio G96)",
            description = "Octa-Core Helio G96 120Hz SoC",
            watchdogAddr = 0x10007000L,
            gcpuBase = 0x10050000L,
            dxccBase = 0x10210000L,
            efuseAddr = 0x11CB0000L,
            var1 = 0x73,
            daMode = "XFLASH",
            is64Bit = true
        ),
        0x0813 to MtkChipProfile(
            hwCode = 0x0813,
            name = "MT6785 (Helio G90T / G95)",
            description = "Octa-Core Helio G90T Gaming SoC",
            watchdogAddr = 0x10007000L,
            gcpuBase = 0x10050000L,
            dxccBase = 0x10210000L,
            cqdmaBase = 0x10212000L,
            efuseAddr = 0x11C10000L,
            daMode = "XFLASH",
            is64Bit = true
        ),
        0x1208 to MtkChipProfile(
            hwCode = 0x1208,
            name = "MT6789 / MT8781 (Helio G99 / 6nm)",
            description = "Octa-Core Helio G99 6nm SoC",
            watchdogAddr = 0x10007000L,
            dxccBase = 0x10210000L,
            efuseAddr = 0x11C10000L,
            daMode = "XML",
            is64Bit = true
        ),

        // Dimensity 5G SoCs
        0x0989 to MtkChipProfile(
            hwCode = 0x0989,
            name = "MT6833 (Dimensity 700 / 810 / 6020 / 6080)",
            description = "Dimensity 700 5G SoC",
            watchdogAddr = 0x10007000L,
            gcpuBase = 0x10050000L,
            dxccBase = 0x10210000L,
            cqdmaBase = 0x10212000L,
            efuseAddr = 0x11C10000L,
            var1 = 0x73,
            daMode = "XFLASH",
            is64Bit = true
        ),
        0x0996 to MtkChipProfile(
            hwCode = 0x0996,
            name = "MT6853 (Dimensity 720 / 800U)",
            description = "Dimensity 720 5G SoC",
            watchdogAddr = 0x10007000L,
            gcpuBase = 0x10050000L,
            dxccBase = 0x10210000L,
            cqdmaBase = 0x10212000L,
            efuseAddr = 0x11C10000L,
            daMode = "XFLASH",
            is64Bit = true
        ),
        0x0886 to MtkChipProfile(
            hwCode = 0x0886,
            name = "MT6873 (Dimensity 800 / 820 5G)",
            description = "Dimensity 800 5G SoC",
            watchdogAddr = 0x10007000L,
            gcpuBase = 0x10050000L,
            dxccBase = 0x10210000L,
            cqdmaBase = 0x10212000L,
            efuseAddr = 0x11C10000L,
            daMode = "XFLASH",
            is64Bit = true
        ),
        0x0959 to MtkChipProfile(
            hwCode = 0x0959,
            name = "MT6877 (Dimensity 900 / 920 / 1080 / 7050)",
            description = "Dimensity 900 / 1080 6nm 5G SoC",
            watchdogAddr = 0x10007000L,
            gcpuBase = 0x10050000L,
            dxccBase = 0x10210000L,
            cqdmaBase = 0x10212000L,
            efuseAddr = 0x11F10000L,
            daMode = "XFLASH",
            is64Bit = true
        ),
        0x0816 to MtkChipProfile(
            hwCode = 0x0816,
            name = "MT6885 / MT6889 (Dimensity 1000 / 1000L / 1000+)",
            description = "Dimensity 1000 Flagship 5G SoC",
            watchdogAddr = 0x10007000L,
            gcpuBase = 0x10050000L,
            dxccBase = 0x10210000L,
            cqdmaBase = 0x10212000L,
            efuseAddr = 0x11C10000L,
            daMode = "XFLASH",
            is64Bit = true
        ),
        0x0950 to MtkChipProfile(
            hwCode = 0x0950,
            name = "MT6891 / MT6893 (Dimensity 1100 / 1200 6nm)",
            description = "Dimensity 1200 Flagship 5G SoC",
            watchdogAddr = 0x10007000L,
            gcpuBase = 0x10050000L,
            dxccBase = 0x10210000L,
            cqdmaBase = 0x10212000L,
            efuseAddr = 0x11C10000L,
            daMode = "XFLASH",
            is64Bit = true
        ),
        0x0907 to MtkChipProfile(
            hwCode = 0x0907,
            name = "MT6983 (Dimensity 9000 / 9000+ 4nm)",
            description = "Dimensity 9000 4nm Ultra Flagship",
            watchdogAddr = 0x1C007000L,
            uartAddr = 0x11001000L,
            gcpuBase = 0x10050000L,
            dxccBase = 0x10210000L,
            cqdmaBase = 0x10212000L,
            efuseAddr = 0x11EE0000L,
            daMode = "XML",
            is64Bit = true
        ),
        0x1296 to MtkChipProfile(
            hwCode = 0x1296,
            name = "MT6985 (Dimensity 9200 / 9200+ 4nm)",
            description = "Dimensity 9200+ Flagship 5G SoC",
            watchdogAddr = 0x1C007000L,
            uartAddr = 0x1C011000L,
            dxccBase = 0x1C807000L,
            sejBase = 0x1C009000L,
            efuseAddr = 0x11E80000L,
            daMode = "XML",
            is64Bit = true
        ),
        0x1236 to MtkChipProfile(
            hwCode = 0x1236,
            name = "MT6989 (Dimensity 9300 / 9300+ All-Big-Core)",
            description = "Dimensity 9300+ Ultra Flagship",
            watchdogAddr = 0x1C00B000L,
            efuseAddr = 0x11F10000L,
            daMode = "XML",
            is64Bit = true
        ),
        0x1357 to MtkChipProfile(
            hwCode = 0x1357,
            name = "MT6991 (Dimensity 9400 3nm Ultra)",
            description = "Dimensity 9400 3nm Next-Gen Flagship",
            watchdogAddr = 0x1C010000L,
            uartAddr = 0x16000000L,
            daMode = "XML",
            is64Bit = true
        )
    )

    fun getProfile(hwCode: Int): MtkChipProfile {
        return CHIP_PROFILES[hwCode] ?: MtkChipProfile(
            hwCode = hwCode,
            name = "MediaTek SoC (0x%04X)".format(hwCode),
            description = "MediaTek Application Processor"
        )
    }

    fun getProfileByString(codeStr: String): MtkChipProfile? {
        val clean = codeStr.trim().removePrefix("0x").removePrefix("0X")
        val codeInt = clean.toIntOrNull(16) ?: return null
        return CHIP_PROFILES[codeInt]
    }
}
