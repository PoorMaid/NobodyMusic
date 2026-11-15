package cn.toside.music.mobile;

import android.os.Bundle;

import com.reactnativenavigation.NavigationActivity;

public class MainActivity extends NavigationActivity {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    try {
      BootLog.w("MainActivity: onCreate enter");
    } catch (Throwable ignored) {}
    try {
      super.onCreate(savedInstanceState);
      BootLog.w("MainActivity: onCreate after super");
    } catch (Throwable t) {
      BootLog.w("MainActivity: onCreate EXCEPTION -> " + android.util.Log.getStackTraceString(t));
      throw t;
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    try {
      BootLog.w("MainActivity: onResume");
    } catch (Throwable ignored) {}
  }
}
