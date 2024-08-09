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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;

import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxScript;
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.BoxTemplate;
import ortus.boxlang.compiler.ast.Issue;
import ortus.boxlang.compiler.ast.Point;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.Source;
import ortus.boxlang.compiler.ast.SourceCode;
import ortus.boxlang.compiler.ast.SourceFile;
import ortus.boxlang.compiler.ast.comment.BoxMultiLineComment;
import ortus.boxlang.compiler.ast.expression.BoxClosure;
import ortus.boxlang.compiler.ast.expression.BoxFQN;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.ast.expression.BoxNull;
import ortus.boxlang.compiler.ast.expression.BoxStringInterpolation;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.compiler.ast.statement.BoxAccessModifier;
import ortus.boxlang.compiler.ast.statement.BoxAnnotation;
import ortus.boxlang.compiler.ast.statement.BoxArgumentDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxBreak;
import ortus.boxlang.compiler.ast.statement.BoxBufferOutput;
import ortus.boxlang.compiler.ast.statement.BoxContinue;
import ortus.boxlang.compiler.ast.statement.BoxDocumentationAnnotation;
import ortus.boxlang.compiler.ast.statement.BoxExpressionStatement;
import ortus.boxlang.compiler.ast.statement.BoxFunctionDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxIfElse;
import ortus.boxlang.compiler.ast.statement.BoxImport;
import ortus.boxlang.compiler.ast.statement.BoxMethodDeclarationModifier;
import ortus.boxlang.compiler.ast.statement.BoxRethrow;
import ortus.boxlang.compiler.ast.statement.BoxReturn;
import ortus.boxlang.compiler.ast.statement.BoxReturnType;
import ortus.boxlang.compiler.ast.statement.BoxScriptIsland;
import ortus.boxlang.compiler.ast.statement.BoxStatementBlock;
import ortus.boxlang.compiler.ast.statement.BoxSwitch;
import ortus.boxlang.compiler.ast.statement.BoxSwitchCase;
import ortus.boxlang.compiler.ast.statement.BoxTry;
import ortus.boxlang.compiler.ast.statement.BoxTryCatch;
import ortus.boxlang.compiler.ast.statement.BoxType;
import ortus.boxlang.compiler.ast.statement.BoxWhile;
import ortus.boxlang.compiler.ast.statement.component.BoxComponent;
import ortus.boxlang.parser.antlr.BoxTemplateGrammar;
import ortus.boxlang.parser.antlr.BoxTemplateGrammar.ArgumentContext;
import ortus.boxlang.parser.antlr.BoxTemplateGrammar.AttributeContext;
import ortus.boxlang.parser.antlr.BoxTemplateGrammar.AttributeValueContext;
import ortus.boxlang.parser.antlr.BoxTemplateGrammar.BoxImportContext;
import ortus.boxlang.parser.antlr.BoxTemplateGrammar.BreakContext;
import ortus.boxlang.parser.antlr.BoxTemplateGrammar.CaseContext;
import ortus.boxlang.parser.antlr.BoxTemplateGrammar.CatchBlockContext;
import ortus.boxlang.parser.antlr.BoxTemplateGrammar.ContinueContext;
import ortus.boxlang.parser.antlr.BoxTemplateGrammar.FunctionContext;
import ortus.boxlang.parser.antlr.BoxTemplateGrammar.GenericOpenCloseComponentContext;
import ortus.boxlang.parser.antlr.BoxTemplateGrammar.GenericOpenComponentContext;
import ortus.boxlang.parser.antlr.BoxTemplateGrammar.IncludeContext;
import ortus.boxlang.parser.antlr.BoxTemplateGrammar.InterpolatedExpressionContext;
import ortus.boxlang.parser.antlr.BoxTemplateGrammar.OutputContext;
import ortus.boxlang.parser.antlr.BoxTemplateGrammar.RethrowContext;
import ortus.boxlang.parser.antlr.BoxTemplateGrammar.ReturnContext;
import ortus.boxlang.parser.antlr.BoxTemplateGrammar.ScriptContext;
import ortus.boxlang.parser.antlr.BoxTemplateGrammar.SetContext;
import ortus.boxlang.parser.antlr.BoxTemplateGrammar.StatementContext;
import ortus.boxlang.parser.antlr.BoxTemplateGrammar.StatementsContext;
import ortus.boxlang.parser.antlr.BoxTemplateGrammar.SwitchContext;
import ortus.boxlang.parser.antlr.BoxTemplateGrammar.TemplateContext;
import ortus.boxlang.parser.antlr.BoxTemplateGrammar.TextContentContext;
import ortus.boxlang.parser.antlr.BoxTemplateGrammar.ThrowContext;
import ortus.boxlang.parser.antlr.BoxTemplateGrammar.TryContext;
import ortus.boxlang.parser.antlr.BoxTemplateGrammar.WhileContext;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.components.ComponentDescriptor;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.services.ComponentService;

public class BoxTemplateParser extends AbstractParser {

	private int				outputCounter		= 0;
	public ComponentService	componentService	= BoxRuntime.getInstance().getComponentService();

	public BoxTemplateParser() {
		super();
	}

	public BoxTemplateParser( int startLine, int startColumn ) {
		super( startLine, startColumn );
	}

	public ParsingResult parse( File file ) throws IOException {
		this.file = file;
		setSource( new SourceFile( file ) );
		BOMInputStream		inputStream			= getInputStream( file );

		Optional<String>	ext					= Parser.getFileExtension( file.getAbsolutePath() );
		Boolean				classOrInterface	= ext.isPresent() && ext.get().equalsIgnoreCase( "bx" );
		BoxNode				ast					= parserFirstStage( inputStream, classOrInterface );
		return new ParsingResult( ast, issues, comments );
	}

	public ParsingResult parse( String code ) throws IOException {
		return parse( code, false );
	}

