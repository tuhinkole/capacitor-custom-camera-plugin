package com.mycompany.plugins.example;

import android.content.Intent;
import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "CustomCamera")
public class CustomCameraPlugin extends Plugin {

    private CustomCamera implementation;

    @Override
    public void load() {
        implementation = new CustomCamera(getActivity());
    }

   @PluginMethod
    public void startCamera(PluginCall call) {
        Intent intent = new Intent(getContext(), CustomCameraActivity.class);
        getActivity().startActivity(intent);
        call.resolve();
    }

    @PluginMethod
    public void startRecording(PluginCall call) {
        implementation.startRecording();
        call.resolve();
    }

    @PluginMethod
    public void takePictureWithEffect(PluginCall call) {
        implementation.takePictureWithEffect();
        call.resolve();
    }

    @PluginMethod
    public void stopRecording(PluginCall call) {
        String videoPath = implementation.stopRecording();
        JSObject ret = new JSObject();
        ret.put("videoPath", videoPath);
        // Later: add captured image paths
        call.resolve(ret);
    }
}
