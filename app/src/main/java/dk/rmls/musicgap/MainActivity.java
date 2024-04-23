package dk.rmls.musicgap;

import static dk.rmls.musicgap.UIUtil.*;
import static dk.rmls.musicgap.UIUtil.ButtonTheme;
import static dk.rmls.musicgap.UIUtil.dp;

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
import androidx.core.graphics.ColorUtils;

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
    public IntervalSettings intervalSettings;
    public IntervalGameState gameState;
    public UIState uiState;
  }

  static public class IntervalGameState {
    public int correctGuesses, totalGuesses;
    public Dyad intervalToGuess;
  }

  static public class IntervalSettings {
    public int lowestNote;
    public int highestNote;
    public int[] intervals;
  }


  static public class UITheme {
    public ButtonTheme intervalButton;
  }

  static public class UIState {
    public UITheme theme;

    public List<Button> intervalButtons;
    public String[] intervalNames;
    public IntervalButtonState[] intervalButtonStates;

    public TextView display;
  }

  static public interface CustomOnClickListener {
    public void onClick(View view, Object customData);
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

    int randomIntervalIndex = MathUtil.getRandomInt(validIntervals.length - 1);

    int randomInterval = validIntervals[randomIntervalIndex];

    int lowestNoteA = lowestNote - Math.min(0, randomInterval);
    int highestNoteA = highestNote - Math.max(0, randomInterval);

    int noteA = MathUtil.getRandomInt(lowestNoteA, highestNoteA);
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

  static private String getDisplayText(UIState uiState, IntervalGameState gameState) {
    if (DEBUG) {
      int interval = getInterval(gameState.intervalToGuess);
      int intervalWithSign = getIntervalWithSign(gameState.intervalToGuess);

      return String.format("%s, %d-%d (%d) %d/%d",
          uiState.intervalNames[interval], gameState.intervalToGuess.noteA, gameState.intervalToGuess.noteB,
          intervalWithSign, gameState.correctGuesses, gameState.totalGuesses);
    }

    float percentageCorrect = 100f;
    if (gameState.totalGuesses > 0) {
      percentageCorrect = gameState.correctGuesses / (float) gameState.totalGuesses * 100f;
    }

    return String.format("%d/%d (%.0f%%)", gameState.correctGuesses, gameState.totalGuesses, percentageCorrect);
  }

  static public void updateDisplayText(UIState uiState, IntervalGameState gameState) {
    String intervalToGuessText = getDisplayText(uiState, gameState);
    uiState.display.setText(intervalToGuessText);
  }

  static public boolean DEBUG = false;

  private AppState state;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    CustomOnClickListener listener = (view, customData) -> {
      UIState uiState = state.uiState;
      IntervalGameState gameState = state.gameState;
      IntervalSettings intervalSettings = state.intervalSettings;

      int guessedInterval = (int) customData;
      int trueInterval = getInterval(gameState.intervalToGuess);

      if (guessedInterval == trueInterval) {
        resetIntervalButtonStates(uiState.intervalButtonStates);

        gameState.correctGuesses += 1;
        gameState.intervalToGuess = generateRandomDyad(
            intervalSettings.lowestNote, intervalSettings.highestNote, intervalSettings.intervals
        );

      } else {
        uiState.intervalButtonStates[guessedInterval] = IntervalButtonState.guessed;
      }

      gameState.totalGuesses += 1;

      updateIntervalButtonsBeingClickable(uiState);
      updateDisplayText(uiState, gameState);

      playInterval(gameState.intervalToGuess);
    };

    IntervalSettings intervalSettings = new IntervalSettings();
    intervalSettings.lowestNote = 21;
    intervalSettings.highestNote = 108;
    intervalSettings.intervals = IntStream.range(-12, 13).toArray();

    IntervalGameState gameState = new IntervalGameState();
    gameState.correctGuesses = 0;
    gameState.totalGuesses = 0;
    gameState.intervalToGuess = generateRandomDyad(intervalSettings.lowestNote, intervalSettings.highestNote, intervalSettings.intervals);

    UIState uiState = createUI(listener);
    uiState.display.setOnClickListener(v -> {
      gameState.correctGuesses = 0;
      gameState.totalGuesses = 0;

      updateDisplayText(uiState, gameState);
    });

    state = new AppState();
    state.intervalSettings = intervalSettings;
    state.gameState = gameState;
    state.uiState = uiState;

    updateDisplayText(state.uiState, state.gameState);

    String soundFontName = "Yamaha-Grand-Lite-v2.0.sf2";
    Path soundFontPath = IOUtil.getAppStoragePath(getBaseContext(), soundFontName);

    if (!soundFontPath.toFile().exists()) {
      IOUtil.copyAssetToAppStorage(getBaseContext(), soundFontName, soundFontName);
    }

    FluidSynth.init();
    FluidSynth.loadSoundFont(soundFontPath.toString());
  }

  @Override
  protected void onResume() {
    super.onResume();

    playInterval(state.gameState.intervalToGuess);
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

    ButtonTheme intervalButtonTheme = new ButtonTheme();
    intervalButtonTheme.roundness = 8;
    intervalButtonTheme.clickableColor = Color.GREEN;
    intervalButtonTheme.unclickableColor = Color.GRAY;
    intervalButtonTheme.hoverColor = ColorUtils.blendARGB(intervalButtonTheme.clickableColor, Color.BLACK, 0.3f);

    UITheme theme = new UITheme();
    theme.intervalButton = intervalButtonTheme;

    result.theme = theme;

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
      //display.setBackgroundColor(Color.RED);
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
      //table.setBackgroundColor(Color.BLUE);
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

      int buttonMarginPx = dp(this, 2);
      int buttonPaddingPx = dp(this, 24);

      buttonParam.setMargins(buttonMarginPx, buttonMarginPx, buttonMarginPx, buttonMarginPx);

      for (int r = 0; r < intervalButtonLayout.length; r++) {
        TableRow row = new TableRow(this);
        table.addView(row);

        for (int c = 0; c < intervalButtonLayout[r].length; c++) {
          final int interval = intervalButtonLayout[r][c];
          if (interval == -1) {
            TextView emptyField = new TextView(this);
            emptyField.setPadding(buttonPaddingPx, buttonPaddingPx, buttonPaddingPx, buttonPaddingPx);
            row.addView(emptyField, buttonParam);
          } else {
            Button2 button = new Button2(this);
            button.setPadding(buttonPaddingPx, buttonPaddingPx, buttonPaddingPx, buttonPaddingPx);
            button.applyTheme(intervalButtonTheme);
            button.setSimpleOnTouchListener(new SimpleTouchListener() {
              @Override
              public void onUpTouchAction() {
                buttonOnClickListener.onClick(button, interval);
              }
            });

            String text = String.format("%s", intervalNames[interval]);
            button.setText(text);

            buttons.add(button);
            row.addView(button, buttonParam);
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

  static private IntervalButtonState resetIntervalButtonState(IntervalButtonState state) {
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

  static private void resetIntervalButtonStates(IntervalButtonState[] states) {
    for (int i = 0; i < states.length; i++) {
      states[i] = resetIntervalButtonState(states[i]);
    }
  }

  static private void updateIntervalButtonsBeingClickable(UIState uiState) {
    List<Button> buttons = uiState.intervalButtons;
    IntervalButtonState[] states = uiState.intervalButtonStates;

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

  @Override
  protected void onDestroy() {
    super.onDestroy();

    FluidSynth.deinit();
  }
}