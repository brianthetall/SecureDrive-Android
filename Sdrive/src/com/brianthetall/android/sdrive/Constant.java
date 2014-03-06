package com.brianthetall.android.sdrive;

public interface Constant {
    
    String SCOPE_GOOGLE_DRIVE = "oauth2:https://www.googleapis.com/auth/drive.file";
    String PREF_NAME_AUTH = "auth";
    String ACCOUNT_NAME = "account_name";
    String ACCESS_TOKEN = "access_token";
    String REST_ROOT_URL = "https://cloud.brianthetall.com/start/SDrive";
    String REST_DOWNLOAD_FORMAT = "https://cloud.brianthetall.com/start/DownloadDriveFile?fileID=%s&androidToken=%s&id=%s";
    String REST_PARAM_ID = "id";
    String REST_PARAM_TOKEN = "androidToken";
    String REST_PARAM_FILE = "file";
    String REST_PARAM_FILE_ID = "fileID";
    String EXTRA_FILE_NAME = "filename";
    
}
