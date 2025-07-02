#include <jni.h>
#include <android/log.h>
#include "Superpowered.h"

#define LOG_TAG "KzmusicNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

// Called when the native library is loaded
extern "C"
JNIEXPORT void JNICALL
Java_com_kzmusic_audio_AudioEngine_initSuperpowered(JNIEnv *env, jobject thiz) {
// Example license key — replace with your own from Superpowered dashboard
const char *licenseKey = "ExampleLicenseKey-WillExpire-OnNextUpdate";

Superpowered::Initialize(licenseKey);
LOGI("Superpowered initialized with license.");
}