package ua.pp.laptev.mygallery;

import android.app.Application;
import android.content.Context;
import android.os.Handler;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class App extends Application {

    private static final String settings_file = "settings.json";

    public static Context appContext;
    private static Handler appHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();
        appHandler = new Handler(appContext.getMainLooper());
    }

    public static void runOnUIThread(Runnable runnable) {
        appHandler.postDelayed(runnable, 1);
    }

    private static File getSettingsFile() {
        return new File(appContext.getCacheDir(), settings_file);
    }

    public static Settings load_settings() {
        Gson gson = new Gson();
        try {
            File f = getSettingsFile();
            char[] buf = new char[(int) f.length()];
            try (FileReader fr = new FileReader(f)) {
                if (fr.read(buf) != buf.length) throw new IOException();
            }
            return gson.fromJson(new String(buf),
                    Settings.class);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return new Settings();
        }
    }

    public static void save_settings(Settings settings) {
        Gson gson = new Gson();
        try (FileWriter fw = new FileWriter(getSettingsFile())) {
            fw.write(gson.toJson(settings));
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

}
