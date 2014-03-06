package com.brianthetall.android.sdrive.service;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.springframework.http.MediaType;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

import com.brianthetall.android.sdrive.Constant;
import com.brianthetall.android.sdrive.R;

public class UploadService extends IntentService {

    private static final String SERVICE_NAME = "SDriveUploadService";
    private static final int NOTIFICATION_ID = 1;
    private static final int PROGRESS_MAX = 100;
    private static final int MIN_PERCENT_INCREMENT = 5;
    
    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mNotificationBuilder;
    private RemoteViews mNotificationRemoteView;
    
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
    
    public UploadService() {
        this(SERVICE_NAME);
    }
    
    public UploadService(String name) {
        super(name);
        setIntentRedelivery(true);
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationRemoteView = new RemoteViews(getPackageName(), R.layout.notification_progress);
        mNotificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContent(mNotificationRemoteView);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        
        String filename = intent.getStringExtra(Constant.EXTRA_FILE_NAME);
        String id = intent.getStringExtra(Constant.ACCOUNT_NAME);
        String accessToken = intent.getStringExtra(Constant.ACCESS_TOKEN);
        String content = intent.getStringExtra(Intent.EXTRA_TEXT);
        
        mNotificationRemoteView.setTextViewText(R.id.notification_content, filename);
        mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
        
        PrintWriter writer = null;
        try {
            String boundary = Long.toHexString(System.currentTimeMillis()); // Just generate some unique random value.
            String CRLF = "\r\n"; // Line separator required by multipart/form-data.
            URLConnection conn = new URL(Constant.REST_ROOT_URL).openConnection();
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            OutputStream out = conn.getOutputStream();
            writer = new PrintWriter(out, true);
            
            // id param
            writer.append("--").append(boundary).append(CRLF)
                    .append("Content-Disposition: form-data; name=\"")
                    .append(Constant.REST_PARAM_ID).append("\"").append(CRLF)
                    .append("Content-Type: text/plain").append(CRLF)
                    .append(CRLF)
                    .append(id).append(CRLF)
                    .flush();
            
            // access token param
            writer.append("--").append(boundary).append(CRLF)
                    .append("Content-Disposition: form-data; name=\"")
                    .append(Constant.REST_PARAM_TOKEN).append("\"").append(CRLF)
                    .append("Content-Type: text/plain").append(CRLF)
                    .append(CRLF)
                    .append(accessToken).append(CRLF)
                    .flush();
            
            
            // file payload
            writer.append("--" + boundary).append(CRLF)
                    .append("Content-Disposition: form-data; name=\"")
                    .append(Constant.REST_PARAM_FILE).append("\"; filename=\"")
                    .append(filename)
                    .append("\"").append(CRLF);
            
            String contentType;
            InputStream in;
            long contentLength;
            boolean unknownLength = false;
            if (intent.getData() != null) {
                // binary file
                contentType = getContentResolver().getType(intent.getData());
                writer.append("Content-Transfer-Encoding: binary").append(CRLF);
                AssetFileDescriptor afd = getContentResolver().openAssetFileDescriptor(intent.getData(), "r");
                contentLength = afd.getLength();
                unknownLength = contentLength == AssetFileDescriptor.UNKNOWN_LENGTH;
                afd.close();
                in = getContentResolver().openInputStream(intent.getData());
            } else {
                contentType = MediaType.TEXT_PLAIN_VALUE;
                contentLength = content.length();
                in = new ByteArrayInputStream(content.getBytes());
            }
            if (!unknownLength) {
                writer.append("Content-Length: ").append(String.valueOf(contentLength)).append(CRLF);
            }
            writer.append("Content-Type: ").append(contentType).append(CRLF)
                    .append(CRLF).flush();
            
            try {
                int lastPercentUpdate = 0;
                long total = 0;
                byte[] buffer = new byte[8192];
                for (int length = 0; (length = in.read(buffer)) > 0;) {
                    out.write(buffer, 0, length);
                    out.flush();
                    total += length;
                    int percent = (int) (((double) total / contentLength) * 100);
                    if (percent - lastPercentUpdate > MIN_PERCENT_INCREMENT || percent == 100) {
                        lastPercentUpdate = percent;
                        mNotificationRemoteView.setProgressBar(R.id.notification_progress, PROGRESS_MAX, percent, false);
                        mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
                    }
                }
            } finally {
                try { in.close(); } catch (IOException ex) {}
            }
            
            // End of multipart/form-data.
            writer.append(CRLF).append("--" + boundary + "--").append(CRLF).flush();
            
            int responseCode = ((HttpsURLConnection) conn).getResponseCode();
            if (responseCode != 200) {
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new InputStreamReader(((HttpsURLConnection) conn).getErrorStream()));
                    String line;
                    while (null != (line = reader.readLine())) {
                        System.out.println(line);
                    }
                } finally {
                    reader.close();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
        mNotificationManager.cancel(NOTIFICATION_ID);
    }

}
