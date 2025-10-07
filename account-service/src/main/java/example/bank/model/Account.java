package example.bank.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import lombok.Data;

@Data
@Table("accounts")
public class Account {
    @Id
    private Long id;
    private String username;
    private String email;
    private String currency;
    private Double balance;
}
