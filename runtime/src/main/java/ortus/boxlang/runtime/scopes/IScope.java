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
package ortus.boxlang.runtime.scopes;

import java.util.Map;

import ortus.boxlang.runtime.dynamic.IReferenceable;

/**
 * All scope implementations must implement this interface
 */
public interface IScope extends Map<Key, Object>, IReferenceable {

	/**
	 * Gets the name of the scope
	 *
	 * @return The name of the scope
	 */
	public Key getName();

	/**
	 * Returns the value of the key safely, nulls will be wrapped in a {@code NullValue} still.
	 *
	 * @param key The key to look for
	 *
	 * @return The value of the key or a {@code NullValue} object, null means the key didn't exist *
	 */
	public Object getRaw( Key key );

}
