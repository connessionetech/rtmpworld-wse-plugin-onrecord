package com.rtmpworld.server.wowza.pluginsvodprocessorscript.executors;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.rtmpworld.server.wowza.plugins.ModuleVODProcessorScript;
import com.rtmpworld.server.wowza.plugins.vodprocessorscript.interfaces.IScriptExecutor;
import com.rtmpworld.server.wowza.utils.WowzaUtils;
import com.rtmpworld.server.wowza.webrtc.constants.OSType;
import com.wowza.wms.logging.WMSLoggerFactory;

public class ScriptExecutor implements IScriptExecutor {

	
	// for threading
	private static String PROP_THREADPOOL_SIZE = ModuleVODProcessorScript.PROP_NAME_PREFIX + "ThreadPoolSize";
	private static String PROP_DELAY_FOR_FAILED_REQUESTS = ModuleVODProcessorScript.PROP_NAME_PREFIX + "DelayForFailedRequests";
	private static String PROP_THREADPOOL_TERMINATION_TIMEOUT = ModuleVODProcessorScript.PROP_NAME_PREFIX + "ThreadPoolTerminationTimeout";
	
	
	private static ThreadPoolExecutor eventRequestThreadPool;
	private static int threadPoolSize;
	private static int threadIdleTimeout;		
	
	private static int threadPoolAwaitTerminationTimeout;
	private static boolean serverDebug;
	
	
	
	static  
	{
		serverDebug = ModuleVODProcessorScript.serverProps.getPropertyBoolean(ModuleVODProcessorScript.PROP_DEBUG, false);
		if (WMSLoggerFactory.getLogger(ModuleVODProcessorScript.class).isDebugEnabled())
			serverDebug = true;
		
		threadPoolSize = ModuleVODProcessorScript.serverProps.getPropertyInt(PROP_THREADPOOL_SIZE, 5);
		threadIdleTimeout = ModuleVODProcessorScript.serverProps.getPropertyInt(PROP_DELAY_FOR_FAILED_REQUESTS, 60);
		threadPoolAwaitTerminationTimeout = ModuleVODProcessorScript.serverProps.getPropertyInt(PROP_THREADPOOL_TERMINATION_TIMEOUT, 5);
		eventRequestThreadPool = new ThreadPoolExecutor(threadPoolSize, threadPoolSize, threadIdleTimeout, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

		Runtime.getRuntime().addShutdownHook(new Thread()
		{
			@Override
			public void run()
			{
				try
				{
					if (serverDebug)
						WMSLoggerFactory.getLogger(getClass()).info(ModuleVODProcessorScript.MODULE_NAME + " Runtime.getRuntime().addShutdownHook");
					eventRequestThreadPool.shutdown();
					if (!eventRequestThreadPool.awaitTermination(threadPoolAwaitTerminationTimeout, TimeUnit.SECONDS))
						eventRequestThreadPool.shutdownNow();
				}
				catch (InterruptedException e)
				{
					// problem
					WMSLoggerFactory.getLogger(ModuleVODProcessorScript.class).error(ModuleVODProcessorScript.MODULE_NAME + ".ShutdownHook.run() InterruptedException", e);
				}
			}
		});
	}
	
	
	
	
	private Process buildExecutingProcess(String scriptPath, List<String> params, String workingDir) throws IOException
	{
		String os = WowzaUtils.getOS();
		WMSLoggerFactory.getLogger(ModuleVODProcessorScript.class).info("Building executable process for {}", os);
		
				
		List<String> list = new ArrayList<String>();  
		
		if(os == OSType.WINDOWS)
		{
			list.add("cmd.exe");
			list.add( "/C");
		}
		
        list.add(scriptPath); // batch or sh
        
        for(String param : params) {
        	list.add(param);
        }        

		ProcessBuilder processBuilder = new ProcessBuilder();
		if(workingDir != null) {
			processBuilder.directory(new File(workingDir));
		}
		processBuilder.command(list);
		return processBuilder.start();
	}
		
	
	
	public CompletableFuture<Integer> execute(String scriptPath, List<String> params) throws IOException
	{
		return execute(scriptPath, params, null);
	}
	
	
	
	public CompletableFuture<Integer> execute(String scriptPath, List<String> params, String workingDir) throws IOException
	{
		WMSLoggerFactory.getLogger(ModuleVODProcessorScript.class).info("Executing script {}", scriptPath);
		File script = new File(scriptPath);
		if(!script.exists()) {
			throw new FileNotFoundException("Script not found at " + scriptPath);
		}
		
		return executeScript(scriptPath, params, workingDir);
	}
	
	
	
	private CompletableFuture<Integer> executeScript(String scriptPath, List<String> params, String workingDir)
	{
				
		return CompletableFuture.supplyAsync(()->{
			
			int exit_code = 0;
			
			try 
			{
				Process proc = buildExecutingProcess(scriptPath, params, workingDir);
				BufferedReader reader =
	                    new BufferedReader(new InputStreamReader(proc.getInputStream()));

	            String line;
	            while ((line = reader.readLine()) != null) {
	            	WMSLoggerFactory.getLogger(ModuleVODProcessorScript.class).info(line);
	            }

	            exit_code = proc.waitFor();
			} 
			catch (IOException | InterruptedException e) 
			{
				WMSLoggerFactory.getLogger(ModuleVODProcessorScript.class).error("An error occurred while executing script {}", e);
			}
			
		    return exit_code;		
			
		}, eventRequestThreadPool);
	}

}
