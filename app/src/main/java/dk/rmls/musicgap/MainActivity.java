package dk.rmls.musicgap;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.nio.file.Path;
import java.util.stream.IntStream;

public class MainActivity extends AppCompatActivity {

  static public class Interval {
    public int from, to;
  }

  static public class AppState {
    public Interval intervalToGuess;
    public int score = 0;

    public String[] intervals = {
        "unison", "minor 2nd", "major 2rd", "minor 3rd", "major 3rd", "perfect 4th", "tritone",
        "perfect 5th", "minor 6th", "major 6th", "minor 7th", "major 7th", "octave",
    };

    public TextView display;
  }

  static public interface CustomOnClickListener {
    public void onClick(View view, Object customData);
  }

  static public int getRandomInt(int fromInclusive, int toInclusive) {
    double rand = (toInclusive - fromInclusive + 1) * Math.random() + fromInclusive;
    return (int) rand;
  }

  static public int getRandomInt(int toInclusive) {
    return getRandomInt(0, toInclusive);
  }

  static public Interval generateInterval() {
    int[] candidateIntervals = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
    return generateInterval(21, 108, candidateIntervals);
  }

  static private int[] getValidIntervals(int maxInterval, int[] candidateIntervals) {
    return IntStream.of(candidateIntervals).filter(interval -> Math.abs(interval) <= maxInterval).toArray();
  }

  static public Interval generateInterval(int lowestNote, int highestNote, int[] intervals) {
    int maxInterval = highestNote - lowestNote;
    int[] validIntervals = getValidIntervals(maxInterval, intervals);

    int randomIntervalIndex = getRandomInt(validIntervals.length - 1);
    int randomInterval = validIntervals[randomIntervalIndex];

    int fromLowestNote = lowestNote - Math.min(0, randomInterval);
    int fromHighestNote = highestNote - Math.max(0, randomInterval);

    int fromNote = getRandomInt(fromLowestNote, fromHighestNote);
    int toNote = fromNote + randomInterval;

    Interval result = new Interval();
    result.from = fromNote;
    result.to = toNote;

    return result;
  }

  static public int getInterval(Interval interval) {
    return interval.to - interval.from;
  }

  static public String getDisplayText(AppState state) {
    int interval = getInterval(state.intervalToGuess);
    return String.format("%s, %d-%d (%d) %d", state.intervals[interval], state.intervalToGuess.from, state.intervalToGuess.to, interval, state.score);
  }

  private AppState state;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    state = new AppState();
    state.intervalToGuess = generateInterval();

    FluidSynth.init();

    String soundFontName = "Yamaha-Grand-Lite-v2.0.sf2";
    IOUtil.copyAssetToDeviceStorage(getBaseContext(), soundFontName, soundFontName);
    Path soundFontPath = IOUtil.getAppStoragePath(getBaseContext(), soundFontName);
    FluidSynth.loadSoundFont(soundFontPath.toString());

    CustomOnClickListener listener = (view, customData) -> {
      int intervalGuess = (int) customData;
      int trueInterval = getInterval(state.intervalToGuess);

      if (intervalGuess == trueInterval) {
        state.score += 1;
      } else {
        state.score = 0;
      }

      state.intervalToGuess = generateInterval();

      String displayText = getDisplayText(state);
      state.display.setText(displayText);

      playInterval(state.intervalToGuess);
    };

    createUI(listener);

    playInterval(state.intervalToGuess);
  }

  static private void playInterval(Interval intervalToGuess) {
    new Thread(() -> {
      FluidSynth.noteOn(0, intervalToGuess.from, 127);

      ThreadUtil.sleep(400);

      FluidSynth.noteOff(0, intervalToGuess.from);
      FluidSynth.noteOn(0, intervalToGuess.to, 127);

      ThreadUtil.sleep(400);

      FluidSynth.noteOff(0, intervalToGuess.to);
    }).start();
  }

  private void createUI(CustomOnClickListener buttonOnClickListener) {
    setContentView(R.layout.container);
    RelativeLayout container = findViewById(R.id.container);

    int[][] intervalButtonLayout = {
        {-1, 0},
        {1, 2},
        {3, 4},
        {-1, 5},
        {6, 7},
        {8, 9},
        {10, 11},
        {-1, 12},
    };

    String[] intervals = {
        "unison", "minor 2nd", "major 2rd", "minor 3rd", "major 3rd", "perfect 4th", "tritone",
        "perfect 5th", "minor 6th", "major 6th", "minor 7th", "major 7th", "octave",
    };

    {
      TextView display = new TextView(this);
      display.setTextSize(32);
      String intervalToGuessText = getDisplayText(state);
      display.setText(intervalToGuessText);
      display.setBackgroundColor(Color.RED);
      state.display = display;

      RelativeLayout.LayoutParams displayLayout = new RelativeLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.WRAP_CONTENT
      );
      displayLayout.addRule(RelativeLayout.ALIGN_PARENT_TOP);
      container.addView(display, displayLayout);
    }

    {
      TableLayout table = new TableLayout(this);
      table.setBackgroundColor(Color.BLUE);
      RelativeLayout.LayoutParams tableLayout = new RelativeLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.WRAP_CONTENT
      );
      tableLayout.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

      container.addView(table, tableLayout);

      TableRow.LayoutParams buttonParam = new TableRow.LayoutParams(
          TableRow.LayoutParams.MATCH_PARENT,
          TableRow.LayoutParams.MATCH_PARENT,
          1.0f
      );

      for (int r = 0; r < intervalButtonLayout.length; r++) {
        TableRow row = new TableRow(this);
        table.addView(row);

        int buttonMarginPx = UIUtil.dp(this, 0);
        int buttonPaddingPx = UIUtil.dp(this, 24);

        buttonParam.setMargins(buttonMarginPx, buttonMarginPx, buttonMarginPx, buttonMarginPx);

        for (int c = 0; c < intervalButtonLayout[r].length; c++) {
          final int interval = intervalButtonLayout[r][c];
          if (interval == -1) {
            TextView emptyField = new TextView(this);
            emptyField.setPadding(buttonPaddingPx, buttonPaddingPx, buttonPaddingPx, buttonPaddingPx);
            row.addView(emptyField, buttonParam);
          } else {
            Button button = new Button(this);
            button.setPadding(buttonPaddingPx, buttonPaddingPx, buttonPaddingPx, buttonPaddingPx);
            button.setOnClickListener(v -> {
              buttonOnClickListener.onClick(v, interval);
            });
            row.addView(button, buttonParam);
            String text = String.format("%s", intervals[interval]);
            button.setText(text);
          }
        }
      }
    }
  }
}