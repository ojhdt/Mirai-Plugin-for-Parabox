package com.ojhdtapp.miraipluginforparabox.data.local.entity

import androidx.room.Entity
import net.mamoe.mirai.utils.DeviceInfo

@Entity
class DeviceInfoEntity(
    val display: ByteArray,
    val product: ByteArray,
    val device: ByteArray,
    val board: ByteArray,
    val brand: ByteArray,
    val model: ByteArray,
    val bootloader: ByteArray,
    val fingerprint: ByteArray,
    val bootId: ByteArray,
    val procVersion: ByteArray,
    val baseBand: ByteArray,
    val version: DeviceInfo.Version,
    val simInfo: ByteArray,
    val osType: ByteArray,
    val macAddress: ByteArray,
    val wifiBSSID: ByteArray,
    val wifiSSID: ByteArray,
    val imsiMd5: ByteArray,
    val imei: String,
    val apn: ByteArray
) {
    fun toMiraiDeviceInfo(): DeviceInfo = DeviceInfo(
        display,
        product,
        device,
        board,
        brand,
        model,
        bootloader,
        fingerprint,
        bootId,
        procVersion,
        baseBand,
        version,
        simInfo,
        osType,
        macAddress,
        wifiBSSID,
        wifiSSID,
        imsiMd5,
        imei,
        apn
    )
}