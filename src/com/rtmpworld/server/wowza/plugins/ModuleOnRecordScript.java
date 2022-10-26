package com.rtmpworld.server.wowza.plugins;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.rtmpworld.server.wowza.pluginsvodprocessorscript.executors.ScriptExecutor;
import com.rtmpworld.server.wowza.utils.WowzaUtils;
import com.wowza.wms.application.*;


import com.wowza.wms.logging.WMSLogger;
import com.wowza.wms.module.*;
import com.wowza.wms.server.Server;
import com.wowza.wms.stream.*;



public class ModuleOnRecordScript extends ModuleBase {
	
	public static String RECORD_COMPLETE = "RECORD_COMPLETE";
	
	
	public static String MODULE_NAME = "ModuleOnRecordScript";
	
	// module name and property name prefix
	public static String PROP_NAME_PREFIX = "onrecord";

	
	// for logging
	public static String PROP_DEBUG = PROP_NAME_PREFIX + "Debug";	
	
	
	// module properties
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
		}



		@Override
		public void onUnPublish(IMediaStream stream, String streamName, boolean isRecord, boolean isAppend)
		{
			if(moduleDebug){
				logger.info(MODULE_NAME + ".onUnPublish => " + stream.getName());
			}
		}
	}	
	
	
	private class WriteListener implements IMediaWriterActionNotify
	{

		@Override
		public void onWriteComplete(IMediaStream stream, File file)
		{
			if(recordCompleteScript != null && String.valueOf(recordCompleteScript) != "") {
				
				String streamName = stream.getName();
				String recording_path = file.getAbsolutePath();
				
				try {	
					
					if(moduleDebug){
						logger.info(MODULE_NAME + ".onWriteComplete => RECORD COMPLETE => " + stream.getName());
					}
					
					List<String> params = new ArrayList<String>();
					params.add(RECORD_COMPLETE);
					params.add(streamName);
					params.add(recording_path);
					
					CompletableFuture<Integer> future = scriptExecutor.execute(recordCompleteScript, params);
					future.thenAccept(value -> {						
						//System.out.println("value" + String.valueOf(value));
						logger.info("Script execution exited with code: {}", String.valueOf(value));
						
					});
				} catch (IOException e) {
					logger.error("An error occurred executing script {}", e);
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
			moduleDebug = WowzaUtils.getPropertyValueBoolean(serverProps, appInstance, PROP_DEBUG, false);
			
			
			if(moduleDebug){
				getLogger().info(MODULE_NAME + ".readProperties moduleDebug mode : " + String.valueOf(moduleDebug));
			}	

			
			try
			{
				workingScriptDir = WowzaUtils.getPropertyValueStr(serverProps, appInstance, PROP_SCRIPT_WORKING_DIR, null);
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
				recordCompleteScript = WowzaUtils.getPropertyValueStr(serverProps, appInstance, PROP_RECORD_COMPLETE_SCRIPT, null);
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

}
