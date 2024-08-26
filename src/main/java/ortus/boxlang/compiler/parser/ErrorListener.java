package ortus.boxlang.compiler.parser;

import java.util.List;
import java.util.Optional;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;

import ortus.boxlang.compiler.ast.Issue;
import ortus.boxlang.compiler.ast.Point;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.Source;

public class ErrorListener extends BaseErrorListener {

	public ErrorListener() {
		this.windowSize = 80;
	}

	private Source		sourceToParse;
	private String[]	sourceLines	= null;
	private List<Issue>	issues;
	private int			windowSize;
	private int			startLine	= 0;
	private int			startColumn	= 0;

	/**
	 * Creates an instance of the error listener and supplies the source code and the list of issues to populate
	 *
	 * @param issues     the list of issues to populate, which may be empty
	 * @param windowSize the size of the window to display around the offending token
	 */
	public ErrorListener( List<Issue> issues, int windowSize ) {
		this.issues		= issues;
		this.windowSize	= windowSize;
	}

	/**
	 * Creates an instance of the error listener and supplies the source code and the list of issues to populate
	 *
	 * @param source     the source code in\n separated form
	 * @param issues     the list of issues to populate, which may be empty
	 * @param windowSize the size of the window to display around the offending token
	 */
	public ErrorListener( Source source, List<Issue> issues, int windowSize ) {
		this.issues		= issues;
		this.windowSize	= windowSize;
		setSource( source );
	}

	/**
	 * Sets the source code from whence we produce error messages
	 *
	 * @param source the source code itself
	 */
	public void setSource( Source source ) {
		this.sourceToParse = source;
	}

	/**
	 * Lazy init these to avoid teh performance hit unless there is an error
	 */
	private String[] getSourceLines() {
		if ( this.sourceLines == null ) {
			this.sourceLines = this.sourceToParse.getCode().replaceAll( "\\r", "" ).split( "\n" );
		}
		return this.sourceLines;
	}

	/**
	 * Resets the error listener, clearing any previous issues
	 */
	public void reset() {
		issues.clear();
	}

	/**
	 * Returns the number of errors that have been detected
	 *
	 * @return the number of errors that have been detected
	 */
	public int getErrorCount() {
		return issues.size();
	}

	/**
	 * Returns true if there are any errors
	 *
	 * @return true if any errors have been recorded
	 */
	public boolean hasErrors() {
		return !issues.isEmpty();
	}

	/**
	 * Informs the error handler that we are doing a partial parse starting at a particular line
	 *
	 * @param startLine the line number where the partial parse starts
	 */
	public void setStartLine( int startLine ) {
		this.startLine = startLine;
	}

	/**
	 * Informs the error handler that we are doing a partial parse starting at a particular column, usually
	 * used alongside setStartLine
	 *
	 * @param startColumn the column number where the partial parse starts
	 */
	public void setStartColumn( int startColumn ) {
		this.startColumn = startColumn;
	}

	/**
	 * Sets the list of issues to populate
	 *
	 * @param issues the list of issues to populate
	 */
	public void setIssues( List<Issue> issues ) {
		this.issues = issues;
	}

	/**
	 * Sets the window size to display around the offending token
	 *
	 * @param windowSize the window size to display around the offending token
	 */
	public void setWindowSize( int windowSize ) {
		this.windowSize = windowSize;
	}

	/**
	 * This method is called by the parser when an error is detected, and we install it in place of the
	 * standard console error listener that Antlr provides. This allows us, in conjunction with a custom
	 * error strategy, to provide more pertinent error messages to the user.
	 *
	 * @param recognizer         the parser that detected the error
	 * @param offendingSymbol    the token that caused the error, when it can be identified
	 * @param line               the line number where the error occurred
	 * @param charPositionInLine the character position within the line where the error occurred
	 * @param msg                the message generated by the custom error strategy
	 * @param e                  the exception that caused the error, if there was one
	 */
	@Override
	public void syntaxError( Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e ) {

		var tokenLength = Optional.ofNullable( ( Token ) offendingSymbol )
		    .map( token -> token.getStopIndex() - token.getStartIndex() + 1 )
		    .orElse( 1 );
		this.issues
		    .add( genIssue( line, charPositionInLine, msg, tokenLength ) );
	}

