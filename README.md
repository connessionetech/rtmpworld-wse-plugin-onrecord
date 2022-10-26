# About
---


The **ModuleOnRecordScript** module for Wowza Streaming Engine™ media server software can be used to execute custom logic after recording of a stream has completed. This means you need not bother about writing complex logic in java to do something after recording has completed. Some of the common tasks undertaken after recording a stream are uploading it to cloud, post processing it to generate alternate media formats via ffmpeg or simply notifying a remote server of recording completion. **ModuleOnRecordScript**  simplifies things for you by wiring the wowza application’s record complete event to an executable shell script / batch file on the same system. You can then use this external script to implement custom logic in-place or just call a server side script from there.

# Dependency Note
---

This module is dependent on the [RTMPWorld Shared Library For Wowza Streaming Engine](https://rtmpworld.com/product/shared-libraries-for-wowza-streaming-engine/). Please follow documentation on how to download & install the dependency jar on your Wowza Streaming Engine.

This library is required to be in your classpath if you plan on compiling the plugin on your own in your favourite IDE.

# Documentation
---

The detailed documentation on the subject can be found [here](https://rtmpworld.com/blog/execute-custom-logic-via-shell-script-after-recording-a-stream-in-the-wowza-streaming-engine/).
