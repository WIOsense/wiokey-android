package de.wiosense.wiokey.ui;

import android.app.Dialog;
import android.view.View;
import android.widget.ImageView;

import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import de.wiosense.wiokey.R;

public class LicenseDialog {

    private static final String TAG = "WioKey|TextDialog";

    private Dialog myDialog;

    public LicenseDialog(FragmentActivity fragmentActivity){
        myDialog = new Dialog(fragmentActivity);
        myDialog.setContentView(R.layout.popup_licenses);

        ImageView mExit = myDialog.findViewById(R.id.button_exit);

        /*
         * Licenses
         */

        RecyclerView mRecyclerViewLicenses = myDialog.findViewById(R.id.listview_licenses);
        List<ExpandAdapter.ExpandItem> mLicenseList = new ArrayList<>();

        mLicenseList.add(new ExpandAdapter.ExpandItem(R.string.license_1_title, R.string.license_1_description));
        mLicenseList.add(new ExpandAdapter.ExpandItem(R.string.license_2_title, R.string.license_2_description));
        mLicenseList.add(new ExpandAdapter.ExpandItem(R.string.license_3_title, R.string.license_3_description));
        mLicenseList.add(new ExpandAdapter.ExpandItem(R.string.license_4_title, R.string.license_4_description));
        mLicenseList.add(new ExpandAdapter.ExpandItem(R.string.license_5_title, R.string.license_5_description));

        ExpandAdapter mLicensesAdapter = new ExpandAdapter(mLicenseList, R.layout.recycler_licenses);
        mRecyclerViewLicenses.setAdapter(mLicensesAdapter);
        mRecyclerViewLicenses.setLayoutManager(new LinearLayoutManager(fragmentActivity));

        mExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myDialog.dismiss();
            }
        });

        myDialog.show();
    }

    public void dismiss() {
        myDialog.dismiss();
    }
}
