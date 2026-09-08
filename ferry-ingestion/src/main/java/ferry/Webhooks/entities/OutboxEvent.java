package ferry.Webhooks.entities;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Entity
@Table(name = "outbox_event")
@Builder
@Data
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    private Long eventId;

    @Enumerated(EnumType.STRING)
    private OutboxStatus status;
    private Instant createdAt;
    private Instant publishedAt;
}
