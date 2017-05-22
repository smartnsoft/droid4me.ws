package com.smartnsoft.ws.httpclient;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An empty interface which states that the underlying client instance should be reused for all HTTP requests, instead of creating a new
 * one each time.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface ReuseHttpClient
{

}