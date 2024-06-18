
/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ortus.boxlang.runtime.bifs.global.i18n;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.util.LocalizationUtil;

public class IsCurrencyTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@AfterAll
	public static void teardown() {

	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "It tests the BIF IsCurrency" )
	@Test
	public void testBif() {
		assertTrue( ( Boolean ) instance.executeStatement( "IsCurrency( '$1.50', 'en_US' )" ) );
		assertTrue( ( Boolean ) instance.executeStatement( "IsCurrency( '1.50', 'en_US' )" ) );
		assertTrue( ( Boolean ) instance.executeStatement( "IsCurrency( '£1.50', 'English (UK)' )" ) );
		assertFalse( ( Boolean ) instance.executeStatement( "IsCurrency( '$1.50', 'de_AT' )" ) );
		assertFalse( ( Boolean ) instance.executeStatement( "IsCurrency( '£1.50', 'China' )" ) );
		assertTrue( ( Boolean ) instance.executeStatement( "IsCurrency( '1.50', 'China' )" ) );
		assertFalse( ( Boolean ) instance.executeStatement( "IsCurrency( 'blah', 'en_US' )" ) );
		// Test currencies which may contain symbol separators
		java.text.NumberFormat formatter = LocalizationUtil.localizedCurrencyFormatter( LocalizationUtil.buildLocale( "ar", "JO" ), "local" );
		assertTrue( ( Boolean ) instance.executeStatement( "IsCurrency( '" + formatter.format( 1000.51 ) + "', 'ar_JO' )" ) );
		formatter = LocalizationUtil.localizedCurrencyFormatter( Locale.CHINA, "local" );
		assertTrue( ( Boolean ) instance.executeStatement( "IsCurrency( '" + formatter.format( 1000.51 ) + "', 'China' )" ) );
	}

	@DisplayName( "It tests the BIF IsCurrency with formats generated by LSCurrencyFormat" )
	@Test
	@Disabled( "Until @jclausen fixes it :)" )
	public void testCanParseFromFormatter() {
		instance.executeStatement( "println(  'Local format:' & lsCurrencyFormat(3.21, 'local', 'en_US') )" );
		assertTrue( ( Boolean ) instance.executeStatement( "IsCurrency( lsCurrencyFormat(3.21, 'local', 'en_US') )" ) );
		instance.executeStatement( "println( 'International format:' & lsCurrencyFormat(3.21, 'international', 'en_US') )" );
		assertTrue( ( Boolean ) instance.executeStatement( "IsCurrency( lsCurrencyFormat(3.21, 'international', 'en_US') )" ) );
	}

}