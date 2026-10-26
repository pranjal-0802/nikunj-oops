package com.iitsm.domain.strategy.classification;

import com.iitsm.domain.model.IncidentCategory;
import com.iitsm.domain.strategy.nlp.TextNormalizer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pattern: Strategy.
 * Rule-based classifier matching high-precision domain keywords and technical lexicons.
 */
public class KeywordClassifier implements IncidentClassifier {

    private static final String CLASSIFIER_NAME = "KEYWORD_CLASSIFIER";

    private final Map<IncidentCategory, Set<String>> categoryLexicon = new EnumMap<>(IncidentCategory.class);

    public KeywordClassifier() {
        initLexicon();
    }

    private void initLexicon() {
        categoryLexicon.put(IncidentCategory.HARDWARE, Set.of(
                "screen", "monitor", "keyboard", "mouse", "fan", "battery", "overheating", "overheat",
                "trackpad", "display", "dock", "docking", "laptop", "ram", "power", "gpu", "cpu",
                "chassis", "hardware", "cable", "hdmi", "displayport", "headset", "audio", "port"
        ));

        categoryLexicon.put(IncidentCategory.SOFTWARE, Set.of(
                "excel", "outlook", "teams", "visual", "studio", "ide", "crash", "freeze", "freezing",
                "dll", "patch", "chrome", "acrobat", "bug", "update", "macro", "slack", "software",
                "app", "application", "browser", "driver", "install", "compilation"
        ));

        categoryLexicon.put(IncidentCategory.NETWORK, Set.of(
                "vpn", "wifi", "wireless", "dns", "latency", "router", "switch", "packet", "subnet",
                "ip", "bgp", "stp", "lan", "wan", "anyconnect", "gateway", "flapping", "ethernet",
                "bandwidth", "firewall", "ssid", "connection", "network"
        ));

        categoryLexicon.put(IncidentCategory.DATABASE, Set.of(
                "postgresql", "postgres", "oracle", "mysql", "redis", "database", "query", "sql",
                "deadlock", "tablespace", "pool", "hikaricp", "hikari", "replication", "mongo",
                "mongodb", "rdbms", "table", "index", "db", "transaction"
        ));

        categoryLexicon.put(IncidentCategory.CLOUD, Set.of(
                "aws", "ec2", "s3", "azure", "gcp", "kubernetes", "k8s", "pod", "terraform",
                "lambda", "docker", "container", "cloudfront", "iam", "oomkilled", "cluster",
                "cloud", "microservice", "ingress", "pubsub"
        ));

        categoryLexicon.put(IncidentCategory.SECURITY, Set.of(
                "phishing", "malware", "ransomware", "breach", "unauthorized", "hacker", "soc",
                "edr", "crowdstrike", "trojan", "suspicious", "sudo", "scan", "dlp", "vulnerability",
                "leak", "security", "threat", "waf", "injection", "compromised"
        ));

        categoryLexicon.put(IncidentCategory.AUTHENTICATION, Set.of(
                "okta", "mfa", "2fa", "sso", "saml", "kerberos", "password", "login", "credentials",
                "active", "directory", "ldap", "locked", "lockout", "token", "yubikey", "webauthn",
                "auth", "authenticate", "fido2", "pin"
        ));

        categoryLexicon.put(IncidentCategory.APPLICATION, Set.of(
                "portal", "crm", "erp", "benefits", "invoice", "hrms", "payroll", "banking",
                "transfer", "billing", "order", "payment", "checkout", "wiki", "frontend",
                "concur", "salesforce", "workday", "website", "service"
        ));

        categoryLexicon.put(IncidentCategory.STORAGE, Set.of(
                "san", "nas", "lun", "backup", "raid", "nfs", "iscsi", "veeam", "commvault",
                "disk", "tape", "volume", "datastore", "storage", "drive", "pool", "filesystem"
        ));
    }

    @Override
    public ClassificationResult classify(String title, String description) {
        String fullText = (title != null ? title : "") + " " + (description != null ? description : "");
        List<String> tokens = TextNormalizer.tokenize(fullText);

        IncidentCategory bestCategory = IncidentCategory.APPLICATION;
        int maxMatches = 0;
        List<String> matchedTerms = new ArrayList<>();

        for (Map.Entry<IncidentCategory, Set<String>> entry : categoryLexicon.entrySet()) {
            int currentMatches = 0;
            List<String> currentTerms = new ArrayList<>();
            for (String token : tokens) {
                if (entry.getValue().contains(token)) {
                    currentMatches++;
                    currentTerms.add(token);
                }
            }
            if (currentMatches > maxMatches) {
                maxMatches = currentMatches;
                bestCategory = entry.getKey();
                matchedTerms = currentTerms;
            }
        }

        double confidence;
        List<String> rationale = new ArrayList<>();
        if (maxMatches >= 3) {
            confidence = 0.90;
            rationale.add("Strong keyword match (" + maxMatches + " terms): " + matchedTerms);
        } else if (maxMatches == 2) {
            confidence = 0.75;
            rationale.add("Moderate keyword match (" + maxMatches + " terms): " + matchedTerms);
        } else if (maxMatches == 1) {
            confidence = 0.50;
            rationale.add("Weak single keyword match: " + matchedTerms);
        } else {
            confidence = 0.20;
            rationale.add("No explicit domain keywords matched; defaulted to APPLICATION");
        }

        return ClassificationResult.of(bestCategory, confidence, CLASSIFIER_NAME, rationale);
    }

    @Override
    public String getName() {
        return CLASSIFIER_NAME;
    }
}

