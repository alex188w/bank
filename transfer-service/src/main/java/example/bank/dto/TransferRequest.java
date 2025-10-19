package example.bank.dto;

import java.math.BigDecimal;

import lombok.Data;

public class TransferRequest {
    private Long fromId;
    private Long toId;
    private String toUsername;
    private BigDecimal amount;

    // Геттеры и сеттеры
    public Long getFromId() { return fromId; }
    public void setFromId(Long fromId) { this.fromId = fromId; }

    public Long getToId() { return toId; }
    public void setToId(Long toId) { this.toId = toId; }

    public String getToUsername() { return toUsername; }
    public void setToUsername(String toUsername) { this.toUsername = toUsername; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
