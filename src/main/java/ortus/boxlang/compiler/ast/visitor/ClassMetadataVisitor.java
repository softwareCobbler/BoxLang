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
package ortus.boxlang.compiler.ast.visitor;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import ortus.boxlang.compiler.ast.BoxClass;
import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxInterface;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.SourceFile;
import ortus.boxlang.compiler.ast.expression.BoxArrayLiteral;
import ortus.boxlang.compiler.ast.expression.BoxFQN;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.ast.expression.BoxStructLiteral;
import ortus.boxlang.compiler.ast.expression.IBoxSimpleLiteral;
import ortus.boxlang.compiler.ast.statement.BoxAnnotation;
import ortus.boxlang.compiler.ast.statement.BoxArgumentDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxDocumentationAnnotation;
import ortus.boxlang.compiler.ast.statement.BoxFunctionDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxProperty;
import ortus.boxlang.compiler.ast.statement.BoxReturnType;
import ortus.boxlang.compiler.ast.statement.BoxType;
import ortus.boxlang.compiler.javaboxpiler.transformer.BoxClassTransformer;
import ortus.boxlang.compiler.parser.Parser;
import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.loader.ClassLocator.ClassLocation;
import ortus.boxlang.runtime.loader.resolvers.BoxResolver;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.ExpressionException;
import ortus.boxlang.runtime.types.exceptions.ParseException;
import ortus.boxlang.runtime.util.FQN;
import ortus.boxlang.runtime.util.FileSystemUtil;
import ortus.boxlang.runtime.util.ResolvedFilePath;

/**
 * I generate metadata for a class or interface based on the AST without needing to instantiate or even compile the code
 */
public class ClassMetadataVisitor extends VoidBoxVisitor {

	private IStruct		meta		= Struct.of(
	    Key._NAME, null,
	    Key.nameAsKey, null,
	    Key.documentation, Struct.of(),
	    Key.annotations, Struct.of(),
	    Key._EXTENDS, Struct.of(),
	    Key.functions, Array.of(),
	    Key.properties, Array.of(),
	    Key.type, null,
	    Key.fullname, null,
	    Key.path, null
	);

	private boolean		accessors	= false;

	private Path		sourcePath;

	private IBoxContext	context;

	private BoxResolver	BXResolver	= BoxResolver.getInstance();

	/**
	 * Constructor
	 */
	public ClassMetadataVisitor( IBoxContext context ) {
		this.context = context;
	}

	/**
	 * Constructor
	 */
	public ClassMetadataVisitor() {
		this.context = BoxRuntime.getInstance().getRuntimeContext();
	}

	/**
	 * Get the metadata generated by this visitor
	 * 
	 * @return The metadata
	 */
	public IStruct getMetadata() {
		return meta;
	}

	public void visit( BoxClass node ) {
		meta.put( Key.type, "class" );
		meta.put( Key._NAME, "" );
		meta.put( Key.fullname, "" );
		meta.put( Key.nameAsKey, Key.of( "" ) );
		meta.put( Key.annotations, processAnnotations( node.getAnnotations() ) );
		meta.put( Key.documentation, processDocumentation( node.getDocumentation() ) );
		Object accessorsAnnotation = meta.getAsStruct( Key.annotations ).get( Key.accessors );
		if ( accessorsAnnotation != null ) {
			var boolAccessors = BooleanCaster.attempt( accessorsAnnotation );
			accessors = boolAccessors.wasSuccessful() && boolAccessors.get();
		} else {
			accessors = true;
		}
		processName( node );
		if ( meta.getAsStruct( Key.annotations ).containsKey( Key._EXTENDS ) ) {
			meta.put( Key._EXTENDS, processSuper( meta.getAsStruct( Key.annotations ).getAsString( Key._EXTENDS ) ) );
		}
		super.visit( node );
	}

	private IStruct processSuper( String superName ) {
		if ( this.sourcePath != null ) {
			context.pushTemplate( ResolvedFilePath.of( this.sourcePath ) );
		}
		Optional<ClassLocation> superLookup = BXResolver.resolve( context, superName, false );
		if ( this.sourcePath != null ) {
			context.popTemplate();
		}
		if ( !superLookup.isPresent() ) {
			throw new BoxRuntimeException( "Super class [" + superName + "] not found" );
		}
		String			superDiskPath	= superLookup.get().path();

		ParsingResult	result			= new Parser().parse( Paths.get( superDiskPath ).toAbsolutePath().toFile() );
		if ( !result.isCorrect() ) {
			throw new ParseException( result.getIssues(), "" );
		}
		ClassMetadataVisitor visitor = new ClassMetadataVisitor();
		result.getRoot().accept( visitor );

		return visitor.getMetadata();
	}

