package com.trioForce.voyage.aftersale

import jakarta.validation.constraints.NotBlank

data class CreateAfterSaleRequest(
    @field:NotBlank val orderNo: String,
    @field:NotBlank val content: String
)

data class UpdateAfterSaleStatusRequest(
    @field:NotBlank val status: String
)

data class AfterSaleView(
    val id: Long,
    val orderNo: String,
    val status: String,
    val content: String
)
