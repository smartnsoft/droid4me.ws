package com.smartnsoft.ws.retrofit.bo

/**
 * @author Anthony Msihid
 * @since 2019.07.16
 */

data class ResponseWithError<SuccessClass, ErrorClass>
(
    val successResponse: SuccessClass? = null,
    val errorResponse: ErrorClass? = null
)