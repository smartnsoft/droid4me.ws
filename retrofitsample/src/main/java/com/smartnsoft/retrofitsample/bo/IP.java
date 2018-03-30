package com.smartnsoft.retrofitsample.bo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author David Fournier
 * @since 2017.11.28
 */

public class IP
{

  public final String origin;

  @JsonCreator
  public IP(@JsonProperty("origin") String origin)
  {
    this.origin = origin;
  }
}
