package cc.infrai.fintech;

import cc.infrai.fintech.config.AppConfig;
import cc.infrai.fintech.domain.PaymentEvent;
import cc.infrai.fintech.domain.PaymentResult;
import cc.infrai.fintech.domain.RiskLevel;
import cc.infrai.fintech.notify.AuditNotification;
import cc.infrai.fintech.notify.AuditNotificationFactory;
import cc.infrai.fintech.service.PaymentActionService;
import cc.infrai.fintech.storage.InfraiStorageClient;
import cc.infrai.fintech.storage.TenantBucketNamer;
import cc.infrai.fintech.storage.TenantBucketProvisioner;

public final class PaymentEvidenceCli {
    public static void main(String[] args) throws Exception {
        AppConfig config = AppConfig.fromEnv();
        InfraiStorageClient infrai = new InfraiStorageClient(config.apiKey());
        TenantBucketNamer namer = new TenantBucketNamer(config.bucketPrefix());
        TenantBucketProvisioner provisioner = new TenantBucketProvisioner(infrai, namer);
        PaymentActionService service = new PaymentActionService(infrai, provisioner, namer, new AuditNotificationFactory());

        PaymentEvent highRisk = new PaymentEvent("acme-payments", "pay_1001", 250000L, "USD", RiskLevel.HIGH);
        PaymentResult highRiskResult = service.handle(highRisk);
        System.out.println("HIGH risk result: " + highRiskResult.action());
        AuditNotification highRiskNotice = highRiskResult.notification();
        System.out.println(highRiskNotice.toLine());

        // The published storage contract has no delete capability. Keep the
        // documented live path free of persistent bucket and object writes.
    }
}
