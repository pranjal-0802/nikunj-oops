package com.iitsm.infrastructure.persistence;

import com.iitsm.application.service.UserFactory;
import com.iitsm.domain.model.AdminLevel;
import com.iitsm.domain.model.Administrator;
import com.iitsm.domain.model.BusinessCriticality;
import com.iitsm.domain.model.Designation;
import com.iitsm.domain.model.Email;
import com.iitsm.domain.model.Employee;
import com.iitsm.domain.model.IncidentCategory;
import com.iitsm.domain.model.ItService;
import com.iitsm.domain.model.Priority;
import com.iitsm.domain.model.Skill;
import com.iitsm.domain.model.SlaPolicy;
import com.iitsm.domain.model.SupportTeam;
import com.iitsm.domain.model.TeamLead;
import com.iitsm.domain.model.Technician;
import com.iitsm.domain.repository.ItServiceRepository;
import com.iitsm.domain.repository.SlaPolicyRepository;
import com.iitsm.domain.repository.SupportTeamRepository;
import com.iitsm.domain.repository.TechnicianRepository;
import com.iitsm.domain.repository.UserRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import net.datafaker.Faker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds the simulated company "Nimbus FinServe" deterministically using seed 42.
 * Generates ~300 employees, 6 teams, ~20 technicians, 14 IT services, and default SLA policies.
 */
