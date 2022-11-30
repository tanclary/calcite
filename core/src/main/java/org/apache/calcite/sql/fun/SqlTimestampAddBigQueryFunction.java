/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.calcite.sql.fun;

import org.apache.calcite.avatica.util.TimeUnit;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlCallBinding;
import org.apache.calcite.sql.SqlFunction;
import org.apache.calcite.sql.SqlFunctionCategory;
import org.apache.calcite.sql.SqlIntervalLiteral;
import org.apache.calcite.sql.SqlIntervalQualifier;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlNumericLiteral;
import org.apache.calcite.sql.type.OperandTypes;
import org.apache.calcite.sql.type.SqlReturnTypeInference;
import org.apache.calcite.sql.type.SqlTypeFamily;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.sql.validate.SqlValidator;
import org.apache.calcite.sql.validate.SqlValidatorScope;
import org.apache.calcite.sql.SqlBasicCall;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.apache.calcite.util.Util.first;

/**
 * The <code>TIMESTAMPADD</code> function, which adds an interval to a
 * datetime (TIMESTAMP, TIME or DATE).
 *
 * <p>The SQL syntax is
 *
 * <blockquote>
 * <code>TIMESTAMPADD(<i>timestamp interval</i>, <i>quantity</i>,
 * <i>datetime</i>)</code>
 * </blockquote>
 *
 * <p>The interval time unit can one of the following literals:<ul>
 * <li>NANOSECOND (and synonym SQL_TSI_FRAC_SECOND)
 * <li>MICROSECOND (and synonyms SQL_TSI_MICROSECOND, FRAC_SECOND)
 * <li>SECOND (and synonym SQL_TSI_SECOND)
 * <li>MINUTE (and synonym  SQL_TSI_MINUTE)
 * <li>HOUR (and synonym  SQL_TSI_HOUR)
 * <li>DAY (and synonym SQL_TSI_DAY)
 * <li>WEEK (and synonym  SQL_TSI_WEEK)
 * <li>MONTH (and synonym SQL_TSI_MONTH)
 * <li>QUARTER (and synonym SQL_TSI_QUARTER)
 * <li>YEAR (and synonym  SQL_TSI_YEAR)
 * </ul>
 *
 * <p>Returns modified datetime.
 */
public class SqlTimestampAddBigQueryFunction extends SqlFunction {

  private static final int MILLISECOND_PRECISION = 3;
  private static final int MICROSECOND_PRECISION = 6;

  private static final SqlReturnTypeInference RETURN_TYPE_INFERENCE =
      opBinding -> {
        SqlCallBinding callBinding = (SqlCallBinding) opBinding;
        SqlCall call = callBinding.getCall();
        SqlBasicCall operandCall = call.operand(1);

        SqlIntervalQualifier qualifier = operandCall.operand(1);
        TimeUnit unit = qualifier.timeUnitRange.startUnit;
        return deduceType(opBinding.getTypeFactory(), unit, opBinding.getOperandType(1),
          opBinding.getOperandType(0));
      };


  public static RelDataType deduceType(RelDataTypeFactory typeFactory,
      @Nullable TimeUnit timeUnit, RelDataType operandType1,
      RelDataType timestamp) {
    final RelDataType type;
    TimeUnit timeUnit2 = first(timeUnit, TimeUnit.EPOCH);
    switch (timeUnit2) {
    case HOUR:
    case MINUTE:
    case SECOND:
    case MILLISECOND:
    case MICROSECOND:
      switch (timeUnit2) {
      case MILLISECOND:
        type = typeFactory.createSqlType(SqlTypeName.TIMESTAMP,
            MILLISECOND_PRECISION);
        break;
      case MICROSECOND:
        type = typeFactory.createSqlType(SqlTypeName.TIMESTAMP,
            MICROSECOND_PRECISION);
        break;
      default:
          type = typeFactory.createSqlType(SqlTypeName.TIMESTAMP);
      }
      break;
    default:
    case EPOCH:
      type = timestamp;
    }
    return typeFactory.createTypeWithNullability(type,
        operandType1.isNullable()
        || timestamp.isNullable());
  }

  @Override public void validateCall(SqlCall call, SqlValidator validator,
      SqlValidatorScope scope, SqlValidatorScope operandScope) {
    super.validateCall(call, validator, scope, operandScope);

    SqlBasicCall opCall = call.operand(1);
    SqlIntervalQualifier qualifier = opCall.operand(1);
    validator.validateTimeFrame(qualifier);
  }

  /** Creates a SqlTimestampAddBigQueryFunction. */
  SqlTimestampAddBigQueryFunction(String name) {
    super(name, SqlKind.TIMESTAMP_ADD, RETURN_TYPE_INFERENCE, null,
        OperandTypes.TIMESTAMP_INTERVAL, SqlFunctionCategory.TIMEDATE);
  }
}
