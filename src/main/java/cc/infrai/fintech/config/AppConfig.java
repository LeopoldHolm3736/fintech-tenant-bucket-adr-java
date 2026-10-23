package cc.infrai.fintech.config;

public record AppConfig(String apiKey, String bucketPrefix) {
    public static AppConfig fromEnv() {
        String apiKey = System.getenv("INFRAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("INFRAI_API_KEY is required");
        }
        String prefix = System.getenv().getOrDefault("TENANT_BUCKET_PREFIX", "fintech-tenant");
        return new AppConfig(apiKey, prefix);
    }
}
