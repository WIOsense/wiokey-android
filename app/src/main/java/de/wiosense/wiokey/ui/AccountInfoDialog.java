package de.wiosense.wiokey.ui;

import android.app.Dialog;
import android.content.Context;
import android.util.Base64;
import android.util.Log;
import android.view.ViewManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Arrays;

import de.wiosense.webauthn.models.PublicKeyCredentialSource;
import de.wiosense.wiokey.R;

public class AccountInfoDialog {

    private static final String TAG = "WioKey|RequestDialog";

    private Dialog myDialog;
    private TextView mDomainName;
    private TextView mDomainID;
    private TextView mUser;
    private TextView mUserId;
    private TextView mCreationDate;
    private LinearLayout mDomainIDLayout;
    private LinearLayout mUserNameLayout;
    private LinearLayout mDateLayout;
    private ImageView mDomainImage;

    private final static byte[] emptyUserId = new byte[4];

    public AccountInfoDialog(Context ctx, PublicKeyCredentialSource account){

        myDialog = new Dialog(ctx);

        myDialog.setContentView(R.layout.popup_accountview);

        mDomainName = myDialog.findViewById(R.id.text_domain);
        mDomainID = myDialog.findViewById(R.id.text_domainId);
        mUser = myDialog.findViewById(R.id.text_username);
        mUserId = myDialog.findViewById(R.id.text_userId);
        mCreationDate = myDialog.findViewById(R.id.text_creationDate);

        mDomainIDLayout = myDialog.findViewById(R.id.layout_domainId);
        mUserNameLayout = myDialog.findViewById(R.id.layout_userName);
        mDateLayout = myDialog.findViewById(R.id.layout_creationDate);

        mDomainImage = myDialog.findViewById(R.id.image_domainPicture);

        if (account.rpIcon != null && !account.rpIcon.equals("")) {
            Log.d(TAG, account.rpId + " -> " + account.rpIcon);
            //TODO: Fetch image from URL if there is a connection and display
        }

        if (account.rpDisplayName != null && !account.rpDisplayName.equals("")) {
            mDomainName.setText(account.rpDisplayName);
            mDomainID.setText(account.rpId);
        } else {
            ((ViewManager) mDomainIDLayout.getParent()).removeView(mDomainIDLayout);
            mDomainName.setText(account.rpId);
        }

        if (account.userDisplayName == null || account.userDisplayName.equals("")) {
            ((ViewManager) mUserNameLayout.getParent()).removeView(mUserNameLayout);
        } else {
            mUser.setText(account.userDisplayName);
        }
        if (account.userHandle == null || Arrays.equals(account.userHandle, emptyUserId)) {
            mUserId.setText(ctx.getString(R.string.credMan_u2fCredential));
        } else {
            mUserId.setText(Base64.encodeToString(account.userHandle, Base64.NO_WRAP));
        }

        //TODO: Implement creation date
        ((ViewManager)mDateLayout.getParent()).removeView(mDateLayout);

        myDialog.show();
    }

    public void dismiss() {
        myDialog.dismiss();
    }
}
