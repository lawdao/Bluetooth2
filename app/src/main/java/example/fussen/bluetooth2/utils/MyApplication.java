package example.fussen.bluetooth2.utils;

import android.app.Application;
import android.content.Context;

/**
 * Created by Fussen on 2016/11/7.
 */

public class MyApplication extends Application {
    private static Context context;
    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
    }

    public static Context getContext(){
        return context;
    }
}
