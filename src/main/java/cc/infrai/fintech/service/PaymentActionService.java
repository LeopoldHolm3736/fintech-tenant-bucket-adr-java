package cc.infrai.fintech.service;

import cc.infrai.fintech.domain.PaymentAction;
import cc.infrai.fintech.domain.PaymentEvent;
import cc.infrai.fintech.domain.PaymentResult;
import cc.infrai.fintech.domain.RiskLevel;
import cc.infrai.fintech.notify.AuditNotification;
import cc.infrai.fintech.notify.AuditNotificationFactory;
import cc.infrai.fintech.storage.InfraiStorageClient;
import cc.infrai.fintech.storage.TenantBucketNamer;
import cc.infrai.fintech.storage.TenantBucketProvisioner;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public final class PaymentActionService {
    private final InfraiStorageClient infrai;
    private final TenantBucketProvisioner provisioner;
    private final TenantBucketNamer namer;
    private final AuditNotificationFactory notificationFactory;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PaymentActionService(
            InfraiStorageClient infrai,
            TenantBucketProvisioner provisioner,
            TenantBucketNamer namer,
            AuditNotificationFactory notificationFactory) {
        this.infrai = infrai;
        this.provisioner = provisioner;
        this.namer = namer;
        this.notificationFactory = notificationFactory;
    }

    public PaymentResult handle(PaymentEvent event) throws Exception {
        String bucket = namer.bucketForTenant(event.tenantId());
        provisioner.ensureTenantBucket(event.tenantId());

        if (event.riskLevel() == RiskLevel.HIGH) {
            AuditNotification notification = notificationFactory.forDecision(event, PaymentAction.MANUAL_REVIEW_REQUIRED, null);
            return new PaymentResult(PaymentAction.MANUAL_REVIEW_REQUIRED, bucket, null, notification);
        }

        String key = evidenceKey(event);
        InfraiStorageClient.HeadResult head = infrai.storage.object.head(bucket, key);
        if (!head.found()) {
            Map<String, Object> evidence = new LinkedHashMap<>();
            evidence.put("tenant_id", event.tenantId());
            evidence.put("payment_id", event.paymentId());
            evidence.put("amount_cents", event.amountCents());
            evidence.put("currency", event.currency());
            evidence.put("risk_level", event.riskLevel().name());
            byte[] body = objectMapper.writeValueAsString(evidence).getBytes(StandardCharsets.UTF_8);
            infrai.storage.object.put(bucket, key, body);
        }

        AuditNotification notification = notificationFactory.forDecision(event, PaymentAction.EVIDENCE_STORED, key);
        return new PaymentResult(PaymentAction.EVIDENCE_STORED, bucket, key, notification);
    }

    private String evidenceKey(PaymentEvent event) {
        return "payments/" + event.paymentId() + "/evidence.json";
    }
}
