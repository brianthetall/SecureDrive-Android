package com.brianthetall.android.sdrive;

import java.net.URI;
import java.net.URLEncoder;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.brianthetall.android.sdrive.adapter.EncryptedFileAdapter;
import com.brianthetall.android.sdrive.rest.EncryptedFileList;
import com.brianthetall.android.sdrive.rest.EncryptedFileListConverter;
import com.brianthetall.android.sdrive.service.UploadService;

public class SdriveEndpoint implements FileEndpoint {

    private final Context context;
    private final SharedPreferences authPrefs;
    private final RestTemplate restTemplate = new RestTemplate();
    
    // DISABLE SSL CHECKS FOR ALL HTTPS REQUESTS
    static {
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, new TrustManager[] { new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
            }}, null);
            HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
        } catch (Exception e) {}
    }

    public SdriveEndpoint(Context context) {
        this.context = context;
        this.authPrefs = context.getSharedPreferences(Constant.PREF_NAME_AUTH, Context.MODE_PRIVATE);
        restTemplate.getMessageConverters()
                .add(new EncryptedFileListConverter(MediaType.parseMediaType("text/html")));
    }
    
    private String getAccountName() {
        return authPrefs.getString(Constant.ACCOUNT_NAME, null);
    }
    
    private String getToken() {
        return authPrefs.getString(Constant.ACCESS_TOKEN, null);
    }
    
    @Override
    public Uri downloadFile(EncryptedFile file) {
        String uriString = String.format(Constant.REST_DOWNLOAD_FORMAT, 
                URLEncoder.encode(file.getId()),
                URLEncoder.encode(getToken()),
                URLEncoder.encode(getAccountName()));
        System.out.println("SDrive downloading: " + uriString);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriString));
        context.startActivity(intent);
        return null;
    }
    
    @Override
    public void uploadFile(EncryptedFile file) {
        Intent intent = new Intent(context, UploadService.class);
        intent.setData(file.getUri());
        intent.putExtra(Constant.ACCOUNT_NAME, getAccountName());
        intent.putExtra(Constant.ACCESS_TOKEN, getToken());
        intent.putExtra(Constant.EXTRA_FILE_NAME, file.getName());
        intent.putExtra(Intent.EXTRA_TEXT, file.getContent());
        context.startService(intent);
    }

    @Override
    public void deleteFile(EncryptedFile file) {
        new AsyncTask<EncryptedFile, Void, Void>() {
            @Override
            protected Void doInBackground(EncryptedFile... files) {
                URI uri = UriComponentsBuilder.fromUriString(Constant.REST_ROOT_URL)
                        .queryParam(Constant.REST_PARAM_ID, getAccountName())
                        .queryParam(Constant.REST_PARAM_TOKEN, getToken())
                        .queryParam(Constant.REST_PARAM_FILE_ID, files[0].getId())
                        .build().toUri();
                try {
                    restTemplate.delete(uri);
                } catch (Exception ex) {
                    Log.e(SdriveEndpoint.class.getSimpleName(), "Failed to delete file", ex);
                    cancel(false);
                }
                return null;
            }
            @Override
            protected void onCancelled() {
                Toast.makeText(context, R.string.file_delete_failed, Toast.LENGTH_SHORT).show();
            }
        }.execute(file);
    }

    @Override
    public List<EncryptedFile> listFiles() {
        URI uri = UriComponentsBuilder.fromUriString(Constant.REST_ROOT_URL)
                .queryParam(Constant.REST_PARAM_ID, getAccountName())
                .queryParam(Constant.REST_PARAM_TOKEN, getToken())
                .build().toUri();
        return restTemplate.getForObject(uri, EncryptedFileList.class);
    }

}
