# Phase 4 Summary: Intelligence Core (Classification, Prioritization, Assignment Strategies, and Duplicate Detection)

## 1. What Was Built

### A. Natural Language Processing Subsystem (`com.iitsm.domain.strategy.nlp`)
- **`TextNormalizer`**:
  - Algorithmic text preprocessor: case normalization (`Locale.ROOT`), non-alphanumeric punctuation stripping, and whitespace compaction.
  - Stopword filtering using an optimized hash set of 80+ standard English stopwords.
  - Custom algorithmic English inflection stemmer handling common morphological suffixes:
    - `-sses` $\rightarrow$ `-ss` (e.g., `processes` $\rightarrow$ `process`)
    - `-ies` $\rightarrow$ root (e.g., `queries` $\rightarrow$ `quer`)
    - `-ing` $\rightarrow$ root (e.g., `crashing` $\rightarrow$ `crash`)
    - `-ed` $\rightarrow$ root (e.g., `crashed` $\rightarrow$ `crash`)
    - `-ly` $\rightarrow$ root (e.g., `slowly` $\rightarrow$ `slow`)
    - `-ment` $\rightarrow$ root (e.g., `department` $\rightarrow$ `depart`)
    - `-ion` $\rightarrow$ root (e.g., `connection` $\rightarrow$ `connect`)
    - `-s` / `-es` singularization (e.g., `servers` $\rightarrow$ `server`)
    - Terminal `-e` reduction (e.g., `database` $\rightarrow$ `databas`)
    - Post-stemming double consonant reduction (e.g., `dropped` $\rightarrow$ `dropp` $\rightarrow$ `drop`).
- **`TfIdfVectorizer`**:
  - In-memory Term Frequency – Inverse Document Frequency (TF-IDF) feature extractor implemented in pure Java without third-party ML dependencies.
  - Term Frequency: normalized sub-linear term occurrence count per document ($tf = \frac{count}{|doc|}$).
  - Inverse Document Frequency: smooth logarithmic weighting:
    $$\text{idf}(t, D) = \ln\left(1 + \frac{|D|}{1 + df(t)}\right)$$
  - Vector normalization: L2 Euclidean unit normalization ($\|v\|_2 = 1.0$) ensuring length-invariant cosine dot product calculation:
    $$\text{similarity}(u, v) = u \cdot v = \sum_{i} u_i v_i$$

### B. Synthetic Training Corpus & Benchmark Data (`src/main/resources/sim/`)
- **`corpus.csv`**:
  - 630 enterprise IT incident samples across all 9 ITIL categories (70 samples per category: `HARDWARE`, `SOFTWARE`, `NETWORK`, `DATABASE`, `SECURITY`, `CLOUD`, `EMAIL`, `ACCESS`, `APPLICATION`).
  - Covers authentic enterprise scenarios: LDAP synchronization, PostgreSQL connection pool exhaustion, BGP route flapping, AnyConnect VPN gateway timeouts, SSO SAML token expirations, and ransomware payloader alerts.
- **`test_tickets.csv`**:
  - 36 distinct, unseen handwritten evaluation incidents used for benchmark accuracy verification ($\ge 80\%$ classification accuracy).

### C. Incident Classification & Service Matching Subsystem (`com.iitsm.domain.strategy.classification`)
- **`IncidentClassifier`**: Strategy interface defining contract `classify(title, description)`.
- **`ClassificationResult`**: Immutable value object encapsulating predicted `category`, `confidence` ($0.0 \dots 1.0$), `classifierName`, `rationale`, and a `requiresTriage` flag triggered whenever confidence drops below $0.60$.
- **`KeywordClassifier`**: High-precision rule-based technical lexicon matcher across all 9 categories.
- **`NaiveBayesClassifier`**:
  - Multinomial Naive Bayes classifier with Laplace smoothing ($\alpha = 1.0$):
    $$P(w | c) = \frac{\text{count}(w, c) + \alpha}{\sum_{w'} \text{count}(w', c) + \alpha \cdot |V|}$$
  - Computes class log-likelihood in log-space ($\sum \ln P(w|c)$) to prevent floating-point underflow.
  - Converts log-likelihoods to calibrated posterior probabilities via numerically stable softmax:
    $$P(c | d) = \frac{\exp(\ln P(c|d) - \max_k \ln P(k|d))}{\sum_j \exp(\ln P(j|d) - \max_k \ln P(k|d))}$$
  - Evaluated on benchmark suite achieving $\ge 80\%$ category prediction accuracy.