	public ParsingResult parse( String code, Boolean classOrInterface ) throws IOException {
		this.sourceCode = code;
		setSource( new SourceCode( code ) );
		InputStream	inputStream	= IOUtils.toInputStream( code, StandardCharsets.UTF_8 );
		BoxNode		ast			= parserFirstStage( inputStream, classOrInterface );
		return new ParsingResult( ast, issues, comments );
	}

	@Override
	protected BoxNode parserFirstStage( InputStream inputStream, Boolean classOrInterface ) throws IOException {
		BoxTemplateLexerCustom	lexer	= new BoxTemplateLexerCustom( CharStreams.fromStream( inputStream, StandardCharsets.UTF_8 ) );
		BoxTemplateGrammar		parser	= new BoxTemplateGrammar( new CommonTokenStream( lexer ) );
		addErrorListeners( lexer, parser );
		BoxTemplateGrammar.TemplateContext templateContext = null;
		if ( classOrInterface ) {
			issues.add( new Issue( "Classes and Interfaces are only supported in Script format.", getPosition( lexer.nextToken() ) ) );
			return null;
		} else {
			templateContext = parser.template();
		}

		// This must run FIRST before resetting the lexer
		validateParse( lexer );
		// This can add issues to an otherwise successful parse
		extractComments( lexer );

		BoxNode rootNode;
		try {
			rootNode = toAst( null, templateContext );
		} catch ( Exception e ) {
			// Ignore issues creating AST if the parsing already had failures
			if ( issues.isEmpty() ) {
				throw e;
			}
			return null;
		}

		if ( isSubParser() ) {
			return rootNode;
		}

		// associate all comments in the source with the appropriate AST nodes
		rootNode.associateComments( this.comments );

		return rootNode;
	}

	private void validateParse( BoxTemplateLexerCustom lexer ) {
		Token token;
		if ( lexer.hasUnpoppedModes() ) {
			List<String>	modes		= lexer.getUnpoppedModes();
			// get position of end of last token from the lexer

			Position		position	= createOffsetPosition( lexer._token.getLine(),
			    lexer._token.getCharPositionInLine() + lexer._token.getText().length() - 1, lexer._token.getLine(),
			    lexer._token.getCharPositionInLine() + lexer._token.getText().length() - 1 );
			// Check for specific unpopped modes that we can throw a specific error for
			if ( lexer.hasMode( BoxTemplateLexerCustom.EXPRESSION_MODE_STRING ) || lexer.hasMode( BoxTemplateLexerCustom.EXPRESSION_MODE_UNQUOTED_ATTVALUE ) ) {
				String	message		= "Unclosed expression starting with #";
				Token	startToken	= lexer.findPreviousToken( BoxTemplateLexerCustom.ICHAR );
				if ( startToken != null ) {
					position = createOffsetPosition( startToken.getLine(), startToken.getCharPositionInLine(), startToken.getLine(),
					    startToken.getCharPositionInLine() + startToken.getText().length() );
				}
				message += " on line " + position.getStart().getLine();
				issues.add( new Issue( message, position ) );
			} else if ( lexer.hasMode( BoxTemplateLexerCustom.EXPRESSION_MODE_COMPONENT ) ) {
				String	message		= "Unclosed expression inside an opening tag";
				Token	startToken	= lexer.findPreviousToken( BoxTemplateLexerCustom.COMPONENT_OPEN );
				if ( startToken == null ) {
					startToken = lexer.findPreviousToken( BoxTemplateLexerCustom.OUTPUT_START );
				}
				if ( startToken != null ) {
					position = createOffsetPosition( startToken.getLine(), startToken.getCharPositionInLine(), startToken.getLine(),
					    startToken.getCharPositionInLine() + startToken.getText().length() );
				}
				message += " on line " + position.getStart().getLine();
				issues.add( new Issue( message, position ) );
			} else if ( lexer.hasMode( BoxTemplateLexerCustom.OUTPUT_MODE ) ) {
				String	message				= "Unclosed output tag";
				Token	outputStartToken	= lexer.findPreviousToken( BoxTemplateLexerCustom.OUTPUT_START );
				if ( outputStartToken != null ) {
					position = createOffsetPosition( outputStartToken.getLine(), outputStartToken.getCharPositionInLine(), outputStartToken.getLine(),
					    outputStartToken.getCharPositionInLine() + outputStartToken.getText().length() );
				}
				message += " on line " + position.getStart().getLine();
				issues.add( new Issue( message, position ) );
			} else if ( lexer.hasMode( BoxTemplateLexerCustom.COMMENT_MODE ) ) {
				String	message				= "Unclosed tag comment";
				Token	outputStartToken	= lexer.findPreviousToken( BoxTemplateLexerCustom.COMMENT_START );
				if ( outputStartToken != null ) {
					position = createOffsetPosition( outputStartToken.getLine(), outputStartToken.getCharPositionInLine(), outputStartToken.getLine(),
					    outputStartToken.getCharPositionInLine() + outputStartToken.getText().length() );
				}
				message += " on line " + position.getStart().getLine();
				issues.add( new Issue( message, position ) );
			} else if ( lexer.hasMode( BoxTemplateLexerCustom.COMPONENT_MODE ) ) {
				String	message		= "Unclosed tag";
				Token	startToken	= lexer.findPreviousToken( BoxTemplateLexerCustom.PREFIX );
				if ( startToken == null ) {
					startToken = lexer.findPreviousToken( BoxTemplateLexerCustom.SLASH_PREFIX );
				}
				if ( startToken != null ) {
					position = createOffsetPosition( startToken.getLine(), startToken.getCharPositionInLine(), startToken.getLine(),
					    startToken.getCharPositionInLine() + startToken.getText().length() );
					List<Token> nameTokens = lexer.findPreviousTokenAndXSiblings( startToken.getType(), 1 );
					if ( !nameTokens.isEmpty() ) {
						message += " [";
						for ( var t : nameTokens ) {
							message += t.getText();
						}
						message += "]";
					}
				}
				message += " starting on line " + position.getStart().getLine();
				issues.add( new Issue( message, position ) );
			} else {
				issues.add( new Issue( "Invalid Syntax. (Unpopped modes) [" + modes.stream().collect( Collectors.joining( ", " ) ) + "]", position ) );
			}
		} else {

			// If there were unpopped modes, we had reset the lexer above to get the position of the unmatched token, so we no longer have
			// the ability to check for unconsumed tokens.

			// Check if there are unconsumed tokens
			token = lexer._token;
			while ( token.getType() != Token.EOF && ( token.getChannel() == BoxTemplateLexerCustom.HIDDEN || token.getText().isBlank() ) ) {
				token = lexer.nextToken();
			}
			if ( token.getType() != Token.EOF ) {

				StringBuffer	extraText	= new StringBuffer();
				int				startLine	= token.getLine();
				int				startColumn	= token.getCharPositionInLine();
				int				endColumn	= startColumn + token.getText().length();
				Position		position	= createOffsetPosition( startLine, startColumn, startLine, endColumn );

				while ( token.getType() != Token.EOF && extraText.length() < 100 ) {
					extraText.append( token.getText() );
					token = lexer.nextToken();
				}
				issues.add( new Issue( "Extra char(s) [" + extraText.toString() + "] at the end of parsing.", position ) );
			}
		}
	}

