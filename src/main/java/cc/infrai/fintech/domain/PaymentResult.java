package cc.infrai.fintech.domain;

import cc.infrai.fintech.notify.AuditNotification;

public record PaymentResult(PaymentAction action, String bucket, String evidenceKey, AuditNotification notification) {
}
