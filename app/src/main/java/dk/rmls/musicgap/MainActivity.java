package dk.rmls.musicgap;

import static dk.rmls.musicgap.UIUtil.Button2;
import static dk.rmls.musicgap.UIUtil.SimpleTouchListener;
import static dk.rmls.musicgap.UIUtil.dp;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
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

    static public class IntervalButtonColor {
      public int notGuessedColor;
      public int guessedColor;
      public int lockedColor;
      public int hoverColor;
    }

    public IntervalButtonColor intervalButton;
  }

  static public class UIState {
    public UITheme theme;

    public List<Button> intervalButtons;
    public String[] intervalNames;
    public IntervalButtonState[] intervalButtonStates;

    public TextView scoreDisplay;
    public ImageView replayIcon;
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

  static private String getScoreDisplayText(UIState uiState, IntervalGameState gameState) {
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

  static public void updateScoreDisplayText(UIState uiState, IntervalGameState gameState) {
    String intervalToGuessText = getScoreDisplayText(uiState, gameState);
    uiState.scoreDisplay.setText(intervalToGuessText);
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

      IntervalButtonState state = uiState.intervalButtonStates[guessedInterval];
      if (!isIntervalButtonClickable(state)) return;

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
      updateScoreDisplayText(uiState, gameState);

      playInterval(gameState.intervalToGuess);
    };

    IntervalSettings intervalSettings = new IntervalSettings();
    intervalSettings.lowestNote = 21;
    intervalSettings.highestNote = 108;
    intervalSettings.intervals = IntStream.rangeClosed(-12, 12).toArray();

    IntervalGameState gameState = new IntervalGameState();
    gameState.correctGuesses = 0;
    gameState.totalGuesses = 0;
    gameState.intervalToGuess = generateRandomDyad(intervalSettings.lowestNote, intervalSettings.highestNote, intervalSettings.intervals);

    UIState uiState = createUI(listener);
    uiState.scoreDisplay.setOnClickListener(v -> {
      gameState.correctGuesses = 0;
      gameState.totalGuesses = 0;

      updateScoreDisplayText(uiState, gameState);
    });
    uiState.replayIcon.setOnClickListener(v -> playInterval(gameState.intervalToGuess));

    state = new AppState();
    state.intervalSettings = intervalSettings;
    state.gameState = gameState;
    state.uiState = uiState;

    updateScoreDisplayText(state.uiState, state.gameState);

    String soundFontName = "Yamaha-Grand-Lite-v2.0.sf2";
    Path soundFontPath = IOUtil.getAppStoragePath(getBaseContext(), soundFontName);

    if (!soundFontPath.toFile().exists()) {
      IOUtil.copyAssetToAppStorage(getBaseContext(), soundFontName, soundFontName);
    }

    FluidSynth.init();
    FluidSynth.loadSoundFont(soundFontPath.toString());
  }

  @Override
  protected void onStart() {
    super.onStart();

    playInterval(state.gameState.intervalToGuess);
  }

  static private void playInterval(Dyad interval) {
    new Thread(() -> {
      FluidSynth.noteOn(0, interval.noteA, 127);

      ThreadUtil.sleep(400);

      FluidSynth.noteOff(0, interval.noteA);
      FluidSynth.noteOn(0, interval.noteB, 127);

      ThreadUtil.sleep(400);

      FluidSynth.noteOff(0, interval.noteB);
    }).start();
  }

  private UIState createUI(CustomOnClickListener buttonOnClickListener) {
    UIState result = new UIState();

    UITheme.IntervalButtonColor intervalButtonColor = new UITheme.IntervalButtonColor();
    intervalButtonColor.notGuessedColor = Color.GREEN;
    intervalButtonColor.guessedColor = Color.GRAY;
    intervalButtonColor.lockedColor = ColorUtils.blendARGB(Color.GRAY, Color.BLACK, 0.5f);
    intervalButtonColor.hoverColor = ColorUtils.blendARGB(intervalButtonColor.notGuessedColor, Color.BLACK, 0.3f);

    UITheme theme = new UITheme();
    theme.intervalButton = intervalButtonColor;

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

    int numberOfIntervalStates = intervalNames.length;
    IntervalButtonState[] states = new IntervalButtonState[numberOfIntervalStates];
    for (int i = 0; i < states.length; i++) {
      states[i] = IntervalButtonState.notGuessed;
    }
    //states[0] = IntervalButtonState.locked; // @TODO: remove
    result.intervalButtonStates = states;

    {

      TextView display = new TextView(this);
      display.setId(View.generateViewId());

      int sizePx = UIUtil.dp(this, 8);
      display.setTextSize(sizePx);
      //display.setBackgroundColor(Color.RED);
      result.scoreDisplay = display;

      RelativeLayout.LayoutParams layout = new RelativeLayout.LayoutParams(
          ViewGroup.LayoutParams.WRAP_CONTENT,
          ViewGroup.LayoutParams.WRAP_CONTENT
      );
      layout.addRule(RelativeLayout.ALIGN_PARENT_TOP);
      layout.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
      container.addView(display, layout);
    }

    {
      ImageView replayView = new ImageView(this);
      replayView.setId(View.generateViewId());
      //replayView.setAdjustViewBounds(true);
      //replayView.setScaleType(ImageView.ScaleType.FIT_XY);
      replayView.setImageResource(R.drawable.play_circle);
      int sizePx = UIUtil.dp(this, 160);
      RelativeLayout.LayoutParams layout = new RelativeLayout.LayoutParams(
          sizePx, sizePx
      );
      int marginPx = dp(this, 24);
      layout.setMargins(marginPx, marginPx, marginPx, marginPx);
      layout.addRule(RelativeLayout.BELOW, result.scoreDisplay.getId());
      layout.addRule(RelativeLayout.CENTER_HORIZONTAL);
      container.addView(replayView, layout);

      result.replayIcon = replayView;
    }

    {
      TableLayout table = new TableLayout(this);
      table.setId(View.generateViewId());
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
      int buttonPaddingPx = dp(this, 18);

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
            button.setCornerRadius(8f);
            button.setBackgroundColor(getIntervalBottomColor(states[interval], theme));
            button.setPadding(buttonPaddingPx, buttonPaddingPx, buttonPaddingPx, buttonPaddingPx);
            button.setOnTouchListener(new SimpleTouchListener() {

              @Override
              public void onDownTouchAction() {
                if (states[interval] != IntervalButtonState.notGuessed) return;
                button.setBackgroundColor(theme.intervalButton.hoverColor);
              }

              @Override
              public void onUpTouchAction() {
                buttonOnClickListener.onClick(button, interval);

                int color = getIntervalBottomColor(states[interval], theme);
                button.setBackgroundColor(color);
              }

              @Override
              public void onCancelTouchAction() {
                int color = getIntervalBottomColor(states[interval], theme);
                button.setBackgroundColor(color);
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
    return result;
  }

  static private int getIntervalBottomColor(IntervalButtonState state, UITheme theme) {
    switch (state) {
      case notGuessed:
        return theme.intervalButton.notGuessedColor;
      case guessed:
        return theme.intervalButton.guessedColor;
      case locked:
        return theme.intervalButton.lockedColor;
      default:
        String error = String.format("unexpected interval button state: %s.", state);
        throw new IllegalStateException(error);
    }
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

      int color = getIntervalBottomColor(state, uiState.theme);
      button.setBackgroundColor(color);
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