	private void extractComments( BoxTemplateLexerCustom lexer ) throws IOException {
		lexer.reset();
		Token token = lexer.nextToken();
		while ( token.getType() != Token.EOF ) {
			if ( token.getType() == BoxTemplateLexerCustom.COMMENT_START ) {
				Token			startToken	= token;
				StringBuffer	tagComment	= new StringBuffer();
				token = lexer.nextToken();
				while ( token.getType() != BoxTemplateLexerCustom.COMMENT_END && token.getType() != Token.EOF ) {
					// validate all tokens MUST be COMMENT_START, or COMMENT_TEXT
					if ( token.getType() != BoxTemplateLexerCustom.COMMENT_START && token.getType() != BoxTemplateLexerCustom.COMMENT_TEXT ) {
						issues.add( new Issue( "Invalid tag comment", getPosition( token ) ) );
						break;
					}
					tagComment.append( token.getText() );
					token = lexer.nextToken();
				}
				String finalCommentText = tagComment.toString();
				// Convert to a proper /* script comment */
				comments.add(
				    new BoxMultiLineComment(
				        finalCommentText.trim(),
				        getPosition( startToken, token ),
				        getSourceText( startToken, token )
				    )
				);
			}
			token = lexer.nextToken();
		}
	}

	protected BoxTemplate toAst( File file, TemplateContext rule ) throws IOException {
		List<BoxStatement> statements = new ArrayList<>();
		if ( rule.statements() != null ) {
			statements = toAst( file, rule.statements() );
		}
		return new BoxTemplate( statements, getPosition( rule ), getSourceText( rule ) );
	}

	private BoxImport toAst( File file, BoxImportContext node ) {
		String				name		= null;
		String				prefix		= null;
		String				module		= null;
		BoxIdentifier		alias		= null;
		List<BoxAnnotation>	annotations	= new ArrayList<>();

		for ( var attr : node.attribute() ) {
			annotations.add( toAst( file, attr ) );
		}
		BoxFQN			nameFQN		= null;
		BoxExpression	nameSearch	= findExprInAnnotations( annotations, "name", false, null, "import", getPosition( node ) );
		if ( nameSearch != null ) {
			name	= getBoxExprAsString( nameSearch, "name", false );
			prefix	= getBoxExprAsString( findExprInAnnotations( annotations, "prefix", false, null, null, null ), "prefix", false );
			if ( prefix != null ) {
				name = prefix + ":" + name;
			}
			nameFQN = new BoxFQN( name, nameSearch.getPosition(), nameSearch.getSourceText() );
		}
		module = getBoxExprAsString( findExprInAnnotations( annotations, "module", false, null, null, null ), "module", false );

		BoxExpression aliasSearch = findExprInAnnotations( annotations, "alias", false, null, null, null );
		if ( aliasSearch != null ) {
			alias = new BoxIdentifier( getBoxExprAsString( aliasSearch, "alias", false ),
			    aliasSearch.getPosition(),
			    aliasSearch.getSourceText() );
		}

		return new BoxImport( nameFQN, alias, getPosition( node ), getSourceText( node ) );
	}

	private List<BoxStatement> toAst( File file, StatementsContext node ) {
		return statementsToAst( file, node );
	}

