package com.rtmpworld.server.wowza.plugins.vodprocessorscript.interfaces;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public interface IScriptExecutor {
	
	CompletableFuture<Integer> execute(String streamName, String scriptPath) throws IOException;
	CompletableFuture<Integer> execute(String streamName, String scriptPath, String workingDir) throws IOException;

}
