package io.split.engine.sse.dtos;

import io.split.client.dtos.Split;
import io.split.client.utils.Json;
import io.split.engine.segments.SegmentSynchronizationTaskImp;
import io.split.engine.sse.NotificationProcessor;
import io.split.engine.sse.enums.CompressType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.zip.DataFormatException;

import static io.split.engine.sse.utils.DecompressionUtil.gZipDecompress;
import static io.split.engine.sse.utils.DecompressionUtil.zLibDecompress;

public class FeatureFlagChangeNotification extends IncomingNotification {
    private static final Logger _log = LoggerFactory.getLogger(SegmentSynchronizationTaskImp.class);
    private final long changeNumber;
    private long previousChangeNumber;
    private Split featureFlagDefinition;
    private CompressType compressType;

    public FeatureFlagChangeNotification(GenericNotificationData genericNotificationData) {
        super(Type.SPLIT_UPDATE, genericNotificationData.getChannel());
        changeNumber = genericNotificationData.getChangeNumber();
        if(genericNotificationData.getPreviousChangeNumber() != null) {
            previousChangeNumber = genericNotificationData.getPreviousChangeNumber();
        }
        compressType =  CompressType.from(genericNotificationData.getCompressType());
        if (compressType == null || genericNotificationData.getFeatureFlagDefinition() == null) {
            return;
        }
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(genericNotificationData.getFeatureFlagDefinition());
            switch (compressType) {
                case GZIP:
                    decodedBytes = gZipDecompress(decodedBytes);
                    break;
                case ZLIB:
                    decodedBytes = zLibDecompress(decodedBytes);
                    break;
            }
            featureFlagDefinition = Json.fromJson(new String(decodedBytes, 0, decodedBytes.length, "UTF-8"), Split.class);
        } catch (UnsupportedEncodingException | IllegalArgumentException e) {
            _log.warn("Could not decode base64 data in flag definition", e);
        } catch (DataFormatException d) {
            _log.warn("Could not decompress feature flag definition with zlib algorithm", d);
        } catch (IOException i) {
            _log.warn("Could not decompress feature flag definition with gzip algorithm", i);
        }
    }

    public long getChangeNumber() {
        return changeNumber;
    }
    public long getPreviousChangeNumber() {
        return previousChangeNumber;
    }

    public Split getFeatureFlagDefinition() {
        return featureFlagDefinition;
    }

    public CompressType getCompressType() {
        return compressType;
    }

    @Override
    public void handler(NotificationProcessor notificationProcessor) {
        notificationProcessor.processSplitUpdate(getChangeNumber());
    }

    @Override
    public String toString() {
        return String.format("Type: %s; Channel: %s; ChangeNumber: %s", getType(), getChannel(), getChangeNumber());
    }
}