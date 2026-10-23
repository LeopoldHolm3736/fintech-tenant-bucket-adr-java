package cc.infrai.fintech.notify;

import cc.infrai.fintech.domain.PaymentAction;
import cc.infrai.fintech.domain.PaymentEvent;

public final class AuditNotificationFactory {
    public AuditNotification forDecision(PaymentEvent event, PaymentAction action, String evidenceKey) {
        String message;
        if (action == PaymentAction.MANUAL_REVIEW_REQUIRED) {
            message = "manual review required before evidence storage";
        } else {
            message = "evidence stored at key " + evidenceKey;
        }
        return new AuditNotification(event.tenantId(), event.paymentId(), "AUDIT_LOG", message);
    }
}
