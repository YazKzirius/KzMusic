
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
#include "SuperpoweredReverb.h"
// Add the FFT header at the top
#include "SuperpoweredFFT.h"
#include <pthread.h> // For the mutex

#define LOG_TAG "AudioPlayer"
#define log_print __android_log_print
static float *fftInputMono;        // A buffer for the mono audio signal
static float *fftReal;             // Will hold the EVEN samples for PolarFFT input
static float *fftImag;             // Will hold the ODD samples for PolarFFT input (used as phase input)
static pthread_mutex_t fftMutex;
static const int fftSizeLog2 = 10; // FFT size is 2^10 = 1024.
static SuperpoweredAndroidAudioIO *audioIO;
static Superpowered::AdvancedAudioPlayer *player;
static Superpowered::Reverb *reverb;

// This simple callback is correct. The player handles all effects internally.
static bool audioProcessing(
        void * __unused clientdata,
        short int *audio,
        int numberOfFrames,
        int samplerate
) {
    player->outputSamplerate = (unsigned int)samplerate;
    float floatBuffer[numberOfFrames * 2];
    //Implementing Advanced Audio Player code
    if (player->processStereo(floatBuffer, false, (unsigned int)numberOfFrames)) {
        Superpowered::FloatToShortInt(floatBuffer, audio, (unsigned int)numberOfFrames);
        return true;
    } else {
        return false;
    }
    //Implementing advanced Polar FFT code
    // 1. Convert stereo audio to mono.
    Superpowered::StereoToMono(floatBuffer, fftInputMono, 0.5f, 0.5f, 0.5f, 0.5f, (unsigned int)numberOfFrames);

    // 2. Prepare the "split real" input for PolarFFT.
    // Even samples go into fftReal, odd samples go into fftImag.
    for (int i = 0; i < (1 << fftSizeLog2); i += 2) {
        fftReal[i >> 1] = fftInputMono[i];
        fftImag[i >> 1] = fftInputMono[i + 1];
    }

    pthread_mutex_lock(&fftMutex);

    // 3. Perform the Polar FFT.
    // The fftReal buffer is used as input AND is overwritten with the magnitude output.
    // The fftImag buffer is used as input AND is overwritten with the phase output (which we don't need).
    Superpowered::PolarFFT(fftReal, fftImag, fftSizeLog2, true);

    pthread_mutex_unlock(&fftMutex);
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
    //Initialising visualiser
    const int fftSize = 1 << fftSizeLog2; // 1024
    fftInputMono = (float *)malloc(fftSize * sizeof(float));
    fftReal = (float *)malloc((fftSize / 2) * sizeof(float));
    fftImag = (float *)malloc((fftSize / 2) * sizeof(float));
    pthread_mutex_init(&fftMutex, NULL);
}

