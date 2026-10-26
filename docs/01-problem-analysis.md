# Problem Analysis: Intelligent IT Service Management (I-ITSM)

## 1. Executive Context & Industry Background
Enterprise IT departments in modern financial services organizations (such as the simulated company **Nimbus FinServe**) operate under rigorous regulatory, availability, and compliance mandates. A disruption to core banking services, wire transfer gateways, or transactional databases translates directly to monetary loss, customer dissatisfaction, and regulatory penalties.

Traditional IT service management (ITSM) platforms suffer from several systemic operational inefficiencies:
1. **Manual Ticket Triage & Domain Misclassification**:
   Employees often describe technical outages in colloquial, ambiguous terms (e.g., *"Cannot send money"* or *"System timing out"*). Traditional helpdesks rely on manual triage by human service desk agents, leading to routing delays, incorrect team assignments, and high reassignment friction.
2. **One-Dimensional Urgency Assessment**:
   Legacy systems allow employees to self-assign priority (often leading to "everything is P1" syndrome) or rely on rigid static lookups. Real-world incident prioritization requires multi-factor evaluation incorporating business criticality, affected user blast radius, reporter organizational seniority, historical frequency, and aging duration.
3. **Capacity Blindness & Technician Burnout**:
   Conventional round-robin or manual dispatchers route tickets without awareness of current technician workload, active ticket concurrency, or domain skill specialization, causing workload imbalances.
4. **Duplicate Ticket Avalanches during Outages**:
   When a widespread infrastructure service drops (such as the corporate VPN or Exchange cluster), dozens or hundreds of employees simultaneously file identical reports. Helpdesks become overwhelmed with redundant tickets, obscuring the primary incident.
5. **Static SLA Monitoring & Weak Escalation Chains**:
   Standard SLA trackers merely record hard deadlines without progressive risk assessment ($0.0 \dots 1.0$), failing to provide warning reminders at 75% or 90% thresholds. Escalations are often ad-hoc rather than following a strict, prioritized Chain of Responsibility.
6. **Data Privacy Leaks Across Personas**:
   Conventional portals frequently expose internal technical notes, root cause hypotheses, and backend priority ranks to regular employees, causing friction and confusion.

---

## 2. Problem Statement
The objective of the **Intelligent IT Service Management (I-ITSM)** project is to design and implement a production-grade, object-oriented incident management platform that automates the complete ITIL incident lifecycle while strictly enforcing core OOP design principles:
- **Zero Framework Leakage**: Maintain a 100% pure Java domain core (`com.iitsm.domain.*`) with zero Spring dependencies.
- **Explainable Machine Intelligence**: Implement algorithmic natural language processing (TF-IDF vectorization, Cosine Similarity, and Multinomial Naive Bayes) entirely in pure Java without black-box external ML dependencies.
- **Architectural Extensibility**: Adhere rigorously to the Open/Closed Principle (OCP) across assignment strategies, priority scoring factors, classifiers, similarity measures, and escalation tiers.
- **Temporal Acceleration**: Provide an interactive, time-accelerated simulation engine (`AdjustableClock`) capable of demonstrating hours or days of SLA degradation and automated escalations in seconds.
- **Strict Persona Isolation**: Enforce persona-specific view models ensuring that employees never observe internal priority calculations, SLA operational deadlines, or technician investigation notes.

---

## 3. Stakeholder & Persona Analysis

| Persona | Role in Organization | Core Goals & Capabilities | Privacy & Authorization Boundaries |
| :--- | :--- | :--- | :--- |
| **Employee** | Business End-User (e.g. Finance, Sales, HR Associate) | Raise incidents with natural language titles and descriptions; monitor simplified 5-stage progress stepper; provide requested diagnostic details; verify fixes with 1–5 star ratings or reject resolution. | **Strictly Private**: Cannot view priority (P1–P4), SLA targets/deadlines, technician workload, or internal investigation notes. Cannot access other employees' tickets. |
| **Technician** | Domain Specialist Engineer (Network, Database, App, Cloud, Sec, Desk) | View priority-ordered work queue; acknowledge assigned tickets; investigate root causes; request user info (pausing SLA); record internal engineering notes; flag wrong-domain tickets; submit structured resolutions. | Restricted to assigned incidents and team queue; cannot modify global SLA policies, reclassify services, or execute administrative overrides. |
| **Team Lead** | Technical Team Supervisor | Oversee team queue and technician load balance; handle Tier 2 SLA warning interventions; approve wrong-domain transfers. | Elevated operational visibility within owning support team. |
| **System Administrator** | IT Operations Admin | Full system oversight; triage queue management; administrative reclassification; runtime assignment strategy hot-swapping; simulation execution; executive reporting. | Unrestricted administrative access across all endpoints (`ROLE_ADMIN`). |
| **IT Operations Manager** | Executive Leadership | Top of escalation chain; receives Tier 4 critical P1 breach alerts; reviews SLA compliance, MTTR/MTTResp metrics, and capacity utilization. | Executive oversight and reporting capabilities (`ROLE_ADMIN`). |

---

## 4. Key Performance Indicators (KPIs) Target

1. **Mean Time to Respond (MTTResp)**: $\le 15$ minutes for P1/P2 incidents.
2. **Mean Time to Resolution (MTTR)**: Reduction of resolution delays by $\ge 40\%$ via automated intelligent routing.
3. **Classification Accuracy**: $\ge 80\%$ category identification on authentic enterprise test incidents.
4. **SLA Compliance**: $\ge 95\%$ compliance across all operational priorities.
5. **Duplicate Correlation Precision**: $\ge 90\%$ correlation of duplicate storm tickets with cosine similarity $\ge 0.70$.

