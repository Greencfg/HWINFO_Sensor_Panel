package com.green.hwinfo.model

import com.google.gson.annotations.SerializedName

data class SensorData(
    @SerializedName("Id") val id: Int,
    @SerializedName("Label") val label: String?,
    @SerializedName("Value") val value: String?,
    @SerializedName("Sensor") val sensor: String?
)
