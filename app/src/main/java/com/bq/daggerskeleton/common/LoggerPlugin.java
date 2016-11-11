package com.bq.daggerskeleton.common;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pools;

import com.bq.daggerskeleton.flux.Store;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.Lazy;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.ClassKey;
import dagger.multibindings.IntoMap;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static android.util.Log.DEBUG;
import static android.util.Log.ERROR;
import static android.util.Log.INFO;
import static android.util.Log.WARN;
import static android.util.Log.d;
import static android.util.Log.e;

@PluginScope
public final class LoggerPlugin extends SimplePlugin {

   private static final boolean LOG_TO_FILE = true; //TODO: Build Config parameters
   private static final boolean LOG_TO_CONSOLE = true;
   private static final long LOG_FILES_MAX_AGE = 0; //Delete for every session

   private static final String STATE_LOG_FILE = "logFile";

   private static final DateFormat FILE_NAME_DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.US);
   private static final DateFormat LOG_FILE_DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS", Locale.US);

   private static final String TAG = "CameraLog";
   private static final String LOG_FOLDER = "__camera_log";

   private final Lazy<Map<Class<?>, Plugin>> pluginMap;
   private Lazy<Set<Store<?>>> storeSet;
   private final Context context;

   private File logRootDirectory;
   private File logFile;
   private final FileLogger fileLogger = new FileLogger();

   @Inject
   LoggerPlugin(Lazy<Map<Class<?>, Plugin>> pluginMap, Lazy<Set<Store<?>>> storeSet, Context context) {
      this.pluginMap = pluginMap;
      this.storeSet = storeSet;
      this.context = context;
   }

   @Override public PluginProperties getProperties() {
      return PluginProperties.MAX;
   }

   @Override public void onCreate(@Nullable Bundle savedInstanceState) {
      Timber.uprootAll();

      logRootDirectory = new File(context.getExternalCacheDir(), LOG_FOLDER);

      if (LOG_TO_CONSOLE) {
         Timber.plant(new Timber.DebugTree());
      }

      if (LOG_TO_FILE) {
         boolean logFileReady = createOrOpenLogFile(savedInstanceState);
         if (logFileReady) {
            Timber.plant(new Timber.Tree() {
               @Override
               protected void log(int priority, String tag, String message, Throwable t) {
                  fileLogger.log(priority, tag, message, t);
               }
            });
         } else {
            Timber.e("Error with log file %s, only console logs are available", LOG_FOLDER);
         }
      }

      deleteOldLogs(LOG_FILES_MAX_AGE)
            .subscribeOn(Schedulers.io())
            .subscribe();
   }

   @Override
   public void onComponentsCreated() {
      scanComponentsAndSubscribe()
            .subscribeOn(Schedulers.io())
            .subscribe();
   }

   /**
    * Delete any log files created under {@link #LOG_FOLDER} older that <code>maxAge</code> in ms.
    * <p>
    * Current log file wont be deleted.
    */
   public Completable deleteOldLogs(long maxAge) {
      return Completable.create(e -> {
         if (logRootDirectory == null || !logRootDirectory.exists()) return;
         int deleted = 0;
         final File[] files = logRootDirectory.listFiles();
         if (files != null) {
            for (File file : files) {
               long lastModified = System.currentTimeMillis() - file.lastModified();
               if (lastModified > maxAge) {
                  boolean isCurrentLogFile = logFile != null &&
                        file.getAbsolutePath().equals(logFile.getAbsolutePath());
                  if (!isCurrentLogFile && file.delete()) deleted++;
               }
            }
         }
         Timber.v("Deleted %d old log files", deleted);
         e.onComplete();
      });
   }

   /**
    * Flush pending writes and return the log file, if any.
    */
   @Nullable
   public File flushAndGetLogFile() {
      if (logFile == null) return null;
      fileLogger.flush();
      return logFile;
   }

   /**
    * Return the current log file, if any.
    * You may want to flush pending writes with {@link #flushAndGetLogFile()}
    */
   @Nullable
   public File getLogFile() {
      return logFile;
   }

   private Completable scanComponentsAndSubscribe() {
      return Completable.create(e -> registerToComponents());
   }

   @SuppressWarnings("unchecked")
   private void registerToComponents() {
      long start = System.nanoTime();

      for (Object object : pluginMap.get().values()) {
         final Class<?> clazz = object.getClass();
         for (Field field : clazz.getDeclaredFields()) {

            if (field.getAnnotation(AutoLog.class) == null) continue;
            final String tag = clazz.getSimpleName();
            final String observableName = field.getName();

            try {
               field.setAccessible(true);
               Object pluginField = field.get(object);
               subscribeToObservableUnsafe(pluginField, tag, observableName);
            } catch (Exception e) {
               Timber.e(e);
            }
         }
      }

      for (Store store : new ArrayList<>(storeSet.get())) {
         subscribeToObservableUnsafe(store.flowable(), store.getClass().getSimpleName(), "State");
      }

      long elapsed = System.nanoTime() - start;
      Timber.d("Logger scan completed in %d ms", TimeUnit.MILLISECONDS.convert(elapsed, TimeUnit.NANOSECONDS));
   }

   @SuppressWarnings("unchecked")
   private void subscribeToObservableUnsafe(Object observable, String tag, String linePrefix) {

      Consumer consumer = value -> Timber.tag(tag).d("%s <- %s", linePrefix, value);
      Consumer errorConsumer = value -> Timber.tag(tag).e("%s <- %s", linePrefix, value);

      Disposable disposable = null;
      if (observable instanceof Observable) {
         disposable = ((Observable) observable).observeOn(Schedulers.io()).subscribe(consumer, errorConsumer);
      } else if (observable instanceof Flowable) {
         disposable = ((Flowable) observable).observeOn(Schedulers.io()).subscribe(consumer, errorConsumer);
      } else if (observable instanceof Single) {
         disposable = ((Single) observable).observeOn(Schedulers.io()).subscribe(consumer, errorConsumer);
      } else if (observable instanceof Maybe) {
         disposable = ((Maybe) observable).observeOn(Schedulers.io()).subscribe(consumer, errorConsumer);
      }

      if (disposable != null) {
         track(disposable);
      }
   }

   private boolean createOrOpenLogFile(@Nullable Bundle savedInstanceState) {
      //Prepare folder
      File cacheDir = context.getCacheDir();
      File logRootDirectory = new File(cacheDir.getAbsolutePath(), LOG_FOLDER);
      if (!logRootDirectory.exists()) {
         if (!logRootDirectory.mkdir()) {
            e(TAG, "Unable to create log directory, nothing will be written on disk");
            return false;
         }
      }

      if (savedInstanceState == null) {
         String logFileName = String.format("%s-%s.log", "camera", FILE_NAME_DATE_FORMAT.format(new Date()));
         logFile = new File(logRootDirectory, logFileName);
         d(TAG, "New session, logs will be stored in: " + logFile.getAbsolutePath());
      } else {
         String logFileName = savedInstanceState.getString(STATE_LOG_FILE);
         if (logFileName != null) {
            logFile = new File(logFileName);
            d(TAG, "Resumed session, logs will be stored in: " + logFile.getAbsolutePath());
         }
      }

      if (logFile != null) {
         try {
            fileLogger.setWriter(new BufferedWriter(new FileWriter(logFile.getAbsolutePath(), true)));
            fileLogger.start();
            return true;
         } catch (IOException e) {
            e(TAG, "Error creating file: " + logFile.getAbsolutePath() + " ex: " + e.getMessage());
            fileLogger.exit();
            logFile = null;
            return false;
         }
      }

      return false;
   }

   @Override
   public void onSaveInstanceState(@NonNull Bundle outState) {
      if (logFile != null) {
         outState.putString(STATE_LOG_FILE, logFile.getAbsolutePath());
      }
   }

   @Override
   public void onDestroy() {
      fileLogger.exit();
      Timber.uprootAll();
   }

   private static class FileLogger extends Thread {
      private volatile Writer writer;

      void flush() {
         if (getWriter() != null) {
            try {
               getWriter().flush();
            } catch (IOException e) {
               e.printStackTrace();
            }
         }
      }

      Writer getWriter() {
         return writer;
      }

      void setWriter(Writer writer) {
         this.writer = writer;
      }

      private class LogLine {
         final Date date = new Date();
         int level;
         String log;
         String tag;

         void clear() {
            log = null;
            tag = null;
            date.setTime(0);
            level = 0;
         }

         String format() {
            String levelString;
            switch (level) {
               case DEBUG:
                  levelString = "Debug";
                  break;
               case INFO:
                  levelString = "Info";
                  break;
               case WARN:
                  levelString = "Warn️";
                  break;
               case ERROR:
                  levelString = "Error️";
                  break;
               default:
                  levelString = "Verbose️";
                  break;
            }

            return String.format(Locale.US, "[%s] %s/%s: %s\n", LOG_FILE_DATE_FORMAT.format(date), levelString, tag, log);
         }
      }

      //Very high capacity so the caller never blocks trying to place the log line
      private final BlockingQueue<LogLine> queue = new ArrayBlockingQueue<>(100);
      private final Pools.SynchronizedPool<LogLine> pool = new Pools.SynchronizedPool<>(20);

      public void run() {
         while (true) {
            try {
               LogLine logLine = queue.take();
               String line = logLine.format();
               if (getWriter() != null) getWriter().write(line);
               logLine.clear();
               pool.release(logLine);
            } catch (InterruptedException e) {
               break; //We are done
            } catch (IOException e) {
               Timber.e(e);
               e.printStackTrace();
               break;
            }
         }
         closeSilently();
      }

      void log(int priority, String tag, String message, Throwable t) {
         if (t != null) {
            message = getStackTraceString(t);
         }
         enqueueLog(priority, tag, message);
      }

      private void enqueueLog(int level, String tag, String log) {
         LogLine logLine = pool.acquire();
         if (logLine == null) {
            logLine = new LogLine();
         }

         logLine.tag = tag;
         logLine.log = log;
         logLine.level = level;
         logLine.date.setTime(System.currentTimeMillis());

         queue.offer(logLine);
      }

      private void exit() {
         this.interrupt();
      }

      private void closeSilently() {
         if (getWriter() != null) {
            try {
               getWriter().flush();
               getWriter().close();
            } catch (IOException e) {
               e.printStackTrace();
            }
         }
      }

      /**
       * Transform a stacktrace into a plain string.
       */
      private static String getStackTraceString(Throwable t) {
         // Don't replace this with Log.getStackTraceString() - it hides
         // UnknownHostException, which is not what we want.
         StringWriter sw = new StringWriter(256);
         PrintWriter pw = new PrintWriter(sw, false);
         t.printStackTrace(pw);
         pw.flush();
         return sw.toString();
      }
   }

   /**
    * Annotation for automatic subscribes to register for callbacks.
    */
   @Retention(RetentionPolicy.RUNTIME)
   @Target({ElementType.METHOD, ElementType.FIELD})
   public @interface AutoLog {

   }

   @Module
   public static abstract class LoggerModule {
      @Provides @PluginScope @IntoMap @ClassKey(LoggerPlugin.class)
      static Plugin provideLoggerPlugin(LoggerPlugin plugin) {
         return plugin;
      }
   }
}
