package cc.infrai.fintech.storage;

public final class TenantBucketProvisioner {
    private final InfraiStorageClient infrai;
    private final TenantBucketNamer namer;

    public TenantBucketProvisioner(InfraiStorageClient infrai, TenantBucketNamer namer) {
        this.infrai = infrai;
        this.namer = namer;
    }

    public String ensureTenantBucket(String tenantId) throws Exception {
        String bucket = namer.bucketForTenant(tenantId);
        infrai.storage.bucket.create(bucket);
        return bucket;
    }
}
