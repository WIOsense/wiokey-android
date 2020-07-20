package de.wiosense.wiokey.ui.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ShareCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import de.wiosense.wiokey.MainActivity;
import de.wiosense.wiokey.ui.ExpandAdapter;
import de.wiosense.wiokey.ui.ExpandAdapter.ExpandItem;
import de.wiosense.wiokey.IntroActivity;
import de.wiosense.wiokey.R;
import de.wiosense.wiokey.ui.LicenseDialog;
import de.wiosense.wiokey.ui.TextDialog;

public class AboutFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "WioKey|AboutFragment";

    public static final String TEXT_PRIVACY = "PrivacyPolicy";
    public static final String TEXT_TERMS = "TermsAndConditions";
    public static final String TEXT_COMPATIBILITY = "Compatibility";

    private Context context;
    private TextDialog mTermsDialog, mPrivacyDialog, mCompatDialog;
    private LicenseDialog mLicenseDialog;
//    private SubscribeDialog mSubscribeDialog;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_about, container, false);

        context = this.getContext();

        LinearLayout restartTutorial = root.findViewById(R.id.layout_tutorial);
        restartTutorial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(AboutFragment.this.getContext(), IntroActivity.class));
            }
        });

        LinearLayout shareApp = root.findViewById(R.id.layout_share);
        shareApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setChooserTitle(getString(R.string.about_shareTitle))
                        .setText(getString(R.string.about_shareOptInTextDescription)+"http://play.google.com/store/apps/details?id=" + getActivity().getPackageName())
                        .startChooser();
            }
        });

        FloatingActionButton mContact = root.findViewById(R.id.contact);
        FloatingActionButton mLinkedIn = root.findViewById(R.id.linkedin);
        FloatingActionButton mMedium = root.findViewById(R.id.medium);
        FloatingActionButton mEmail = root.findViewById(R.id.email);
        //TextView mSubscribe = root.findViewById(R.id.text_unsubscribe);
        TextView mTermsConditions = root.findViewById(R.id.text_termsconditions);
        TextView mPrivacyPolicy = root.findViewById(R.id.text_privacypolicy);
        TextView mLicenses = root.findViewById(R.id.text_licenseinfo);
        TextView mCompatibility = root.findViewById(R.id.text_compatibility);

        mContact.setOnClickListener(this);
        mLinkedIn.setOnClickListener(this);
        mMedium.setOnClickListener(this);
        mEmail.setOnClickListener(this);

        mCompatibility.setOnClickListener(this);
        mLicenses.setOnClickListener(this);
        mTermsConditions.setOnClickListener(this);
        mPrivacyPolicy.setOnClickListener(this);

        /*mSubscribe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try{
                    MainActivity activity = (MainActivity)getActivity();
                    mSubscribeDialog = new SubscribeDialog(getActivity(),activity.mDatabase,true);
                } catch (Exception e){
                    Log.e(TAG,"Unexpected Error");
                }
            }
        });*/

        /*
         * FAQ
         */

        RecyclerView mRecyclerViewFaq = root.findViewById(R.id.listview_faq);
        List<ExpandItem> mExpandItemList = new ArrayList<>();

        mExpandItemList.add(new ExpandItem(R.string.faq_1_question, R.string.faq_1_answer));
        mExpandItemList.add(new ExpandItem(R.string.faq_2_question, R.string.faq_2_answer));
        mExpandItemList.add(new ExpandItem(R.string.faq_3_question, R.string.faq_3_answer));
        mExpandItemList.add(new ExpandItem(R.string.faq_4_question, R.string.faq_4_answer));
        mExpandItemList.add(new ExpandItem(R.string.faq_5_question, R.string.faq_5_answer));
        mExpandItemList.add(new ExpandItem(R.string.faq_6_question, R.string.faq_6_answer));
        mExpandItemList.add(new ExpandItem(R.string.faq_7_question, R.string.faq_7_answer));
        mExpandItemList.add(new ExpandItem(R.string.faq_8_question, R.string.faq_8_answer));
        mExpandItemList.add(new ExpandItem(R.string.faq_9_question, R.string.faq_9_answer, getStorageString()));
        mExpandItemList.add(new ExpandItem(R.string.faq_10_question, R.string.faq_10_answer));
        mExpandItemList.add(new ExpandItem(R.string.faq_11_question, R.string.faq_11_answer));

        ExpandAdapter mExpandAdapter = new ExpandAdapter(mExpandItemList, R.layout.recycler_faq);
        mRecyclerViewFaq.setAdapter(mExpandAdapter);
        mRecyclerViewFaq.setLayoutManager(new LinearLayoutManager(getActivity()));

        return root;
    }
    
    private String getStorageString() {
        SharedPreferences appPreferences = context.getSharedPreferences(MainActivity.PREFERENCES, Context.MODE_PRIVATE);
        String preference = appPreferences.getString(MainActivity.HAS_HW_STORAGE, "unset");

        switch (preference) {
            case "true":
                return getString(R.string.faq_9_hardware);
            case "false":
                return getString(R.string.faq_9_sofware);
            case "unset":
            default:
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        SharedPreferences handlerPref = context.getSharedPreferences(MainActivity.PREFERENCES, Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = handlerPref.edit();
                        Boolean credentialsInHardware = null;

                        if (MainActivity.mAuthenticator != null) {
                            credentialsInHardware = MainActivity.mAuthenticator.credentialsInHardware();
                        }

                        if (credentialsInHardware == null) {
                            // Do nothing!
                        } else if (credentialsInHardware) {
                            editor.putString(MainActivity.HAS_HW_STORAGE, "true");
                        } else {
                            editor.putString(MainActivity.HAS_HW_STORAGE, "false");
                        }

                        editor.apply();
                    }
                });
                return getString(R.string.faq_9_empty);
        }
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case(R.id.contact):
                startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(getString(R.string.companyWebsiteURL))));
                break;
            case(R.id.linkedin):
                startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(getString(R.string.companyLinkedInURL))));
                break;
            case(R.id.medium):
                startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(getString(R.string.companyMediumURL))));
                break;
            case(R.id.email):
                String wioEmail = getString(R.string.companyEmail);
                Intent intent = new Intent (Intent.ACTION_VIEW , Uri.parse(getString(R.string.about_mailto, wioEmail)));
                startActivity(intent);
                break;
            case(R.id.text_termsconditions):
                mTermsDialog = new TextDialog(getActivity(),TEXT_TERMS);
                break;
            case(R.id.text_privacypolicy):
                mPrivacyDialog = new TextDialog(getActivity(),TEXT_PRIVACY);
                break;
            case(R.id.text_compatibility):
                mCompatDialog = new TextDialog(getActivity(),TEXT_COMPATIBILITY);
                break;
            case(R.id.text_licenseinfo):
                mLicenseDialog = new LicenseDialog(getActivity());
                break;
            default:
        }
    }

    private void cleanUpDialogs() {
        if (mTermsDialog != null) mTermsDialog.dismiss();
        if (mPrivacyDialog != null) mPrivacyDialog.dismiss();
        if (mCompatDialog != null) mCompatDialog.dismiss();
        if (mLicenseDialog != null) mLicenseDialog.dismiss();
//        if (mSubscribeDialog != null) mSubscribeDialog.dismiss();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cleanUpDialogs();
    }
}
