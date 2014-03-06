package com.brianthetall.android.sdrive.activity;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;
import org.springframework.http.MediaType;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.WindowManager.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.brianthetall.android.sdrive.EncryptedFile;
import com.brianthetall.android.sdrive.FileEndpoint;
import com.brianthetall.android.sdrive.FileEndpointFactory;
import com.brianthetall.android.sdrive.GDriveFile;
import com.brianthetall.android.sdrive.R;
import com.brianthetall.android.sdrive.util.AccountUtil;

@EActivity(R.layout.dialog_upload)
public class UploadActivity extends SherlockActivity {
    
    private FileEndpoint mEndpoint;
    private String[] mAccounts;
    private ContentResolver mContentResolver;
    
    private Set<EncryptedFile> mFilesToUpload;
    
    @ViewById(R.id.upload_filename)
    EditText mFilename;
    
    @ViewById(R.id.upload_account_spinner)
    Spinner mAccountSpinner;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContentResolver = getContentResolver();
        mFilesToUpload = parseIntent(getIntent());
        if (mFilesToUpload.isEmpty()) {
            showInvalidAndFinish();
        }
        mEndpoint = FileEndpointFactory.createEndpoint(this);
        mAccounts = AccountUtil.getAccountNames(this);
    }
    
    @AfterViews
    void initForm() {
        if (Intent.ACTION_SEND_MULTIPLE.equals(getIntent().getAction())) {
            mFilename.setText("(" + mFilesToUpload.size() + " files)");
            mFilename.setEnabled(false);
        } else {
            mFilename.setText(mFilesToUpload.iterator().next().getName());
        }
    }
    
    @AfterViews
    void initAccountSpinner() {
        ArrayAdapter<String> accountAdapter = new ArrayAdapter<String>(this, R.layout.sherlock_spinner_dropdown_item, mAccounts);
        mAccountSpinner.setAdapter(accountAdapter);
        String account = AccountUtil.getMostRecentAccountName(this, mAccounts[mAccountSpinner.getSelectedItemPosition()]);
        for (int i=0; i<mAccounts.length; i++) {
            if (mAccounts[i].equals(account)) {
                mAccountSpinner.setSelection(i);
            }
        }
    }
    
    @Click(R.id.upload_ok)
    void okClicked() {
        if (mFilesToUpload.size() == 1) {
            String filename = mFilename.getText().toString();
            EncryptedFile file = mFilesToUpload.iterator().next();
            mFilesToUpload.clear();
            mFilesToUpload.add(new GDriveFile(file.getId(), filename, file.getUri(), file.getContent()));
        }
        for (EncryptedFile file : mFilesToUpload) {
            mEndpoint.uploadFile(file);
        }
        if (Intent.ACTION_SEND_MULTIPLE.equals(getIntent().getAction())) {
            Toast.makeText(this, R.string.uploading_multiple, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.uploading, Toast.LENGTH_SHORT).show();
        }
        finish();
    }
    
    @Click(R.id.upload_cancel)
    void cancelClicked() {
        finish();
    }
    
    private Set<EncryptedFile> parseIntent(Intent intent) {
        Set<EncryptedFile> files = new HashSet<EncryptedFile>();
        logIntent(intent);
        if (Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction())) {
            Bundle extras = intent.getExtras();
            if (extras.containsKey(Intent.EXTRA_STREAM)) {
                if (extras.get(Intent.EXTRA_STREAM) instanceof ArrayList) {
                    for (Uri uri : extras.<Uri>getParcelableArrayList(Intent.EXTRA_STREAM)) {
                        String filename = getFileName(uri);
                        if (filename != null) {
                            files.add(GDriveFile.withUri(filename, uri));
                        } else {
                            showInvalidAndFinish();
                        }
                    }
                }
            }
        } else if (intent.hasExtra(Intent.EXTRA_STREAM)){
            Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            files.add(GDriveFile.withUri(getFileName(uri), uri));
        } else if (MediaType.TEXT_PLAIN_VALUE.equals(intent.getType())) {
            String filename = (intent.hasExtra(Intent.EXTRA_SUBJECT)) 
                    ? intent.getStringExtra(Intent.EXTRA_SUBJECT) 
                    : null;
            files.add(GDriveFile.withTextContent(filename, intent.getStringExtra(Intent.EXTRA_TEXT)));
        }
        return files;
    }

    private void logIntent(Intent intent) {
        System.out.println("Uri: " + intent.getData());
        System.out.println("Type: " + intent.getType());
        System.out.println("Scheme: " + intent.getScheme());
        System.out.println("___ Extras: ___");
        Bundle extras = intent.getExtras();
        if (extras != null) {
            for (String key : extras.keySet()) {
                System.out.println(key + ": " + extras.get(key));
            }
        }
        
    }

    private void showInvalidAndFinish() {
        Toast.makeText(this, R.string.invalid_share, Toast.LENGTH_SHORT).show();
        finish();
    }
    
    private String getFileName(Uri uri) {
        if ("file".equals(uri.getScheme())) {
            return uri.getLastPathSegment();
        }
        
        // attempt to resolve from content provider
        String[] projection = { MediaStore.Images.ImageColumns.DATA };
        Cursor cursor = mContentResolver.query(uri, projection, null, null, null);
        cursor.moveToFirst();
        try {
            String file = cursor.getString(cursor.getColumnIndex(projection[0]));
            return new File(file).getName();
        } catch (RuntimeException ex) {
            return null;
        } finally {
            cursor.close();
        }
    }
    
}
