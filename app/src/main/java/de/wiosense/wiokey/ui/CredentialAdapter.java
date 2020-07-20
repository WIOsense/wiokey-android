package de.wiosense.wiokey.ui;

import android.content.Context;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import de.wiosense.webauthn.models.PublicKeyCredentialSource;
import de.wiosense.wiokey.R;

public class CredentialAdapter extends RecyclerView.Adapter<CredentialAdapter.ViewHolder> {

    private List<PublicKeyCredentialSource> accounts;
    private Context context;
    private ViewHolderListener listener;
    private final static byte[] emptyUserId = new byte[4];

    public interface ViewHolderListener {
        void onClick(int position);

        void onDelete(View view, int position);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        View itemView;
        TextView rpName;
        TextView userName;
        ImageView deleteIcon;
        private ViewHolderListener listener;

        ViewHolder(@NonNull View itemView, ViewHolderListener listener) {
            super(itemView);
            this.listener = listener;

            this.itemView = itemView;
            rpName = itemView.findViewById(R.id.text_account_domain);
            userName = itemView.findViewById(R.id.text_account_user);
            deleteIcon = itemView.findViewById(R.id.img_deleteicon);

            rpName.setOnClickListener(this);
            userName.setOnClickListener(this);
            deleteIcon.setOnClickListener(this);
        }

        @Override
        public void onClick(View view){
            if(view.equals(rpName) || view.equals(userName)) {
                listener.onClick(getAdapterPosition());
            } else if (view.equals(deleteIcon)) {
                listener.onDelete(view, getAdapterPosition());
            }
        }
    }

    public CredentialAdapter(Context ctx, List<PublicKeyCredentialSource> accounts, ViewHolderListener listener){
        context = ctx;
        this.accounts = accounts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CredentialAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.recycler_row_text,parent,false);
        return new ViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull CredentialAdapter.ViewHolder holder, int position) {
        holder.rpName.setText(accounts.get(position).rpId);
        String userName;
        if (Arrays.equals(accounts.get(position).userHandle, emptyUserId)) {
            userName = holder.itemView.getContext().getString(R.string.credMan_u2fCredential);
        } else if(accounts.get(position).userDisplayName == null) {
            userName = Base64.encodeToString(accounts.get(position).userHandle, Base64.NO_WRAP);
        } else {
            userName = accounts.get(position).userDisplayName;
        }
        holder.userName.setText(userName);
    }

    @Override
    public int getItemCount() {
        return accounts.size();
    }
}