	private void processName( BoxNode node ) {
		if ( node.getPosition() != null && node.getPosition().getSource() != null && node.getPosition().getSource() instanceof SourceFile sf ) {
			File sourceFile = sf.getFile();
			this.sourcePath = sourceFile.toPath();
			var		contractedPath	= FileSystemUtil.contractPath( context, sourceFile.toString() );
			String	name			= sourceFile.getName().replaceFirst( "[.][^.]+$", "" );
			String	packageName		= FQN.of( Paths.get( contractedPath.relativePath() ) ).getPackageString();
			String	fullName		= packageName.length() > 0 ? packageName + "." + name : name;
			meta.put( Key._NAME, name );
			meta.put( Key.fullname, fullName );
			meta.put( Key.nameAsKey, Key.of( fullName ) );
			meta.put( Key.path, sourceFile.getAbsolutePath() );
		}
	}

	public void visit( BoxInterface node ) {
		meta.put( Key.type, "interface" );
		meta.put( Key._NAME, "" );
		meta.put( Key.fullname, "" );
		meta.put( Key.nameAsKey, Key.of( "" ) );
		processName( node );
		super.visit( node );
	}

	public void visit( BoxProperty prop ) {
		List<BoxAnnotation>	finalAnnotations	= BoxClassTransformer.normlizePropertyAnnotations( prop );

		BoxAnnotation		nameAnnotation		= finalAnnotations.stream().filter( it -> it.getKey().getValue().equalsIgnoreCase( "name" ) ).findFirst()
		    .orElseThrow( () -> new ExpressionException( "Property [" + prop.getSourceText() + "] missing name annotation", prop ) );
		BoxAnnotation		typeAnnotation		= finalAnnotations.stream().filter( it -> it.getKey().getValue().equalsIgnoreCase( "type" ) ).findFirst()
		    .orElseThrow( () -> new ExpressionException( "Property [" + prop.getSourceText() + "] missing type annotation", prop ) );
		BoxAnnotation		defaultAnnotation	= finalAnnotations.stream().filter( it -> it.getKey().getValue().equalsIgnoreCase( "default" ) ).findFirst()
		    .orElse( null );

		String				name				= getBoxExprAsSimpleValue( nameAnnotation.getValue() ).toString();
		String				type				= getBoxExprAsSimpleValue( typeAnnotation.getValue() ).toString();
		meta.getAsArray( Key.properties ).add(
		    Struct.of(
		        Key._NAME, name,
		        Key.nameAsKey, Key.of( name ),
		        Key.type, type,
		        Key.defaultValue, getBoxExprAsSimpleValue( defaultAnnotation.getValue(), "[Runtime Expression]", false ),
		        Key.annotations, processAnnotations( prop.getAllAnnotations() ),
		        Key.documentation, processDocumentation( prop.getDocumentation() )
		    )
		);

		if ( accessors ) {
			meta.getAsArray( Key.functions ).add(
			    Struct.of(
			        Key._NAME, "get" + name,
			        Key.nameAsKey, Key.of( "get" + name ),
			        Key.returnType, type,
			        Key.access, "public",
			        Key.documentation, Struct.of(),
			        Key.annotations, Struct.of(),
			        Key.parameters, Array.of(),
			        Key.closure, false,
			        Key.lambda, false
			    )
			);
			meta.getAsArray( Key.functions ).add(
			    Struct.of(
			        Key._NAME, "set" + name,
			        Key.nameAsKey, Key.of( "set" + name ),
			        Key.returnType, "void",
			        Key.access, "public",
			        Key.documentation, Struct.of(),
			        Key.annotations, Struct.of(),
			        Key.parameters, processArguments( List.of( new BoxArgumentDeclaration( true, type, name, null, List.of(), List.of(), null, null ) ) ),
			        Key.closure, false,
			        Key.lambda, false
			    )
			);

		}
	}

