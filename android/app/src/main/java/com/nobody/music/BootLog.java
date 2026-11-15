package com.nobody.music;

import android.content.Context;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class BootLog {

  private static File sDir;
  private static volatile boolean sInstalled;

  private BootLog() {}

  public static synchronized void initDir(Context ctx) {
    if (sDir != null) return;
    try {
      File d = null;
      try {
        d = ctx.getExternalFilesDir(null);
      } catch (Throwable ignored) {}
      if (d == null) {
        try {
          d = ctx.getFilesDir();
        } catch (Throwable ignored) {}
      }
      if (d != null) {
        d.mkdirs();
        sDir = d;
      }
    } catch (Throwable ignored) {}
  }

  public static synchronized void w(String msg) {
    android.util.Log.e("NobodyBoot", msg);
    File dir = sDir;
    if (dir == null) return;
    try {
      File f = new File(dir, "java_boot.log");
      FileWriter fw = new FileWriter(f, true);
      try {
        fw.write("[" + new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US).format(new Date()) + "] " + msg + "\n");
        fw.flush();
      } finally {
        fw.close();
      }
    } catch (Throwable ignored) {}
  }

  public static void installCrashHandler() {
    if (sInstalled) return;
    sInstalled = true;
    final Thread.UncaughtExceptionHandler def = Thread.getDefaultUncaughtExceptionHandler();
    Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
      @Override
      public void uncaughtException(Thread t, Throwable e) {
        try {
          StringWriter sw = new StringWriter();
          e.printStackTrace(new PrintWriter(sw));
          w("===== FATAL thread=" + t.getName() + " =====\n" + sw.toString());
        } catch (Throwable ignored) {}
        if (def != null) def.uncaughtException(t, e);
      }
    });
  }
}
