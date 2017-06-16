package uit.se.vdq.voicecommandprototype;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class VoiceCommandPrototypeMainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_command_prototype_main);

        final Intent intent = new Intent(this, VoiceCommandPrototypeService.class);

        ((ToggleButton) findViewById(R.id.voiceCommandPrototypeServiceToggleButton)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    startService(intent);
                } else {
                    stopService(intent);
                }
            }
        });
    }
}
