package com.iitsm.regression;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Regression test suite verifying architectural hexagonal layering constraints (Finding A1).
 * Tagged with @Tag("regression") and must fail against the current unpatched code.
 */
@Tag("regression")
public class ArchitectureRegressionTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.iitsm");
    }

    @Test
    @DisplayName("A1: Application layer must not import or depend on infrastructure layer types")
    void A1_applicationLayerMustNotDependOnInfrastructure() {
        // Finding A1: application.* imports com.iitsm.infrastructure.* directly (EscalationRecordJpaEntity, JwtTokenProvider, etc.)
        noClasses()
                .that().resideInAPackage("com.iitsm.application..")
                .should().dependOnClassesThat().resideInAPackage("com.iitsm.infrastructure..")
                .check(importedClasses);
    }

    @Test
    @DisplayName("A1: Web layer must not import or depend on infrastructure layer types")
    void A1_webLayerMustNotDependOnInfrastructure() {
        // Finding A1: web.* imports com.iitsm.infrastructure.* directly (AuditRecordJpaEntity, SpringDataAuditRecordRepository, etc.)
        noClasses()
                .that().resideInAPackage("com.iitsm.web..")
                .should().dependOnClassesThat().resideInAPackage("com.iitsm.infrastructure..")
                .check(importedClasses);
    }

    @Test
    @DisplayName("A1: Web controllers must only return DTOs or framework types, never domain or JPA entities")
    void A1_controllersMustNotReturnDomainOrJpaEntities() {
        // Finding A1/S4: AdminController returns List<Technician> (domain) and List<ReassignmentRequestJpaEntity> (JPA entity)
        noClasses()
                .that().resideInAPackage("com.iitsm.web.controller..")
                .should().dependOnClassesThat().resideInAnyPackage("com.iitsm.infrastructure.persistence.entity..")
                .check(importedClasses);
    }

    @Test
    @DisplayName("A1: Web controllers must call services only and not depend on Spring Data or Domain repositories")
    void A1_controllersMustNotDependOnRepositories() {
        // Finding A1: EmployeeController, AdminController, NotificationController inject Spring Data repositories directly
        noClasses()
                .that().resideInAPackage("com.iitsm.web.controller..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.iitsm.infrastructure.persistence.repository..",
                        "org.springframework.data.jpa.repository.."
                )
                .check(importedClasses);
    }
}
