package com.brianthetall.android.sdrive.adapter;

import java.util.ArrayList;
import java.util.List;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.brianthetall.android.sdrive.EncryptedFile;
import com.brianthetall.android.sdrive.FileEndpoint;
import com.brianthetall.android.sdrive.R;

public class EncryptedFileAdapter extends BaseAdapter {
    
    private final List<EncryptedFile> files = new ArrayList<EncryptedFile>();
    private final Context context;
    private final FileEndpoint endpoint;
    private final LayoutInflater inflater;
    
    public EncryptedFileAdapter(Context context, FileEndpoint endpoint) {
        this.context = context;
        this.endpoint = endpoint;
        this.inflater = LayoutInflater.from(context);
    }
    
    public void refresh() {
        new ListUpdater().execute(); // updates this.files
    }

    @Override
    public int getCount() {
        return files.size();
    }

    @Override
    public Object getItem(int position) {
        return files.get(position);
    }

    @Override
    public long getItemId(int position) {
        return files.get(position).getId().hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LinearLayout view;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.file_item, parent, false);
        }
        view = (LinearLayout) convertView;
        TextView text = (TextView) view.findViewById(R.id.file_item_text_view);
        text.setText(files.get(position).getName());
        return view;
    }
    
    private final class ListUpdater extends AsyncTask<Void, Void, List<EncryptedFile>> implements DialogInterface.OnCancelListener {
        
        private final ProgressDialog dialog = new ProgressDialog(context);
        private boolean userCancelled;
        
        public ListUpdater() {
            dialog.setMessage(context.getString(R.string.loading));
            dialog.setIndeterminate(true);
            dialog.setCancelable(true);
            dialog.setCanceledOnTouchOutside(false);
            dialog.setOnCancelListener(this);
        }
        
        @Override
        protected void onPreExecute() {
            files.clear();
            notifyDataSetChanged();
            dialog.show();
        }
        
        @Override
        protected List<EncryptedFile> doInBackground(Void... params) {
            try {
                return endpoint.listFiles();
            } catch (Exception ex) {
                Log.e(EncryptedFileAdapter.class.getSimpleName(), "Failed to fetch files", ex);
                cancel(false);
                return null;
            }
        }
        
        @Override
        protected void onPostExecute(List<EncryptedFile> result) {
            files.addAll(result);
            notifyDataSetChanged();
            dialog.dismiss();
        }
        
        @Override
        protected void onCancelled() {
            dialog.dismiss();
            if (!userCancelled) {
                Toast.makeText(context, R.string.list_update_failed, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            Toast.makeText(context, R.string.cancelled, Toast.LENGTH_SHORT).show();
            userCancelled = true;
            cancel(false);
        }
        
    }

}
