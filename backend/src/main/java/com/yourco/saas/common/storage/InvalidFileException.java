package com.yourco.saas.common.storage;

public class InvalidFileException extends StorageException {
    public InvalidFileException(String message) {
        super(message);
    }
}
