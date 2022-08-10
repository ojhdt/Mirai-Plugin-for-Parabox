package com.ojhdtapp.miraipluginforparabox.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
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
    val simInfo: ByteArray,
    val osType: ByteArray,
    val macAddress: ByteArray,
    val wifiBSSID: ByteArray,
    val wifiSSID: ByteArray,
    val imsiMd5: ByteArray,
    val imei: String,
    val apn: ByteArray,
    @PrimaryKey var id: Long = 0
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
        DeviceInfo.Version(),
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

fun DeviceInfo.toDeviceInfoEntity() = DeviceInfoEntity(
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
    simInfo,
    osType,
    macAddress,
    wifiBSSID,
    wifiSSID,
    imsiMd5,
    imei,
    apn
)