// These Functions below implement the main player functionality for Superpowered
extern "C" JNIEXPORT void JNICALL
Java_com_example_kzmusic_PlayerService_openFile(JNIEnv *env, jobject __unused obj, jstring path) {
    const char *str = env->GetStringUTFChars(path, 0);
    player->open(str);
    env->ReleaseStringUTFChars(path, str);
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_kzmusic_PlayerService_setPitchShift(JNIEnv* env, jobject obj, jint cents) {
    if (player) {
        player->pitchShiftCents = cents;
    } else {
        __android_log_print(ANDROID_LOG_WARN, "PlayerService", "setPitchShift: player is null");
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_kzmusic_PlayerService_setTempo(JNIEnv* env, jobject obj, jdouble rate) {
    if (player) {
        player->playbackRate = rate;
    } else {
        __android_log_print(ANDROID_LOG_WARN, "PlayerService", "setTempo: player is null");
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_kzmusic_PlayerService_play(JNIEnv* env, jobject obj) {
    if (player) {
        player->play();
        Superpowered::CPU::setSustainedPerformanceMode(true);
    } else {
        __android_log_print(ANDROID_LOG_WARN, "PlayerService", "play: player is null");
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_kzmusic_PlayerService_pause(JNIEnv* env, jobject obj) {
    if (player) {
        player->pause();
        Superpowered::CPU::setSustainedPerformanceMode(false);
    } else {
        __android_log_print(ANDROID_LOG_WARN, "PlayerService", "pause: player is null");
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_kzmusic_PlayerService_onBackground(JNIEnv* env, jobject obj) {
    if (audioIO) {
        audioIO->onBackground();
    } else {
        __android_log_print(ANDROID_LOG_WARN, "PlayerService", "onBackground: audioIO is null");
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_kzmusic_PlayerService_onForeground(JNIEnv* env, jobject obj) {
    if (audioIO) {
        audioIO->onForeground();
    } else {
        __android_log_print(ANDROID_LOG_WARN, "PlayerService", "onForeground: audioIO is null");
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_kzmusic_PlayerService_cleanup(JNIEnv* env, jobject obj) {
    if (audioIO) {
        delete audioIO;
        audioIO = nullptr;
    }
    if (player) {
        delete player;
        player = nullptr;
    }
    free(fftInputMono);
    free(fftReal);
    free(fftImag);
    pthread_mutex_destroy(&fftMutex);
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_kzmusic_PlayerService_seekTo(JNIEnv* env, jobject obj, jdouble positionMs) {
    if (player) {
        player->setPosition(positionMs, false, false);
    } else {
        __android_log_print(ANDROID_LOG_WARN, "PlayerService", "seekTo: player is null");
    }
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_kzmusic_PlayerService_isPlaying(JNIEnv* env, jobject obj) {
    if (player == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, "PlayerService", "isPlaying: player is null");
        return JNI_FALSE;
    }
    return player->isPlaying() ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jdouble JNICALL
Java_com_example_kzmusic_PlayerService_getPositionMs(JNIEnv* env, jobject obj) {
    if (player) {
        return player->getPositionMs();
    }
    __android_log_print(ANDROID_LOG_WARN, "PlayerService", "getPositionMs: player is null");
    return 0.0;
}

extern "C" JNIEXPORT jdouble JNICALL
Java_com_example_kzmusic_PlayerService_getDurationMs(JNIEnv* env, jobject obj) {
    if (player) {
        return player->getDurationMs();
    }
    __android_log_print(ANDROID_LOG_WARN, "PlayerService", "getDurationMs: player is null");
    return 0.0;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_kzmusic_PlayerService_setLooping(JNIEnv* env, jobject obj, jboolean isLooping) {
    if (player) {
        player->loopOnEOF = isLooping;
    } else {
        __android_log_print(ANDROID_LOG_WARN, "PlayerService", "setLooping: player is null");
    }
}

extern "C" JNIEXPORT jint JNICALL
Java_com_example_kzmusic_PlayerService_getPlayerEvent(JNIEnv* env, jobject obj) {
    if (player) {
        return (jint)player->getLatestEvent();
    }
    __android_log_print(ANDROID_LOG_WARN, "PlayerService", "getPlayerEvent: player is null");
    return 0;
}
//This function implements KzMusic Advanced Audio effects for Superpowered
extern "C" JNIEXPORT void JNICALL
Java_com_example_kzmusic_PlayerService_initialiseReverb(JNIEnv* env, jobject obj, jfloat dry, jfloat wet, jfloat mix, jfloat width, jfloat damp, jfloat roomSize, jfloat predelayMs, jfloat lowCutHz) {
    reverb = new Superpowered::Reverb(44100, 44100);
    reverb->dry = dry;
    reverb->wet = wet;
    reverb->mix = mix;
    reverb->width = width;
    reverb->damp = damp;
}
extern "C" JNIEXPORT void JNICALL
Java_com_example_kzmusic_PlayerService_getLatestFftData(JNIEnv *env, jobject __unused obj, jfloatArray javaArray) {
    if (!fftReal) return; // The magnitudes are now in fftReal

    jfloat *cArray = env->GetFloatArrayElements(javaArray, NULL);
    if (cArray == NULL) return;

    pthread_mutex_lock(&fftMutex);
    // Copy the magnitude data from fftReal into the Java array.
    memcpy(cArray, fftReal, ((1 << (fftSizeLog2 - 1))) * sizeof(float));
    pthread_mutex_unlock(&fftMutex);

    env->ReleaseFloatArrayElements(javaArray, cArray, 0);
}

