package de.wiosense.wiokey.ui;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import de.wiosense.webauthn.util.WioRequestDialog;
import de.wiosense.wiokey.R;

import static android.content.Context.MODE_PRIVATE;
import static de.wiosense.wiokey.MainActivity.PREFERENCES;
import static de.wiosense.wiokey.utils.FirebaseManager.DB_EMAIL;
import static de.wiosense.wiokey.utils.FirebaseManager.EMAIL_PUSHED;
import static de.wiosense.wiokey.utils.FirebaseManager.FIREBASE_DBID;
import static de.wiosense.wiokey.utils.FirebaseManager.SUBSCRIBE_EMAIL;

public class SubscribeDialog implements View.OnClickListener {

    private static final String TAG = "WioKey|RequestDialog";

    private FragmentActivity fragmentActivity;
    private Dialog myDialog;
    private EditText mEmail;
    private TextView mCancel, mSubscribe;

    private FirebaseDatabase mDatabase;
    private SharedPreferences appPreferences;
    private boolean unsubscribeOption;

    public SubscribeDialog(FragmentActivity fragmentActivity, FirebaseDatabase database, boolean unsubscribeOption){

        this.fragmentActivity = fragmentActivity;
        this.mDatabase = database;
        this.unsubscribeOption = unsubscribeOption;
        myDialog = new Dialog(fragmentActivity);

        myDialog.setContentView(R.layout.popup_emailregister);

        mEmail = myDialog.findViewById(R.id.editText_email);
        mCancel = myDialog.findViewById(R.id.text_cancel);
        mSubscribe = myDialog.findViewById(R.id.text_subscribe);

        if(unsubscribeOption){
            mCancel.setText(fragmentActivity.getString(R.string.subscribe_unsubscribe));
        }

        mCancel.setOnClickListener(this);
        mSubscribe.setOnClickListener(this);

        appPreferences = fragmentActivity.getSharedPreferences(PREFERENCES, MODE_PRIVATE);

        myDialog.show();
    }

    @Override
    public void onClick(View view){
        if(view.equals(mCancel)){
            if(unsubscribeOption){
                Log.d(TAG,"Unsubscribing");
                unsubscribe();
            } else{
                Log.d(TAG,"Cancelling Dialog");
                myDialog.dismiss();
            }
        } else if(view.equals(mSubscribe) && mEmail.getText().toString().contains("@")){
            Log.d(TAG,"Subscribing");
            subscribe();
        }
    }

    private void subscribe(){
        String storedEmail = appPreferences.getString(SUBSCRIBE_EMAIL,null);
        String email = mEmail.getText().toString();
        String dbId = appPreferences.getString(FIREBASE_DBID,null);

        if(storedEmail!=null && !storedEmail.equals(email)){
            WioRequestDialog dialog = WioRequestDialog.create(
                    fragmentActivity.getString(R.string.dialog_title_resubscribe),
                    fragmentActivity.getString(R.string.dialog_body_resubscribe,storedEmail),
                    new WioRequestDialog.PromptCallback() {
                @Override
                public void onResult(boolean result) {
                    if(result){
                        Log.d(TAG,"Replacing Email");
                        pushAccount(email, dbId, true);
                    }
                }
            });
            dialog.show(fragmentActivity);
        } else {
            Log.d(TAG,"Adding Email");
            pushAccount(email, null,false);
        }

        myDialog.dismiss();
    }

    private void pushAccount(String email, String dbId, boolean replace){
        SharedPreferences.Editor editor = appPreferences.edit();
        editor.putString(SUBSCRIBE_EMAIL,mEmail.getText().toString());
        editor.apply();
        Log.d(TAG,"Saved email offline");

        if(replace){
            try{
                mDatabase.getReference().child(dbId).child(DB_EMAIL).setValue(email);
            } catch (NullPointerException e){
                Log.e(TAG,"No DatabaseId was found");
            }
        } else {
            DatabaseReference myRef = mDatabase.getReference().push();
            myRef.child(DB_EMAIL).setValue(email);

            editor.putString(FIREBASE_DBID,myRef.getKey());
            editor.apply();
            Log.d(TAG,"Saved Dbid offline");
        }
        Toast.makeText(fragmentActivity,fragmentActivity.getString(R.string.toast_subscribed), Toast.LENGTH_LONG).show();

        editor.putBoolean(EMAIL_PUSHED,true);
        editor.apply();
        Log.d(TAG,"Saved email online");
    }

    private void unsubscribe(){
        String dbId = appPreferences.getString(FIREBASE_DBID,null);
        boolean emailPushed = appPreferences.getBoolean(EMAIL_PUSHED,false);
        if(emailPushed) {
            WioRequestDialog dialog = WioRequestDialog.create(
                    fragmentActivity.getString(R.string.dialog_title_unsubscribe),
                    fragmentActivity.getString(R.string.dialog_body_unsubscribe),
                    new WioRequestDialog.PromptCallback() {
                @Override
                public void onResult(boolean result) {
                    if(result){
                        SharedPreferences.Editor editor = appPreferences.edit();
                        editor.putString(SUBSCRIBE_EMAIL,null);
                        editor.apply();
                        Log.d(TAG,"Removed email offline");

                        mDatabase.getReference().child(dbId).removeValue();

                        editor.putBoolean(EMAIL_PUSHED,false);
                        editor.apply();
                        Log.d(TAG,"Removed email online");
                    }
                }
            });
            dialog.show(fragmentActivity);
        } else {
            Log.d(TAG,"User not registered");
        }

        Toast.makeText(fragmentActivity,fragmentActivity.getString(R.string.toast_unsubscribe), Toast.LENGTH_LONG).show();

        myDialog.dismiss();
    }

    public void dismiss() {
        myDialog.dismiss();
    }
}
