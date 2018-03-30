package com.smartnsoft.retrofitsample.bo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author David Fournier
 * @since 2018.03.29
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PostResponse
{

  static public class Form
  {

    public final String ip;

    @JsonCreator
    public Form(@JsonProperty("ip") String ip)
    {
      this.ip = ip;
    }
  }

  public final Form form;

  @JsonCreator
  public PostResponse(@JsonProperty("form") Form form)
  {
    this.form = form;
  }
}
