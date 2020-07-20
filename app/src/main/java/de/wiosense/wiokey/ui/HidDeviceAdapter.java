package de.wiosense.wiokey.ui;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Set;

import de.wiosense.wiokey.R;
import de.wiosense.wiokey.utils.MutableSet;

public class HidDeviceAdapter extends RecyclerView.Adapter<HidDeviceAdapter.ViewHolder> {

    private Set<BluetoothDevice> bluetoothDevices;
    private Context context;
    private ViewHolderListener listener;

    public interface ViewHolderListener {
        void onClick(int position);

        boolean onDelete(int position);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener, View.OnLongClickListener {

        TextView devName, devAddr;
        ImageView devIcon;
        ConstraintLayout devGroup;
        ImageView devDelete;
        private ViewHolderListener listener;

        ViewHolder(@NonNull View itemView, ViewHolderListener listener){
            super(itemView);
            this.listener = listener;
            devName = itemView.findViewById(R.id.text1);
            devAddr = itemView.findViewById(R.id.text2);
            devGroup = itemView.findViewById(R.id.device_select_group);
            devIcon = itemView.findViewById(R.id.img_device);
            devDelete = itemView.findViewById(R.id.img_device_delete);
            devGroup.setOnClickListener(this);
            devDelete.setOnClickListener(this);
        }

        @Override
        public void onClick(View view){
            if (listener != null) {
                if (view.equals(devGroup)) {
                    listener.onClick(getAdapterPosition());
                } else if (view.equals(devDelete)) {
                    listener.onDelete(getAdapterPosition());
                }
            }
        }

        @Override
        public boolean onLongClick(View v) {
            if (listener != null) {
                return listener.onDelete(getAdapterPosition());
            }
            return false;
        }
    }

    public HidDeviceAdapter(Context ctx,
                            Set<BluetoothDevice> devices,
                            ViewHolderListener listener) {
        context = ctx;
        bluetoothDevices = devices;
        this.listener = listener;
    }

    @NonNull
    @Override
    public HidDeviceAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.recycler_row,parent,false);
        return new HidDeviceAdapter.ViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull HidDeviceAdapter.ViewHolder holder, int position) {
        holder.devName.setText(((MutableSet<BluetoothDevice>)bluetoothDevices).get(position).getName());
        holder.devAddr.setText(((MutableSet<BluetoothDevice>)bluetoothDevices).get(position).getAddress());
    }

    @Override
    public int getItemCount() {
        return bluetoothDevices.size();
    }
}
