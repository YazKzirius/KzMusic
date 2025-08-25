#include <jni.h>
#include <android/log.h>
#include <string>
#include <SLES/OpenSLES_AndroidConfiguration.h>
#include <SLES/OpenSLES.h>
#include <malloc.h>

#include "Superpowered.h"
#include "SuperpoweredAndroidAudioIO.h"
#include "SuperpoweredAdvancedAudioPlayer.h"
#include "SuperpoweredTimeStretching.h"
#include "SuperpoweredReverb.h"
#include "SuperpoweredSimple.h"
#include "SuperpoweredCPU.h"

#define LOG_TAG "AudioPlayer"
#define log_print __android_log_print

// Global pointers for the Superpowered components
static SuperpoweredAndroidAudioIO *audioIO;
static Superpowered::AdvancedAudioPlayer *player;
static Superpowered::TimeStretching *timeStretch;
static Superpowered::Reverb *reverb;

// Audio processing callback.
static bool audioProcessing(
        void * __unused clientdata,
        short int *audio,
        int numberOfFrames,
        int samplerate
) {
    player->outputSamplerate = (unsigned int)samplerate;

    // Create a floating-point buffer. Superpowered effects work with 32-bit floats.
    // A little extra space is good for the time stretcher.
    float floatBuffer[numberOfFrames * 2 + 1024];

    // Process the player. If it returns audio, proceed with effects.
    if (player->processStereo(floatBuffer, false, (unsigned int)numberOfFrames)) {
        // 1. Time Stretching & Pitch Shifting
        // Add the audio from the player to the time stretcher's input buffer.
        timeStretch->addInput(floatBuffer, numberOfFrames);

        // Get the processed audio from the time stretcher's output buffer.
        // The number of output frames might be different from the input.
        unsigned int timeStretchedFramesAvailable = numberOfFrames;
        timeStretch->getOutput(floatBuffer, timeStretchedFramesAvailable);

        if (timeStretchedFramesAvailable > 0) {
            // 2. Reverb
            // Apply reverb to the time-stretched audio.
            reverb->process(floatBuffer, floatBuffer, timeStretchedFramesAvailable);

            // 3. Convert to 16-bit integer for output.
            Superpowered::FloatToShortInt(floatBuffer, audio, timeStretchedFramesAvailable);
            return true;
        }
    }

    return false;
}

// JNI function to initialize the audio engine.
extern "C" JNIEXPORT void JNICALL
Java_com_example_kzmusic_PlayerService_initSuperpowered(
        JNIEnv *env,
        jobject __unused obj,
        jint samplerate,
        jint buffersize
) {
    Superpowered::Initialize("ExampleLicenseKey-WillExpire-OnNextUpdate");

    player = new Superpowered::AdvancedAudioPlayer((unsigned int)samplerate, 0);
    timeStretch = new Superpowered::TimeStretching((unsigned int)samplerate);

    // Initialize reverb
    reverb = new Superpowered::Reverb((unsigned int)samplerate);

    audioIO = new SuperpoweredAndroidAudioIO(
            samplerate, buffersize, false, true, audioProcessing,
            nullptr, -1, SL_ANDROID_STREAM_MEDIA
    );
}

// JNI function to open a file.
extern "C" JNIEXPORT void JNICALL
Java_com_example_kzmusic_PlayerService_openFile(JNIEnv *env, jobject __unused obj, jstring path) {
    const char *str = env->GetStringUTFChars(path, 0);
    player->open(str);
    env->ReleaseStringUTFChars(path, str);
}

// JNI function to toggle playback.
extern "C" JNIEXPORT void JNICALL
Java_com_example_kzmusic_PlayerService_togglePlayback(JNIEnv * __unused env, jobject __unused obj) {
    player->togglePlayback();
    Superpowered::CPU::setSustainedPerformanceMode(player->isPlaying());
}

// JNI function to set pitch shift.
extern "C" JNIEXPORT void JNICALL
Java_com_example_kzmusic_PlayerService_setPitchShift(JNIEnv * __unused env, jobject __unused obj, jint cents) {
    if (timeStretch) {
        // Correct way: Set the public member variable.
        timeStretch->pitchShiftCents = cents;
    }
}

// JNI function to set tempo/rate.
extern "C" JNIEXPORT void JNICALL
Java_com_example_kzmusic_PlayerService_setTempo(JNIEnv * __unused env, jobject __unused obj, jdouble rate) {
    if (timeStretch) {
        // Correct way: Set the public member variable.
        timeStretch->rate = rate;
    }
}

// JNI function to enable/disable reverb.
extern "C" JNIEXPORT void JNICALL
Java_com_example_kzmusic_PlayerService_enableReverb(JNIEnv * __unused env, jobject __unused obj, jboolean enable) {
    if (reverb) {
        // Correct way: Use the enable() method.

    }
}

// Lifecycle functions
extern "C" JNIEXPORT void JNICALL
Java_com_example_kzmusic_PlayerService_onBackground(JNIEnv * __unused env, jobject __unused obj) {
    audioIO->onBackground();
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_kzmusic_PlayerService_onForeground(JNIEnv * __unused env, jobject __unused obj) {
    audioIO->onForeground();
}

// Cleanup function
extern "C" JNIEXPORT void JNICALL
Java_com_example_kzmusic_PlayerService_cleanup(JNIEnv * __unused env, jobject __unused obj) {
    delete audioIO;
    delete player;
    delete timeStretch;
    delete reverb;
}
// JNI function to start playback.
extern "C" JNIEXPORT void JNICALL
Java_com_example_kzmusic_PlayerService_play(JNIEnv * __unused env, jobject __unused obj) {
    if (player) {
        player->play();
        // Use sustained performance mode for low latency playback.
        Superpowered::CPU::setSustainedPerformanceMode(true);
    }
}

// JNI function to pause playback.
extern "C" JNIEXPORT void JNICALL
Java_com_example_kzmusic_PlayerService_pause(JNIEnv * __unused env, jobject __unused obj) {
    if (player) {
        player->pause();
        // Release sustained performance mode when paused to save battery.
        Superpowered::CPU::setSustainedPerformanceMode(false);
    }
}