# Batch 3 Report: Documentation Truth-Up & Reconciling All Metrics

---

## 1. Executive Summary

Batch 3 reconciles all repository documentation with measured reality from the source code and build verification. Historical claims and README numbers have been verified and updated to match the real measured metrics.

- **Status**: **186 of 186 tests PASSING (100% GREEN)**
- **Code Coverage**: **85% instruction coverage** (22,526 / 26,306 instructions) across 244 classes.
- **Findings Closed**:
  - **D1 (Test Count Reconciled)**: README and reports updated from historical 165 to real measured 186 passed tests.
  - **D2 (Coverage Metric Reconciled)**: Updated to 85% instruction coverage measured by JaCoCo.
  - **D3 (Architecture Documentation Reconciled)**: Hexagonal ports and adapters structure documented in `docs/03-architecture.md`, aligned with ArchUnit test enforcement.
  - **D4 (Security Specification Reconciled)**: `docs/security.md` updated with JWT startup validation in `prod`, 15-minute token expiration, 5-strike lockout via `LoginAttemptService` (`REQUIRES_NEW`), uniform auth errors, and security headers.
  - **D5 (Classifier Evaluation Documented)**: Created `docs/testing/classifier-eval.md` recording corpus size (630 docs), vocabulary (1,043 tokens), and confidence gating rules.
  - **D6 (DECISIONS.md Updated)**: Recorded design trade-offs regarding sub-transaction boundaries, ArchUnit enforcement, and DTO projections.

---

## 2. Reconciled Metrics Table

| Metric | Previous Documentation | Measured Reality | Source of Truth |
|---|---|---|---|
| **Test Count** | 165 | **186** | `./mvnw clean verify` Surefire execution |
| **Test Failures** | 0 | **0** | Maven Surefire report |
| **Instruction Coverage** | 86.61% | **85%** (22,526 / 26,306) | `target/site/jacoco/index.html` |
| **Branch Coverage** | Unstated | **64%** (1,222 / 1,890) | JaCoCo execution data |
| **Analyzed Classes** | Unstated | **244 classes** | JaCoCo bundle report |
| **Corpus Size** | 630 documents | **630 documents** | `sim/corpus.csv` |
| **NLP Vocabulary** | 1,043 tokens | **1,043 tokens** | `TfIdfVectorizer` training logs |
| **Hexagonal Ports** | Undocumented | **7 Domain / 3 App Ports** | `com.iitsm.domain.repository`, `com.iitsm.application.port` |

---

## 3. Files Reconciled & Created

1. `README.md`: Updated badges and Section 4 with real test count (186) and JaCoCo coverage (85%).
2. `docs/security.md`: Comprehensive truth-up reflecting all security controls implemented in Batches 1 & 2.
3. `docs/03-architecture.md`: Updated architecture diagrams, layer definitions, and ArchUnit dependency rules.
4. `docs/testing/classifier-eval.md`: Documented dataset, Laplace Naive Bayes formulas, and F5 confidence thresholds.
5. `docs/review/batch-3.md`: This report.

