package example.bank.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    private String type; // deposit, withdraw, transfer, keep-alive
    private String username;
    private Long fromId;
    private Long toId;
    private String message;
    private BigDecimal amount;
    private Long accountId;
}
