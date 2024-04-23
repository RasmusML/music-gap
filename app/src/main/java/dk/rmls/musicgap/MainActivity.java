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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class MainActivity extends AppCompatActivity {

  static public enum IntervalButtonState {
    notGuessed,
    guessed,
    locked,
  }

  static public class Dyad {
    public int noteA, noteB;
  }

  static public class AppState {
    public int correctGuesses, totalGuesses;
    public Dyad intervalToGuess;

    public int lowestNote;
    public int highestNote;
    public int[] intervals;

    public UIState uiState;
  }

  static public class UIState {
    public List<Button> intervalButtons;
    public String[] intervalNames;
    public IntervalButtonState[] intervalButtonStates;

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

  static private int[] getValidIntervals(int maxInterval, int[] candidateIntervals) {
    return IntStream.of(candidateIntervals).filter(interval -> Math.abs(interval) <= maxInterval).toArray();
  }

  static public Dyad generateRandomDyad(int lowestNote, int highestNote, int[] candidateIntervals) {
    int largestInterval = highestNote - lowestNote;
    int[] validIntervals = getValidIntervals(largestInterval, candidateIntervals);

    if (validIntervals.length == 0) {
      String error = String.format("no candidate intervals within the range [%d, %d]", lowestNote, highestNote);
      throw new IllegalStateException(error);
    }

    int randomIntervalIndex = getRandomInt(validIntervals.length - 1);

    int randomInterval = validIntervals[randomIntervalIndex];

    int lowestNoteA = lowestNote - Math.min(0, randomInterval);
    int highestNoteA = highestNote - Math.max(0, randomInterval);

    int noteA = getRandomInt(lowestNoteA, highestNoteA);
    int noteB = noteA + randomInterval;

    Dyad result = new Dyad();
    result.noteA = noteA;
    result.noteB = noteB;

    return result;
  }

  static public int getIntervalWithSign(Dyad dyad) {
    return dyad.noteB - dyad.noteA;
  }

  static public int getInterval(Dyad dyad) {
    return Math.abs(dyad.noteB - dyad.noteA);
  }

  static public String getDisplayText(AppState state) {
    int interval = getInterval(state.intervalToGuess);
    int intervalWithSign = getIntervalWithSign(state.intervalToGuess);
    return String.format("%s, %d-%d (%d) %d/%d",
        state.uiState.intervalNames[interval], state.intervalToGuess.noteA, state.intervalToGuess.noteB,
        intervalWithSign, state.correctGuesses, state.totalGuesses);
  }

  static public void updateDisplayText(AppState state) {
    String intervalToGuessText = getDisplayText(state);
    state.uiState.display.setText(intervalToGuessText);
  }

  private AppState state;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    CustomOnClickListener listener = (view, customData) -> {
      int guessedInterval = (int) customData;
      int trueInterval = getInterval(state.intervalToGuess);

      if (guessedInterval == trueInterval) {
        state.correctGuesses += 1;
        state.intervalToGuess = generateRandomDyad(state.lowestNote, state.highestNote, state.intervals);

        clearIntervalButtonStates(state.uiState.intervalButtonStates);
      } else {
        state.uiState.intervalButtonStates[guessedInterval] = IntervalButtonState.guessed;
      }

      state.totalGuesses += 1;

      updateIntervalButtonStates(state.uiState.intervalButtons, state.uiState.intervalButtonStates);
      updateDisplayText(state);

      playInterval(state.intervalToGuess);
    };

    UIState uiState = createUI(listener);

    state = new AppState();
    state.highestNote = 108;
    state.lowestNote = 21;
    state.intervals = IntStream.range(-12, 13).toArray();
    state.intervalToGuess = generateRandomDyad(state.lowestNote, state.highestNote, state.intervals);
    state.uiState = uiState;

    updateDisplayText(state);

    FluidSynth.init();

    String soundFontName = "Yamaha-Grand-Lite-v2.0.sf2";
    IOUtil.copyAssetToAppStorage(getBaseContext(), soundFontName, soundFontName);
    Path soundFontPath = IOUtil.getAppStoragePath(getBaseContext(), soundFontName);
    FluidSynth.loadSoundFont(soundFontPath.toString());
  }

  @Override
  protected void onResume() {
    super.onResume();

    playInterval(state.intervalToGuess);
  }

  static private void playInterval(Dyad intervalToGuess) {
    new Thread(() -> {
      FluidSynth.noteOn(0, intervalToGuess.noteA, 127);

      ThreadUtil.sleep(400);

      FluidSynth.noteOff(0, intervalToGuess.noteA);
      FluidSynth.noteOn(0, intervalToGuess.noteB, 127);

      ThreadUtil.sleep(400);

      FluidSynth.noteOff(0, intervalToGuess.noteB);
    }).start();
  }

  private UIState createUI(CustomOnClickListener buttonOnClickListener) {
    UIState result = new UIState();

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

    String[] intervalNames = {
        "unison", "minor 2nd", "major 2rd", "minor 3rd", "major 3rd", "perfect 4th", "tritone",
        "perfect 5th", "minor 6th", "major 6th", "minor 7th", "major 7th", "octave",
    };
    result.intervalNames = intervalNames;

    {
      TextView display = new TextView(this);
      display.setTextSize(32);
      display.setBackgroundColor(Color.RED);
      result.display = display;

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

      List<Button> buttons = new ArrayList<>();

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
            String text = String.format("%s", intervalNames[interval]);
            button.setText(text);
            buttons.add(button);
          }
        }
      }

      result.intervalButtons = buttons;
    }

    int numberOfIntervalStates = intervalNames.length;
    IntervalButtonState[] states = new IntervalButtonState[numberOfIntervalStates];
    for (int i = 0; i < states.length; i++) {
      states[i] = IntervalButtonState.notGuessed;
    }
    result.intervalButtonStates = states;

    return result;
  }

  static private IntervalButtonState clearIntervalButtonState(IntervalButtonState state) {
    switch (state) {
      case notGuessed:
        return IntervalButtonState.notGuessed;
      case guessed:
        return IntervalButtonState.notGuessed;
      case locked:
        return IntervalButtonState.locked;
      default:
        String error = String.format("unexpected interval button state: %s.", state);
        throw new IllegalStateException(error);
    }
  }

  static public void clearIntervalButtonStates(IntervalButtonState[] states) {
    for (int i = 0; i < states.length; i++) {
      states[i] = clearIntervalButtonState(states[i]);
    }
  }

  static public void updateIntervalButtonStates(List<Button> buttons, IntervalButtonState[] states) {
    for (int i = 0; i < states.length; i++) {
      IntervalButtonState state = states[i];
      Button button = buttons.get(i);

      boolean clickable = isIntervalButtonClickable(state);
      button.setClickable(clickable);
    }
  }

  static private boolean isIntervalButtonClickable(IntervalButtonState state) {
    switch (state) {
      case notGuessed:
        return true;
      case guessed:
        return false;
      case locked:
        return false;
      default:
        String error = String.format("unexpected interval button state: %s.", state);
        throw new IllegalStateException(error);
    }
  }
}