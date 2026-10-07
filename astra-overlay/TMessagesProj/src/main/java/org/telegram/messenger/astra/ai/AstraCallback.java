package org.telegram.messenger.astra.ai;

public interface AstraCallback<T> {
    void onSuccess(T value);
    void onError(Throwable error);
}
