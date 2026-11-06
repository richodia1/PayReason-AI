package ai.payreason.risk.repository;

import ai.payreason.risk.entities.RiskAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.UUID;

public interface RiskRepository extends JpaRepository<RiskAssessment, UUID>, JpaSpecificationExecutor<RiskAssessment> {}
