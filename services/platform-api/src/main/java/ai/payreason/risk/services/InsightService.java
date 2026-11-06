package ai.payreason.risk.services;

import ai.payreason.payments.enums.PaymentChannel;
import ai.payreason.risk.enums.ReviewDecision;
import java.time.Clock;
import java.time.Year;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InsightService {
    private final CaseQueryService cases;
    private final Clock clock;
    private static final Pattern COUNT = Pattern.compile("how many (blocked )?cases(?: (?:came )?from (mobile|web))?(?: (this year|in (20[0-9]{2})))?");
    public InsightService(CaseQueryService cases, Clock clock) { this.cases = cases; this.clock = clock; }
    public Map<String, Object> answer(String question) {
        String text = question.toLowerCase(Locale.ROOT).strip().replaceAll("[?.]+$", "").replaceAll("\\s+", " ");
        CaseQueryService.CasePage result;
        String summary;
        Map<String, Object> filters = new LinkedHashMap<>();
        if (Set.of("what is the latest case", "latest case", "show the latest case").contains(text)) {
            result = cases.query(0, 1, null, null, null);
            summary = result.items().isEmpty() ? "No assessed cases are recorded." : "The latest assessed case is " + result.items().getFirst().payment().reference() + ".";
        } else {
            var matcher = COUNT.matcher(text);
            if (!matcher.matches()) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Supported questions: What is the latest case? How many cases? How many blocked cases from mobile this year? Channels: web or mobile. Cases are assessments, not confirmed fraud.");
            PaymentChannel channel = matcher.group(2) == null ? null : PaymentChannel.valueOf(matcher.group(2).toUpperCase(Locale.ROOT));
            Integer year = matcher.group(3) == null ? null : matcher.group(4) == null ? Year.now(clock).getValue() : Integer.valueOf(matcher.group(4));
            ReviewDecision decision = matcher.group(1) == null ? null : ReviewDecision.BLOCK;
            result = cases.query(0, 5, channel, year, decision);
            filters.put("channel", channel == null ? "ALL" : channel.name());
            filters.put("year", year == null ? "ALL" : year);
            filters.put("decision", decision == null ? "ALL" : decision.name());
            summary = result.total() + (result.total() == 1 ? " assessed case matches" : " assessed cases match") + " these filters. A case or block is not proof of fraud.";
        }
        return Map.of("answer", summary, "count", result.total(), "filters", filters, "evidence", result.items(),
                "asOf", clock.instant(), "method", "DATABASE_QUERY", "evidenceLimit", result.size());
    }
}
