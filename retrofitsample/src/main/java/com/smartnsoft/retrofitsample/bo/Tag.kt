package com.smartnsoft.retrofitsample.bo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable

/**
 * @author Anthony Msihid
 * @since 2019.06.18
 */

@JsonIgnoreProperties(ignoreUnknown = true)
data class Tag(@JsonProperty("color") val color: String?, @JsonProperty("title") val name: String?)
  : Serializable
{

  companion object
  {
    private const val serialVersionUID = 1L
  }

}