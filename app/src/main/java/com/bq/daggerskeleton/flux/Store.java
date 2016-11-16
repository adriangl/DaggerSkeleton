package com.bq.daggerskeleton.flux;

import io.reactivex.Flowable;
import io.reactivex.processors.PublishProcessor;

public abstract class Store<S> {

    private S state = initialState();

    private PublishProcessor<S> statePublishProcessor = PublishProcessor.create();

    protected abstract S initialState();

    public S state() {
        return state;
    }

    protected void setState(S state) {
        if (this.state.equals(state)) return;
        this.state = state;
        statePublishProcessor.onNext(state);
    }

    public Flowable<S> flowable() {
        return statePublishProcessor;
    }


}