@Component
public class CompanySeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TechnicianRepository technicianRepository;
    private final SupportTeamRepository supportTeamRepository;
    private final ItServiceRepository itServiceRepository;
    private final SlaPolicyRepository slaPolicyRepository;
    private final UserFactory userFactory;
    private final PasswordEncoder passwordEncoder;

    @Value("${iitsm.simulation.auto-seed:true}")
    private boolean autoSeed;

    @Value("${iitsm.simulation.seed:42}")
    private long seed;

    public CompanySeeder(
            UserRepository userRepository,
            TechnicianRepository technicianRepository,
            SupportTeamRepository supportTeamRepository,
            ItServiceRepository itServiceRepository,
            SlaPolicyRepository slaPolicyRepository,
            UserFactory userFactory,
            PasswordEncoder passwordEncoder) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.technicianRepository = Objects.requireNonNull(technicianRepository);
        this.supportTeamRepository = Objects.requireNonNull(supportTeamRepository);
        this.itServiceRepository = Objects.requireNonNull(itServiceRepository);
        this.slaPolicyRepository = Objects.requireNonNull(slaPolicyRepository);
        this.userFactory = Objects.requireNonNull(userFactory);
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder);
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!autoSeed || userRepository.count() > 0) {
            return;
        }
        seedCompanyData();
    }

    public void seedCompanyData() {
        Random random = new Random(seed);
        Faker faker = new Faker(random);
        String defaultHash = passwordEncoder.encode("Password123!");

        // 1. Seed SLA Policies (P1..P4 defaults)
        for (Priority priority : Priority.values()) {
            slaPolicyRepository.save(SlaPolicy.defaultFor(priority));
        }

        // 2. Seed Administrators (System Admin and IT Manager - Section 7.1)
        Administrator systemAdmin = userFactory.createAdministrator(
                "ADMIN-SYS-01",
                "System Administrator",
                "admin@nimbusfinserve.com",
                defaultHash,
                AdminLevel.SYSTEM_ADMIN);
        userRepository.save(systemAdmin);

        Administrator itManager = userFactory.createAdministrator(
                "ADMIN-MGR-01",
                "IT Operations Manager",
                "manager@nimbusfinserve.com",
                defaultHash,
                AdminLevel.IT_MANAGER);
        userRepository.save(itManager);

        // 3. Seed 6 Support Teams and Technicians
        SupportTeam netTeam = seedTeamWithTechnicians(
                "TEAM-NET", "Network",
                Set.of(IncidentCategory.NETWORK),
                "lead.network@nimbusfinserve.com", "Sarah Jenkins",
                List.of(
                        new TechSpec("tech.network@nimbusfinserve.com", "David Kim", 6, 7, Set.of("Cisco Routing", "BGP", "Firewalls")),
                        new TechSpec("tech.net2@nimbusfinserve.com", "Elena Rostova", 4, 6, Set.of("DNS/DHCP", "VPN Gateways")),
                        new TechSpec("tech.net3@nimbusfinserve.com", "Marcus Vance", 2, 5, Set.of("LAN Switching", "Wi-Fi APs"))
                ),
                defaultHash, random);

        SupportTeam dbTeam = seedTeamWithTechnicians(
                "TEAM-DB", "Database & Storage",
                Set.of(IncidentCategory.DATABASE, IncidentCategory.STORAGE),
                "lead.db@nimbusfinserve.com", "Michael Chang",
                List.of(
                        new TechSpec("tech.db1@nimbusfinserve.com", "Priya Patel", 8, 8, Set.of("PostgreSQL Tuning", "Oracle RAC", "Replication")),
                        new TechSpec("tech.db2@nimbusfinserve.com", "Liam O'Connor", 5, 6, Set.of("Redis Cache", "SQL Optimization")),
                        new TechSpec("tech.db3@nimbusfinserve.com", "Amina Diallo", 3, 5, Set.of("SAN Storage", "Backup & Recovery"))
                ),
                defaultHash, random);

        SupportTeam appTeam = seedTeamWithTechnicians(
                "TEAM-APP", "Application & Software",
                Set.of(IncidentCategory.APPLICATION, IncidentCategory.SOFTWARE),
                "lead.app@nimbusfinserve.com", "Vikram Malhotra",
                List.of(
                        new TechSpec("tech.app1@nimbusfinserve.com", "Chloe Dubois", 7, 8, Set.of("Java Microservices", "Spring Boot", "Kafka")),
                        new TechSpec("tech.app2@nimbusfinserve.com", "Tomas Novak", 4, 6, Set.of("React/Node.js", "REST APIs")),
                        new TechSpec("tech.app3@nimbusfinserve.com", "Fatima Al-Sayed", 3, 5, Set.of("CRM Integration", "HRMS Portals"))
                ),
                defaultHash, random);

        SupportTeam secTeam = seedTeamWithTechnicians(
                "TEAM-SEC", "Security & Identity",
                Set.of(IncidentCategory.SECURITY, IncidentCategory.AUTHENTICATION),
                "lead.sec@nimbusfinserve.com", "Hannah Fischer",
                List.of(
                        new TechSpec("tech.sec1@nimbusfinserve.com", "Carlos Mendez", 9, 8, Set.of("OAuth2/OIDC", "Active Directory", "MFA")),
                        new TechSpec("tech.sec2@nimbusfinserve.com", "Olga Ivanova", 5, 6, Set.of("SIEM", "Incident Response", "Vulnerability Scanning")),
                        new TechSpec("tech.sec3@nimbusfinserve.com", "Kevin Zhang", 3, 5, Set.of("Access Control", "SSL/TLS Certificates"))
                ),
                defaultHash, random);

        SupportTeam cloudTeam = seedTeamWithTechnicians(
                "TEAM-CLOUD", "Cloud Platform",
                Set.of(IncidentCategory.CLOUD),
                "lead.cloud@nimbusfinserve.com", "Rajesh Sharma",
                List.of(
                        new TechSpec("tech.cloud1@nimbusfinserve.com", "Emily Watson", 8, 8, Set.of("Kubernetes", "AWS Infrastructure", "Terraform")),
                        new TechSpec("tech.cloud2@nimbusfinserve.com", "Noah Becker", 5, 6, Set.of("Docker", "CI/CD Pipelines", "Object Storage")),
                        new TechSpec("tech.cloud3@nimbusfinserve.com", "Lucas Silva", 2, 5, Set.of("Monitoring/Prometheus", "Cloud Networking"))
                ),
                defaultHash, random);

        SupportTeam deskTeam = seedTeamWithTechnicians(
                "TEAM-DESK", "Service Desk",
                Set.of(IncidentCategory.HARDWARE),
                "lead.desk@nimbusfinserve.com", "Amanda Brooks",
                List.of(
                        new TechSpec("tech.desk1@nimbusfinserve.com", "Jordan Bell", 6, 9, Set.of("Laptop Provisioning", "MacOS/Windows", "Display Docks")),
                        new TechSpec("tech.desk2@nimbusfinserve.com", "Samantha Reed", 4, 8, Set.of("Printers & Peripherals", "Office Deskside Support")),
                        new TechSpec("tech.desk3@nimbusfinserve.com", "Daniel Cooper", 2, 7, Set.of("Asset Management", "Hardware Diagnostics"))
                ),
                defaultHash, random);

        // 4. Seed 14 IT Services (Section 7.1)
        seedService("SVC-PAYMENT", "Payment Gateway", cloudTeam.getId(), BusinessCriticality.TIER_4_MISSION_CRITICAL, 150000,
                Set.of("payment", "checkout", "stripe", "visa", "mastercard", "transaction", "502", "gateway"));
        seedService("SVC-CORE-DB", "Core Banking DB", dbTeam.getId(), BusinessCriticality.TIER_4_MISSION_CRITICAL, 200000,
                Set.of("database", "db", "postgres", "sql", "query", "deadlock", "timeout", "banking db", "ledger"));
        seedService("SVC-SSO", "SSO/Identity Provider", secTeam.getId(), BusinessCriticality.TIER_4_MISSION_CRITICAL, 50000,
                Set.of("sso", "login", "identity", "saml", "okta", "auth", "mfa", "password reset", "account locked"));
        seedService("SVC-CRM", "Customer Support CRM", appTeam.getId(), BusinessCriticality.TIER_3_BUSINESS_OPERATIONAL, 12000,
                Set.of("crm", "customer", "support tickets", "zendesk", "salesforce", "client records"));
        seedService("SVC-VPN", "Corporate VPN", netTeam.getId(), BusinessCriticality.TIER_3_BUSINESS_OPERATIONAL, 15000,
                Set.of("vpn", "tunnel", "remote", "wireguard", "openvpn", "connect", "london office"));
        seedService("SVC-EMAIL", "Email & Calendar", cloudTeam.getId(), BusinessCriticality.TIER_3_BUSINESS_OPERATIONAL, 40000,
                Set.of("email", "calendar", "outlook", "exchange", "mailbox", "smtp", "inbox"));
        seedService("SVC-DWH", "Data Warehouse", dbTeam.getId(), BusinessCriticality.TIER_3_BUSINESS_OPERATIONAL, 8000,
                Set.of("warehouse", "dwh", "snowflake", "bigquery", "analytics", "etl", "reporting db"));
        seedService("SVC-CICD", "CI/CD Pipeline", cloudTeam.getId(), BusinessCriticality.TIER_2_DEPARTMENTAL, 3000,
                Set.of("ci/cd", "pipeline", "jenkins", "github actions", "deploy", "build failing", "artifact"));
        seedService("SVC-HRMS", "HRMS", appTeam.getId(), BusinessCriticality.TIER_2_DEPARTMENTAL, 5000,
                Set.of("hrms", "payroll", "leave", "workday", "hr portal", "benefits", "attendance"));
        seedService("SVC-STORAGE", "Cloud Object Storage", cloudTeam.getId(), BusinessCriticality.TIER_3_BUSINESS_OPERATIONAL, 25000,
                Set.of("s3", "storage", "blob", "bucket", "object storage", "download failing"));
        seedService("SVC-WIFI", "Office Wi-Fi", netTeam.getId(), BusinessCriticality.TIER_2_DEPARTMENTAL, 4000,
                Set.of("wi-fi", "wifi", "wireless", "ssid", "office network", "disconnecting"));
        seedService("SVC-PRINT", "Printing", deskTeam.getId(), BusinessCriticality.TIER_1_INTERNAL_SUPPORT, 1500,
                Set.of("printer", "printing", "paper jam", "toner", "scanner", "plotter"));
        seedService("SVC-HARDWARE", "Laptop & Peripherals", deskTeam.getId(), BusinessCriticality.TIER_1_INTERNAL_SUPPORT, 2000,
                Set.of("laptop", "monitor", "dock", "keyboard", "mouse", "screen flickering", "charger"));
        seedService("SVC-WIKI", "Intranet Wiki", appTeam.getId(), BusinessCriticality.TIER_1_INTERNAL_SUPPORT, 3500,
                Set.of("wiki", "confluence", "intranet", "knowledge base", "docs"));

        // 5. Seed Primary Demo Employee
        Employee demoEmployee = userFactory.createEmployee(
                "EMP-DEMO-01",
                "Demo Employee",
                "employee@nimbusfinserve.com",
                defaultHash,
                Designation.SENIOR_ASSOCIATE,
                "Engineering",
                4);
        userRepository.save(demoEmployee);

        // 6. Seed ~300 Employees across 8 departments (Section 7.1)
        String[] departments = {"Engineering", "Finance", "HR", "Sales", "Operations", "Customer Support", "Legal", "Executive"};
        Designation[] designations = Designation.values();

        for (int i = 1; i <= 300; i++) {
            String firstName = faker.name().firstName();
            String lastName = faker.name().lastName();
            String email = String.format("%s.%s%d@nimbusfinserve.com",
                    firstName.toLowerCase().replaceAll("[^a-z]", ""),
                    lastName.toLowerCase().replaceAll("[^a-z]", ""),
                    i);

            String dept = departments[random.nextInt(departments.length)];
            Designation desig = designations[random.nextInt(designations.length)];
            int seniority = random.nextInt(15);

            Employee emp = userFactory.createEmployee(
                    "EMP-" + String.format("%04d", i),
                    firstName + " " + lastName,
                    email,
                    defaultHash,
                    desig,
                    dept,
                    seniority);
            userRepository.save(emp);
        }
    }

    private SupportTeam seedTeamWithTechnicians(
            String teamId,
            String teamName,
            Set<IncidentCategory> categories,
            String leadEmail,
            String leadName,
            List<TechSpec> specs,
            String defaultHash,
            Random random) {

        String leadId = "LEAD-" + teamId.replace("TEAM-", "");
        Set<Skill> leadSkills = new HashSet<>();
        for (IncidentCategory cat : categories) {
            leadSkills.add(new Skill(teamName + " Architecture", cat));
        }

        TeamLead lead = userFactory.createTeamLead(
                leadId, leadName, leadEmail, defaultHash, leadSkills, 10, 8, teamId);
        technicianRepository.save(lead);

        SupportTeam team = new SupportTeam(teamId, teamName, categories, leadId);

        for (int i = 0; i < specs.size(); i++) {
            TechSpec spec = specs.get(i);
            String techId = "TECH-" + teamId.replace("TEAM-", "") + "-" + (i + 1);
            Set<Skill> skills = new HashSet<>();
            for (String s : spec.skills) {
                skills.add(new Skill(s, categories.iterator().next()));
            }

            Technician tech = userFactory.createTechnician(
                    techId, spec.name, spec.email, defaultHash, skills, spec.experience, spec.capacity, teamId);
            technicianRepository.save(tech);
            team.addMember(techId);
        }

        return supportTeamRepository.save(team);
    }

    private void seedService(
            String id, String name, String teamId, BusinessCriticality crit, int userBase, Set<String> keywords) {
        ItService service = new ItService(id, name, teamId, crit, userBase, keywords);
        itServiceRepository.save(service);
    }

    private record TechSpec(String email, String name, int experience, int capacity, Set<String> skills) {}
}
