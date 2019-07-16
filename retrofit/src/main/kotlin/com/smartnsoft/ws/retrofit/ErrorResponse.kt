package com.smartnsoft.ws.retrofit

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
    @JsonProperty("status_code") var statusCode: Int,
    @JsonProperty("message") var message: String
) : Serializable