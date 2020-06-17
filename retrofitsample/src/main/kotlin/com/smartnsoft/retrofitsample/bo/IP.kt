package com.smartnsoft.retrofitsample.bo

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * @author David Fournier
 * @since 2017.11.28
 */

data class IP @JsonCreator
constructor(@param:JsonProperty("origin") val origin: String)
