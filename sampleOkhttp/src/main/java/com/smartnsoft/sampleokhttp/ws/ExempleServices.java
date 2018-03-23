package com.smartnsoft.sampleokhttp.ws;

import android.util.Xml.Encoding;

import com.smartnsoft.ws.okhttp.OkHttpClientWebServiceCaller;
import com.smartnsoft.ws.okhttp.ReuseOkHttpClient;

/**
 * @author Ludovic Roland
 * @since 2018.03.23
 */
@ReuseOkHttpClient
public final class ExempleServices
    extends OkHttpClientWebServiceCaller
{

  private static volatile ExempleServices instance;

  // We accept the "out-of-order writes" case
  public static ExempleServices getInstance()
  {
    if (instance == null)
    {
      synchronized (ExempleServices.class)
      {
        if (instance == null)
        {
          instance = new ExempleServices();
        }
      }
    }

    return instance;
  }

  private ExempleServices()
  {
    super(5_000, 5_000, true);
  }

  @Override
  protected String getUrlEncoding()
  {
    return Encoding.UTF_8.toString();
  }

  @Override
  protected String getContentEncoding()
  {
    return Encoding.ISO_8859_1.toString();
  }

}
