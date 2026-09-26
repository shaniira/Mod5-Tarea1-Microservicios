package com.andinaseguros.notification.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Mismas reglas que el backend (plantilla del microservicio, paso 1). */
@AnalyzeClasses(
        packages = "com.andinaseguros.notification",
        importOptions = ImportOption.DoNotIncludeTests.class)
class CleanArchitectureTest {
    @ArchTest
    static final ArchRule entities_are_independent =
            noClasses()
                    .that()
                    .resideInAPackage("..entities..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("..usecases..", "..interfaceadapters..", "..frameworksdrivers..");

    @ArchTest
    static final ArchRule use_cases_only_point_inward =
            noClasses()
                    .that()
                    .resideInAPackage("..usecases..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("..interfaceadapters..", "..frameworksdrivers..");

    @ArchTest
    static final ArchRule interface_adapters_do_not_know_frameworks =
            noClasses()
                    .that()
                    .resideInAPackage("..interfaceadapters..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..frameworksdrivers..");

    @ArchTest
    static final ArchRule entities_have_no_framework_dependencies =
            noClasses()
                    .that()
                    .resideInAPackage("..entities..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework..", "org.bson..", "com.mongodb..", "io.github.resilience4j..");

    @ArchTest
    static final ArchRule use_cases_have_no_framework_dependencies =
            noClasses()
                    .that()
                    .resideInAPackage("..usecases..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework..",
                            "org.bson..",
                            "com.mongodb..",
                            "io.github.resilience4j..",
                            "com.rabbitmq..");

    @ArchTest
    static final ArchRule output_ports_are_interfaces =
            classes()
                    .that(
                            DescribedPredicate.describe(
                                    "reside in ..usecases.port.out..",
                                    clazz -> clazz.getPackageName().contains(".usecases.port.out")))
                    .should()
                    .beInterfaces();

    @ArchTest
    static final ArchRule listeners_live_in_interface_adapters =
            classes()
                    .that()
                    .haveSimpleNameEndingWith("Listener")
                    .should()
                    .resideInAPackage("..interfaceadapters.in.messaging..");

    @ArchTest
    static final ArchRule nothing_reads_the_backend_database =
            noClasses()
                    .should()
                    .accessClassesThat()
                    .haveSimpleName("ClienteDocument")
                    .because("fase 1: notification-service ya no lee la coleccion clientes del backend");
}
