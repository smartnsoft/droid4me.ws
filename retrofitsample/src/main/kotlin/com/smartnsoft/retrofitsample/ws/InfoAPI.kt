package com.smartnsoft.retrofitsample.ws

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import retrofit2.Call
import retrofit2.http.*
import java.io.Serializable

/**
 * @author David Fournier
 * @since 2019.07.01
 */
interface InfoAPI
{

  companion object Constants
  {

    const val apiToken: String = "123456789abcdef"

  }

  @GET("info")
  fun getInfo(): Call<InfoContainer>

}

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class InfoContainer
(
    @JsonProperty("info") var name: Info
) : Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Info
(
    @JsonProperty("name") var name: String,
    @JsonProperty("age") var age: String,
    @JsonProperty("gender") var genre: String
) : Serializable