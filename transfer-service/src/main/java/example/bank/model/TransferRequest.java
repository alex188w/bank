package example.bank.model;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransferRequest {
    private String fromUsername;
    private Long fromAccountId;
    private String toUsername;
    private Long toAccountId;
    private BigDecimal amount;
}