package com.rtmpworld.server.wowza.plugins;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import com.rtmpworld.server.wowza.pluginsvodprocessorscript.executors.ScriptExecutor;
import com.wowza.wms.application.*;


import com.wowza.wms.logging.WMSLogger;
import com.wowza.wms.module.*;
import com.wowza.wms.server.Server;
import com.wowza.wms.stream.*;



public class ModuleVODProcessorScript extends ModuleBase {
	
	public static String MODULE_NAME = "ModuleVODProcessorScript";
	
	// module name and property name prefix
	public static String PROP_NAME_PREFIX = "vodprocessorscript";

	
	// for logging
	public static String PROP_DEBUG = PROP_NAME_PREFIX + "Debug";	
	
	
	public static String PROP_SCRIPT_WORKING_DIR = PROP_NAME_PREFIX + "ScriptWorkingDir";
	public static String PROP_RECORD_START_SCRIPT = PROP_NAME_PREFIX + "RecordStartScript";
	public static String PROP_RECORD_STOP_SCRIPT = PROP_NAME_PREFIX + "RecordStopScript";
	
	private IApplicationInstance appInstance;
	private boolean moduleDebug;
	private String workingScriptDir;
	private String recordStartScript;
	private String recordStopScript;
	
	public static WMSProperties serverProps = Server.getInstance().getProperties();

	
	private WMSLogger logger;
	private StreamListener streamListener = new StreamListener();
	private ScriptExecutor scriptExecutor = new ScriptExecutor();
	
	
	class StreamListener extends MediaStreamActionNotifyBase
	{
		
		@Override
		public void onPause(IMediaStream stream, boolean isPause, double location)
		{
			if(moduleDebug){
				logger.info(MODULE_NAME + ".onPause => " + stream.getName());
			}
		}
		
		
		@Override
		public void onStop(IMediaStream stream)
		{
			if(moduleDebug){
				logger.info(MODULE_NAME + ".onStop => " + stream.getName());
			}
		}
		

		@Override
		public void onPlay(IMediaStream stream, String streamName, double playStart, double playLen, int playReset) {
				
			if(moduleDebug){
				logger.info(MODULE_NAME + ".onPlay => " + stream.getName());
			}
		}
		

		@Override
		public void onPublish(IMediaStream stream, String streamName, boolean isRecord, boolean isAppend)
		{
			if(moduleDebug){
				logger.info(MODULE_NAME + ".onPublish => " + stream.getName());
			}
			
			if (appInstance.getMediaCasterStreams().getMediaCaster(streamName) != null)
				return;
			
			if(isRecord) {
				if(recordStartScript != null && String.valueOf(recordStartScript) != "") {
					try {
						CompletableFuture<Integer> future = scriptExecutor.execute(streamName, recordStartScript);
						future.thenAccept(value -> {
							
							logger.info("Script execution exited with code: {}", value);
							
						});
					} catch (IOException e) {
						logger.error("An rror occurred executing script {}", e);						
					}
				}
			}
		}



		@Override
		public void onUnPublish(IMediaStream stream, String streamName, boolean isRecord, boolean isAppend)
		{
			if(moduleDebug){
				logger.info(MODULE_NAME + ".onUnPublish => " + stream.getName());
			}
			
			if (appInstance.getMediaCasterStreams().getMediaCaster(streamName) != null)
				return;
			
			if(isRecord) {
				if(recordStopScript != null && String.valueOf(recordStopScript) != "") {
					
					try {
						CompletableFuture<Integer> future = scriptExecutor.execute(streamName, recordStopScript);
						future.thenAccept(value -> {
							
							logger.info("Script execution exited with code: {}", value);
							
						});
					} catch (IOException e) {
						logger.error("An rror occurred executing script {}", e);
					}
				}
			}
		}
	}	
	
	
	public void onAppCreate(IApplicationInstance appInstance)
	{
		this.logger = getLogger();
		this.appInstance = appInstance;
		
		if(moduleDebug){
			this.logger.info(MODULE_NAME + ".onAppCreate");
		}
		
		this.readProperties();
		
	}


	public void onStreamCreate(IMediaStream stream) {
		getLogger().info("onStreamCreate: " + stream.getSrc());
		stream.addClientListener(streamListener);
	}

	
	public void onStreamDestroy(IMediaStream stream) {
		getLogger().info("onStreamDestroy: " + stream.getSrc());
		stream.removeClientListener(streamListener);
	}
	
	
	
	/**
	 * Read application properties from configuration
	 */
	private void readProperties()
	{
		getLogger().info(MODULE_NAME + ".readProperties => reading properties");
		
		try
		{
			moduleDebug = getPropertyValueBoolean(PROP_DEBUG, false);
			ModuleRemoteUsernamePasswordProvider.moduleDebug = moduleDebug;
			if(moduleDebug){
				getLogger().info(MODULE_NAME + ".readProperties moduleDebug mode : " + String.valueOf(moduleDebug));
			}
			
			
				
			workingScriptDir = getPropertyValueStr(PROP_SCRIPT_WORKING_DIR, null);
			if(moduleDebug){
				getLogger().info(MODULE_NAME + ".readProperties workingScriptDir : " + String.valueOf(workingScriptDir));
			}
		
			
			recordStartScript = getPropertyValueStr(PROP_RECORD_START_SCRIPT, null);
			if(moduleDebug){
				getLogger().info(MODULE_NAME + ".recordStartScript : " + String.valueOf(recordStartScript));

			}
			
			
			try
			{
				recordStopScript = getPropertyValueStr(PROP_RECORD_STOP_SCRIPT, null);
				if(moduleDebug){
					getLogger().info(MODULE_NAME + ".readProperties recordStopScript : " + String.valueOf(recordStopScript));
				}
							
			}
			catch(Exception e)
			{
				getLogger().error(MODULE_NAME + ".readProperties error reading authenticationEndpoint."+e.getMessage());
			}

		}
		catch(Exception e)
		{
			getLogger().error(MODULE_NAME + " Error reading properties {}", e);
		}
	}
	
	
	/**
	 * Retrieves a server property value as a string
	 * 
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	private String getPropertyValueStr(String key, String defaultValue)
	{
		String value = serverProps.getPropertyStr(key, defaultValue);
		value = appInstance.getProperties().getPropertyStr(key, value);
		return value;
	}
	
	
	
	/**
	 * Retrieves a server property value as a boolean
	 * 
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	private boolean getPropertyValueBoolean(String key, boolean defaultValue)
	{
		boolean value = serverProps.getPropertyBoolean(key, defaultValue);
		value = appInstance.getProperties().getPropertyBoolean(key, value);
		return value;
	}
	

}