	/**
	 * A semantic error is an error that occurs after the parser has successfully parsed the input, but the tree walk
	 * or some other process has detected an error in the code. we use the given position to create a nicely formatted
	 * description of the error, with the offending source code highlighted.
	 *
	 * @param msg      The message that should accompany the error highlighting
	 * @param position the position in the source code where the error was detected
	 */
	public void semanticError( String msg, Position position ) {

		// A semantic error could span multiple lines, but we don't want to try and highlight
		// all of them. So we use the start position as the error start and the end position column
		// if it is the same line as the start.
		var	startLine	= position.getStart().getLine();
		var	endLine		= position.getEnd().getLine();
		var	startColumn	= position.getStart().getColumn();
		var	endColumn	= startLine == endLine ? position.getEnd().getColumn() : getSourceLines()[ startLine - 1 ].length();
		var	length		= endColumn - startColumn;
		if ( length <= 0 ) {
			length = 1;
		}
		this.issues.add( genIssue( startLine, startColumn, msg, length ) );
	}

	/**
	 * Creates a nicely formatted error message for the user, with the offending source code highlighted
	 *
	 * @param line               the line number where the error occurred
	 * @param charPositionInLine the character position within the line where the error occurred
	 * @param msg                the message generated by the custom error strategy
	 * @param tokenLength        the length of the input that caused the error
	 *
	 * @return a standard BoxLang Issue object
	 */
	private Issue genIssue( int line, int charPositionInLine, String msg, int tokenLength ) {
		String		errorMessage	= msg != null ? msg : "almost, but not quite, entirely unlike tea.";
		Position	position		= new Position( new Point( line + startLine, charPositionInLine + startColumn ),
		    new Point( line + startLine, charPositionInLine + startColumn ), sourceToParse );

		String[]	theSourceLines	= getSourceLines();
		// We have the message as built by our ErrorStrategy, so now we create a window on the source code
		// with a marker of ^^^ underneath the text of the offending token
		if ( line < 1 )
			line = 1;
		if ( line > theSourceLines.length )
			line = theSourceLines.length;

		var		offensiveLine	= theSourceLines[ line - 1 ];
		var		fatness			= offensiveLine.length();
		var		slimness		= ( windowSize - fatness ) / 2;
		var		trimLeft		= fatness > windowSize && charPositionInLine >= fatness;
		var		trimRight		= fatness > windowSize && fatness - charPositionInLine - tokenLength >= slimness;
		var		mark			= "...";
		int		markTwain		= 0;
		String	trimmedLine		= "";

		// Work out which side of the source line we need to trim, if any. Note clauses 3 and 4 are over-specified
		// in logic for clarity.
		if ( !trimLeft && !trimRight ) {
			markTwain	= charPositionInLine;
			trimmedLine	= offensiveLine;
		} else if ( trimLeft && !trimRight ) {
			markTwain	= windowSize - ( fatness - charPositionInLine );
			trimmedLine	= mark + offensiveLine.substring( fatness - windowSize + mark.length() );
		} else if ( !trimLeft && trimRight ) {
			markTwain	= charPositionInLine;
			trimmedLine	= offensiveLine.substring( 0, windowSize - mark.length() ) + mark;
		} else if ( trimLeft && trimRight ) {

			markTwain = charPositionInLine - slimness;
			var	s		= charPositionInLine - slimness;
			var	trimmed	= offensiveLine.substring( s, Math.min( s + windowSize, fatness - 1 ) );
			trimmedLine = mark + trimmed.substring( mark.length(), mark.length() + windowSize - 2 * mark.length() ) + mark;
		}

		var fullMessage = errorMessage + "\n" + trimmedLine + "\n" + " ".repeat( markTwain ) + "^".repeat( tokenLength );

		return new Issue( fullMessage, position );
	}
}