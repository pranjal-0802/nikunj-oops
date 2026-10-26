# NLP & Text Classifier Evaluation Report: I-ITSM

---

## 1. Overview & Dataset

The I-ITSM intelligence engine uses a pure-Java algorithmic natural language processing (NLP) and machine learning pipeline without external ML libraries (like TensorFlow, PyTorch, or Weka). The classifier is trained on startup using the domain corpus (`sim/corpus.csv`).

### Dataset Summary:
- **Total Training Documents**: **630 incident records**
- **Categories (9)**: `HARDWARE`, `SOFTWARE`, `NETWORK`, `DATABASE`, `CLOUD`, `SECURITY`, `AUTHENTICATION`, `APPLICATION`, `STORAGE`
- **Vocabulary Size**: **1,043 unique terms** (after lowercasing, regex tokenization, stopword removal, and Porter stemming via `TextNormalizer`)
- **Corpus Balance**: 70 incident examples per category across ITIL enterprise service domains.

---

## 2. Multi-Stage Classification Pipeline

The classification architecture employs the **Composite Pattern** (`CompositeClassifier`) combining two distinct classification strategies:

1. **Rule-Based Keyword Classifier (`KeywordClassifier`)**:
   - Maps technical domain lexicons across all 9 categories (e.g. Cisco/BGP/VLAN for Network, HikariCP/PostgreSQL/deadlock for Database, Okta/MFA/SAML for Authentication).
   - Scoring heuristics:
     - $\ge 3$ terms: High confidence ($0.90$)
     - 2 terms: Moderate confidence ($0.75$)
     - 1 term: Weak confidence ($0.50$)
     - 0 terms: Default ($0.20$)

2. **Multinomial Naive Bayes Classifier (`NaiveBayesClassifier`)**:
   - Computes log-prior probabilities $P(C)$ and log-likelihoods $\sum \ln P(w|C)$ with Laplace smoothing ($\alpha = 1.0$):
     $$P(w|C) = \frac{\text{count}(w, C) + \alpha}{\sum_{w'} \text{count}(w', C) + \alpha \cdot |V|}$$
   - Normalized posterior probabilities computed via softmax.

3. **Composite Fusion Logic (`CompositeClassifier`)**:
   - If Keyword confidence $\ge 0.85$, immediate adoption.
   - If Keyword and Naive Bayes agree with strong individual confidence ($\ge 0.60$), a concordance boost is applied ($1.15 \times \text{confidence} + 0.10$, capped at $0.99$).
   - **Finding F5 Gating**: If keyword support is weak (confidence $\le 0.50$), confidence is strictly capped at $0.55$, ensuring ambiguous or vague tickets remain below the `iitsm.classification.min-confidence: 0.60` threshold.

---

## 3. Human-in-the-Loop Triage Verification

- When composite classification confidence is below the minimum threshold ($0.60$), the incident is flagged with `needsTriage = true`, left unassigned in the `CLASSIFIED` state, and routed to the Administrator Triage Queue (`/api/admin/triage/queue`).
- Verified by regression test `F5_lowConfidenceClassificationLeavesIncidentInTriageQueue`.
- Verified in scenario `S3_wrongDomainAndTriage`.

