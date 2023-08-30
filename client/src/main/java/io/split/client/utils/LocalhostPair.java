package io.split.client.utils;

public class LocalhostPair {

    private final InputStreamProvider _inputStreamProvider;
    private final FileTypeEnum _fileTypeEnum;

    public LocalhostPair(InputStreamProvider inputStreamProvider, FileTypeEnum fileType) {
        _inputStreamProvider = inputStreamProvider;
        _fileTypeEnum = fileType;
    }

    public InputStreamProvider getInputStreamProvider() {
        return _inputStreamProvider;
    }

    public FileTypeEnum getFileTypeEnum() {
        return _fileTypeEnum;
    }
}