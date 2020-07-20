package de.wiosense.wiokey;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.hololo.tutorial.library.Step;
import com.hololo.tutorial.library.TutorialActivity;


public class IntroActivity extends TutorialActivity {

    private final static String TAG = "WioKey|IntroActivity";

    private boolean askForCredentials;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        askForCredentials = intent.getBooleanExtra(MainActivity.ASK_CREDENTIALS, false);

        addFragment(new Step.Builder()
                .setTitle(getString(R.string.tutorial_1_title))
                .setContent(getString(R.string.tutorial_1_content))
                .setSummary(getString(R.string.tutorial_1_summary))
                .setBackgroundColor(R.color.colorPrimary)
                .setDrawable(R.drawable.wiokey_by_wiosense_white).build());

        addFragment(new Step.Builder()
                .setTitle(getString(R.string.tutorial_2_title))
                .setContent(getString(R.string.tutorial_2_content))
                .setSummary(getString(R.string.tutorial_2_summary))
                .setBackgroundColor(R.color.colorPrimary)
                .setDrawable(R.drawable.tutorial_fingerprint).build());

        addFragment(new Step.Builder()
                .setTitle(getString(R.string.tutorial_3_title))
                .setContent(getString(R.string.tutorial_3_content))
                .setSummary(getString(R.string.tutorial_3_summary))
            .setBackgroundColor(R.color.colorPrimary)
            .setDrawable(R.drawable.tutorial_android_devices).build());

        addFragment(new Step.Builder()
                .setTitle(getString(R.string.tutorial_4_title))
                .setContent(getString(R.string.tutorial_4_content))
                .setSummary(getString(R.string.tutorial_4_summary))
                .setBackgroundColor(R.color.colorPrimary)
                .setDrawable(R.drawable.tutorial_upwards).build());
    }

    @Override
    public void finishTutorial() {
        if (askForCredentials) {
            SharedPreferences appPreferences = getSharedPreferences(MainActivity.PREFERENCES, MODE_PRIVATE);
            SharedPreferences.Editor editor = appPreferences.edit();
            editor.putBoolean(MainActivity.FIRST_START, false).commit();
            startActivity(new Intent(IntroActivity.this, SplashActivity.class));
        }
        finish();
    }
}
