package com.smartnsoft.okhttpclientwebservicecaller;

import java.util.concurrent.TimeUnit;

import com.smartnsoft.droid4me.ext.json.jackson.JacksonParser;
import com.smartnsoft.droid4me.ext.json.jackson.ObjectMapperComputer;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;

/**
 * @author Ludovic Roland
 * @since 2017.05.15
 */
public abstract class JacksonOkHttpClientWebServiceCaller
    extends OkHttpClientWebServiceCaller
    implements ObjectMapperComputer
{

  public final JacksonParser jacksonParser;

  private final int readTimeOutInMilliseconds;

  private final int connectTimeOutInMilliseconds;

  protected JacksonOkHttpClientWebServiceCaller(int readTimeOutInMilliseconds, int connectTimeOutInMilliseconds)
  {
    this.jacksonParser = new JacksonParser(this);
    this.readTimeOutInMilliseconds = readTimeOutInMilliseconds;
    this.connectTimeOutInMilliseconds = connectTimeOutInMilliseconds;
  }

  @Override
  public ObjectMapper computeObjectMapper()
  {
    return null;
  }

  @Override
  protected OkHttpClient.Builder computeHttpClient()
  {
    final OkHttpClient.Builder builder = super.computeHttpClient();
    builder.connectTimeout(connectTimeOutInMilliseconds, TimeUnit.MILLISECONDS);
    builder.readTimeout(readTimeOutInMilliseconds, TimeUnit.MILLISECONDS);

    return builder;
  }

}
