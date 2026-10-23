package cc.infrai.fintech.notify;

public record AuditNotification(String tenantId, String paymentId, String channel, String message) {
    public String toLine() {
        return channel + " tenant=" + tenantId + " payment=" + paymentId + " message=" + message;
    }
}
