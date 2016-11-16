package com.bq.daggerskeleton.flux;

import android.os.Looper;
import android.support.annotation.IntRange;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

import io.reactivex.functions.Consumer;

public class Dispatcher {

    private static final String TAG = "Dispatcher";

    private static final int DEFAULT_PRIORITY = 50;
    private static final int MIN_PRIORITY = 100;
    private static final int MAX_PRIORITY = 0;

    private static final Map<Class<? extends Action>, PriorityQueue<PriorityConsumer<? extends Action>>> consumerActionMap = new HashMap<>();

    public static void dispatch(Action action) {
        isRunningInUIThread();

        Log.d(TAG, String.format("Action -> %s", action));

        if (consumerActionMap.containsKey(action.getClass())) {
            for (PriorityConsumer<? extends Action> consumer : consumerActionMap.get(action.getClass())) {
                try {
                    consumer.accept(action);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public static <T extends Action> void subscribe(Class<T> actionClass, Consumer<T> consumer) {
        subscribe(actionClass, consumer, DEFAULT_PRIORITY);
    }

    public static <T extends Action> void subscribe(Class<T> actionClass,
                                                    Consumer<T> consumer,
                                                    @IntRange(from = MAX_PRIORITY, to = MIN_PRIORITY) int priority) {
        isRunningInUIThread();

        Log.d(TAG,
                String.format(
                        "Subscription -> %s with priority %d from %s.%s",
                        actionClass.getSimpleName(),
                        priority,
                        Thread.currentThread().getStackTrace()[4].getClassName(),
                        Thread.currentThread().getStackTrace()[4].getMethodName()));

        if (!consumerActionMap.containsKey(actionClass)) {
            consumerActionMap.put(actionClass, new PriorityQueue<PriorityConsumer<? extends Action>>());
        }
        consumerActionMap.get(actionClass).add(new PriorityConsumer<>(consumer, priority));
    }

    private static void isRunningInUIThread() {
        if (!Thread.currentThread().equals(Looper.getMainLooper().getThread())) {
            throw new UnsupportedOperationException("The dispatcher must be called from the UI thread only");
        }
    }
}
