package org.telegram.messenger;

import org.telegram.messenger.astra.AstraCrashReporter;
import org.telegram.messenger.regular.BuildConfig;

public class ApplicationLoaderImpl extends ApplicationLoader {
    @Override
    public void onCreate() {
        AstraCrashReporter.install(this);
        super.onCreate();
    }

    @Override
    protected String onGetApplicationId() {
        return BuildConfig.APPLICATION_ID;
    }
}
