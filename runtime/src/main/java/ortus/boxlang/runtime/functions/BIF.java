package ortus.boxlang.runtime.functions;

import ortus.boxlang.runtime.context.IBoxContext;

public abstract class BIF {

	public static void invoke( IBoxContext context, Object... arguments ) throws RuntimeException {
		throw new UnsupportedOperationException( "Please implement the function" );
	}

}
