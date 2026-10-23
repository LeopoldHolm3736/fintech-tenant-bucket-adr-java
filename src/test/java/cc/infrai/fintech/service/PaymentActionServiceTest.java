package cc.infrai.fintech.service;

import cc.infrai.fintech.domain.PaymentAction;
import cc.infrai.fintech.domain.PaymentEvent;
import cc.infrai.fintech.domain.PaymentResult;
import cc.infrai.fintech.domain.RiskLevel;
import cc.infrai.fintech.notify.AuditNotificationFactory;
import cc.infrai.fintech.storage.InfraiStorageClient;
import cc.infrai.fintech.storage.TenantBucketNamer;
import cc.infrai.fintech.storage.TenantBucketProvisioner;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PaymentActionServiceTest {
    @Test
    void highRiskPaymentGoesToManualReviewWithoutEvidenceKey() throws Exception {
        InfraiStorageClient infrai = new NoopInfraiStorageClient();
        TenantBucketNamer namer = new TenantBucketNamer("fintech-tenant");
        TenantBucketProvisioner provisioner = new TenantBucketProvisioner(infrai, namer);
        PaymentActionService service = new PaymentActionService(infrai, provisioner, namer, new AuditNotificationFactory());

        PaymentEvent input = new PaymentEvent("Acme Payments", "pay_1001", 250000L, "USD", RiskLevel.HIGH);
        PaymentResult result = service.handle(input);

        assertEquals(PaymentAction.MANUAL_REVIEW_REQUIRED, result.action());
        assertNull(result.evidenceKey());
        assertTrue(result.notification().message().contains("manual review required"));
        assertEquals("fintech-tenant-acme-payments", result.bucket());
    }

    static final class NoopInfraiStorageClient extends InfraiStorageClient {
        NoopInfraiStorageClient() {
            super(HttpClient.newHttpClient(), new ObjectMapper(), "test-key");
        }

        @Override
        protected JsonNode callJson(String method, String path, String jsonBody) {
            return new ObjectMapper().createObjectNode();
        }
    }
}
