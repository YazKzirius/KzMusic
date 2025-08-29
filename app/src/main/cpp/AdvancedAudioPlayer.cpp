
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
static const int fftSizeLog2 = 10;                // FFT size = 1024
static const int fftSize = 1 << fftSizeLog2;      // 1024
static float *fftAccumulator;                     // This buffer will accumulate audio
static int fftAccumulatorPosition = 0;            // How much audio is in the accumulator
static float *fftReal;                            // Buffer for FFT magnitudes
static float *fftImag;                            // Buffer for FFT phases
static pthread_mutex_t fftMutex;           // How much audio is in the accumulator
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
    if (!player) return false;
    player->outputSamplerate = (unsigned int)samplerate;
    float floatBuffer[numberOfFrames * 2];

    if (player->processStereo(floatBuffer, false, (unsigned int)numberOfFrames)) {

        // --- [ROBUST FFT PROCESSING] ---
        if (fftInputMono) {
            // 1. Clear the entire FFT input buffer with silence. This is a critical safety step.
            memset(fftInputMono, 0, fftSize * sizeof(float));

            // 2. Convert the new audio to mono and copy it to the START of the cleared buffer.
            Superpowered::StereoToMono(floatBuffer, fftInputMono, 0.5f, 0.5f, 0.5f, 0.5f, (unsigned int)numberOfFrames);

            // 3. Prepare the "split real" input for PolarFFT.
            for (int i = 0; i < fftSize; i += 2) {
                fftReal[i >> 1] = fftInputMono[i];
                fftImag[i >> 1] = fftInputMono[i + 1];
            }

            // 4. Lock, run the FFT on the now-full and safe buffer, then unlock.
            pthread_mutex_lock(&fftMutex);
            Superpowered::PolarFFT(fftReal, fftImag, fftSizeLog2, true);
            pthread_mutex_unlock(&fftMutex);
        }
        // --- [END OF FIX] ---

        if (reverb && reverb->enabled) {
            reverb->process(floatBuffer, floatBuffer, (unsigned int)numberOfFrames);
        }
        Superpowered::FloatToShortInt(floatBuffer, audio, (unsigned int)numberOfFrames);
        return true;
    }
    return false;
}

// --- JNI Functions ---
extern "C" JNIEXPORT void JNICALL
Java_com_example_kzmusic_PlayerService_initSuperpowered(JNIEnv *env, jobject __unused obj, jint samplerate, jint buffersize) {
    Superpowered::Initialize("ExampleLicenseKey-WillExpire-OnNextUpdate");

    player = new Superpowered::AdvancedAudioPlayer((unsigned int)samplerate, 0);
    player->timeStretchingSound = 2;
    player->timeStretching = true;

    reverb = new Superpowered::Reverb((unsigned int)samplerate);

    // Initialize visualiser components
    fftInputMono = (float *)malloc(fftSize * sizeof(float));
    fftReal = (float *)malloc((fftSize / 2) * sizeof(float));
    fftImag = (float *)malloc((fftSize / 2) * sizeof(float));
    pthread_mutex_init(&fftMutex, NULL);

    audioIO = new SuperpoweredAndroidAudioIO(
            samplerate, buffersize, false, true, audioProcessing, nullptr, -1, SL_ANDROID_STREAM_MEDIA);
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
Java_com_example_kzmusic_PlayerService_cleanup(JNIEnv* env, jobject __unused obj) {
    if (audioIO != nullptr) {
        delete audioIO;
        audioIO = nullptr; // Set to null immediately after deletion
        log_print(ANDROID_LOG_DEBUG, "SuperpoweredEngine", "audioIO cleaned up.");
    }

    if (player != nullptr) {
        delete player;
        player = nullptr;
        log_print(ANDROID_LOG_DEBUG, "SuperpoweredEngine", "player cleaned up.");
    }

    if (reverb != nullptr) {
        delete reverb;
        reverb = nullptr;
        log_print(ANDROID_LOG_DEBUG, "SuperpoweredEngine", "reverb cleaned up.");
    }

    if (fftInputMono != nullptr) {
        free(fftInputMono);
        fftInputMono = nullptr;
        log_print(ANDROID_LOG_DEBUG, "SuperpoweredEngine", "fftInputMono cleaned up.");
    }

    if (fftReal != nullptr) {
        free(fftReal);
        fftReal = nullptr;
        log_print(ANDROID_LOG_DEBUG, "SuperpoweredEngine", "fftReal cleaned up.");
    }

    if (fftImag != nullptr) {
        free(fftImag);
        fftImag = nullptr;
        log_print(ANDROID_LOG_DEBUG, "SuperpoweredEngine", "fftImag cleaned up.");
    }
    if (player != nullptr || audioIO != nullptr) { // A simple check to infer initialization
        pthread_mutex_destroy(&fftMutex);
        log_print(ANDROID_LOG_DEBUG, "SuperpoweredEngine", "fftMutex destroyed.");
    }
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
    // --- STEP 1: Safety Check ---
    // If the FFT has not been initialized or the passed Java array is invalid, do nothing.
    if (!fftReal || !javaArray) {
        return;
    }

    // --- STEP 2: Get a direct pointer to the Java array's memory ---
    // This allows C++ to write directly into the memory used by your Android UI's fftData array.
    jfloat *cArray = env->GetFloatArrayElements(javaArray, NULL);
    if (cArray == NULL) {
        // This can happen if the system is out of memory.
        return;
    }

    // --- STEP 3: Use a Mutex for Thread Safety ---
    // The audio thread is constantly writing to fftReal. The UI thread is about to read from it.
    // The mutex ensures the audio thread waits for the UI thread to finish copying, preventing corrupted data.
    pthread_mutex_lock(&fftMutex);

    // --- STEP 4: Copy the Data ---
    // This is the core of the function. It copies the magnitude data from our C++ fftReal buffer
    // into the Java array. We only need the first half of the FFT results.
    // The size is 2^(fftSizeLog2 - 1), which is 1024 / 2 = 512.
    memcpy(cArray, fftReal, (1 << (fftSizeLog2 - 1)) * sizeof(float));

    // --- STEP 5: Unlock the Mutex ---
    // Release the lock so the audio thread can continue processing.
    pthread_mutex_unlock(&fftMutex);

    // --- STEP 6: Release the Pointer ---
    // This tells the Java Virtual Machine that we are done writing to the array's memory.
    // The '0' argument means "copy the changes back and free the C++ copy".
    env->ReleaseFloatArrayElements(javaArray, cArray, 0);
}

