package com.brianthetall.android.sdrive.util;

import java.io.IOException;

public class IOExceptionWithCause extends IOException {
    
    private static final long serialVersionUID = 1L;

    public IOExceptionWithCause() {
        super();
    }

    public IOExceptionWithCause(String detailMessage) {
        super(detailMessage);
    }

    public IOExceptionWithCause(Throwable cause) {
        super();
        this.initCause(cause);
    }

    public IOExceptionWithCause(String message, Throwable cause) {
        super(message);
        this.initCause(cause);
    }

}
