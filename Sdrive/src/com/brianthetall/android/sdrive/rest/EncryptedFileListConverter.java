package com.brianthetall.android.sdrive.rest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import com.brianthetall.android.sdrive.GDriveFile;

public class EncryptedFileListConverter extends AbstractHttpMessageConverter<EncryptedFileList> {

    private static final String RECORD_DELIMITER_REGEX = "<br><br>";
    private static final String FIELD_DELIMITER_REGEX = "<br>";

    public EncryptedFileListConverter() {
        super();
    }

    public EncryptedFileListConverter(MediaType... supportedMediaTypes) {
        super(supportedMediaTypes);
    }

    public EncryptedFileListConverter(MediaType mediaType) {
        super(mediaType);
    }

    @Override
    protected EncryptedFileList readInternal(Class<? extends EncryptedFileList> clazz,
            HttpInputMessage message) throws IOException, HttpMessageNotReadableException {
        EncryptedFileList files = new EncryptedFileList();
        InputStream is = message.getBody();
        BufferedReader reader = null;
        StringBuilder sb = new StringBuilder();
        try {
            reader = new BufferedReader(new InputStreamReader(is));
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } finally {
            if (reader != null) {
                try { reader.close(); } catch (IOException ex) {}
            }
        }
        for (String fileHtml : sb.toString().split(RECORD_DELIMITER_REGEX)) {
            String[] nameAndId = fileHtml.split(FIELD_DELIMITER_REGEX);
            if (nameAndId.length == 2) {
                files.add(new GDriveFile(nameAndId[1], nameAndId[0]));
            }
        }
        return files;
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return clazz == EncryptedFileList.class;
    }

    @Override
    protected void writeInternal(EncryptedFileList arg0, HttpOutputMessage arg1)
            throws IOException, HttpMessageNotWritableException {
        throw new HttpMessageNotWritableException("Not writable");        
    }

}
