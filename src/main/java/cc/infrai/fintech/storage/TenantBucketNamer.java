package cc.infrai.fintech.storage;

import java.util.Locale;

public final class TenantBucketNamer {
    private final String prefix;

    public TenantBucketNamer(String prefix) {
        this.prefix = sanitize(prefix);
    }

    public String bucketForTenant(String tenantId) {
        return prefix + "-" + sanitize(tenantId);
    }

    private String sanitize(String input) {
        String lowered = input.toLowerCase(Locale.ROOT);
        String cleaned = lowered.replaceAll("[^a-z0-9-]", "-").replaceAll("-+", "-");
        return cleaned.replaceAll("^-", "").replaceAll("-$", "");
    }
}
