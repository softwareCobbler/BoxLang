
package ortus.boxlang.runtime.bifs.global.struct;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.IStruct;

@BoxBIF
@BoxMember( type = BoxLangType.STRUCT )

public class StructDelete extends BIF {

	/**
	 * Constructor
	 */
	public StructDelete() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "struct", Key.struct ),
		    new Argument( true, "string", Key.key ),
		    new Argument( false, "boolean", Key.indicateNotExists, false )
		};
	}

	/**
	 * Deletes a key from a struct
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.struct The struct target
	 *
	 * @argument.key The key to delete
	 *
	 * @argument.indicateNotExsists Applies only to BIFs - will return false if the key attempted for deletion does not exist
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		IStruct	target				= arguments.getAsStruct( Key.struct );
		Key		key					= Key.of( arguments.getAsString( Key.key ) );
		Boolean	indicateNotExists	= arguments.getAsBoolean( Key.indicateNotExists );
		if ( !target.containsKey( key ) ) {
			return arguments.getAsBoolean( __isMemberExecution )
			    ? target
			    : indicateNotExists ? false : true;
		}
		target.remove( key );
		return arguments.getAsBoolean( __isMemberExecution ) ? target : true;
	}

}
