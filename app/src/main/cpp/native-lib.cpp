//Native C++ Superpowered Library
#include <jni.h>
#include <android/log.h>
#include "Superpowered.h"
#include <SuperpoweredTimeStretching.h>
#include <SuperpoweredSimple.h>

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
Superpowered::TimeStretching *pitchEngine;

extern "C" JNIEXPORT void JNICALL
Java_com_example_SuperpoweredProcessor_init(JNIEnv *env, jobject, jint sampleRate, jint channels, jfloat pitchShiftCents) {
    pitchEngine = new Superpowered::TimeStretching((unsigned int)sampleRate);
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_SuperpoweredProcessor_processBuffer(JNIEnv *env, jobject, jshortArray input, jint length) {
    jshort *inputShorts = env->GetShortArrayElements(input, nullptr);
    env->ReleaseShortArrayElements(input, inputShorts, 0);
}