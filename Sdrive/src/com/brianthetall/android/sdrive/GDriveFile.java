package com.brianthetall.android.sdrive;

import android.net.Uri;

public class GDriveFile implements EncryptedFile {

    private String id;
	private String name;
	private Uri uri;
    private String content;
	
    public GDriveFile(String name) {
        this.name = name;
    }
    
	public GDriveFile(String id, String name) {
		this.id = id;
		this.name = name;
	}
	
	public GDriveFile(String id, String name, Uri resource, String content) {
	    this.id = id;
	    this.name = name;
	    this.uri = resource;
	    this.content = content;
	}
	
	public static GDriveFile withUri(String name, Uri resource) {
        GDriveFile file = new GDriveFile(name);
        file.uri = resource;
        return file;
    }
    
    public static GDriveFile withTextContent(String name, String content) {
        GDriveFile file = new GDriveFile(name);
        file.content = content;
        return file;
    }
	
    @Override
	public String getId() {
		return id;
	}

	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public Uri getUri() {
        return uri;
    }

	@Override
    public String getContent() {
        return content;
    }

}
