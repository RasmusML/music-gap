#include <jni.h>
#include <string>
#include <unistd.h>
#include <android/log.h>
#include <assert.h>

#include "fluidsynth.h"

fluid_settings_t *settings;
fluid_synth_t *synth;
fluid_audio_driver_t *audioDriver;

extern "C" {

JNIEXPORT jint JNICALL
Java_dk_rmls_musicgap_FluidSynth_init(JNIEnv *env, jclass /* this */) {
    settings = new_fluid_settings();
    //fluid_settings_setstr(settings, "audio.oboe.sharing-mode", "Exclusive");
    //fluid_settings_setstr(settings, "audio.oboe.performance-mode", "LowLatency");
    //fluid_settings_setstr(settings, "audio.oboe.sample-rate-conversion-quality", "Fastest");

    synth = new_fluid_synth(settings);
    audioDriver = new_fluid_audio_driver(settings, synth);

    return FLUID_OK;
}

JNIEXPORT int JNICALL
Java_dk_rmls_musicgap_FluidSynth_deinit(JNIEnv *env, jclass /* this */) {
    delete_fluid_audio_driver(audioDriver);
    delete_fluid_synth(synth);
    delete_fluid_settings(settings);

    audioDriver = NULL;
    synth = NULL;
    settings = NULL;

    return FLUID_OK;
}

JNIEXPORT jint JNICALL
Java_dk_rmls_musicgap_FluidSynth_loadSoundFont(JNIEnv *env, jclass /* this */, jstring path) {
    const char *soundfontPath = env->GetStringUTFChars(path, nullptr);
    jint result = (jint) fluid_synth_sfload(synth, soundfontPath, 1);
    //__android_log_print(ANDROID_LOG_ERROR, "TRACKERS", "%s %d", soundfontPath, result);
    return result;
}

JNIEXPORT jint JNICALL
Java_dk_rmls_musicgap_FluidSynth_noteOn(JNIEnv *env, jclass /* this */, jint channel, jint key,
                                        jint velocity) {
    jint result = (jint) fluid_synth_noteon(synth, channel, key, velocity);
    return result;
}

JNIEXPORT jint JNICALL
Java_dk_rmls_musicgap_FluidSynth_noteOff(JNIEnv *env, jclass /* this */, jint channel, jint key) {
    jint result = (jint) fluid_synth_noteoff(synth, channel, key);
    return result;
}

} // extern "C"