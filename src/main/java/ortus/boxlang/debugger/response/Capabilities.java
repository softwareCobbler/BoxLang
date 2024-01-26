package ortus.boxlang.debugger.response;

public class Capabilities {

	/**
	 * The debug adapter supports the `configurationDone` request.
	 */
	public boolean	supportsConfigurationDoneRequest		= true;
	/**
	 * The debug adapter supports function breakpoints.
	 */
	public boolean	supportsFunctionBreakpoints				= true;
	/**
	 * The debug adapter supports conditional breakpoints.
	 */
	public boolean	supportsConditionalBreakpoints			= true;
	/**
	 * The debug adapter supports breakpoints that break execution after a
	 * specified number of hits.
	 */
	public boolean	supportsHitConditionalBreakpoints		= true;
	/**
	 * The debug adapter supports a (side effect free) `evaluate` request for data
	 * hovers.
	 */
	public boolean	supportsEvaluateForHovers				= false;
	/**
	 * Available exception filter options for the `setExceptionBreakpoints`
	 * request.
	 */
	// booleanexceptionBreakpointFilters?: ExceptionBreakpointsFilter[];

	/**
	 * The debug adapter supports stepping back via the `stepBack` and
	 * `reverseContinue` requests.
	 */
	public boolean	supportsStepBack						= false;
	/**
	 * The debug adapter supports setting a variable to a value.
	 */
	public boolean	supportsSetVariable						= false;
	/**
	 * The debug adapter supports restarting a frame.
	 */
	public boolean	supportsRestartFrame					= false;
	/**
	 * The debug adapter supports the `gotoTargets` request.
	 */
	public boolean	supportsGotoTargetsRequest				= false;
	/**
	 * The debug adapter supports the `stepInTargets` request.
	 */
	public boolean	supportsStepInTargetsRequest			= false;
	/**
	 * The debug adapter supports the `completions` request.
	 */
	public boolean	supportsCompletionsRequest				= false;
	/**
	 * The set of characters that should trigger completion in a REPL. If not
	 * specified, the UI should assume the `.` character.
	 */
	// completionTriggerCharacters?: string[];

	/**
	 * The debug adapter supports the `modules` request.
	 */
	public boolean	supportsModulesRequest					= false;
	/**
	 * The set of additional module information exposed by the debug adapter.
	 */
	// additionalModuleColumns?: ColumnDescriptor[];

	/**
	 * Checksum algorithms supported by the debug adapter.
	 */
	// supportedChecksumAlgorithms?: ChecksumAlgorithm[];

	/**
	 * The debug adapter supports the `restart` request. In this case a client
	 * should not implement `restart` by terminating and relaunching the adapter
	 * but by calling the `restart` request.
	 */
	public boolean	supportsRestartRequest					= false;
	/**
	 * The debug adapter supports `exceptionOptions` on the
	 * `setExceptionBreakpoints` request.
	 */
	public boolean	supportsExceptionOptions				= false;
	/**
	 * The debug adapter supports a `format` attribute on the `stackTrace`,
	 * `variables`, and `evaluate` requests.
	 */
	public boolean	supportsValueFormattingOptions			= false;
	/**
	 * The debug adapter supports the `exceptionInfo` request.
	 */
	public boolean	supportsExceptionInfoRequest			= false;
	/**
	 * The debug adapter supports the `terminateDebuggee` attribute on the
	 * `disconnect` request.
	 */
	public boolean	supportTerminateDebuggee				= false;
	/**
	 * The debug adapter supports the `suspendDebuggee` attribute on the
	 * `disconnect` request.
	 */
	public boolean	supportSuspendDebuggee					= false;
	/**
	 * The debug adapter supports the delayed loading of parts of the stack, which
	 * requires that both the `startFrame` and `levels` arguments and the
	 * `totalFrames` result of the `stackTrace` request are supported.
	 */
	public boolean	supportsDelayedStackTraceLoading		= false;
	/**
	 * The debug adapter supports the `loadedSources` request.
	 */
	public boolean	supportsLoadedSourcesRequest			= false;
	/**
	 * The debug adapter supports log points by interpreting the `logMessage`
	 * attribute of the `SourceBreakpoint`.
	 */
	public boolean	supportsLogPoints						= false;
	/**
	 * The debug adapter supports the `terminateThreads` request.
	 */
	public boolean	supportsTerminateThreadsRequest			= false;
	/**
	 * The debug adapter supports the `setExpression` request.
	 */
	public boolean	supportsSetExpression					= false;
	/**
	 * The debug adapter supports the `terminate` request.
	 */
	public boolean	supportsTerminateRequest				= false;
	/**
	 * The debug adapter supports data breakpoints.
	 */
	public boolean	supportsDataBreakpoints					= true;
	/**
	 * The debug adapter supports the `readMemory` request.
	 */
	public boolean	supportsReadMemoryRequest				= false;
	/**
	 * The debug adapter supports the `writeMemory` request.
	 */
	public boolean	supportsWriteMemoryRequest				= false;
	/**
	 * The debug adapter supports the `disassemble` request.
	 */
	public boolean	supportsDisassembleRequest				= false;
	/**
	 * The debug adapter supports the `cancel` request.
	 */
	public boolean	supportsCancelRequest					= false;
	/**
	 * The debug adapter supports the `breakpointLocations` request.
	 */
	public boolean	supportsBreakpointLocationsRequest		= true;
	/**
	 * The debug adapter supports the `clipboard` context value in the `evaluate`
	 * request.
	 */
	public boolean	supportsClipboardContext				= false;
	/**
	 * The debug adapter supports stepping granularities (argument `granularity`)
	 * for the stepping requests.
	 */
	public boolean	supportsSteppingGranularity				= false;
	/**
	 * The debug adapter supports adding breakpoints based on instruction
	 * references.
	 */
	public boolean	supportsInstructionBreakpoints			= true;
	/**
	 * The debug adapter supports `filterOptions` as an argument on the
	 * `setExceptionBreakpoints` request.
	 */
	public boolean	supportsExceptionFilterOptions			= false;
	/**
	 * The debug adapter supports the `singleThread` property on the execution
	 * requests (`continue`, `next`, `stepIn`, `stepOut`, `reverseContinue`,
	 * `stepBack`).
	 */
	public boolean	supportsSingleThreadExecutionRequests	= false;
}
