package de.wiosense.wiokey.ui.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricPrompt.CryptoObject;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import de.wiosense.webauthn.models.PublicKeyCredentialSource;
import de.wiosense.wiokey.MainActivity;
import de.wiosense.wiokey.R;
import de.wiosense.wiokey.ui.AccountInfoDialog;
import de.wiosense.wiokey.ui.CredentialAdapter;

import de.wiosense.webauthn.util.WioBiometricPrompt.PromptCallback;
import de.wiosense.wiokey.utils.FirebaseManager;

import static de.wiosense.wiokey.utils.FirebaseManager.EVENT_NUMBEROFACCOUNTS;
import static de.wiosense.wiokey.utils.FirebaseManager.EVENT_ONPOPUP;

public class CredentialFragment extends Fragment {

    FloatingActionButton mDeleteAll;
    FloatingActionButton mResetPin;
    FloatingActionButton mHardwareBacked;
    AlertDialog mHardwareDialog;
    AlertDialog mPinDialog;
    RecyclerView mCredentials;
    CredentialAdapter mCredentialAdapter;
    Dialog mClientPinDialog;
    AccountInfoDialog mAccountInfoDialog;

    static final String TAG = "WioKey|CredentialFragment";

    List<PublicKeyCredentialSource> credentialList;

    CredentialAdapter.ViewHolderListener listener = new CredentialAdapter.ViewHolderListener() {
        @Override
        public void onClick(int position) {
            onSelect(position);
        }

        @Override
        public void onDelete(View view, int position) {
            final PromptCallback callback = new PromptCallback(true) {
                @Override
                public void onResult(boolean result, CryptoObject cryptoObject) {
                    if (!result) {
                        Toast.makeText(getActivity(),getString(R.string.credMan_operationCanceled),
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    mCredentials.removeView(view);
                    mCredentialAdapter.notifyItemRemoved(position);
                    mCredentialAdapter.notifyItemRangeChanged(position,credentialList.size());
                    final PublicKeyCredentialSource credential = credentialList.remove(position);

                    Toast.makeText(getActivity(),
                                    getString(R.string.credMan_removeSuccess,
                                            credential.rpId, credential.userDisplayName),
                                    Toast.LENGTH_LONG).show();
                }
            };

            if ((MainActivity.mAuthenticator != null) && (MainActivity.isOnForeground())) {
                MainActivity.mAuthenticator.deleteCredential(getActivity(),
                        credentialList.get(position),
                        callback);
            }
        }
    };

    View.OnClickListener listenerOnDeleteAll = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            final PromptCallback callback = new PromptCallback(true) {
                @Override
                public void onResult(boolean result, CryptoObject cryptoObject) {
                    if (!result) {
                        Toast.makeText(getActivity(),
                                        getString(R.string.credMan_operationCanceled),
                                        Toast.LENGTH_LONG).show();
                        return;
                    }

                    int listSize = credentialList.size();
                    mCredentials.removeAllViews();
                    mCredentialAdapter.notifyItemRangeRemoved(0, listSize);
                    mCredentialAdapter.notifyItemRangeChanged(0, listSize);
                    credentialList.clear();

                    Toast.makeText(getActivity(),
                            getString(R.string.credMan_resetSuccess),
                            Toast.LENGTH_LONG).show();
                }
            };

            if ((MainActivity.mAuthenticator != null) && (MainActivity.isOnForeground())) {
                MainActivity.mAuthenticator.deleteAllCredentials(getActivity(), callback);
            }
        }
    };