	public void visit( BoxFunctionDeclaration func ) {
		BoxReturnType	boxReturnType	= func.getType();
		BoxType			returnType		= BoxType.Any;
		String			fqn				= null;
		if ( boxReturnType != null ) {
			returnType = boxReturnType.getType();
			if ( returnType.equals( BoxType.Fqn ) ) {
				fqn = boxReturnType.getFqn();
			}
		}
		meta.getAsArray( Key.functions ).add(
		    Struct.of(
		        Key._NAME, func.getName(),
		        Key.nameAsKey, func.getName(),
		        Key.returnType, returnType.equals( BoxType.Fqn ) ? fqn : returnType.name(),
		        Key.access, func.getAccessModifier() != null ? func.getAccessModifier().name().toLowerCase() : "public",
		        Key.documentation, processDocumentation( func.getDocumentation() ),
		        Key.annotations, processAnnotations( func.getAnnotations() ),
		        Key.parameters, processArguments( func.getArgs() ),
		        Key.closure, false,
		        Key.lambda, false
		    )
		);
	}

	private Array processArguments( List<BoxArgumentDeclaration> arguments ) {
		Array arr = Array.of();
		arguments.forEach( argument -> {
			arr.add( Struct.of(
			    Key._NAME, argument.getName(),
			    Key.nameAsKey, Key.of( argument.getName() ),
			    Key.required, argument.getRequired(),
			    Key.type, argument.getType() != null ? argument.getType() : "any",
			    Key._DEFAULT, getBoxExprAsSimpleValue( argument.getValue(), "[Runtime Expression]", false ),
			    Key.documentation, processDocumentation( argument.getDocumentation() ),
			    Key.annotations, processAnnotations( argument.getAnnotations() )
			) );
		} );
		return arr;
	}

	private IStruct processAnnotations( List<BoxAnnotation> annotations ) {
		IStruct struct = Struct.of();
		annotations.forEach( annotation -> {
			String	key		= getBoxExprAsSimpleValue( annotation.getKey() ).toString();
			Object	value	= getBoxExprAsLiteralValue( annotation.getValue() );
			struct.put( key, value );
		} );
		return struct;
	}

	private IStruct processDocumentation( List<BoxDocumentationAnnotation> documentation ) {
		IStruct struct = Struct.of();
		documentation.forEach( annotation -> {
			String	key		= getBoxExprAsSimpleValue( annotation.getKey() ).toString();
			Object	value	= getBoxExprAsSimpleValue( annotation.getValue() );
			struct.put( key, value );
		} );
		return struct;
	}

	private Object getBoxExprAsSimpleValue( BoxExpression expr ) {
		return getBoxExprAsSimpleValue( expr, null, false );
	}

	private Object getBoxExprAsSimpleValue( BoxExpression expr, Object defaultValue, boolean identifierAsText ) {
		if ( expr == null ) {
			return "";
		}
		if ( expr instanceof IBoxSimpleLiteral lit ) {
			return lit.getValue();
		}
		if ( expr instanceof BoxFQN fqn ) {
			return fqn.getValue();
		}
		if ( identifierAsText && expr instanceof BoxIdentifier id ) {
			return id.getName();
		}
		if ( defaultValue != null ) {
			return defaultValue;
		} else {
			throw new ExpressionException( "Unsupported BoxExpr type: " + expr.getClass().getSimpleName(), expr );
		}
	}

	private Object getBoxExprAsLiteralValue( BoxExpression expr ) {
		if ( expr == null ) {
			return "";
		}
		if ( expr instanceof IBoxSimpleLiteral lit ) {
			return lit.getValue();
		}
		if ( expr instanceof BoxFQN fqn ) {
			return fqn.getValue();
		}
		if ( expr instanceof BoxArrayLiteral arr ) {
			Array array = Array.of();
			arr.getValues().forEach( value -> {
				array.add( getBoxExprAsLiteralValue( value ) );
			} );
			return array;
		}
		if ( expr instanceof BoxStructLiteral str ) {
			IStruct					struct		= Struct.of();
			Iterator<BoxExpression>	iterator	= str.getValues().iterator();
			while ( iterator.hasNext() ) {
				BoxExpression key = iterator.next();
				if ( iterator.hasNext() ) {
					BoxExpression value = iterator.next();
					struct.put( Key.of( getBoxExprAsSimpleValue( key, null, true ) ), getBoxExprAsLiteralValue( value ) );
				} else {
					// Handle odd number of values
					throw new IllegalArgumentException( "Invalid number of values in BoxStructLiteral" );
				}
			}
			return struct;
		}
		throw new ExpressionException( "Non-literal value in BoxExpr type: " + expr.getClass().getSimpleName(), expr );
	}
}
