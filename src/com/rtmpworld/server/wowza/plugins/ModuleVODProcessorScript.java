package com.rtmpworld.server.wowza.plugins;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
	public static String PROP_RECORD_COMPLETE_SCRIPT = PROP_NAME_PREFIX + "RecordCompleteScript";
	
	private IApplicationInstance appInstance;
	private boolean moduleDebug;
	private String workingScriptDir;
	private String recordStartScript;
	private String recordStopScript;
	private String recordCompleteScript;
	
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
						
						List<String> params = new ArrayList<String>();
						params.add(streamName);
						
						CompletableFuture<Integer> future = scriptExecutor.execute(recordStartScript, params);
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
						List<String> params = new ArrayList<String>();
						params.add(streamName);
						
						CompletableFuture<Integer> future = scriptExecutor.execute(recordStopScript, params);
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
	
	
	private class WriteListener implements IMediaWriterActionNotify
	{

		@Override
		public void onWriteComplete(IMediaStream stream, File file)
		{
			if(recordStopScript != null && String.valueOf(recordStopScript) != "") {
				
				String streamName = stream.getName();
				String recording_path = file.getAbsolutePath();
				
				try {					
					List<String> params = new ArrayList<String>();
					params.add(streamName);
					
					CompletableFuture<Integer> future = scriptExecutor.execute(recordStopScript, params);
					future.thenAccept(value -> {
						
						logger.info("Script execution exited with code: {}", value);
						
					});
				} catch (IOException e) {
					logger.error("An rror occurred executing script {}", e);
				}
			}
		}


		@Override
		public void onFLVAddMetadata(IMediaStream arg0, Map<String, Object> arg1) {
			// TODO Auto-generated method stub
			
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
		this.appInstance.addMediaWriterListener(new WriteListener());
		
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
			
			
			if(moduleDebug){
				getLogger().info(MODULE_NAME + ".readProperties moduleDebug mode : " + String.valueOf(moduleDebug));
			}	

			
			try
			{
				workingScriptDir = getPropertyValueStr(PROP_SCRIPT_WORKING_DIR, null);
				if(moduleDebug){
					getLogger().info(MODULE_NAME + ".readProperties workingScriptDir : " + String.valueOf(workingScriptDir));
				}
							
			}
			catch(Exception e)
			{
				getLogger().error(MODULE_NAME + ".readProperties error reading workingScriptDir."+e.getMessage());
			}
			
		
			
			
			try
			{
				recordStartScript = getPropertyValueStr(PROP_RECORD_START_SCRIPT, null);
				if(moduleDebug){
					getLogger().info(MODULE_NAME + ".recordStartScript : " + String.valueOf(recordStartScript));

				}
							
			}
			catch(Exception e)
			{
				getLogger().error(MODULE_NAME + ".readProperties error reading recordStartScript."+e.getMessage());
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
				getLogger().error(MODULE_NAME + ".readProperties error reading recordStopScript."+e.getMessage());
			}

			
			
			try
			{
				recordCompleteScript = getPropertyValueStr(PROP_RECORD_COMPLETE_SCRIPT, null);
				if(moduleDebug){
					getLogger().info(MODULE_NAME + ".readProperties recordCompleteScript : " + String.valueOf(recordCompleteScript));
				}
							
			}
			catch(Exception e)
			{
				getLogger().error(MODULE_NAME + ".readProperties error reading recordCompleteScript."+e.getMessage());
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
