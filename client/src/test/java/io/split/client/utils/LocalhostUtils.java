package io.split.client.utils;

import io.split.client.LocalhostSplit;
import io.split.client.SplitAndKey;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

public class LocalhostUtils {

    public static void writeFile(File f, StringWriter content) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(f));
        writer.write(content.toString());
        writer.flush();
        writer.close();
    }

    public static void writeFile(File f, Map<SplitAndKey, LocalhostSplit> map) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(f));

        for (Map.Entry<SplitAndKey, LocalhostSplit> entry : map.entrySet()) {
            String line = toString(entry);
            writer.write(line);
        }

        writer.flush();
        writer.close();
    }

    private static String toString(Map.Entry<SplitAndKey, LocalhostSplit> entry) {
        StringBuilder bldr = new StringBuilder();
        bldr.append(entry.getKey().split());
        bldr.append(' ');
        bldr.append(entry.getValue().treatment);
        if (entry.getKey().key() != null) {
            bldr.append(' ');
            bldr.append(entry.getKey().key());
        }
        bldr.append('\n');
        return bldr.toString();
    }
}