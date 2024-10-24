package com.example.myhouse

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.Response
import retrofit2.http.DELETE
import okhttp3.RequestBody
import retrofit2.http.Headers


import retrofit2.http.HTTP


data class RegisterRequest(val correo: String, val contrasena: String)
data class RegisterResponse(val message: String, val id: Int)
data class Device(val ID_Dispositivo: Int, val NombreDispositivo: String, val NombreTipo: String)
data class CameraResponse(val ID_Dispositivo: Int, val guardar_fotografia: String, val fecha: String, val hora: String, val NombreDispositivo: String)
data class UserInfo(val correo: String, val contrasena: String, val cantidad_dispositivos: Int)
data class AddDeviceRequest(val userId: Int, val nombreDispositivo: String, val contrasenaDispositivo: String)
data class AddDeviceResponse(val message: String)
data class AirQualityResponse(val indice_calidad_aire: Int, val fecha: String, val hora: String, val NombreDispositivo: String)
data class ErrorResponse(val message: String)
data class TemperatureResponse(val temperatura: Double, val fecha: String, val hora: String, val NombreDispositivo: String)
data class AirQualityAverageResponse(val fecha: String, val hora: Int, val promedio_calidad_aire: Double)
data class TemperatureAverageResponse(val fecha: String, val hora: Int, val promedio_temperatura: Double)
data class DeleteRequest(val ID_Dispositivo: Int, val ID_USER: Int)
data class DeleteResponse(
    val message: String
)



data class NewTemperatureResponse(val temperatura: Double)

data class NewAirQualityResponse(val indice_calidad_aire: Int)

data class ImageRequest(val userId: Int, val deviceId: Int)
data class ImageResponse(val image: String)
data class CameraStatusResponse(val estado: String)
interface ApiService {
    @POST("/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @POST("/register")
    fun register(@Body request: RegisterRequest): Call<RegisterResponse>

    @GET("dispositivos/{id}")
    suspend fun getDevices(@Path("id") userId: Int): List<Device>

    @GET("recursocamara/{id}")
    suspend fun getCameraResource(@Path("id") deviceId: Int): CameraResponse

    @GET("usuario/{id}")
    suspend fun getUserInfo(@Path("id") id: Int): UserInfo

    @POST("/agregar-dispositivo")
    fun addDevice(@Body request: AddDeviceRequest): Call<AddDeviceResponse>

    @GET("calidad-aire/{userId}")
    fun getAirQuality(@Path("userId") userId: Int): Call<AirQualityResponse>

    @GET("temperatura/{id}")
    fun getTemperature(@Path("id") userId: Int): Call<TemperatureResponse>

    @GET("promedio-calidad-aire/{userId}")
    fun getAirQualityAverage(@Path("userId") userId: Int): Call<List<AirQualityAverageResponse>>

    @GET("promedio-temperatura/{id}")
    fun getTemperatureAverage(@Path("id") userId: Int): Call<List<TemperatureAverageResponse>>

    @HTTP(method = "DELETE", path = "/eliminar-recurso", hasBody = true)
    fun deleteDevice(@Body request: RequestBody): Call<DeleteResponse>
    @POST("ver-imagen")
    fun getImage(@Body request: ImageRequest): Call<ImageResponse>

    @GET("estado-camara/{id}")
    fun getCameraStatus(@Path("id") deviceId: Int): Call<CameraStatusResponse>

    @GET("ver-calidad-aire/{id}")
    fun fetchAirQuality(@Path("id") id: Int): Call<NewAirQualityResponse>

    @GET("ver-temperatura/{id}")
    fun fetchTemperature(@Path("id") id: Int): Call<NewTemperatureResponse>

}