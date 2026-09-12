package ferry.Webhooks.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "outbox_event")
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private Long eventId;

    @Enumerated(EnumType.STRING)
    private OutboxStatus status;
    private Instant createdAt;
    private Instant publishedAt;

    @OneToOne
    @JoinColumn(name="eventId", referencedColumnName = "eventId",insertable = false,
            updatable = false)
    private WebhookEvent webhookEvent;
}
