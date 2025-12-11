package example.bank.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(schema = "outbox", name = "outbox_events")
public class OutboxEvent {

    @Id
    private Long id;

    private String aggregateType;   // "ACCOUNT"
    private String aggregateId;     // accountId как String
    private String eventType;       // "ACCOUNT_CREATED"

    private String payload;         // JSON Notification

    private Instant createdAt;

    private boolean processed;      // отправлено в Kafka или нет
}