- **`CompositeClassifier`**:
  - Composite/Strategy combining `KeywordClassifier` and `NaiveBayesClassifier`.
  - When both classifiers agree, grants a $+0.15$ confidence boost (capped at $0.99$).
  - When classifiers disagree, accepts Naive Bayes if confidence $\ge 0.60$, or falls back to Keyword classifier if matching terms exist; otherwise routes the ticket to the Admin Wrong-Domain Triage Queue (`requiresTriage = true`).
- **`ServiceMatcher`**: Matches incident tokens against registered `ItService` names and keywords to automatically link tickets to impacted infrastructure services.

### D. Multi-Factor Priority Scoring Engine (`com.iitsm.domain.strategy.priority`)
- **`PriorityFactor`**: Strategy interface evaluating an incident across six distinct business dimensions:
  1. **`ServiceCriticalityFactor` (Weight: 0.30)**: Assesses service tier from `ItService.getBusinessCriticality().getScore()` (Tier 4 Mission Critical = 1.0, Tier 3 Operational = 0.75, Tier 2 Departmental = 0.50, Tier 1 Internal = 0.25).
  2. **`ImpactFactor` (Weight: 0.20)**: Analyzes enterprise-wide vs departmental user scope terms, plus adds $+0.10$ per linked duplicate ticket.
  3. **`UrgencyFactor` (Weight: 0.15)**: Detects operational urgency terms (`outage`, `blocked`, `emergency`, `critical`, `degraded`).
  4. **`ReporterDesignationFactor` (Weight: 0.15)**: Reflects organizational stakeholder impact based on `Designation.getPriorityWeight()` (Intern: 0.15, Associate: 0.35, Lead: 0.60, Manager: 0.75, Director: 0.90, VP/C-Level: 1.00).
  5. **`AgingFactor` (Weight: 0.10)**: Time elapsed without resolution ($< 4\text{h}: 0.10$, $4\text{h}-24\text{h}: 0.30$, $1\text{d}-3\text{d}: 0.60$, $3\text{d}-7\text{d}: 0.85$, $\ge 7\text{d}: 1.00$).
  6. **`ReporterHistoryFactor` (Weight: 0.10)**: Analyzes repeat incident friction from the reporting user ($\le 1: 0.20$, $2-4: 0.50$, $\ge 5: 0.90$).
- **`CompositePriorityCalculator`**:
  - Calculates weighted composite score: $\text{score} = \sum_{i=1}^{6} w_i \times s_i$, where $\sum w_i = 1.00$.
  - Maps score to base priority:
    - $\ge 0.75 \rightarrow \text{P1 Critical}$
    - $\ge 0.55 \rightarrow \text{P2 High}$
    - $\ge 0.35 \rightarrow \text{P3 Medium}$
    - $< 0.35 \rightarrow \text{P4 Low}$
  - **Policy Override Rules Chain**:
    - **Rule 1 (`RULE_SECURITY_BREACH`)**: Category `SECURITY` with confirmed breach terms forces minimum priority $\text{P2}$.
    - **Rule 2 (`RULE_CRITICAL_SERVICE_OUTAGE`)**: Tier 4 Mission Critical service with outage terms forces $\text{P1}$.
    - **Rule 3 (`RULE_DUPLICATE_AVALANCHE`)**: 5 or more linked duplicates elevates priority by one level.
  - Produces an immutable, fully explainable `PriorityDecision` record storing composite score, all factor contributions, and audit timestamps.

### E. Swappable Technician Assignment Strategies (`com.iitsm.domain.strategy.assignment`)
- **`AssignmentStrategy`**: Strategy interface recommending optimal technician (`recommend(incident, candidates)`).
- **`CandidateFilter`**:
  - Enforces hard capacity constraints (`technician.hasCapacity()`).
  - Filters out the rejecting technician on wrong-domain reassignment.
  - Prioritizes members of the owning support team first, with graceful fallback to all eligible technicians.
