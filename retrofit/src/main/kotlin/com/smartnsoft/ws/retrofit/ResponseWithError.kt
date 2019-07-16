package com.smartnsoft.ws.retrofit

/**
 * @author Anthony Msihid
 * @since 2019.07.16
 */
class ResponseWithError<SuccessClass, ErrorClass>(var successResponse: SuccessClass? = null, var errorResponse: ErrorClass? = null)