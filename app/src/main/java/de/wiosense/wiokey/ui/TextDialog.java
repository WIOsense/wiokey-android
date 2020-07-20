package de.wiosense.wiokey.ui;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.FragmentActivity;

import de.wiosense.wiokey.MainActivity;
import de.wiosense.wiokey.R;

import static android.content.Context.MODE_PRIVATE;
import static de.wiosense.wiokey.MainActivity.PREFERENCES;
import static de.wiosense.wiokey.ui.fragments.AboutFragment.TEXT_COMPATIBILITY;
import static de.wiosense.wiokey.ui.fragments.AboutFragment.TEXT_PRIVACY;
import static de.wiosense.wiokey.ui.fragments.AboutFragment.TEXT_TERMS;
import static de.wiosense.wiokey.utils.FirebaseManager.FIREBASE_OPTOUT_CRASHES;
import static de.wiosense.wiokey.utils.FirebaseManager.FIREBASE_OPTOUT_EVENTS;
import static de.wiosense.wiokey.utils.FirebaseManager.FIREBASE_OPTOUT_NOTIFICATIONS;

public class TextDialog implements CompoundButton.OnCheckedChangeListener {

    private static final String TAG = "WioKey|TextDialog";

    private Dialog myDialog;
    private FragmentActivity fragmentActivity;

    private  SharedPreferences mPreferences;

    public TextDialog(FragmentActivity fragmentActivity, String textOption){
        this.fragmentActivity = fragmentActivity;
        myDialog = new Dialog(fragmentActivity);
        myDialog.setContentView(R.layout.popup_justtext);

        LinearLayout mPrivacyLayout = myDialog.findViewById(R.id.layout_privacy);
        SwitchCompat mOptEvents = myDialog.findViewById(R.id.switch_events);
        SwitchCompat mOptCrashes = myDialog.findViewById(R.id.switch_crash);
        SwitchCompat mOptNotifications = myDialog.findViewById(R.id.switch_notifications);

        TextView mText = myDialog.findViewById(R.id.text_longtext);
        ImageView mExit = myDialog.findViewById(R.id.button_exit);

        mPreferences = fragmentActivity.getSharedPreferences(PREFERENCES, MODE_PRIVATE);

        switch(textOption){
            case TEXT_PRIVACY:
                boolean optedoutEvents = mPreferences.getBoolean(FIREBASE_OPTOUT_EVENTS, false);
                boolean optedoutCrashes = mPreferences.getBoolean(FIREBASE_OPTOUT_CRASHES, false);
                boolean optedoutNotifications = mPreferences.getBoolean(FIREBASE_OPTOUT_NOTIFICATIONS, false);

                if(optedoutEvents){
                    mOptEvents.setChecked(false);
                }
                if(optedoutCrashes){
                    mOptCrashes.setChecked(false);
                }
                if(optedoutNotifications){
                    mOptNotifications.setChecked(false);
                }

                mText.setText(R.string.privacypolicy);
                mPrivacyLayout.setVisibility(View.VISIBLE);
                break;
            case TEXT_TERMS:
                mText.setText(R.string.termsconditions);
                mPrivacyLayout.setVisibility(View.GONE);
                break;
            case TEXT_COMPATIBILITY:
                mText.setText(R.string.compatibilityinfo);
                mText.setMovementMethod(LinkMovementMethod.getInstance());
                mPrivacyLayout.setVisibility(View.GONE);
                break;
            default:
                Log.e(TAG,"Invalid text option to be displayed");
        }

        mOptEvents.setOnCheckedChangeListener(this);
        mOptCrashes.setOnCheckedChangeListener(this);
        mOptNotifications.setOnCheckedChangeListener(this);

        mExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myDialog.dismiss();
            }
        });

        myDialog.show();

    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked){
        SharedPreferences.Editor editor = mPreferences.edit();
        switch(buttonView.getId()){
            case R.id.switch_events:
                editor.putBoolean(FIREBASE_OPTOUT_EVENTS,!isChecked);
                editor.apply();
                ((MainActivity)fragmentActivity).mFirebaseManager.subscribeAnalytics(isChecked);
                Log.d(TAG,"Set Optout Events to"+!isChecked);
                break;
            case R.id.switch_crash:
                editor.putBoolean(FIREBASE_OPTOUT_CRASHES,!isChecked);
                editor.apply();
                ((MainActivity)fragmentActivity).mFirebaseManager.subscribeCrash(isChecked);
                Log.d(TAG,"Set Optout Events to"+!isChecked);
                break;
            case R.id.switch_notifications:
                editor.putBoolean(FIREBASE_OPTOUT_NOTIFICATIONS,!isChecked);
                editor.apply();
                ((MainActivity)fragmentActivity).mFirebaseManager.subscribeNotifications(isChecked);
                Log.d(TAG,"Set Optout Events to"+!isChecked);
                break;
            default:
                break;
        }
    }

    public void dismiss() {
        myDialog.dismiss();
    }
}
