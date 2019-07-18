package com.smartnsoft.ws.retrofit.bo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable

/**
 * @author Anthony Msihid
 * @since 2019.07.15
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class ErrorResponse
(
    @JsonProperty("status_code") val statusCode: Int,
    @JsonProperty("message") val message: String
) : Serializable