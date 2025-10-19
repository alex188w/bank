package example.bank.model;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class TransferRequest {
    private String fromUsername; // отправитель
    private Long fromId;         // ID счёта отправителя
    private String toUsername;   // получатель
    private Long toId;           // ID счёта получателя
    private BigDecimal amount;   // сумма перевода
}
