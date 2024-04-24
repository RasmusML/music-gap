package dk.rmls.musicgap;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;

public class UIUtil {

  static public class Button2 extends AppCompatButton {

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

    private void init() {
      setBackground(new GradientDrawable());
    }

    @Override
    public void setBackgroundColor(int color) {
      GradientDrawable drawable = (GradientDrawable) getBackground();
      drawable.setColor(color);
    }

    public void setCornerRadius(float radius) {
      GradientDrawable drawable = (GradientDrawable) getBackground();
      drawable.setCornerRadius(radius);
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