package uit.se.vdq.voicecommandprototype;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.AlarmClock;
import android.provider.ContactsContract;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.widget.Toast;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class VoiceCommandPrototypeService extends Service {
    private AudioManager audioManager;
    private SpeechRecognizer speechRecognizer;
    Intent intent;

    private static final String callCommandKeyword = "gọi";
    private final String unaccentedCallCommandKeyword = unaccentString(callCommandKeyword);
    private static final String callNumberKeyword = "số";
    private final String unaccentedCallNumberKeyword = unaccentString(callNumberKeyword);

    private static final String prepareSMSCommandKeyword = "soạn";
    private final String unaccentedPrepareSMSCommandKeyword = unaccentString(prepareSMSCommandKeyword);
    private static final String sendSMSCommandKeyword = "nhắn";
    private final String unaccentedSendSMSCommandKeyword = unaccentString(sendSMSCommandKeyword);
    private static final String smsNameKeyword = "cho";
    private static final String smsContextKeyword = "nội dung";
    private final String unaccentedSMSContextKeyword = unaccentString(smsContextKeyword);

    private static final String setAlarmKeyword = "đặt báo thức";
    private final String unaccentedSetAlarmKeyword = unaccentString(setAlarmKeyword);
    private static final String timeKeyword = "lúc";
    private final String unaccentedTimeKeyword = unaccentString(timeKeyword);
    private static final String hourKeyword = "giờ";
    private final String unaccentedHourKeyword = unaccentString(hourKeyword);
    private static final String minuteKeyword = "phút";
    private final String unaccentedMinuteKeyword = unaccentString(minuteKeyword);

    private static final String turnOnWifiKeyword = "bật WiFi";
    private final String unaccentedTurnOnWifiKeyword = unaccentString(turnOnWifiKeyword).toLowerCase();
    private static final String turnOffWifiKeyword = "tắt WiFi";
    private final String unaccentedTurnOffWifiKeyword = unaccentString(turnOffWifiKeyword).toLowerCase();

    private static final String activeName = "ở đợ";
    private final String unaccentedActiveName = unaccentString(activeName);

    private static final String openApplicationKeyword = "mở ứng dụng";
    private final  String unaccentedOpenApplicationKeyword = unaccentString(openApplicationKeyword);

    public VoiceCommandPrototypeService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0);
            }

            @Override
            public void onBeginningOfSpeech() {
            }

            @Override
            public void onRmsChanged(float v) {

            }

            @Override
            public void onBufferReceived(byte[] bytes) {

            }

            @Override
            public void onEndOfSpeech() {

            }

            @Override
            public void onError(int i) {
                restartListening();
            }

            @Override
            public void onResults(Bundle bundle) {
                ArrayList<String> results = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (results != null) {
                    String result = results.get(0);
                    String unaccentedResult = unaccentString(result).toLowerCase();

                    final int activeNameIndex = unaccentedResult.indexOf(unaccentedActiveName);
                    final int callCommandKeywordIndex = unaccentedResult.indexOf(unaccentedCallCommandKeyword);
                    final int smsCommandKeywordIndex = unaccentedResult.indexOf(unaccentedSendSMSCommandKeyword);
                    final int setAlarmKeywordIndex = unaccentedResult.indexOf(unaccentedSetAlarmKeyword);
                    final int turnOnWifiKeywordIndex = unaccentedResult.indexOf(unaccentedTurnOnWifiKeyword);
                    final int turnOffWifiKeywordIndex = unaccentedResult.indexOf(unaccentedTurnOffWifiKeyword);
                    final int openApplicationKeywordIndex = unaccentedResult.indexOf(unaccentedOpenApplicationKeyword);

                    if (activeNameIndex != -1) {
                        Toast.makeText(VoiceCommandPrototypeService.this, "Bạn cần gì?", Toast.LENGTH_SHORT).show();
                    }
                    else if (callCommandKeywordIndex != -1) {
                        String phoneNumber;
                        final int callNumberKeywordIndex = unaccentedResult.indexOf(unaccentedCallNumberKeyword);
                        if (callNumberKeywordIndex != -1) {
                            phoneNumber = unaccentedResult.substring(callNumberKeywordIndex + unaccentedCallNumberKeyword.length() + 1).replaceAll("\\s+", "");
                        } else {
                            final String name = unaccentedResult.substring(callCommandKeywordIndex + unaccentedCallCommandKeyword.length() + 1);
                            phoneNumber = findFirstPhoneNumberWithUnaccentedContactName(name);
                        }

                        if (phoneNumber != null && PhoneNumberUtils.isGlobalPhoneNumber(phoneNumber)) {
                            call(phoneNumber);
                        }
                    }
                    else if (smsCommandKeywordIndex != -1) {
                        final int nameKeywordIndex = unaccentedResult.indexOf(smsNameKeyword);
                        final int contextKeywordIndex = unaccentedResult.indexOf(unaccentedSMSContextKeyword);
                        if (nameKeywordIndex != -1 && contextKeywordIndex != -1) {
                            final String name = unaccentedResult.substring(nameKeywordIndex + smsNameKeyword.length() + 1, contextKeywordIndex - 1);
                            if (!name.isEmpty()) {
                                final String smsContext = result.substring(unaccentedResult.indexOf(unaccentedSMSContextKeyword) + unaccentedSMSContextKeyword.length() + 1);
                                if (!smsContext.isEmpty()) {
                                    final String phoneNumber = findFirstPhoneNumberWithUnaccentedContactName(name);
                                    if (phoneNumber != null) {
                                        final int prepareSMSCommandKeywordIndex = unaccentedResult.indexOf(unaccentedPrepareSMSCommandKeyword);
                                        if (prepareSMSCommandKeywordIndex != -1) {
                                            prepareSMS(phoneNumber.replaceAll("\\s+", ""), smsContext);
                                        } else {
                                            sendSMS(phoneNumber, smsContext);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else if (setAlarmKeywordIndex != -1) {
                        final int timeKeywordIndex = unaccentedResult.indexOf(unaccentedTimeKeyword);
                        if (timeKeywordIndex != -1) {
                            final int hourKeywordIndex = unaccentedResult.indexOf(unaccentedHourKeyword);
                            if (hourKeywordIndex != -1) {
                                final int hour = Integer.valueOf(unaccentedResult.substring(timeKeywordIndex + unaccentedTimeKeyword.length() + 1, hourKeywordIndex - 1));
                                int minutes = 0;;

                                final int minuteKeywordIndex = unaccentedResult.indexOf(unaccentedMinuteKeyword);
                                if (minuteKeywordIndex != -1) {
                                    minutes  = Integer.valueOf(unaccentedResult.substring(hourKeywordIndex + unaccentedHourKeyword.length() + 1, minuteKeywordIndex - 1));
                                }

                                setAlarm(hour, minutes);
                            }
                        }
                    }
                    else if (turnOnWifiKeywordIndex != -1) {
                        final WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                        wifiManager.setWifiEnabled(true);
                    }
                    else if (turnOffWifiKeywordIndex != -1) {
                        final WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                        wifiManager.setWifiEnabled(false);
                    }
                    else if (openApplicationKeywordIndex != -1) {
                        final String applicationName = unaccentedResult.substring(openApplicationKeywordIndex + unaccentedOpenApplicationKeyword.length() + 1).toLowerCase();
                        if (!applicationName.isEmpty()) {
                            final PackageManager packageManager = getPackageManager();
                            final List<PackageInfo> installedPackages = packageManager.getInstalledPackages(PackageManager.GET_META_DATA);
                            for (PackageInfo packageInfo : installedPackages) {
                                String packageName = packageInfo.packageName;
                                if (packageName.toLowerCase().contains(applicationName)) {
                                    final Intent intent = packageManager.getLaunchIntentForPackage(packageName);
                                    if  (intent != null) {
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                                        if (intent.resolveActivity(getPackageManager()) != null) {
                                            startActivity(intent);
                                        }
                                    }

                                    break;
                                }
                            }
                        }
                    }
                }

                restartListening();
            }

            @Override
            public void onPartialResults(Bundle bundle) {

            }

            @Override
            public void onEvent(int i, Bundle bundle) {

            }
        });

        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);

        speechRecognizer.startListening(intent);
    }

    private void restartListening() {
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);

        speechRecognizer.startListening(intent);
    }

    private String unaccentString(String string) {
        String temp = Normalizer.normalize(string, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(temp).replaceAll("").replaceAll("Đ", "D").replace("đ", "");
    }

    private String findFirstPhoneNumberWithUnaccentedContactName(String contactName) {
        if (contactName == null || contactName.isEmpty()) {
            return null;
        }

        String result = null;

        final String[] projection = {
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
                ContactsContract.CommonDataKinds.Phone.NUMBER
        };
        Cursor cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection, null, null, null);
        if (cursor != null) {
            final int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY);
            if (cursor.moveToFirst()) {
                do {
                    if (unaccentString(cursor.getString(nameIndex)).toLowerCase().contains(contactName.toLowerCase())) {
                        final int phoneNumberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                        result = cursor.getString(phoneNumberIndex);
                        break;
                    }
                }
                while (cursor.moveToNext());
            }
            cursor.close();
        }

        return result;
    }
    //Make call by phoneNumber
    private void call(String phoneNumber) {
        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:" + phoneNumber));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    private void prepareSMS(String phoneNumber, String smsContext) {
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + phoneNumber));
        intent.putExtra("sms_body", smsContext);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    private void sendSMS(String phoneNumber, String smsContext) {
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(phoneNumber, null, smsContext, null, null);
    }

    private void setAlarm(int hour, int minutes) {
        Intent intent = new Intent(AlarmClock.ACTION_SET_ALARM).putExtra(AlarmClock.EXTRA_HOUR, hour).putExtra(AlarmClock.EXTRA_MINUTES, minutes);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }
}
