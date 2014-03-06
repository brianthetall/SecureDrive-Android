package com.brianthetall.android.sdrive;

import java.util.List;

import android.net.Uri;

public interface FileEndpoint {

    Uri downloadFile(EncryptedFile file);
    void uploadFile(EncryptedFile file);
    void deleteFile(EncryptedFile file);
    List<EncryptedFile> listFiles();
    
}
