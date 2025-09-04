package io.split.client.utils;

import io.split.client.dtos.ChangeDto;
import io.split.client.dtos.FallbackTreatmentsConfiguration;
import io.split.engine.evaluator.EvaluatorImp.TreatmentLabelAndChangeNumber;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.HttpEntities;
import org.apache.hc.core5.net.URIBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by adilaijaz on 6/10/16.
 */
public class Utils {

    public static HttpEntity toJsonEntity(Object obj) {
        String json = Json.toJson(obj);
        return HttpEntities.create(json, ContentType.APPLICATION_JSON);
    }


    public static void forceClose(CloseableHttpResponse response) {
        try {
            if (response != null) {
                response.close();
            }
        } catch (IOException e) {
            // ignore
        }
    }

    public static URI appendPath(URI root, String pathToAppend) throws URISyntaxException {
        checkNotNull(root);
        checkNotNull(pathToAppend);
        //Add or not the backslash depending on whether the roots ends with / or not
        String path = String.format("%s%s%s", root.getPath(), root.getPath().endsWith("/") ? "" : "/", pathToAppend);
        return new URIBuilder(root).setPath(path).build();
    }

    public static <T> boolean checkExitConditions(ChangeDto<T> change, long cn) {
        return change.t < cn && change.t != -1;
    }

    public static TreatmentLabelAndChangeNumber checkFallbackTreatments(String treatment, String label,
                                                                        String feature_name, Long changeNumber,
                                                                        FallbackTreatmentsConfiguration fallbackTreatmentsConfiguration) {
        if (fallbackTreatmentsConfiguration != null) {
            if (fallbackTreatmentsConfiguration.getByFlagFallbackTreatment() != null
                    && fallbackTreatmentsConfiguration.getByFlagFallbackTreatment().get(feature_name) != null
                    && !fallbackTreatmentsConfiguration.getByFlagFallbackTreatment().get(feature_name).getTreatment().isEmpty()) {
                return new TreatmentLabelAndChangeNumber(
                        fallbackTreatmentsConfiguration.getByFlagFallbackTreatment().get(feature_name).getTreatment(),
                        fallbackTreatmentsConfiguration.getByFlagFallbackTreatment().get(feature_name).getLabel() + label,
                        changeNumber);
            }

            if (fallbackTreatmentsConfiguration.getGlobalFallbackTreatment() != null
                    && !fallbackTreatmentsConfiguration.getGlobalFallbackTreatment().getTreatment().isEmpty()) {
                return new TreatmentLabelAndChangeNumber(fallbackTreatmentsConfiguration.getGlobalFallbackTreatment().getTreatment(),
                        fallbackTreatmentsConfiguration.getGlobalFallbackTreatment().getLabel() + label,
                        changeNumber);
            }
        }

        return new TreatmentLabelAndChangeNumber(treatment,
        label,
        changeNumber);
    }
}