package com.smartnsoft.ws.retrofit.bo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * @author Anthony Msihid
 * @since 2019.07.15
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class ErrorResponse
(
    @field:JsonProperty("code") val statusCode: Int,
    @field:JsonProperty("message") val message: String
)