	private List<BoxStatement> statementsToAst( File file, ParserRuleContext node ) {
		List<BoxStatement> statements = new ArrayList<>();
		if ( node.children != null ) {
			for ( var child : node.children ) {
				if ( child instanceof StatementContext statement ) {
					if ( statement.genericCloseComponent() != null ) {
						String				componentName	= statement.genericCloseComponent().componentName().getText();
						ComponentDescriptor	descriptor		= componentService.getComponent( componentName );
						if ( descriptor != null ) {
							if ( !descriptor.allowsBody() ) {
								issues.add( new Issue( "The [" + componentName + "] component does not allow a body", getPosition( node ) ) );
							}
						}
						// see if statements list has a BoxComponent with this name
						int		size		= statements.size();
						boolean	foundStart	= false;
						int		removeAfter	= -1;
						// loop backwards checking for a BoxComponent with this name
						for ( int i = size - 1; i >= 0; i-- ) {
							BoxStatement boxStatement = statements.get( i );
							if ( boxStatement instanceof BoxComponent boxComponent ) {
								if ( boxComponent.getName().equalsIgnoreCase( componentName ) && boxComponent.getBody() == null ) {
									foundStart = true;
									// slice all statements from this position to the end and set them as the body of the start component
									boxComponent.setBody( new ArrayList<>( statements.subList( i + 1, size ) ) );
									boxComponent.getPosition().setEnd( getPosition( statement.genericCloseComponent() ).getEnd() );
									boxComponent.setSourceText( getSourceText( boxComponent.getSourceStartIndex(), statement.genericCloseComponent() ) );
									removeAfter = i;
									break;
								} else if ( boxComponent.getBody() == null && boxComponent.getRequiresBody() ) {
									issues.add( new Issue( "Component [" + boxComponent.getName() + "] requires a body.", boxComponent.getPosition() ) );
								}
							}
						}
						// remove all items in list after removeAfter index
						if ( removeAfter >= 0 ) {
							statements.subList( removeAfter + 1, size ).clear();
						}
						if ( !foundStart ) {
							issues.add( new Issue( "Found end component [" + componentName + "] without matching start component",
							    getPosition( statement.genericCloseComponent() ) ) );
						}

					} else {
						statements.add( toAst( file, statement ) );
					}
				} else if ( child instanceof TextContentContext textContent ) {
					statements.addAll( toAst( file, textContent ) );
				} else if ( child instanceof ScriptContext script ) {
					if ( script.scriptBody() != null ) {
						statements.add(
						    new BoxScriptIsland(
						        parseBoxStatements( script.scriptBody().getText(), getPosition( script.scriptBody() ) ),
						        getPosition( script.scriptBody() ),
						        getSourceText( script.scriptBody() )
						    )
						);
					}
				} else if ( child instanceof BoxImportContext importContext ) {
					statements.add( toAst( file, importContext ) );
				}
			}
		}
		// Loop over statements and look for any BoxComponents who require a body but it's null
		for ( BoxStatement statement : statements ) {
			if ( statement instanceof BoxComponent boxComponent ) {
				if ( boxComponent.getBody() == null && boxComponent.getRequiresBody() ) {
					issues.add( new Issue( "Component [" + boxComponent.getName() + "] requires a body.", boxComponent.getPosition() ) );
				}
			}
		}
		return statements;
	}

	private BoxStatement toAst( File file, StatementContext node ) {
		if ( node.output() != null ) {
			return toAst( file, node.output() );
		} else if ( node.set() != null ) {
			return toAst( file, node.set() );
		} else if ( node.if_() != null ) {
			return toAst( file, node.if_() );
		} else if ( node.try_() != null ) {
			return toAst( file, node.try_() );
		} else if ( node.function() != null ) {
			return toAst( file, node.function() );
		} else if ( node.return_() != null ) {
			return toAst( file, node.return_() );
		} else if ( node.while_() != null ) {
			return toAst( file, node.while_() );
		} else if ( node.break_() != null ) {
			return toAst( file, node.break_() );
		} else if ( node.continue_() != null ) {
			return toAst( file, node.continue_() );
		} else if ( node.include() != null ) {
			return toAst( file, node.include() );
		} else if ( node.rethrow() != null ) {
			return toAst( file, node.rethrow() );
		} else if ( node.throw_() != null ) {
			return toAst( file, node.throw_() );
		} else if ( node.switch_() != null ) {
			return toAst( file, node.switch_() );
		} else if ( node.genericOpenCloseComponent() != null ) {
			return toAst( file, node.genericOpenCloseComponent() );
		} else if ( node.genericOpenComponent() != null ) {
			return toAst( file, node.genericOpenComponent() );
		} else if ( node.boxImport() != null ) {
			return toAst( file, node.boxImport() );
		}
		issues.add( new Issue( "Statement node parsing not implemented yet", getPosition( node ) ) );
		return null;

	}

	private BoxStatement toAst( File file, GenericOpenCloseComponentContext node ) {
		List<BoxAnnotation> attributes = new ArrayList<>();
		for ( var attr : node.attribute() ) {
			attributes.add( toAst( file, attr ) );
		}
		return new BoxComponent( node.componentName().getText(), attributes, List.of(), node.getStart().getStartIndex(), getPosition( node ),
		    getSourceText( node ) );
	}

	private BoxStatement toAst( File file, GenericOpenComponentContext node ) {
		List<BoxAnnotation> attributes = new ArrayList<>();
		for ( var attr : node.attribute() ) {
			attributes.add( toAst( file, attr ) );
		}
		String name = node.componentName().getText();

		// Special check for loop condition to avoid runtime eval
		if ( name.equalsIgnoreCase( "loop" ) ) {
			for ( var attr : attributes ) {
				if ( attr.getKey().getValue().equalsIgnoreCase( "condition" ) ) {
					BoxExpression condition = attr.getValue();
					if ( condition instanceof BoxStringLiteral str ) {
						// parse as Box script expression and update value
						condition = parseBoxExpression( str.getValue(), condition.getPosition() );
					}
					BoxExpression newCondition = new BoxClosure(
					    List.of(),
					    List.of(),
					    new BoxReturn( condition, null, null ),
					    null,
					    null );
					attr.setValue( newCondition );
				}
			}
		}

		// Body may get set later, if we find an end component
		var					comp		= new BoxComponent( name, attributes, null, node.getStart().getStartIndex(), getPosition( node ),
		    getSourceText( node ) );

		ComponentDescriptor	descriptor	= componentService.getComponent( name );
		if ( descriptor != null && descriptor.requiresBody() ) {
			comp.setRequiresBody( true );
		}

		return comp;

	}

	private BoxStatement toAst( File file, SwitchContext node ) {
		BoxExpression		expression;
		List<BoxAnnotation>	annotations	= new ArrayList<>();
		List<BoxSwitchCase>	cases		= new ArrayList<>();

		for ( var attr : node.attribute() ) {
			annotations.add( toAst( file, attr ) );
		}

		expression = findExprInAnnotations( annotations, "expression", true, null, "switch", getPosition( node ) );

		if ( node.switchBody() != null && node.switchBody().children != null ) {
			for ( var c : node.switchBody().children ) {
				if ( c instanceof BoxTemplateGrammar.CaseContext caseNode ) {
					cases.add( toAst( file, caseNode ) );
					// We're willing to overlook text, but not other BoxLang components
				} else if ( ! ( c instanceof BoxTemplateGrammar.TextContentContext ) ) {
					issues.add( new Issue( "Switch body can only contain case statements - ", getPosition( ( ParserRuleContext ) c ) ) );
				}
			}
		}
		return new BoxSwitch( expression, cases, getPosition( node ), getSourceText( node ) );
	}

