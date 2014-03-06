
package com.brianthetall.android.sdrive.activity;

import java.net.URLEncoder;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.brianthetall.android.sdrive.Constant;
import com.brianthetall.android.sdrive.EncryptedFile;
import com.brianthetall.android.sdrive.FileEndpoint;
import com.brianthetall.android.sdrive.FileEndpointFactory;
import com.brianthetall.android.sdrive.R;
import com.brianthetall.android.sdrive.adapter.EncryptedFileAdapter;
import com.brianthetall.android.sdrive.util.AccountUtil;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.GooglePlayServicesUtil;

@EActivity(R.layout.activity_main)
public class MainActivity extends SherlockActivity 
        implements ActionBar.OnNavigationListener,
                    AdapterView.OnItemClickListener, 
                    AdapterView.OnItemLongClickListener {
    
    private static final int PLAY_REQUEST_CODE = 1000;
    private static final int AUTH_REQUEST_CODE = 1001;
    
    @ViewById(R.id.file_list)
    ListView mFileListView;
    
    private SharedPreferences mPrefs;
    private String accountName;
    private String accessToken;
    private ActionBar mActionBar;
    private String[] accounts;
    private EncryptedFileAdapter mFileAdapter;
    private FileEndpoint mEndpoint;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPrefs = getSharedPreferences(Constant.PREF_NAME_AUTH, MODE_PRIVATE);
        mEndpoint = FileEndpointFactory.createEndpoint(this);
        accounts = AccountUtil.getAccountNames(this);
        ArrayAdapter<String> accountsAdapter = new ArrayAdapter<String>(this, R.layout.sherlock_spinner_dropdown_item, accounts);
        mActionBar = getSupportActionBar();
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        mActionBar.setListNavigationCallbacks(accountsAdapter, this);
        restoreAccountName();
    }
    
    @AfterViews
    void setListAdapter() {
        mFileAdapter = new EncryptedFileAdapter(this, mEndpoint);
        mFileListView.setAdapter(mFileAdapter);
        mFileListView.setOnItemClickListener(this);
        mFileListView.setOnItemLongClickListener(this);
    }
    
    private void restoreAccountName() {
        accountName = AccountUtil.getMostRecentAccountName(this, accounts[mActionBar.getSelectedNavigationIndex()]);
        for (int i=0; i<accounts.length; i++) {
            if (accounts[i].equals(accountName)) {
                mActionBar.setSelectedNavigationItem(i);
            }
        }
    }
    
    void refreshFileList() {
        authenticateBefore(new Runnable() {
            public void run() {
                refreshAdapter();
            }
        });
    }
    
    @UiThread
    void refreshAdapter() {
        mFileAdapter.refresh();
    }

    @Background
    void authenticateBefore(Runnable successCallback) {
        try {
            accessToken = GoogleAuthUtil.getToken(this, accountName, Constant.SCOPE_GOOGLE_DRIVE);
            mPrefs.edit()
                    .putString(Constant.ACCOUNT_NAME, accountName)
                    .putString(Constant.ACCESS_TOKEN, accessToken)
                    .commit();
            successCallback.run();
        } catch (GooglePlayServicesAvailabilityException playEx) {
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(
                    playEx.getConnectionStatusCode(),
                    this,
                    PLAY_REQUEST_CODE);
                dialog.show();
        } catch (UserRecoverableAuthException recoverableException) {
            startActivityForResult(recoverableException.getIntent(), AUTH_REQUEST_CODE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        accountName = accounts[mActionBar.getSelectedNavigationIndex()];
        refreshFileList();
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long rowId) {
        final EncryptedFile file = (EncryptedFile) adapterView.getItemAtPosition(position);
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_file_download)
                .setMessage(file.getName())
                .setPositiveButton(android.R.string.ok, new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mEndpoint.downloadFile(file);
                    }
                })
                .setNegativeButton(android.R.string.cancel, new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long rowId) {
        final EncryptedFile file = (EncryptedFile) adapterView.getItemAtPosition(position);
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_file_delete)
                .setMessage(file.getName())
                .setPositiveButton(android.R.string.ok, new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mEndpoint.deleteFile(file);
                    }
                })
                .setNegativeButton(android.R.string.cancel, new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
        return true;
    }

}
