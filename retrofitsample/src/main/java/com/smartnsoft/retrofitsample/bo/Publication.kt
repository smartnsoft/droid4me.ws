package com.smartnsoft.retrofitsample.bo

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable

/**
 * @author Anthony Msihid
 * @since 2019.06.18
 */

@JsonIgnoreProperties(ignoreUnknown = true)
data class Publication(@JsonProperty("type") val type: Type = Type.unknown,
                       @JsonProperty("id") val identifier: Int? = 0,
                       @JsonProperty("pictureUrl") val pictureUrl: String? = "",
                       @JsonProperty("title") val title: String? = "",
                       @JsonProperty("tags") val tags: List<Tag>?,
                       @JsonProperty("publicationDate") val publicationDate: Long? = 0,
                       @JsonProperty("content") val content: String? = "",
                       @JsonProperty("passwordRequired") val passwordRequired: Boolean? = false,
                       @JsonProperty("URL") val url: String? = "",
                       @JsonProperty("files") var pdfList: List<Pdf>?,
                       @JsonProperty("internalPosition") @JsonIgnore var internalPosition: Int = 0)
  : Serializable
{

  companion object
  {

    private const val serialVersionUID = 1L
  }

  enum class Type
  {

    article,
    poll,
    unknown
  }

  var contentWithoutAccent: String = ""
  var titleWithoutAccent: String = ""


}