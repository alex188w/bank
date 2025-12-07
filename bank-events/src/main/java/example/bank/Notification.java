package example.bank;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    private String type;      // deposit, withdraw, transfer, keep-alive, account_created и т.п.
    private String username;
    private Long fromId;
    private Long toId;
    private String message;
    private BigDecimal amount;
    private Long accountId;
}