package com.smartnsoft.okhttpclientwebservicecaller;

import java.util.concurrent.TimeUnit;

import com.smartnsoft.droid4me.ext.json.jackson.JacksonParser;
import com.smartnsoft.droid4me.ext.json.jackson.ObjectMapperComputer;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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

  protected JacksonOkHttpClientWebServiceCaller(int readTimeOutInMilliseconds, int connectTimeOutInMilliseconds,
      boolean acceptGzip)
  {
    super(readTimeOutInMilliseconds, connectTimeOutInMilliseconds, acceptGzip);
    this.jacksonParser = new JacksonParser(this);
  }

  @Override
  public ObjectMapper computeObjectMapper()
  {
    final ObjectMapper theObjectMapper = new ObjectMapper();
    // We indicate to the parser not to fail in case of unknown properties, for backward compatibility reasons
    // See http://stackoverflow.com/questions/6300311/java-jackson-org-codehaus-jackson-map-exc-unrecognizedpropertyexception
    theObjectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    theObjectMapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, true);
    return theObjectMapper;
  }

  @Override
  protected OkHttpClient.Builder computeHttpClient()
  {
    final OkHttpClient.Builder builder = super.computeHttpClient();
    builder.connectTimeout(getConnectTimeOut(), TimeUnit.MILLISECONDS);
    builder.readTimeout(getReadTimeOut(), TimeUnit.MILLISECONDS);

    return builder;
  }

}
