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

package ortus.boxlang.runtime.components.threading;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.parser.BoxScriptType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class ThreadTest {

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

	@DisplayName( "It can thread tag" )
	@Test
	public void testCanthreadTag() {

		instance.executeSource(
		    """
		        <cfthread name="myThread" foo="bar">
		          	<cfset printLn( "thread is done!" )>
		          	<cfset sleep( 1000 )>
		          </cfthread>
		       <cfset printLn( "thread tag done" )>
		    <cfset sleep( 2000 ) >
		    <cfset printLn( "test is done done" )>

		                """,
		    context, BoxScriptType.CFMARKUP );
	}

	@DisplayName( "It can thread script" )
	@Test
	public void testCanthreadScript() {

		instance.executeSource(
		    """
		    thread name="myThread" foo="bar"{
		    	printLn( "thread is done!" )
		    	sleep( 1000 )
		    }
		    printLn( "thread tag done" )
		    sleep( 2000 )
		    printLn( "test is done done" )
		          """,
		    context );
	}

	@DisplayName( "It can thread ACF script" )
	@Test
	public void testCanthreadACFScript() {

		instance.executeSource(
		    """
		    cfthread( name="myThread", foo="bar" ){
		    	printLn( "thread is done!" )
		    	sleep( 1000 )
		    }
		    printLn( "thread tag done" )
		    sleep( 2000 )
		    printLn( "test is done done" )
		         """,
		    context );
	}

}
