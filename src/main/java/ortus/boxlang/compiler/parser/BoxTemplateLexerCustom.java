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
package ortus.boxlang.compiler.parser;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;

import ortus.boxlang.parser.antlr.BoxTemplateLexer;

/**
 * I extend the generated ANTLR lexer to add some custom methods for getting unpopped modes
 * so we can perform better validation after parsing.
 */
public class BoxTemplateLexerCustom extends BoxTemplateLexer {

	/**
	 * Constructor
	 *
	 * @param input input stream
	 */
	public BoxTemplateLexerCustom( CharStream input ) {
		super( input );
	}

	/**
	 * Check if there are unpopped modes on the Lexer's mode stack
	 *
	 * @return true if there are unpopped modes
	 */
	public boolean hasUnpoppedModes() {
		return !_modeStack.isEmpty();
	}

	/**
	 * Get the unpopped modes on the Lexer's mode stack
	 *
	 * @return list of unpopped modes
	 */
	public List<Integer> getUnpoppedModesInts() {
		List<Integer> results = new ArrayList<Integer>();
		for ( int mode : _modeStack.toArray() ) {
			results.add( mode );
		}
		results.add( _mode );
		return results;
	}

	/**
	 * Get the unpopped modes on the Lexer's mode stack
	 *
	 * @return list of unpopped modes
	 */
	public List<String> getUnpoppedModes() {
		return getUnpoppedModesInts().stream().map( mode -> modeNames[ mode ] ).toList();
	}

	/**
	 * Check if a specific mode is on the stack
	 *
	 * @param mode mode to check
	 *
	 * @return true if the mode is on the stack
	 */
	public boolean hasMode( int mode ) {
		if ( !hasUnpoppedModes() ) {
			return false;
		}
		return getUnpoppedModesInts().contains( mode );
	}

	/**
	 * Check if the last mode was a specific mode
	 *
	 * @param mode mode to check
	 *
	 * @return true if the last mode was the specified mode
	 */
	public boolean lastModeWas( int mode ) {
		if ( !hasUnpoppedModes() ) {
			return false;
		}
		return getUnpoppedModesInts().get( 0 ) == mode;
	}

	/**
	 * Get the last token of a specific type
	 *
	 * @param type type of token to find
	 *
	 * @return the last token of the specified type
	 */
	public Token findPreviousToken( int type ) {
		reset();
		var tokens = getAllTokens();
		for ( int i = tokens.size() - 1; i >= 0; i-- ) {
			Token t = tokens.get( i );
			if ( t.getType() == type ) {
				return t;
			}
		}
		return null;
	}

	/**
	 * Get the last token in a stream
	 *
	 * @return the last token of the specified type
	 */
	public Token getLastToken() {
		reset();
		var tokens = getAllTokensSafe();
		if ( !tokens.isEmpty() ) {
			return tokens.get( tokens.size() - 1 );
		}
		return null;
	}

	public List<? extends Token> getAllTokensSafe() {
		List<Token> tokens = new ArrayList<Token>();
		try {
			Token t = nextToken();
			while ( t.getType() != Token.EOF ) {
				tokens.add( t );
				t = nextToken();
			}
		} catch ( Exception e ) {
			return tokens;
		}
		return tokens;
	}

	/**
	 * Get the last token of a specific type and the next x siblings
	 * Returns empty list if not found
	 * 
	 * @param type  type of token to find
	 * @param count number of siblings to find
	 * 
	 * @return the list of tokens starting from the specified type
	 */
	public List<Token> findPreviousTokenAndXSiblings( int type, int count ) {
		reset();
		List<Token>	results	= new ArrayList<Token>();
		var			tokens	= getAllTokens();
		for ( int i = tokens.size() - 1; i >= 0; i-- ) {
			Token t = tokens.get( i );
			if ( t.getType() == type ) {
				results.add( t );
				for ( int j = 1; j <= count; j++ ) {
					results.add( tokens.get( i + j ) );
				}
				return results;
			}
		}
		return results;
	}

}
