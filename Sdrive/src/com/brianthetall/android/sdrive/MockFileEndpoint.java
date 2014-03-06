package com.brianthetall.android.sdrive;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

public class MockFileEndpoint implements FileEndpoint {
    
    private static final String TAG = MockFileEndpoint.class.getSimpleName();

    private final SharedPreferences authPrefs;
    private final Context context;

    public MockFileEndpoint(Context context) {
        this.context = context;
        this.authPrefs = context.getSharedPreferences(Constant.PREF_NAME_AUTH, Context.MODE_PRIVATE);
    }
    
    private String getToken() {
        return authPrefs.getString(Constant.ACCESS_TOKEN, null);
    }
    
    @Override
    public Uri downloadFile(EncryptedFile file) {
        return new Uri.Builder().build();
    }
    
    @Override
    public void uploadFile(EncryptedFile file) {
        Log.d(TAG, String.format("Uploading %s with accessToken=%s", file.getUri(), getToken()));
        BufferedReader reader = null;
        try {
            InputStream is = context.getContentResolver().openInputStream(file.getUri());
            reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                Log.d(TAG, line);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try { reader.close(); } catch (IOException e) {}
            }
        }
        
    }

    @Override
    public void deleteFile(EncryptedFile file) {
        // TODO Auto-generated method stub

    }

    @Override
    public List<EncryptedFile> listFiles() {
        Log.d(TAG, "Fetching file list...");
        // sleep to simulate network request
        try { Thread.sleep(3000); } catch (InterruptedException e) {}
        return new ArrayList<EncryptedFile>() {{
            for (int i=0; i<50; i++) {
                add(new GDriveFile("Encrypted File " + Math.random(), String.valueOf(i)));
            }
        }};
    }

}
