package ua.pp.laptev.mygallery;

import static ua.pp.laptev.mygallery.App.load_settings;
import static ua.pp.laptev.mygallery.App.save_settings;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {

    private Settings settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View layout_settings = View.inflate(this, R.layout.layout_settings, null);
        setContentView(layout_settings);
        setTitle(R.string.settings);

        settings = load_settings();
        setValue(R.id.serverNameTextEdit, settings.server);
        setValue(R.id.serverUserTextEdit, settings.user);
        setValue(R.id.serverPassTextEdit, settings.pass);
        setValue(R.id.serverDomainTextEdit, settings.domain);
        setValue(R.id.serverShareTextEdit, settings.share);
        setValue(R.id.serverPathTextEdit, settings.path);
        setValue(R.id.showDelayTextEdit, settings.showDelay / 1000);
        setValue(R.id.showFilenameCheckBox, settings.showFilename);
        setValue(R.id.showClockCheckBox, settings.showClock);
        setValue(R.id.clockFormatTextEdit, settings.clockFormat);
        setValue(R.id.stretchImageCheckBox, settings.stretchImage);
    }

    View getViewById(int id) {
        View view = findViewById(id);
        if (view == null) throw new IllegalArgumentException();
        return view;
    }

    private void setValue(int id, Object value) {
        View view = getViewById(id);
        switch (Objects.requireNonNull(view.getClass().getSuperclass()).getSimpleName()) {
            case "EditText":
                if ((((EditText) view).getInputType() & InputType.TYPE_CLASS_NUMBER) > 0)
                    ((EditText) view).setText(String.valueOf(value));
                else
                    ((EditText) view).setText((String) value);
                break;
            case "CheckBox":
                ((CheckBox) view).setChecked((Boolean) value);
                break;
        }
    }

    private Object getValue(int id) {
        View view = getViewById(id);
        switch (Objects.requireNonNull(view.getClass().getSuperclass()).getSimpleName()) {
            case "EditText":
                if ((((EditText) view).getInputType() & InputType.TYPE_CLASS_NUMBER) > 0)
                    return Integer.parseInt(((EditText) view).getText().toString());
                else
                    return ((EditText) view).getText().toString();
            case "CheckBox":
                return ((CheckBox) view).isChecked();
        }
        throw new IllegalArgumentException();
    }

    @Override
    protected void onPause() {
        super.onPause();
        settings.server = (String) getValue(R.id.serverNameTextEdit);
        settings.user = (String) getValue(R.id.serverUserTextEdit);
        settings.pass = (String) getValue(R.id.serverPassTextEdit);
        settings.domain = (String) getValue(R.id.serverDomainTextEdit);
        settings.share = (String) getValue(R.id.serverShareTextEdit);
        settings.path = (String) getValue(R.id.serverPathTextEdit);
        settings.showDelay = (int) getValue(R.id.showDelayTextEdit) * 1000;
        settings.showFilename = (boolean) getValue(R.id.showFilenameCheckBox);
        settings.showClock = (boolean) getValue(R.id.showClockCheckBox);
        settings.clockFormat = (String) getValue(R.id.clockFormatTextEdit);
        settings.stretchImage = (boolean) getValue(R.id.stretchImageCheckBox);
        save_settings(settings);
    }
}
