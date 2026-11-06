package ai.payreason.risk.services;

import ai.payreason.payments.dto.PaymentResponse;
import ai.payreason.payments.entities.PaymentDraft;
import ai.payreason.payments.enums.PaymentChannel;
import ai.payreason.payments.repository.PaymentRepository;
import ai.payreason.risk.dto.AssessmentResponse;
import ai.payreason.risk.entities.RiskAssessment;
import ai.payreason.risk.enums.ReviewDecision;
import ai.payreason.risk.repository.RiskRepository;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
@Transactional(readOnly = true)
public class CaseQueryService {
    public record CaseRow(PaymentResponse payment, AssessmentResponse assessment) {}
    public record CasePage(List<CaseRow> items, long total, int page, int size) {}
    private final RiskRepository risks;
    private final PaymentRepository payments;
    private final ObjectMapper json;
    public CaseQueryService(RiskRepository risks, PaymentRepository payments, ObjectMapper json) {
        this.risks = risks; this.payments = payments; this.json = json;
    }
    public CasePage query(int page, int size, PaymentChannel channel, Integer year, ReviewDecision decision) {
        Specification<RiskAssessment> spec = (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (decision != null) predicates.add(cb.equal(root.get("decision"), decision));
            if (year != null) {
                var start = LocalDate.of(year, 1, 1).atStartOfDay().atOffset(ZoneOffset.UTC);
                predicates.add(cb.greaterThanOrEqualTo(root.get("assessedAt"), start));
                predicates.add(cb.lessThan(root.get("assessedAt"), start.plusYears(1)));
            }
            if (channel != null) {
                var sub = query.subquery(UUID.class);
                var p = sub.from(PaymentDraft.class);
                sub.select(p.get("id")).where(cb.equal(p.get("channel"), channel));
                predicates.add(root.get("paymentId").in(sub));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        var result = risks.findAll(spec, PageRequest.of(page, size, Sort.by(Sort.Order.desc("assessedAt"), Sort.Order.desc("paymentId"))));
        var rows = result.getContent().stream().map(r -> new CaseRow(PaymentResponse.from(payments.findById(r.getPaymentId()).orElseThrow()), AssessmentResponse.from(r, json))).toList();
        return new CasePage(rows, result.getTotalElements(), page, size);
    }
}
