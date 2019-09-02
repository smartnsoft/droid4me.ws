package com.smartnsoft.ws.retrofit.bo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * @author David Fournier
 * @since 2019.06.21
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class AccessToken
(
    @field:JsonProperty("access_token") val accessToken: String,
    @field:JsonProperty("refresh_token") val refreshToken: String,
    @field:JsonProperty("token_type") val tokenType: String,
    @field:JsonProperty("expires_in") val expiresIn: Long
)