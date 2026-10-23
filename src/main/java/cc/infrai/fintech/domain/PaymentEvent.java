package cc.infrai.fintech.domain;

public record PaymentEvent(String tenantId, String paymentId, long amountCents, String currency, RiskLevel riskLevel) {
}
