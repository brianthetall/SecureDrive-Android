package com.brianthetall.android.sdrive;

import android.net.Uri;

public interface EncryptedFile {

    String getId();
    String getName();
    Uri getUri();
    String getContent();
    
}
