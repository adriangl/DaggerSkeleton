package com.bq.daggerskeleton.common;

import android.support.v4.util.Pools;

import com.bq.daggerskeleton.flux.Dispatcher;
import com.bq.daggerskeleton.flux.InitAction;
import com.bq.daggerskeleton.flux.Store;
import com.bq.daggerskeleton.sample.app.App;
import com.bq.daggerskeleton.sample.app.AppScope;

import org.reactivestreams.Publisher;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.inject.Inject;

import dagger.Lazy;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.ClassKey;
import dagger.multibindings.IntoMap;
import dagger.multibindings.IntoSet;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static android.content.ContentValues.TAG;
import static android.util.Log.DEBUG;
import static android.util.Log.ERROR;
import static android.util.Log.INFO;
import static android.util.Log.WARN;
import static android.util.Log.d;
import static android.util.Log.e;

public class LoggerStore extends Store<LoggerState> {

   private static final String LOG_FOLDER = "__camera_log";
   private static final long LOG_FILES_MAX_AGE = 0; //Delete for every session
   private static final DateFormat FILE_NAME_DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.US);
   private static final DateFormat LOG_FILE_DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS", Locale.US);

   private final App app;

   @Override protected LoggerState initialState() {
      return new LoggerState();
   }

   @Inject LoggerStore(App app, Lazy<Set<Store<?>>> stores) {
      this.app = app;
      Timber.plant(new FileLoggerTree());

      createLogFile()
            .concatWith(deleteOldLogs(LOG_FILES_MAX_AGE))
            .subscribeOn(Schedulers.io())
            .subscribe();

      Dispatcher.subscribe(InitAction.class)
            .observeOn(Schedulers.io())
            .subscribe(a -> {
               for (Store<?> store : stores.get()) {
                  subscribeToObservableUnsafe(store.flowable(), store.getClass().getSimpleName(), "State");
               }
            });
   }

   private Completable createLogFile() {
      return Completable.create(s -> {
         File cacheDir = app.getCacheDir();
         File logRootDirectory = new File(cacheDir.getAbsolutePath(), LOG_FOLDER);
         if (!logRootDirectory.exists()) {
            if (!logRootDirectory.mkdir()) {
               e(TAG, "Unable to create log directory, nothing will be written on disk");
               s.onError(new SecurityException());
               return;
            }
         }

         String logFileName = String.format("%s-%s.log", "camera", FILE_NAME_DATE_FORMAT.format(new Date()));
         File logFile = new File(logRootDirectory, logFileName);
         d(TAG, "New session, logs will be stored in: " + logFile.getAbsolutePath());
         try {
            state().fileLogger.setWriter(new BufferedWriter(new FileWriter(logFile.getAbsolutePath(), true)));
            state().fileLogger.start();
         } catch (IOException e) {
            e(TAG, "Error creating file: " + logFile.getAbsolutePath() + " ex: " + e.getMessage());
            state().fileLogger.exit();
            logFile = null;
         }

         if (logFile == null) {
            s.onError(new FileNotFoundException());
         } else {
            s.onComplete();
         }
      });
   }

   /**
    * Delete any log files created under {@link #LOG_FOLDER} older that <code>maxAge</code> in ms.
    * <p>
    * Current log file wont be deleted.
    */
   private Completable deleteOldLogs(long maxAge) {
      File logRootDirectory = new File(app.getCacheDir().getAbsolutePath(), LOG_FOLDER);
      return Completable.create(e -> {
         if (!logRootDirectory.exists()) return;
         int deleted = 0;
         final File[] files = logRootDirectory.listFiles();
         if (files != null) {
            for (File file : files) {
               long lastModified = System.currentTimeMillis() - file.lastModified();
               if (lastModified > maxAge) {
                  File logFile = state().currentLogFile;
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

   @SuppressWarnings("unchecked")
   public static Disposable subscribeToObservableUnsafe(Object observable, String tag, String linePrefix) {

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

      return disposable;
   }

   public static final class FileLogger extends Thread {
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

   @Module
   public static abstract class LoggerModule {
      @Provides @AppScope @IntoSet
      static Store<?> provideLoggerPlugin(LoggerStore store) {
         return store;
      }
   }

   private final class FileLoggerTree extends Timber.Tree {
      @Override protected void log(int priority, String tag, String message, Throwable t) {
         state().fileLogger.log(priority, tag, message, t);
      }
   }
}