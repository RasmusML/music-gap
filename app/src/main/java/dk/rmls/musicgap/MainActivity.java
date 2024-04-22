package dk.rmls.musicgap;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import java.nio.file.Path;

import dk.rmls.musicgap.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FluidSynth.init();

        String soundFontName = "U20PIANO.sf2";
        IOUtil.copyAssetToDeviceStorage(getBaseContext(), soundFontName, soundFontName);
        Path soundFontPath = IOUtil.getAppStoragePath(getBaseContext(), soundFontName);
        FluidSynth.loadSoundFont(soundFontPath.toString());
    }

    @Override
    protected void onResume() {
        super.onResume();

        FluidSynth.noteOn(0, 66, 127);
    }
}