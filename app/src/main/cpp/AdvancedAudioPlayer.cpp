
#include <jni.h>
#include <android/log.h>
#include <string>
#include <SLES/OpenSLES_AndroidConfiguration.h>
#include <SLES/OpenSLES.h>
#include <malloc.h>

#include "Superpowered.h"
#include "SuperpoweredAndroidAudioIO.h"
#include "SuperpoweredAdvancedAudioPlayer.h"
#include "SuperpoweredSimple.h"
#include "SuperpoweredCPU.h"

#define LOG_TAG "AudioPlayer"
#define log_print __android_log_print

static SuperpoweredAndroidAudioIO *audioIO;
static Superpowered::AdvancedAudioPlayer *player;

// This simple callback is correct. The player handles all effects internally.
static bool audioProcessing(
        void * __unused clientdata,
        short int *audio,
        int numberOfFrames,
        int samplerate
) {
    player->outputSamplerate = (unsigned int)samplerate;
    float floatBuffer[numberOfFrames * 2];

    if (player->processStereo(floatBuffer, false, (unsigned int)numberOfFrames)) {
        Superpowered::FloatToShortInt(floatBuffer, audio, (unsigned int)numberOfFrames);
        return true;
    } else {
        return false;
    }
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

    // Ensure high-quality sound mode is enabled for the best audio.
    player->timeStretchingSound = 2;
    // Ensure the time stretching feature is enabled.
    player->timeStretching = true;

    // IMPORTANT: 'buffersize' now comes from your Java/Kotlin code,
    // which should be the larger, safer value.
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

// JNI function to set pitch shift. NO special logic needed.
extern "C" JNIEXPORT void JNICALL
Java_com_example_kzmusic_PlayerService_setPitchShift(JNIEnv * __unused env, jobject __unused obj, jint cents) {
    if (player) {
        player->pitchShiftCents = cents;
    }
}

// JNI function to set tempo/rate. NO special logic needed.
extern "C" JNIEXPORT void JNICALL
Java_com_example_kzmusic_PlayerService_setTempo(JNIEnv * __unused env, jobject __unused obj, jdouble rate) {
    if (player) {
        player->playbackRate = rate;
    }
}


// --- Standard Playback and Lifecycle Functions ---

extern "C" JNIEXPORT void JNICALL
Java_com_example_kzmusic_PlayerService_play(JNIEnv * __unused env, jobject __unused obj) {
    if (player) player->play();
    Superpowered::CPU::setSustainedPerformanceMode(true);
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_kzmusic_PlayerService_pause(JNIEnv * __unused env, jobject __unused obj) {
    if (player) player->pause();
    Superpowered::CPU::setSustainedPerformanceMode(false);
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_kzmusic_PlayerService_onBackground(JNIEnv * __unused env, jobject __unused obj) {
    if (audioIO) audioIO->onBackground();
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_kzmusic_PlayerService_onForeground(JNIEnv * __unused env, jobject __unused obj) {
    if (audioIO) audioIO->onForeground();
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_kzmusic_PlayerService_cleanup(JNIEnv * __unused env, jobject __unused obj) {
    delete audioIO;
    delete player;
}