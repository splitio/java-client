package io.split.engine.sse.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;

public class DecompressionUtil {

    public static byte[] zLibDecompress(byte[] toDecompress) throws DataFormatException {
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
            throw e;
        } finally {
            decompressor.end();
        }
        return byteArrayOutputStream.toByteArray();
    }

    public static byte[] gZipDecompress(byte[] toDecompress) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(toDecompress));
            int res = 0;
            byte buf[] = new byte[toDecompress.length];
            while (res >= 0) {
                res = gzipInputStream.read(buf, 0, buf.length);
                if (res > 0) {
                    out.write(buf, 0, res);
                }
            }
        } catch(IOException e){
            throw e;
        }
        return out.toByteArray();
    }
}