package com.material.management.api.module;

import org.json.JSONObject;

public interface ViewCallbackListener {
    void callbackFromController(JSONObject result);
    void callbackFromController(JSONObject result, String tag);
    void callbackFromController(JSONObject result, Throwable throwable, String error);
}
