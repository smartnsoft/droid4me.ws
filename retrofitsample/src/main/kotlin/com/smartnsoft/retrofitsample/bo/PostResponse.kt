package com.smartnsoft.retrofitsample.bo

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * @author David Fournier
 * @since 2018.03.29
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class PostResponse @JsonCreator
constructor(@param:JsonProperty("form") val form: Form)
{

  class Form @JsonCreator
  constructor(@param:JsonProperty("ip") val ip: String)
}
