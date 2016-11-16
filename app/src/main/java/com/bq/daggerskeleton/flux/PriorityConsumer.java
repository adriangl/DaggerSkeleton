package com.bq.daggerskeleton.flux;

import android.support.annotation.NonNull;

import io.reactivex.functions.Consumer;

public class PriorityConsumer<T extends Action> implements Consumer<Action>, Comparable<PriorityConsumer<T>> {
    private final Consumer<Action> consumer;
    private final int priority;

    public PriorityConsumer(@NonNull Consumer<T> consumer, int priority) {
        this.consumer = (Consumer<Action>) consumer;
        this.priority = priority;
    }

    @Override
    public void accept(Action t) throws Exception {
        consumer.accept(t);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PriorityConsumer<?> that = (PriorityConsumer<?>) o;

        if (priority != that.priority) return false;
        return consumer.equals(that.consumer);
    }

    @Override
    public int hashCode() {
        int result = consumer.hashCode();
        result = 31 * result + priority;
        return result;
    }

    @Override
    public int compareTo(PriorityConsumer<T> other) {
        return Integer.compare(this.priority, other.priority);
    }
}
