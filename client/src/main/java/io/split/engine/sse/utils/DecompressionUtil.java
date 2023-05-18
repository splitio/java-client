package io.split.engine.sse.utils;

import java.io.ByteArrayOutputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class DecompressionUtil {

    public static byte[] zLibDecompress(byte[] toDecompress){
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(toDecompress.length);
        Inflater decompressor = new Inflater();
        try {
            decompressor.setInput(toDecompress);
            final byte[] buf = new byte[toDecompress.length];
            while (!decompressor.finished()) {
                int count = decompressor.inflate(buf);
                byteArrayOutputStream.write(buf, 0, count);
            }
        } catch (DataFormatException e) {
            throw new RuntimeException(e);
        } finally {
            decompressor.end();
        }
        return byteArrayOutputStream.toByteArray();
    }
}