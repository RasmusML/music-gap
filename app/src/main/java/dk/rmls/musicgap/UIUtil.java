package dk.rmls.musicgap;

import android.content.res.Resources;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;

public class UIUtil {

  static public int dp(ContextThemeWrapper c, float dp) {
    Resources res = c.getResources();
    int px = (int) TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp,
        res.getDisplayMetrics()
    );
    return px;
  }
}
