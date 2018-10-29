/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.olingo.commons.core.edm.primitivetype;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeException;

public final class EdmTimeOfDay extends SingletonPrimitiveType {

  private static final Pattern PATTERN = Pattern.compile(
      "(\\p{Digit}{2}):(\\p{Digit}{2})(?::(\\p{Digit}{2})(\\.(\\p{Digit}{0,}?)0*)?)?");

  private static final EdmTimeOfDay INSTANCE = new EdmTimeOfDay();

  public static EdmTimeOfDay getInstance() {
    return INSTANCE;
  }

  @Override
  public Class<?> getDefaultType() {
    return Calendar.class;
  }

  @Override
  protected <T> T internalValueOfString(final String value,
      final Boolean isNullable, final Integer maxLength, final Integer precision,
      final Integer scale, final Boolean isUnicode, final Class<T> returnType) throws EdmPrimitiveTypeException {

    final Matcher matcher = PATTERN.matcher(value);
    if (!matcher.matches()) {
      throw new EdmPrimitiveTypeException("The literal '" + value + "' has illegal content.");
    }

    final Calendar dateTimeValue = Calendar.getInstance();
    dateTimeValue.clear();
    dateTimeValue.set(Calendar.HOUR_OF_DAY, Byte.parseByte(matcher.group(1)));
    dateTimeValue.set(Calendar.MINUTE, Byte.parseByte(matcher.group(2)));
    dateTimeValue.set(Calendar.SECOND, matcher.group(3) == null ? 0 : Byte.parseByte(matcher.group(3)));

    int nanoSeconds = 0;
    if (matcher.group(4) != null) {
      if (matcher.group(4).length() == 1 || matcher.group(4).length() > 13) {
        throw new EdmPrimitiveTypeException("The literal '" + value + "' has illegal content.");
      }
      final String decimals = matcher.group(5);
      if (decimals.length() > (precision == null ? 0 : precision)) {
        throw new EdmPrimitiveTypeException("The literal '" + value + "' does not match the facets' constraints.");
      }
      if (returnType.isAssignableFrom(Timestamp.class)) {
        if (decimals.length() <= 9) {
          nanoSeconds = Integer.parseInt(decimals + "000000000".substring(decimals.length()));
        } else {
          throw new EdmPrimitiveTypeException("The literal '" + value
              + "' cannot be converted to value type " + returnType + ".");
        }
      } else {
        if (decimals.length() <= 3) {
          final String milliSeconds = decimals + "000".substring(decimals.length());
          dateTimeValue.set(Calendar.MILLISECOND, Short.parseShort(milliSeconds));
        } else {
          throw new EdmPrimitiveTypeException("The literal '" + value
              + "' cannot be converted to value type " + returnType + ".");
        }
      }
    }

    try {
      return EdmDateTimeOffset.convertDateTime(dateTimeValue, nanoSeconds, returnType);
    } catch (final IllegalArgumentException e) {
      throw new EdmPrimitiveTypeException("The literal '" + value + "' has illegal content.", e);
    } catch (final ClassCastException e) {
      throw new EdmPrimitiveTypeException("The value type " + returnType + " is not supported.", e);
    }
  }

  @Override
  protected <T> String internalValueToString(final T value,
      final Boolean isNullable, final Integer maxLength, final Integer precision,
      final Integer scale, final Boolean isUnicode) throws EdmPrimitiveTypeException {

    final Calendar dateTimeValue = EdmDateTimeOffset.createDateTime(value, true);

    StringBuilder result = new StringBuilder();
    EdmDateTimeOffset.appendTwoDigits(result, dateTimeValue.get(Calendar.HOUR_OF_DAY));
    result.append(':');
    EdmDateTimeOffset.appendTwoDigits(result, dateTimeValue.get(Calendar.MINUTE));
    result.append(':');
    EdmDateTimeOffset.appendTwoDigits(result, dateTimeValue.get(Calendar.SECOND));

    final int fractionalSecs = value instanceof Timestamp ?
        ((Timestamp) value).getNanos() :
        dateTimeValue.get(Calendar.MILLISECOND);
    try {
      EdmDateTimeOffset.appendFractionalSeconds(result, fractionalSecs, value instanceof Timestamp, precision);
    } catch (final IllegalArgumentException e) {
      throw new EdmPrimitiveTypeException("The value '" + value + "' does not match the facets' constraints.", e);
    }

    return result.toString();
  }
}
