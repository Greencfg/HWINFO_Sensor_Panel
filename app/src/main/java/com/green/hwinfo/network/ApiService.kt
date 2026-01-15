package com.green.hwinfo.network

import com.green.hwinfo.model.SensorData
import retrofit2.http.GET
import retrofit2.http.Url

interface ApiService {
    @GET
    suspend fun getSensorData(@Url url: String): List<SensorData>
}
