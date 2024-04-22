package dk.rmls.musicgap;

public class FluidSynth {

    static {
        System.loadLibrary("musicgap");
    }

    static public int FLUID_OK = 1;
    static public int FLUID_FAILED = -1;

    static public native int init();

    static public native int deinit();

    static public native int loadSoundFont(String filename);

    static public native int noteOn(int channel, int key, int velocity);

    static public native int noteOff(int channel, int key);
}