    View.OnClickListener listenerResetPin = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            final DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case (DialogInterface.BUTTON_POSITIVE): {
                            // We got a call to update (set / change) the PIN - so fire the popUp
                            onChangePinPopUp();
                            break;
                        }
                        case (DialogInterface.BUTTON_NEGATIVE): {
                            // We got a call to reset the PIN of the authenticator so we do so
                            final PromptCallback callback = new PromptCallback(true) {
                                @Override
                                public void onResult(boolean result, CryptoObject cryptoObject) {
                                    if (result) {
                                        Toast.makeText(getActivity(),
                                                getString(R.string.credMan_pinResetSuccess),
                                                Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(getActivity(),
                                                getString(R.string.credMan_pinResetCanceled),
                                                Toast.LENGTH_LONG).show();
                                    }
                                }
                            };

                            if ((MainActivity.mAuthenticator != null) && (MainActivity.isOnForeground())) {
                                MainActivity.mAuthenticator.resetPin(getActivity(), callback);
                            }
                            break;
                        }
                        default:
                            // Nothing to do here
                    }
                }
            };

            String body = getString(R.string.dialog_body_clientpin);
            if (MainActivity.mAuthenticator != null && MainActivity.mAuthenticator.isPinSet()) {
                body += getString(R.string.dialog_status_clientpin_set);
            } else {
                body += getString(R.string.dialog_status_clientpin_not_set);
            }
            mPinDialog = new AlertDialog.Builder(getContext(), R.style.DialogTheme)
                    .setTitle(getString(R.string.dialog_title_clientpin))
                    .setCancelable(true)
                    .setIcon(R.drawable.ic_pin_pad_white)
                    .setMessage(body)
                    .setNegativeButton(getString(R.string.dialog_btn_factoryreset_clientpin), listener)
                    .setPositiveButton(getString(R.string.dialog_btn_changepin_clientpin), listener)
                    .show();
        }
    };

    View.OnClickListener listenerHardwareBacked = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mHardwareDialog.show();
        }
    };

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_credential, container, false);

        mCredentials = root.findViewById(R.id.listview_credentials);
        mDeleteAll = root.findViewById(R.id.button_deleteall);
        mResetPin = root.findViewById(R.id.button_resetPin);
        mHardwareBacked = root.findViewById(R.id.button_hardwareBacked);
        mClientPinDialog = new Dialog(Objects.requireNonNull(getActivity()));

        mDeleteAll.setOnClickListener(listenerOnDeleteAll);
        mResetPin.setOnClickListener(listenerResetPin);
        mHardwareBacked.setOnClickListener(listenerHardwareBacked);

        mHardwareDialog = new AlertDialog.Builder(this.getContext(), R.style.DialogTheme)
                .setMessage(R.string.credMan_hardwareBacked)
                .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();

        SharedPreferences appPreferences = this.getContext().getSharedPreferences(
                                                                    MainActivity.PREFERENCES,
                                                                    Context.MODE_PRIVATE);
        String hasHwStorage = appPreferences.getString(MainActivity.HAS_HW_STORAGE, "false");
        if (!hasHwStorage.equals("true")){
            mHardwareBacked.hide();
        }

        try {
            credentialList = MainActivity.mAuthenticator.getAllCredentials();
            FirebaseManager.sendLogEvent(EVENT_NUMBEROFACCOUNTS,
                                         MainActivity.mAuthenticator.getAllCredentials().size());
        } catch (NullPointerException e){
            Log.e(TAG, "Exception caught on retrieving credentials: " + e);
            credentialList = new ArrayList<>();
        }

        mCredentialAdapter = new CredentialAdapter(getActivity(), credentialList, listener);

        mCredentials.setAdapter(mCredentialAdapter);
        mCredentials.setLayoutManager(new LinearLayoutManager(getActivity()));

        return root;
    }

    boolean onSelect(int position){
        mAccountInfoDialog = new AccountInfoDialog(getActivity(),credentialList.get(position));
        return true;
    }

    private void onChangePinPopUp() {
        FirebaseManager.sendLogEvent(EVENT_ONPOPUP,null);
        if (MainActivity.mAuthenticator != null && MainActivity.isOnForeground()) {
            boolean isPinSet = MainActivity.mAuthenticator.isPinSet();
            if (isPinSet) {
                mClientPinDialog.setContentView(R.layout.popup_changeclientpinview);
            } else {
                mClientPinDialog.setContentView(R.layout.popup_setclientpinview);
            }

            mClientPinDialog.show();
            EditText oldPinText = mClientPinDialog.findViewById(R.id.old_pin);
            EditText newPinText = mClientPinDialog.findViewById(R.id.new_pin);
            EditText repeatedPinText = mClientPinDialog.findViewById(R.id.repeated_pin);
            Button btnChangePin = mClientPinDialog.findViewById(R.id.btn_changepin);

            final Button.OnClickListener btnListener = new Button.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String pinCandidate = newPinText.getText().toString();
                    String oldPin;
                    if (isPinSet) {
                        oldPin = oldPinText.getText().toString();
                    } else {
                        oldPin = null;
                    }

                    // Minimal form verification prior to submitting job
                    if (!pinCandidate.equals(repeatedPinText.getText().toString())) {
                        Toast.makeText(getActivity(),
                                getString(R.string.credMan_changeClientPinNoMatch),
                                Toast.LENGTH_LONG).show();
                        newPinText.getText().clear();
                        if (isPinSet) oldPinText.getText().clear();
                        repeatedPinText.getText().clear();
                        return;
                    }

                    // Minimal form verification prior to submitting job
                    if (pinCandidate.length() < 4) {
                        Toast.makeText(getActivity(),
                                getString(R.string.credMan_changeClientPinTooShort),
                                Toast.LENGTH_LONG).show();
                        newPinText.getText().clear();
                        if (isPinSet) oldPinText.getText().clear();
                        repeatedPinText.getText().clear();
                        return;
                    }

                    final PromptCallback callback = new PromptCallback(true) {
                        @Override
                        public void onResult(boolean result, CryptoObject cryptoObject) {
                            if (result) {
                                Toast.makeText(getActivity(),
                                        getString(R.string.credMan_changeClientPinSuccess),
                                        Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(getActivity(),
                                        getString(R.string.credMan_changeClientPinCanceled),
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    };

                    // Now the job is belonging to the Authenticator internals
                    MainActivity.mAuthenticator.selfSetPin(getActivity(), pinCandidate, oldPin, callback);

                    // Once we are done we close the dialog and return to previous screen
                    mClientPinDialog.dismiss();
                }
            };

            btnChangePin.setOnClickListener(btnListener);
        }
    }

    private void onCleanUpDialogs() {
        if (mHardwareDialog != null) mHardwareDialog.dismiss();
        if (mPinDialog != null) mPinDialog.dismiss();
        if (mClientPinDialog != null) mClientPinDialog.dismiss();
        if (mAccountInfoDialog != null) mAccountInfoDialog.dismiss();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        onCleanUpDialogs();
    }
}
