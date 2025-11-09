package com.bank.notificationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

@SpringBootApplication
@ConfigurationPropertiesScan
public class NotificationServiceApplication {

    public static void main(String[] args) {
        // Load env/local.env (if present) and populate System properties so Spring can bind them
        try {
            // Prefer Kubernetes-mounted config if present, otherwise fall back to env/local.env
            if (!loadK8sMountedFiles()) {
                loadLocalEnv();
            }
        } catch (IOException e) {
            // Log to stderr; do not fail startup because local.env is optional
            System.err.println("Failed to load env/local.env: " + e.getMessage());
        }

        SpringApplication.run(NotificationServiceApplication.class, args);
    }

    private static void loadLocalEnv() throws IOException {
        Path p = Paths.get("env", "local.env");
        if (!Files.exists(p)) {
            return;
        }

        List<String> lines = Files.readAllLines(p);
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            int eq = line.indexOf('=');
            if (eq <= 0) {
                continue;
            }

            String key = line.substring(0, eq).trim();
            String value = line.substring(eq + 1).trim();
            // strip optional surrounding quotes
            if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                value = value.substring(1, value.length() - 1);
            }

            // set raw key as system property as-is (useful for some libraries)
            System.setProperty(key, value);

            String up = key.toUpperCase(Locale.ROOT);
            // Support NOTIFICATION_* keys by mapping to notification.<section>.<property>
            if (up.startsWith("NOTIFICATION_")) {
                String rest = key.substring("NOTIFICATION_".length());
                String[] parts = rest.split("_");
                if (parts.length >= 1) {
                    String section = parts[0].toLowerCase(Locale.ROOT);
                    // build kebab-case for remaining parts
                    if (parts.length > 1) {
                        String[] rem = java.util.Arrays.copyOfRange(parts, 1, parts.length);
                        StringBuilder kebab = new StringBuilder();
                        StringBuilder camel = new StringBuilder();
                        for (int i = 0; i < rem.length; i++) {
                            String seg = rem[i].toLowerCase(Locale.ROOT);
                            if (i > 0) kebab.append('-');
                            kebab.append(seg);

                            if (i == 0) {
                                camel.append(seg);
                            } else {
                                camel.append(Character.toUpperCase(seg.charAt(0))).append(seg.substring(1));
                            }
                        }
                        String propKebab = String.format("notification.%s.%s", section, kebab.toString());
                        String propCamel = String.format("notification.%s.%s", section, camel.toString());
                        System.setProperty(propKebab, value);
                        System.setProperty(propCamel, value);
                    } else {
                        // just notification.<section>=value
                        System.setProperty("notification." + section, value);
                    }
                }
            }

            // Support TWILIO_* keys by mapping to notification.sms.<camel>
            if (up.startsWith("TWILIO_")) {
                String rest = key.substring("TWILIO_".length());
                String[] parts = rest.split("_");
                StringBuilder camel = new StringBuilder();
                for (int i = 0; i < parts.length; i++) {
                    String seg = parts[i].toLowerCase(Locale.ROOT);
                    if (i == 0) camel.append(seg);
                    else camel.append(Character.toUpperCase(seg.charAt(0))).append(seg.substring(1));
                }
                String prop = "notification.sms." + camel.toString();
                System.setProperty(prop, value);
            }
        }
    }

    /**
     * Load configuration from common mount paths used for ConfigMap / Secret volumes in Kubernetes.
     * Returns true if any files were loaded.
     */
    private static boolean loadK8sMountedFiles() {
        boolean loaded = false;
        Path configDir = Paths.get("/etc", "config");
        Path secretDir = Paths.get("/etc", "secrets");

        try {
            if (Files.exists(configDir) && Files.isDirectory(configDir)) {
                loaded |= loadFilesFromDirectory(configDir);
            }
            if (Files.exists(secretDir) && Files.isDirectory(secretDir)) {
                loaded |= loadFilesFromDirectory(secretDir);
            }
        } catch (Exception ex) {
            // do not block startup for I/O errors; log and continue
            System.err.println("Error loading k8s-mounted files: " + ex.getMessage());
        }

        return loaded;
    }

    private static boolean loadFilesFromDirectory(Path dir) {
        AtomicBoolean any = new AtomicBoolean(false);
        try {
            Files.list(dir).filter(Files::isRegularFile).forEach(p -> {
                try {
                    String name = p.getFileName().toString();
                    String raw = Files.readString(p).trim();
                    if (raw.isEmpty()) return;
                    // set raw file name as system property
                    System.setProperty(name, raw);

                    String up = name.toUpperCase(Locale.ROOT);
                    if (up.startsWith("NOTIFICATION_")) {
                        String rest = name.substring("NOTIFICATION_".length());
                        String prop = mapToNotificationProperty(rest);
                        System.setProperty(prop, raw);
                    }
                    if (up.startsWith("TWILIO_")) {
                        String rest = name.substring("TWILIO_".length());
                        String camel = toCamelCase(rest);
                        System.setProperty("notification.sms." + camel, raw);
                    }
                    any.set(true);
                } catch (Exception e) {
                    System.err.println("Failed to load file " + p + ": " + e.getMessage());
                }
            });
        } catch (IOException e) {
            System.err.println("Failed to read files from " + dir + ": " + e.getMessage());
        }
        return any.get();
    }

    private static String mapToNotificationProperty(String rest) {
        // rest may contain underscores like SMS_API_KEY -> sms.apiKey or sms.api-key
        String[] parts = rest.split("_");
        if (parts.length == 0) return "notification." + rest.toLowerCase(Locale.ROOT);
        String section = parts[0].toLowerCase(Locale.ROOT);
        if (parts.length == 1) return "notification." + section;
        String[] rem = java.util.Arrays.copyOfRange(parts, 1, parts.length);
        String camel = toCamelCase(String.join("_", rem));
        return "notification." + section + "." + camel;
    }

    private static String toCamelCase(String s) {
        String[] parts = s.split("[_-]");
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String seg = parts[i].toLowerCase(Locale.ROOT);
            if (i == 0) b.append(seg);
            else b.append(Character.toUpperCase(seg.charAt(0))).append(seg.substring(1));
        }
        return b.toString();
    }
}
