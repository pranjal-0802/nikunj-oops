package com.iitsm.domain.chain.escalation;

/**
 * Factory assembling the default Multi-Tier Escalation Chain of Responsibility.
 * Flow: Tier 4 (IT Manager) -> Tier 3 (System Admin) -> Tier 2 (Team Lead) -> Tier 1 (Technician).
 */
public final class EscalationChainBuilder {

    private EscalationChainBuilder() {}

    public static EscalationHandler buildDefaultChain() {
        EscalationHandler tier4 = new Tier4ItManagerHandler();
        EscalationHandler tier3 = new Tier3SystemAdminHandler();
        EscalationHandler tier2 = new Tier2TeamLeadHandler();
        EscalationHandler tier1 = new Tier1TechnicianHandler();

        tier4.setNext(tier3);
        tier3.setNext(tier2);
        tier2.setNext(tier1);

        return tier4;
    }
}

