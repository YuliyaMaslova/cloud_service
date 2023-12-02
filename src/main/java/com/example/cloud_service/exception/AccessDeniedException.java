package com.example.cloud_service.exception;

import java.util.function.Supplier;

public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException (String msg) {
        super(msg);
    }

}
