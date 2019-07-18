package logger

/**
 * @author Anthony Msihid
 * @since 2019.07.18
 */
// The MIT License (MIT)
//
// Copyright (c) 2017 Smart&Soft
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.

/**
 * Just in order to have various loggers.
 *
 * @author Édouard Mercier
 * @since 2007.12.23
 */
interface Logger
{

  val isDebugEnabled: Boolean

  val isInfoEnabled: Boolean

  val isWarnEnabled: Boolean

  val isErrorEnabled: Boolean

  val isFatalEnabled: Boolean

  fun debug(message: String)

  fun info(message: String)

  fun warn(message: String)

  fun warn(message: String, throwable: Throwable)

  fun warn(message: StringBuffer, throwable: Throwable)

  fun error(message: String)

  fun error(message: String, throwable: Throwable)

  fun error(message: StringBuffer, throwable: Throwable)

  fun fatal(message: String)

  fun fatal(message: String, throwable: Throwable)

}
