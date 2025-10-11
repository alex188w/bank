package example.bank.model;

import java.math.BigDecimal;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import lombok.Data;

@Data
@Table("account")
public class Account {
    @Id
    private Long id;

    private String username;

    @Column("owner_id")
    private String ownerId;

    private String currency;
    private BigDecimal balance;
}