	private BoxSwitchCase toAst( File file, CaseContext node ) {
		BoxExpression	value		= null;
		BoxExpression	delimiter	= null;

		// Only check for these on case nodes, not default case
		if ( !node.CASE().isEmpty() ) {
			List<BoxAnnotation> annotations = new ArrayList<>();

			for ( var attr : node.attribute() ) {
				annotations.add( toAst( file, attr ) );
			}

			value		= findExprInAnnotations( annotations, "value", true, null, "case", getPosition( node ) );
			delimiter	= findExprInAnnotations( annotations, "delimiter", false, new BoxStringLiteral( ",", null, null ), "case", getPosition( node ) );
		}

		List<BoxStatement> statements = new ArrayList<>();
		if ( node.statements() != null ) {
			statements.addAll( toAst( file, node.statements() ) );
		}

		// In component mode, the break is implied
		statements.add( new BoxBreak( null, null ) );

		return new BoxSwitchCase( value, delimiter, statements, getPosition( node ), getSourceText( node ) );
	}

	private BoxStatement toAst( File file, ThrowContext node ) {
		BoxExpression		object			= null;
		BoxExpression		type			= null;
		BoxExpression		message			= null;
		BoxExpression		detail			= null;
		BoxExpression		errorcode		= null;
		BoxExpression		extendedinfo	= null;

		List<BoxAnnotation>	annotations		= new ArrayList<>();

		for ( var attr : node.attribute() ) {
			annotations.add( toAst( file, attr ) );
		}

		// Using generic component so attributeCollection can work
		return new BoxComponent(
		    "throw",
		    annotations,
		    getPosition( node ),
		    getSourceText( node )
		);
	}

	private BoxStatement toAst( File file, RethrowContext node ) {
		return new BoxRethrow( getPosition( node ), getSourceText( node ) );
	}

	private BoxStatement toAst( File file, IncludeContext node ) {
		List<BoxAnnotation> annotations = new ArrayList<>();

		for ( var attr : node.attribute() ) {
			annotations.add( toAst( file, attr ) );
		}

		return new BoxComponent(
		    "include",
		    annotations,
		    getPosition( node ),
		    getSourceText( node )
		);
	}

	private BoxStatement toAst( File file, ContinueContext node ) {
		String label = null;
		if ( node.label != null ) {
			label = node.label.getText();
		}
		return new BoxContinue( label, getPosition( node ), getSourceText( node ) );
	}

	private BoxStatement toAst( File file, BreakContext node ) {
		String label = null;
		if ( node.label != null ) {
			label = node.label.getText();
		}
		return new BoxBreak( label, getPosition( node ), getSourceText( node ) );
	}

	private BoxStatement toAst( File file, WhileContext node ) {
		BoxExpression		condition;
		List<BoxStatement>	bodyStatements	= new ArrayList<>();
		List<BoxAnnotation>	annotations		= new ArrayList<>();

		for ( var attr : node.attribute() ) {
			annotations.add( toAst( file, attr ) );
		}

		BoxExpression conditionSearch = findExprInAnnotations( annotations, "condition", true, null, "while", getPosition( node ) );
		condition = parseBoxExpression(
		    getBoxExprAsString(
		        conditionSearch,
		        "condition",
		        false
		    ),
		    conditionSearch.getPosition()
		);

		if ( node.statements() != null ) {
			bodyStatements.addAll( toAst( file, node.statements() ) );
		}
		BoxStatement	body		= new BoxStatementBlock( bodyStatements, getPosition( node.statements() ), getSourceText( node.statements() ) );
		BoxExpression	labelSearch	= findExprInAnnotations( annotations, "label", false, null, "while", getPosition( node ) );
		String			label		= getBoxExprAsString( labelSearch, "label", false );

		return new BoxWhile( label, condition, body, getPosition( node ), getSourceText( node ) );
	}

	private BoxStatement toAst( File file, ReturnContext node ) {
		BoxExpression expr;
		if ( node.expression() != null ) {
			expr = parseBoxExpression( node.expression().getText(), getPosition( node.expression() ) );
		} else {
			expr = new BoxNull( null, null );
		}
		return new BoxReturn( expr, getPosition( node ), getSourceText( node ) );
	}

	private BoxFunctionDeclaration toAst( File file, FunctionContext node ) {
		BoxReturnType						returnType		= null;
		String								name			= null;
		List<BoxStatement>					body			= new ArrayList<>();
		List<BoxArgumentDeclaration>		args			= new ArrayList<>();
		List<BoxAnnotation>					annotations		= new ArrayList<>();
		List<BoxDocumentationAnnotation>	documentation	= new ArrayList<>();
		BoxAccessModifier					accessModifier	= null;
		List<BoxMethodDeclarationModifier>	modifiers		= new ArrayList<>();

		for ( var attr : node.attribute() ) {
			annotations.add( toAst( file, attr ) );
		}

		name = getBoxExprAsString( findExprInAnnotations( annotations, "name", true, null, "function", getPosition( node ) ), "name", false );

		String accessText = getBoxExprAsString( findExprInAnnotations( annotations, "function", false, null, null, null ), "access", true );
		if ( accessText != null ) {
			accessText = accessText.toLowerCase();
			if ( accessText.equals( "public" ) ) {
				accessModifier = BoxAccessModifier.Public;
			} else if ( accessText.equals( "private" ) ) {
				accessModifier = BoxAccessModifier.Private;
			} else if ( accessText.equals( "remote" ) ) {
				accessModifier = BoxAccessModifier.Remote;
			} else if ( accessText.equals( "package" ) ) {
				accessModifier = BoxAccessModifier.Package;
			}
		}

		BoxExpression	returnTypeSearch	= findExprInAnnotations( annotations, "returnType", false, null, null, null );
		String			returnTypeText		= getBoxExprAsString( returnTypeSearch, "returnType", true );
		if ( returnTypeText != null ) {
			BoxType	boxType	= BoxType.fromString( returnTypeText );
			String	fqn		= boxType.equals( BoxType.Fqn ) ? returnTypeText : null;
			returnType = new BoxReturnType( boxType, fqn, returnTypeSearch.getPosition(), returnTypeSearch.getSourceText() );
		}

		for ( var arg : node.argument() ) {
			args.add( toAst( file, arg ) );
		}

		body.addAll( toAst( file, node.body ) );

		return new BoxFunctionDeclaration( accessModifier, modifiers, name, returnType, args, annotations, documentation, body, getPosition( node ),
		    getSourceText( node ) );
	}

