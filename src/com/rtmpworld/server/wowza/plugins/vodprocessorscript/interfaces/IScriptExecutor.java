package com.rtmpworld.server.wowza.plugins.vodprocessorscript.interfaces;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface IScriptExecutor {
	
	CompletableFuture<Integer> execute(String scriptPath, List<String> params) throws IOException;
	CompletableFuture<Integer> execute(String scriptPath, List<String> params, String workingDir) throws IOException;

}
