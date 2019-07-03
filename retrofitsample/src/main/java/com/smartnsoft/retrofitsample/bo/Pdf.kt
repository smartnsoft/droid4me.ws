package com.smartnsoft.retrofitsample.bo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable

/**
 * @author Anthony Msihid
 * @since 2019.06.18
 */

@JsonIgnoreProperties(ignoreUnknown = true)
data class Pdf(@JsonProperty("url") val url: String?, @JsonProperty("name") val name: String?)
  : Serializable