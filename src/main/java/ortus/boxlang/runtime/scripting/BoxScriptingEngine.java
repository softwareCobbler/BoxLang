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
package ortus.boxlang.runtime.scripting;

import java.io.Reader;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.runnables.RunnableLoader;

/**
 * The BoxScriptingEngine is the JSR-223 implementation for BoxLang. It is the
 * entry point for executing BoxLang code on the JVM.
 *
 * @see ScriptEngine
 */
public class BoxScriptingEngine implements ScriptEngine, Compilable {

	private IBoxContext			boxContext;
	private BoxScriptingFactory	boxScriptingFactory;
	private BoxRuntime			boxRuntime;
	private ScriptContext		scriptContext;

	/**
	 * Constructor for the BoxScriptingEngine
	 *
	 * @param boxScriptingFactory The factory for the BoxScriptingEngine
	 *
	 * @see BoxScriptingFactory
	 */
	public BoxScriptingEngine( BoxScriptingFactory boxScriptingFactory ) {
		this.boxScriptingFactory	= boxScriptingFactory;
		this.boxContext				= new ScriptingRequestBoxContext( BoxRuntime.getInstance().getRuntimeContext() );
		this.boxRuntime				= BoxRuntime.getInstance();
		this.scriptContext			= new BoxScriptingContext( boxContext );
	}

	/**
	 * Evaluate a script in the context of the ScriptContext
	 *
	 * @param script  The script to evaluate
	 * @param context The context to evaluate the script in
	 *
	 * @return The result of the script evaluation
	 */
	public Object eval( String script, ScriptContext context ) throws ScriptException {
		scriptContext = context;
		return eval( script );
	}

	/**
	 * Evaluate a script in the context of the ScriptContext
	 *
	 * @param reader  The reader to read the script from
	 * @param context The context to evaluate the script in
	 *
	 * @return The result of the script evaluation
	 */
	public Object eval( Reader reader, ScriptContext context ) throws ScriptException {
		scriptContext = context;
		return eval( reader );
	}

	/**
	 * Evaluate a script bound only to the top-level BoxRuntime context
	 *
	 * @param script The script to evaluate
	 *
	 * @return The result of the script evaluation
	 */
	public Object eval( String script ) throws ScriptException {
		return boxRuntime.executeStatement( script, boxContext );
	}

	/**
	 * Evaluate a script bound only to the top-level BoxRuntime context
	 *
	 * @param reader The reader to read the script from
	 *
	 * @return The result of the script evaluation
	 */
	public Object eval( Reader reader ) throws ScriptException {
		return eval( reader.toString() );
	}

	/**
	 * Create a new Bindings object
	 *
	 * @return A new Bindings object
	 */
	public Bindings createBindings() {
		return new SimpleBindings();
	}

	/**
	 * Evaluate a script using the given Bindings
	 *
	 * @param script The script to evaluate
	 * @param n      The Bindings to use
	 *
	 * @return The result of the script evaluation
	 */
	@Override
	public Object eval( String script, Bindings n ) throws ScriptException {
		setBindings( n, ScriptContext.ENGINE_SCOPE );
		return eval( script );
	}

	@Override
	public Object eval( Reader reader, Bindings n ) throws ScriptException {
		setBindings( n, ScriptContext.ENGINE_SCOPE );
		return eval( reader );
	}

	@Override
	public void put( String key, Object value ) {
		getBindings( ScriptContext.ENGINE_SCOPE ).put( key, value );
	}

	@Override
	public Object get( String key ) {
		return getBindings( ScriptContext.ENGINE_SCOPE ).get( key );
	}

	/**
	 * Get the bindings for the given scope
	 *
	 * @param scope The scope to get the bindings for
	 *
	 * @return The bindings for the given scope if found, else null
	 */
	@Override
	public Bindings getBindings( int scope ) {
		return this.scriptContext.getBindings( scope );
	}

	/**
	 * Set the bindings for the given scope
	 *
	 * @param bindings The bindings to set
	 * @param scope    The scope to set the bindings for
	 *
	 * @throws IllegalArgumentException If the scope is invalid
	 */
	@Override
	public void setBindings( Bindings bindings, int scope ) {
		this.scriptContext.setBindings( bindings, scope );
	}

	/**
	 * Get the ScriptContext for the BoxScriptingEngine
	 *
	 * @return The ScriptContext for the BoxScriptingEngine
	 */
	public ScriptContext getContext() {
		return this.scriptContext;
	}

	/**
	 * Set the ScriptContext for the BoxScriptingEngine
	 *
	 * @param context The ScriptContext to set
	 */
	public void setContext( ScriptContext context ) {
		this.scriptContext = context;
	}

	@Override
	public ScriptEngineFactory getFactory() {
		return boxScriptingFactory;
	}

	/**
	 * Compile a script
	 *
	 * @param script The script to compile
	 *
	 * @return The compiled script
	 */
	@Override
	public CompiledScript compile( String script ) throws ScriptException {
		return new BoxCompiledScript( this, RunnableLoader.getInstance().loadStatement( script ) );
	}

	/**
	 * Compile a script
	 *
	 * @param script The script to compile
	 *
	 * @return The compiled script
	 *
	 */
	@Override
	public CompiledScript compile( Reader script ) throws ScriptException {
		return new BoxCompiledScript( this, RunnableLoader.getInstance().loadStatement( script.toString() ) );
	}

	/**
	 * Get the BoxContext for the BoxScriptingEngine
	 *
	 * @return The BoxContext for the BoxScriptingEngine
	 */
	public IBoxContext getBoxContext() {
		return this.boxContext;
	}
}
