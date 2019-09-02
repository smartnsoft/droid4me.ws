package com.smartnsoft.ws.retrofit.bo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * @author Anthony Msihid
 * @since 2019.08.23
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class LoginBody
(
    @field:JsonProperty("username") val username: String,
    @field:JsonProperty("password") val password: String
)