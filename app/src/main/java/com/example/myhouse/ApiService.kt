package com.example.myhouse

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Path


data class RegisterRequest(val correo: String, val contrasena: String)
data class RegisterResponse(
    val message: String,
    val id: Int
)
data class Device(
    val ID_Dispositivo: Int,
    val NombreDispositivo: String,
    val NombreTipo: String
)
data class CameraResponse(
    val guardar_fotografia: String,
    val fecha: String,
    val hora: String,
    val NombreDispositivo: String
)

data class UserInfo(
    val correo: String,
    val contrasena: String,
    val cantidad_dispositivos: Int
)
data class AddDeviceRequest(val userId: Int, val nombreDispositivo: String, val contrasenaDispositivo: String)
data class AddDeviceResponse(val message: String)

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

}