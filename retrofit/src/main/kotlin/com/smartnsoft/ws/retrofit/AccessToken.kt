package com.smartnsoft.ws.retrofit

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable

/**
 * The class description here.
 *
 * @author David Fournier
 * @since 2019.06.21
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class AccessToken
(
    @JsonProperty("access_token") var accessToken : String,
    @JsonProperty("refresh_token") var refreshToken: String,
    @JsonProperty("token_type") var tokenType: String,
    @JsonProperty("expires_in") var expiresIn: Long
) : Serializable