	private BoxArgumentDeclaration toAst( File file, ArgumentContext node ) {
		Boolean								required		= false;
		String								type			= "Any";
		String								name			= "undefined";
		BoxExpression						expr			= null;
		List<BoxAnnotation>					annotations		= new ArrayList<>();
		List<BoxDocumentationAnnotation>	documentation	= new ArrayList<>();

		for ( var attr : node.attribute() ) {
			annotations.add( toAst( file, attr ) );
		}

		name		= getBoxExprAsString( findExprInAnnotations( annotations, "name", true, null, "function", getPosition( node ) ), "name", false );

		required	= BooleanCaster.cast(
		    getBoxExprAsString(
		        findExprInAnnotations( annotations, "required", false, null, null, null ),
		        "required",
		        false
		    )
		);

		expr		= findExprInAnnotations( annotations, "default", false, null, null, null );
		type		= getBoxExprAsString( findExprInAnnotations( annotations, "type", false, new BoxStringLiteral( "Any", null, null ), null, null ), "type",
		    false );

		return new BoxArgumentDeclaration( required, type, name, expr, annotations, documentation, getPosition( node ), getSourceText( node ) );
	}

	private BoxAnnotation toAst( File file, AttributeContext attribute ) {
		BoxFQN			name	= new BoxFQN( attribute.attributeName().getText(), getPosition( attribute.attributeName() ),
		    getSourceText( attribute.attributeName() ) );
		BoxExpression	value;
		if ( attribute.attributeValue() != null ) {
			value = toAst( file, attribute.attributeValue() );
		} else {
			value = new BoxStringLiteral( "", null, null );
		}
		return new BoxAnnotation( name, value, getPosition( attribute ), getSourceText( attribute ) );
	}

	private BoxExpression toAst( File file, AttributeValueContext node ) {
		if ( node.unquotedValue() != null ) {
			return new BoxStringLiteral( node.unquotedValue().getText(), getPosition( node ),
			    getSourceText( node ) );
		}
		if ( node.interpolatedExpression() != null ) {
			return toAst( file, node.interpolatedExpression() );
		} else {
			return toAst( file, node.quotedString() );
		}
	}

	private BoxStatement toAst( File file, TryContext node ) {
		List<BoxStatement> tryBody = new ArrayList<>();
		for ( var statements : node.statements() ) {
			tryBody.addAll( toAst( file, statements ) );
		}
		List<BoxTryCatch>	catches		= node.catchBlock().stream().map( it -> toAst( file, it ) ).toList();
		List<BoxStatement>	finallyBody	= new ArrayList<>();
		if ( node.finallyBlock() != null ) {
			finallyBody.addAll( toAst( file, node.finallyBlock().statements() ) );
		}
		return new BoxTry( tryBody, catches, finallyBody, getPosition( node ), getSourceText( node ) );
	}

	private BoxTryCatch toAst( File file, CatchBlockContext node ) {
		BoxExpression		exception	= new BoxIdentifier( "bxcatch", null, null );
		List<BoxExpression>	catchTypes;
		List<BoxStatement>	catchBody	= new ArrayList<>();

		if ( node.attribute() != null ) {
			var typeSearch = node.attribute().stream()
			    .filter( ( it ) -> it.attributeName().getText().equalsIgnoreCase( "type" ) && it.attributeValue() != null ).findFirst();
			if ( typeSearch.isPresent() ) {
				BoxExpression type;
				if ( typeSearch.get().attributeValue().unquotedValue() != null ) {
					type = new BoxStringLiteral( typeSearch.get().attributeValue().unquotedValue().getText(), getPosition( typeSearch.get().attributeValue() ),
					    getSourceText( typeSearch.get().attributeValue() ) );
				} else {
					type = toAst( file, typeSearch.get().attributeValue().quotedString() );
				}
				catchTypes = List.of( type );
			} else {
				catchTypes = List.of( new BoxFQN( "any", null, null ) );
			}
		} else {
			catchTypes = List.of( new BoxFQN( "any", null, null ) );
		}
		if ( node.statements() != null ) {
			catchBody = toAst( file, node.statements() );
		}
		return new BoxTryCatch( catchTypes, exception, catchBody, getPosition( node ), getSourceText( node ) );
	}

	private BoxExpression toAst( File file, BoxTemplateGrammar.QuotedStringContext node ) {
		String quoteChar = node.getText().substring( 0, 1 );
		if ( node.interpolatedExpression().isEmpty() ) {
			String s = node.getText();
			// trim leading and trailing quote
			s = s.substring( 1, s.length() - 1 );
			return new BoxStringLiteral(
			    escapeStringLiteral( quoteChar, s ),
			    getPosition( node ),
			    getSourceText( node )
			);

		} else {
			List<BoxExpression> parts = new ArrayList<>();
			node.children.forEach( it -> {
				if ( it != null && it instanceof BoxTemplateGrammar.QuotedStringPartContext str ) {
					parts.add( new BoxStringLiteral( escapeStringLiteral( quoteChar, getSourceText( str ) ),
					    getPosition( str ),
					    getSourceText( str ) ) );
				}
				if ( it != null && it instanceof BoxTemplateGrammar.InterpolatedExpressionContext interp ) {
					parts.add( toAst( file, interp ) );
				}
			} );
			return new BoxStringInterpolation( parts, getPosition( node ), getSourceText( node ) );
		}
	}