- **5 Swappable Concrete Strategies**:
  1. **`SkillBasedStrategy`**: Scores candidate technicians by keyword match between their declared skills and incident category/content.
  2. **`LoadBasedStrategy`**: Selects technician with lowest active load ratio ($\frac{\text{activeIncidents}}{\text{maxCapacity}}$).
  3. **`ExperienceBasedStrategy`**: Selects technician with greatest years of professional experience.
  4. **`RoundRobinStrategy`**: Atomic circular round-robin selector (`AtomicInteger`) among eligible candidates.
  5. **`WeightedCompositeStrategy` (Default)**: Combines Skill (0.50), Load (0.30), and Experience (0.20), dynamically adjusting weights for high-priority tickets (e.g., boosting Skill to 0.60 for P1/P2).
- **`AssignmentStrategyRegistry`**: Thread-safe registry (`ConcurrentHashMap`) managing strategies and supporting runtime strategy hot-swapping via REST endpoints without server restarts.

### F. Duplicate Detection & Correlation Engine (`com.iitsm.domain.strategy.similarity`, `com.iitsm.application.service`)
- **`SimilarityMeasure`**: Strategy interface computing mathematical similarity between two texts.
  - `CosineTfIdfSimilarity`: Dot product between L2 unit normalized TF-IDF feature vectors.
  - `JaccardSimilarity`: Token set intersection over union: $J(A, B) = \frac{|A \cap B|}{|A \cup B|}$.
- **`DuplicateDetectionService`**:
  - Scans active incidents within the same category and service over a 72-hour sliding window.
  - If similarity $\ge 0.70$, automatically links tickets bidirectionally (`parent.linkIncident`, `child.linkIncident`).
  - Escalates the parent ticket's impact score (+0.10 per duplicate) and publishes `DomainEvent.DuplicateDetected`.

### G. Administrative REST Endpoints (`com.iitsm.web.controller.AdminController`)
- `GET /api/admin/assignment/strategies`: Lists all registered routing strategies and marks the currently active one.
- `PUT /api/admin/assignment/strategies/active`: Hot-swaps the active routing strategy at runtime.
- `POST /api/admin/incidents/{id}/reclassify`: Allows IT administrator to reclassify tickets from the triage queue with updated category, service ID, and recalculates priority and assignment.
- `GET /api/admin/incidents/{id}/duplicates`: Queries all correlated duplicate incidents for a ticket.

---

## 2. Design Patterns Implemented & Viva Defensibility

| Design Pattern | Concrete Location | Architectural Purpose & Viva Rationale |
| :--- | :--- | :--- |
| **Strategy Pattern** | `IncidentClassifier.java`<br>`PriorityFactor.java`<br>`AssignmentStrategy.java`<br>`SimilarityMeasure.java` | Decouples classification, prioritization, routing, and text similarity algorithms from aggregate roots and application services. Allows independent testing and swapping of algorithms. |
| **Composite Pattern** | `CompositeClassifier.java`<br>`CompositePriorityCalculator.java` | Combines multiple child strategies into a unified strategy. `CompositeClassifier` treats `KeywordClassifier` and `NaiveBayesClassifier` uniformly, boosting confidence on consensus. |
| **Registry Pattern** | `AssignmentStrategyRegistry.java` | Centralized, thread-safe dynamic registry managing pluggable routing strategies. Demonstrates the Open/Closed Principle by allowing new custom strategies to be registered at runtime without modifying client code. |
| **Value Object / Immutability** | `ClassificationResult.java`<br>`PriorityDecision.java`<br>`AssignmentRecommendation.java`<br>`IncidentEvaluationContext.java` | Immutable Java records encapsulating multi-dimensional algorithm results with zero side-effects and complete auditability. |

---

## 3. Verification & JaCoCo Coverage Metrics

- **Total Unit & Integration Tests**: **131 tests passing (100% green, 0 failures, 0 errors)**.
- **Overall Instruction Coverage**: **83.26%** (exceeds the 70% threshold).
- **Domain + Application Coverage**: **82.51%** (exceeds the 70% threshold).
- **Pure Domain Integrity**: Zero Spring Framework imports in `com.iitsm.domain.*`.

---

## 4. Known Gaps Intentionally Reserved for Phase 5

1. **SLA Monitoring Background Daemon**: Scheduled cron/timer thread detecting approaching SLA warning thresholds (75%) and hard breaches.
2. **Multi-Tier Escalation Engine**: Automated reassignment and managerial alerting when incidents remain unacknowledged or unaddressed.
3. **Simulation Engine**: Interactive time-accelerated simulation driver (`AdjustableClock`) executing demo scenarios at $\times 1$, $\times 60$, and $\times 600$ speeds.
