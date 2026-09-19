package com.yourco.saas.common.storage;

public class FileSizeExceededException extends StorageException {
    public FileSizeExceededException(String message) {
        super(message);
    }
}