	public BoxExpression toAst( File file, InterpolatedExpressionContext interp ) {
		return parseBoxExpression( interp.expression().getText(), getPosition( interp.expression() ) );
	}

	/**
	 * Escape double up quotes and pounds in a string literal
	 *
	 * @param quoteChar the quote character used to surround the string
	 * @param string    the string to escape
	 *
	 * @return the escaped string
	 */
	public String escapeStringLiteral( String quoteChar, String string ) {
		String escaped = string.replace( "##", "#" );
		return escaped.replace( quoteChar + quoteChar, quoteChar );
	}

	private BoxIfElse toAst( File file, BoxTemplateGrammar.IfContext node ) {
		// if condition will always exist
		BoxExpression		condition			= parseBoxExpression( node.ifCondition.getText(), getPosition( node.ifCondition ) );
		List<BoxStatement>	thenBodyStatements	= new ArrayList<>();
		List<BoxStatement>	elseBodyStatements	= new ArrayList<>();
		BoxStatement		elseBody			= null;

		// Then body will always exist
		thenBodyStatements.addAll( toAst( file, node.thenBody ) );

		if ( node.ELSE() != null ) {
			elseBodyStatements.addAll( toAst( file, node.elseBody ) );
			elseBody = new BoxStatementBlock( elseBodyStatements, getPosition( node.elseBody ), getSourceText( node.elseBody ) );
		}

		// Loop backward over elseif conditions, each one becoming the elseBody of the next.
		for ( int i = node.elseIfCondition.size() - 1; i >= 0; i-- ) {
			int		stopIndex;
			Point	end	= new Point( node.elseIfComponentClose.get( i ).getLine(),
			    node.elseIfComponentClose.get( i ).getCharPositionInLine() );
			stopIndex = node.elseIfComponentClose.get( i ).getStopIndex();
			if ( node.elseThenBody.get( i ).statement().size() > 0 ) {
				end			= new Point( node.elseThenBody.get( i ).statement( node.elseThenBody.get( i ).statement().size() - 1 ).getStop().getLine(),
				    node.elseThenBody.get( i ).statement( node.elseThenBody.get( i ).statement().size() - 1 ).getStop().getCharPositionInLine() );
				stopIndex	= node.elseThenBody.get( i ).statement( node.elseThenBody.get( i ).statement().size() - 1 ).getStop().getStopIndex();
			}
			Position		pos				= new Position(
			    new Point( node.ELSEIF( i ).getSymbol().getLine(), node.ELSEIF( i ).getSymbol().getCharPositionInLine() - 3 ),
			    end, sourceToParse );
			BoxExpression	thisCondition	= parseBoxExpression( node.elseIfCondition.get( i ).getText(), getPosition( node.elseIfCondition.get( i ) ) );
			elseBodyStatements	= List.of(
			    new BoxIfElse(
			        thisCondition,
			        // TODO: I don't think this pos var is correct
			        new BoxStatementBlock( toAst( file, node.elseThenBody.get( i ) ), pos, getSourceText( node.elseThenBody.get( i ) ) ),
			        elseBody,
			        pos,
			        getSourceText( node, node.ELSEIF().get( i ).getSymbol().getStartIndex() - 3, stopIndex )
			    )
			);
			elseBody			= new BoxStatementBlock( elseBodyStatements, pos,
			    getSourceText( node, node.ELSEIF().get( i ).getSymbol().getStartIndex() - 3, stopIndex ) );
		}

		BoxStatement thenBody = new BoxStatementBlock( thenBodyStatements, getPosition( node.thenBody ), getSourceText( node.thenBody ) );
		// If there were no elseif's, the elsebody here will be the <bs:else>. Otherwise, it will be the last elseif.
		return new BoxIfElse( condition, thenBody, elseBody, getPosition( node ), getSourceText( node ) );
	}

	private BoxStatement toAst( File file, SetContext set ) {
		// In components, a <bx:set ...> component is an Expression Statement.
		return new BoxExpressionStatement( parseBoxExpression( set.expression().getText(), getPosition( set.expression() ) ), getPosition( set ),
		    getSourceText( set ) );
	}

	private BoxStatement toAst( File file, OutputContext node ) {
		List<BoxStatement>	statements	= new ArrayList<>();
		List<BoxAnnotation>	annotations	= new ArrayList<>();

		for ( var attr : node.attribute() ) {
			annotations.add( toAst( file, attr ) );
		}
		if ( node.statements() != null ) {
			outputCounter++;
			statements.addAll( toAst( file, node.statements() ) );
			outputCounter--;
		}

		return new BoxComponent( "output", annotations, statements, getPosition( node ), getSourceText( node ) );
	}

	/**
	 * A helper function to find a specific annotation by name and return the value expression
	 *
	 * @param annotations             the list of annotations to search
	 * @param name                    the name of the annotation to find
	 * @param required                whether the annotation is required. If required, and not present a parsing Issue is created.
	 * @param defaultValue            the default value to return if the annotation is not found. Ignored if requried is false.
	 * @param containingComponentName the name of the component that contains the annotation, used in error handling
	 * @param position                the position of the component, used in error handling
	 *
	 * @return the value expression of the annotation, or the default value if the annotation is not found
	 *
	 */
	private BoxExpression findExprInAnnotations( List<BoxAnnotation> annotations, String name, boolean required, BoxExpression defaultValue,
	    String containingComponentName,
	    Position position ) {
		var search = annotations.stream().filter( ( it ) -> it.getKey().getValue().equalsIgnoreCase( name ) ).findFirst();
		if ( search.isPresent() ) {
			return search.get().getValue();
		} else if ( !required ) {
			return defaultValue;
		} else {
			issues.add( new Issue( "Missing " + name + " attribute on " + containingComponentName + " component", position ) );
			return new BoxNull( null, null );
		}

	}

