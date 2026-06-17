package com.everycent.service;

public class NoLedgerPermissionException extends RuntimeException {

    public NoLedgerPermissionException(String message) {
        super(message);
    }
}
