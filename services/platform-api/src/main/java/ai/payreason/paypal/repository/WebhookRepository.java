package ai.payreason.paypal.repository;

import ai.payreason.paypal.entities.WebhookReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookRepository extends JpaRepository<WebhookReceipt, String> {}
