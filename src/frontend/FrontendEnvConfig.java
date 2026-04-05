package frontend;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Frontend Environment Configuration Loader
 * Loads configuration from .env file with fallback to environment variables
 */
public class FrontendEnvConfig {
    private static final Map<String, String> config = new HashMap<>();
    private static boolean loaded = false;
    
    // Default configuration values (must match backend EnvConfig defaults)
    private static final Map<String, String> defaults = new HashMap<>() {{
        put("DB_HOST", "localhost");
        put("DB_PORT", "5434");
        put("DB_NAME", "mydb");
        put("DB_USER", "myuser");
        put("DB_PASSWORD", "mypassword");
        put("SERVER_PORT", "8080");
        put("FRONTEND_PORT", "3000");
        put("API_BASE_URL", "http://localhost:8080/api");
        put("OPENAI_API_KEY", "");
        put("ANTHROPIC_API_KEY", "");
        put("AI_MODEL", "gpt-3.5-turbo");
        put("AI_API_KEY", "");
        put("AI_API_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions");
        put("AI_MODEL_NAME", "qwen-turbo");
        put("DOCKER_NETWORK", "eecs-network");
        put("POSTGRES_IMAGE", "postgres:15");
        put("JAVA_IMAGE", "eclipse-temurin:17-jdk-alpine");
    }};
    
    /**
     * Load configuration from .env.example file (single source of truth)
     * Priority: .env.example → environment variables → defaults
     */
    public static synchronized void load() {
        if (loaded) return;
        
        // Try multiple locations for .env.example file
        String[] envPaths = {
            ".env.example",
            "../.env.example",
            "../../.env.example",
            System.getProperty("user.dir") + "/.env.example"
        };
        
        boolean fileLoaded = false;
        for (String path : envPaths) {
            Path envPath = Paths.get(path);
            if (Files.exists(envPath)) {
                try {
                    loadFromFile(envPath.toString());
                    System.out.println("[FrontendEnvConfig] Loaded from: " + envPath.toAbsolutePath());
                    fileLoaded = true;
                    break;
                } catch (IOException e) {
                    System.err.println("[FrontendEnvConfig] Failed to load " + path + ": " + e.getMessage());
                }
            }
        }
        
        if (!fileLoaded) {
            System.out.println("[FrontendEnvConfig] No .env.example file found, using defaults");
        }
        
        // Environment variables override .env.example values
        for (String key : defaults.keySet()) {
            String envValue = System.getenv(key);
            if (envValue != null && !envValue.isEmpty()) {
                config.put(key, envValue);
            }
        }
        
        loaded = true;
    }
    
    /**
     * Load configuration from a specific file
     */
    private static void loadFromFile(String filepath) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(filepath)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // Skip comments and empty lines
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                // Parse KEY=VALUE format
                int equalsIndex = line.indexOf('=');
                if (equalsIndex > 0) {
                    String key = line.substring(0, equalsIndex).trim();
                    String value = line.substring(equalsIndex + 1).trim();
                    // Remove quotes if present
                    if ((value.startsWith("\"") && value.endsWith("\"")) ||
                        (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }
                    config.put(key, value);
                }
            }
        }
    }
    
    /**
     * Get configuration value with fallback to default
     */
    public static String get(String key) {
        if (!loaded) load();
        return config.getOrDefault(key, defaults.getOrDefault(key, ""));
    }
    
    /**
     * Get configuration value with custom default
     */
    public static String get(String key, String defaultValue) {
        if (!loaded) load();
        String value = config.get(key);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        String envValue = System.getenv(key);
        return envValue != null ? envValue : defaultValue;
    }
    
    /**
     * Get integer configuration value
     */
    public static int getInt(String key) {
        try {
            return Integer.parseInt(get(key));
        } catch (NumberFormatException e) {
            String defaultVal = defaults.get(key);
            return defaultVal != null ? Integer.parseInt(defaultVal) : 0;
        }
    }
    
    /**
     * Get API base URL
     */
    public static String getApiBaseUrl() {
        return get("API_BASE_URL");
    }
}
