package com.musfit.data.remote.food

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path

interface OpenFoodFactsApi {
    @GET("api/v2/product/{barcode}.json")
    suspend fun getProduct(
        @Path("barcode") barcode: String,
    ): ResponseBody
}
