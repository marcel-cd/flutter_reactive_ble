package com.signify.hue.flutterreactiveble.channelhandlers

import android.os.ParcelUuid
import com.signify.hue.flutterreactiveble.ProtobufModel as pb
import com.signify.hue.flutterreactiveble.converters.ProtobufMessageConverter
import com.signify.hue.flutterreactiveble.converters.UuidConverter
import com.signify.hue.flutterreactiveble.model.ScanMode
import com.signify.hue.flutterreactiveble.model.createScanMode
import io.flutter.plugin.common.EventChannel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable

class ScanDevicesHandler(private val bleClient: com.signify.hue.flutterreactiveble.ble.BleClient) : EventChannel.StreamHandler {

    private var scanDevicesSink: EventChannel.EventSink? = null
    private lateinit var scanForDevicesDisposable: Disposable
    private val converter = ProtobufMessageConverter()

    companion object {
        private var scanParameters: ScanParameters? = null
    }

    override fun onListen(objectSink: Any?, eventSink: EventChannel.EventSink?) {
        eventSink?.let {
            scanDevicesSink = eventSink
            startDeviceScan()
        }
    }

    override fun onCancel(objectSink: Any?) {
        stopDeviceScan()
        scanDevicesSink = null
    }

    private fun startDeviceScan() {
        scanParameters?.let { params ->
            scanForDevicesDisposable = bleClient.scanForDevices(params.filterService, params.filterCompany, params.mode, params.locationServiceIsMandatory)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            { scanResult ->
                                handleDeviceScanResult(converter.convertScanInfo(scanResult))
                            },
                            { throwable ->
                                handleDeviceScanResult(converter.convertScanErrorInfo(throwable.message))
                            }
                    )
        }
                ?: handleDeviceScanResult(converter.convertScanErrorInfo("Scanning parameters are not set"))
    }

    fun stopDeviceScan() {
        if (this::scanForDevicesDisposable.isInitialized) scanForDevicesDisposable.let {
            if (!it.isDisposed) {
                it.dispose()
                scanParameters = null
            }
        }
    }

    fun prepareScan(scanMessage: pb.ScanForDevicesRequest) {
        stopDeviceScan()
        val filterService = scanMessage.serviceUuidsList
                .map { ParcelUuid(UuidConverter().uuidFromByteArray(it.data.toByteArray())) }
        val filterCompany = scanMessage.companyIdsList
            .map { it }
        val scanMode = createScanMode(scanMessage.scanMode)
        scanParameters = ScanParameters(filterService, filterCompany, scanMode, scanMessage.requireLocationServicesEnabled)
    }

    private fun handleDeviceScanResult(discoveryMessage: pb.DeviceScanInfo) {
        scanDevicesSink?.success(discoveryMessage.toByteArray())
    }
}

private data class ScanParameters(val filterService: List<ParcelUuid>, val filterCompany: List<Int>, val mode: ScanMode, val locationServiceIsMandatory: Boolean)
