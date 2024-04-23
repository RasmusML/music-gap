package dk.rmls.musicgap;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;

public class UIUtil {

  static public class ButtonTheme {
    public int clickableColor;
    public int unclickableColor;
    public int hoverColor;
    public float roundness;
  }

  static public class Button2 extends AppCompatButton {

    private ClickableListener componentClickableChangeListener;
    private SimpleTouchListener userTouchListener;
    private ButtonTheme theme;

    public Button2(@NonNull Context context) {
      super(context);
      init();
    }

    public Button2(@NonNull Context context, @Nullable AttributeSet attrs) {
      super(context, attrs);
      init();
    }

    public Button2(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
      super(context, attrs, defStyleAttr);
      init();
    }

    public void setSimpleOnTouchListener(SimpleTouchListener l) {
      this.userTouchListener = l;
    }

    private void init() {
      setBackground(new GradientDrawable());

      setOnTouchListener(new SimpleTouchListener() {
        @Override
        public void onDownTouchAction() {
          if (theme != null) {
            GradientDrawable background = (GradientDrawable) getBackground();
            background.setColor(theme.hoverColor);
            invalidate();
          }
          if (userTouchListener != null) userTouchListener.onDownTouchAction();
        }

        @Override
        public void onUpTouchAction() {
          if (theme != null) {
            GradientDrawable background = (GradientDrawable) getBackground();
            background.setColor(theme.clickableColor);
            invalidate();
          }
          if (userTouchListener != null) userTouchListener.onUpTouchAction();
        }

        @Override
        public void onCancelTouchAction() {
          if (theme != null) {
            GradientDrawable background = (GradientDrawable) getBackground();
            background.setColor(theme.clickableColor);
            invalidate();
          }
          if (userTouchListener != null) userTouchListener.onCancelTouchAction();
        }
      });

      componentClickableChangeListener = new ClickableListener() {
        @Override
        public void onChange(boolean clickable) {
          if (theme != null) {
            int color = clickable ? theme.clickableColor : theme.unclickableColor;
            GradientDrawable background = (GradientDrawable) getBackground();
            background.setColor(color);
            invalidate();
          }
        }
      };
    }

    public void applyTheme(ButtonTheme theme) {
      this.theme = theme;

      GradientDrawable shape = (GradientDrawable) getBackground();
      shape.setCornerRadius(theme.roundness);

      int color = isClickable() ? theme.clickableColor : theme.unclickableColor;
      shape.setColor(color);

      setBackground(shape);
      invalidate();
    }

    @Override
    public void setClickable(boolean clickable) {
      boolean stateChange = clickable != isClickable();

      super.setClickable(clickable);

      if (componentClickableChangeListener != null && stateChange) {
        componentClickableChangeListener.onChange(clickable);
      }
    }
  }

  static public class ClickableListener {
    public void onChange(boolean clickable) {
    }
  }

  static public class SimpleTouchListener implements View.OnTouchListener {

    private boolean touchStayedWithinViewBounds;

    @Override
    public boolean onTouch(View view, MotionEvent event) {
      if (!view.isClickable()) return false;

      switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
          touchStayedWithinViewBounds = true;
          onDownTouchAction();
          return true;

        case MotionEvent.ACTION_UP:
          if (touchStayedWithinViewBounds) {
            onUpTouchAction();
          }
          return true;

        case MotionEvent.ACTION_MOVE:
          if (touchStayedWithinViewBounds
              && !isMotionEventInsideView(view, event)) {
            onCancelTouchAction();
            touchStayedWithinViewBounds = false;
          }
          return true;

        case MotionEvent.ACTION_CANCEL:
          onCancelTouchAction();
          return true;

        default:
          return false;
      }
    }

    public void onDownTouchAction() {
    }

    public void onUpTouchAction() {
    }

    public void onCancelTouchAction() {
    }

    private boolean isMotionEventInsideView(View view, MotionEvent event) {
      Rect viewRect = new Rect(
          view.getLeft(),
          view.getTop(),
          view.getRight(),
          view.getBottom()
      );

      return viewRect.contains(
          view.getLeft() + (int) event.getX(),
          view.getTop() + (int) event.getY()
      );
    }
  }

  static public int dp(ContextThemeWrapper contextWrapper, float dp) {
    Resources res = contextWrapper.getResources();
    int px = (int) TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp,
        res.getDisplayMetrics()
    );
    return px;
  }
}
