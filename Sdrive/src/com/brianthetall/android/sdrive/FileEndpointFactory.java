package com.brianthetall.android.sdrive;

import android.content.Context;

public class FileEndpointFactory {

    private FileEndpointFactory() {}
    
    public static FileEndpoint createEndpoint(Context context) {
//        return new MockFileEndpoint(context);
        return new SdriveEndpoint(context);
    }

}
