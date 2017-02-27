package io.split.grammar;

/**
 * Utility methods for dealing with special treatments
 *
 * @author adil
 */
public class Treatments {

    public static final String CONTROL = "control";

    /**
     * OFF is a synonym for CONTROL.
     */
    public static final String OFF = "off";
    public static final String ON = "on";

    public static boolean isControl(String treatment) {
        return CONTROL.equals(treatment) || OFF.equals(treatment);
    }

    public static String controlSynonym(String treatment) {
        if (!isControl(treatment)) {
            throw new IllegalArgumentException("Not a control treatment: " + treatment);
        }
        if (Treatments.OFF.equals(treatment)) {
            return Treatments.CONTROL;
        }
        return Treatments.OFF;
    }

}
