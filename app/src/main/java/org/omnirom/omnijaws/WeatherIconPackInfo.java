package org.omnirom.omnijaws;

public class WeatherIconPackInfo {
    public final String label;
    public final String value;
    public final boolean supportsVariants;
    public final String lightPrefix;
    public final String darkPrefix;

    public WeatherIconPackInfo(
            String label,
            String value,
            boolean supportsVariants,
            String lightPrefix,
            String darkPrefix) {
        this.label = label;
        this.value = value;
        this.supportsVariants = supportsVariants;
        this.lightPrefix = lightPrefix;
        this.darkPrefix = darkPrefix;
    }

    public boolean hasExplicitVariants() {
        return supportsVariants
                && lightPrefix != null && !lightPrefix.isEmpty()
                && darkPrefix != null && !darkPrefix.isEmpty();
    }
}