	/**
	 * A helper function to take a BoxExpr and return the value expression as a string.
	 * If the expression is not a string literal, an Issue is created.
	 *
	 * @param expr       the expression to get the value from
	 * @param name       the name of the attribute, used in error handling
	 * @param allowEmpty whether an empty string is allowed. If not allowed, an Issue is created.
	 *
	 * @return the value of the expression as a string, or null if the expression is null
	 */
	private String getBoxExprAsString( BoxExpression expr, String name, boolean allowEmpty ) {
		if ( expr == null ) {
			return null;
		}
		if ( expr instanceof BoxStringLiteral str ) {
			if ( !allowEmpty && str.getValue().trim().isEmpty() ) {
				issues.add( new Issue( "Attribute [" + name + "] cannot be empty", expr.getPosition() ) );
			}
			return str.getValue();
		} else {
			issues.add( new Issue( "Attribute [" + name + "] attribute must be a string literal", expr.getPosition() ) );
			return "";
		}
	}

	private List<BoxStatement> toAst( File file, BoxTemplateGrammar.TextContentContext node ) {
		List<BoxStatement>		statements	= new ArrayList<>();
		List<ParserRuleContext>	nodes		= new ArrayList<>();
		boolean					allLiterals	= true;

		for ( var child : node.children ) {
			if ( child instanceof BoxTemplateGrammar.InterpolatedExpressionContext intrpexpr && intrpexpr.expression() != null ) {
				allLiterals = false;
				nodes.add( intrpexpr );
			} else if ( child instanceof BoxTemplateGrammar.NonInterpolatedTextContext strlit ) {
				nodes.add( strlit );
			} else if ( child instanceof BoxTemplateGrammar.CommentContext ) {
				if ( !nodes.isEmpty() ) {
					statements.add( processTextContent( file, nodes, allLiterals ) );
					allLiterals = true;
					nodes.clear();
				}
			}
		}
		if ( !nodes.isEmpty() ) {
			statements.add( processTextContent( file, nodes, allLiterals ) );
		}
		return statements;
	}

	private BoxStatement processTextContent( File file, List<ParserRuleContext> nodes, boolean allLiterals ) {
		BoxExpression	expr;
		Position		pos			= getPosition( nodes.get( 0 ), nodes.get( nodes.size() - 1 ) );
		String			sourceText	= getSourceText( nodes.get( 0 ), nodes.get( nodes.size() - 1 ) );
		// No interpolated nodes, only string
		if ( allLiterals ) {
			expr = new BoxStringLiteral(
			    // combine all the literal strings down into one
			    escapeStringLiteral( nodes.stream().map( n -> n.getText() ).collect( Collectors.joining( "" ) ) ),
			    pos,
			    sourceText
			);
		} else {
			List<BoxExpression> expressions = new ArrayList<>();
			for ( var child : nodes ) {
				if ( child instanceof BoxTemplateGrammar.InterpolatedExpressionContext intrpexpr && intrpexpr.expression() != null ) {
					// parse the text between the hash signs as an expression
					expressions.add( toAst( file, intrpexpr ) );
				} else if ( child instanceof BoxTemplateGrammar.NonInterpolatedTextContext strlit ) {
					expressions.add( new BoxStringLiteral( escapeStringLiteral( strlit.getText() ), getPosition( strlit ), getSourceText( strlit ) ) );
				}
			}
			expr = new BoxStringInterpolation( expressions, pos, sourceText );
		}
		return new BoxBufferOutput( expr, pos, sourceText );
	}

	/**
	 * Escape pounds in a string literal
	 *
	 * @param string the string to escape
	 *
	 * @return the escaped string
	 */
	private String escapeStringLiteral( String string ) {
		return string.replace( "##", "#" );
	}

	public BoxExpression parseBoxExpression( String code, Position position ) {
		try {
			ParsingResult result = new BoxScriptParser( position.getStart().getLine(), position.getStart().getColumn() )
			    .setSource( sourceToParse )
			    .setSubParser( true )
			    .parseExpression( code );
			this.comments.addAll( result.getComments() );
			if ( result.getIssues().isEmpty() ) {
				return ( BoxExpression ) result.getRoot();
			} else {
				// Add these issues to the main parser
				issues.addAll( result.getIssues() );
				return new BoxNull( null, null );
			}
		} catch ( IOException e ) {
			issues.add( new Issue( "Error parsing interpolated expression " + e.getMessage(), position ) );
			return new BoxNull( null, null );
		}
	}

	public List<BoxStatement> parseBoxStatements( String code, Position position ) {
		try {
			ParsingResult result = new BoxScriptParser( position.getStart().getLine(), position.getStart().getColumn(), ( outputCounter > 0 ) )
			    .setSource( sourceToParse )
			    .setSubParser( true )
			    .parse( code );
			this.comments.addAll( result.getComments() );
			if ( result.getIssues().isEmpty() ) {
				BoxNode root = result.getRoot();
				if ( root instanceof BoxScript script ) {
					return script.getStatements();
				} else if ( root instanceof BoxStatement statement ) {
					return List.of( statement );
				} else {
					issues.add( new Issue( "Unexpected root node type [" + root.getClass().getName() + "] in script island.", position ) );
					return List.of();
				}
			} else {
				// Add these issues to the main parser
				issues.addAll( result.getIssues() );
				return List.of( new BoxExpressionStatement( new BoxNull( null, null ), null, null ) );
			}
		} catch ( IOException e ) {
			issues.add( new Issue( "Error parsing interpolated expression " + e.getMessage(), position ) );
			return List.of();
		}
	}

	@Override
	BoxTemplateParser setSource( Source source ) {
		if ( this.sourceToParse != null ) {
			return this;
		}
		this.sourceToParse = source;
		this.errorListener.setSource(this.sourceToParse);
		return this;
	}

	@Override
	public BoxTemplateParser setSubParser( boolean subParser ) {
		this.subParser = subParser;
		return this;
	}
}
