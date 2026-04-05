package com.gemininano.bridge

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.DeviceEventManagerModule

class ReactNativeEventEmitter(
  private val reactApplicationContext: ReactApplicationContext,
) {
  fun emit(
    eventName: String,
    payload: WritableMap,
  ) {
    if (!reactApplicationContext.hasActiveReactInstance()) {
      return
    }

    reactApplicationContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      .emit(eventName, payload)
  }
}
