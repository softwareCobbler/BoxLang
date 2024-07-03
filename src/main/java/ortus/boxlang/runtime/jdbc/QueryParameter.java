/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.runtime.jdbc;

import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.QueryColumnType;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.util.ListUtil;

public class QueryParameter {

	private final Object			value;
	private final QueryColumnType	type;
	private final Integer			maxLength;
	private final Integer			scale;

	/**
	 * Construct a new QueryParameter from a given IStruct of parameters.
	 * <p>
	 * The IStruct may contain the following properties:
	 * <ul>
	 * <li>`value` - The value of the parameter.</li>
	 * <li>`sqltype` - The SQL type of the parameter. Defaults to `VARCHAR`.</li>
	 * <li>`nulls` - Whether the parameter can be null. Defaults to `false`.</li>
	 * <li>`list` - Whether the parameter is a list. Defaults to `false`.</li>
	 * <li>`separator` - The separator for the list. Defaults to `,`.</li>
	 * <li>`maxLength` - The maximum length of the parameter. Defaults to `null`.</li>
	 * <li>`scale` - The scale of the parameter, used only on `double` and `decimal` types. Defaults to `null`.</li>
	 */
	private QueryParameter( IStruct param ) {
		String sqltype = ( String ) param.getOrDefault( Key.sqltype, "VARCHAR" );
		if ( ( Boolean ) param.getOrDefault( Key.nulls, false ) ) {
			sqltype = "NULL";
		}

		Object v = param.get( Key.value );
		if ( ( Boolean ) param.getOrDefault( Key.list, false ) ) {
			v		= ListUtil.asList( ( String ) v, ( String ) param.getOrDefault( Key.separator, "," ) );
			sqltype	= "ARRAY";
		}

		this.value		= v;
		this.type		= QueryColumnType.fromString( sqltype.replaceAll( "(?i)CF_SQL_", "" ) );
		this.maxLength	= param.getAsInteger( Key.maxLength );
		this.scale		= param.getAsInteger( Key.scale );
	}

	/**
	 * Construct a new QueryParameter from a given value.
	 * <p>
	 * If the value is an IStruct, it will be used as the construction arguments to {@link QueryParameter#QueryParameter(IStruct)}. Otherwise, the QueryParameter will be constructed with the value as the `value` property of the IStruct, and no sqltype, null, list, or maxLength/scale properties.
	 */
	public static QueryParameter fromAny( Object value ) {
		CastAttempt<IStruct> castAsStruct = StructCaster.attempt( value );
		if ( castAsStruct.wasSuccessful() ) {
			return new QueryParameter( castAsStruct.getOrFail() );
		}
		return new QueryParameter( Struct.of( "value", value ) );
	}

	public Object getValue() {
		return value;
	}

	/**
	 * Retrieve the SQL type as an ordinal from {@link java.sql.Types}.
	 * <p>
	 * For example, {@link java.sql.Types#VARCHAR} is 12.
	 */
	public int getSqlTypeAsInt() {
		return this.type.sqlType;
	}

	/**
	 * Retrieve the `scale` or `maxLength` of the parameter.
	 * <p>
	 * For {@link QueryColumnType#DOUBLE} and {@link QueryColumnType#DECIMAL}, this is the `scale`. For all other types, this is the `maxLength` property.
	 */
	public Integer getScaleOrLength() {
		return switch ( type ) {
			case DOUBLE, DECIMAL -> this.scale;
			default -> this.maxLength;
		};
	}

}
