package com.andinaseguros.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.andinaseguros", importOptions = ImportOption.DoNotIncludeTests.class)
class CleanArchitectureTest {
        @ArchTest
        static final ArchRule entities_are_independent =
                noClasses().that().resideInAPackage("..entities..")
                        .should().dependOnClassesThat()
                        .resideInAnyPackage("..usecases..", "..interfaceadapters..", "..frameworksdrivers..");

        @ArchTest
        static final ArchRule use_cases_only_point_inward =
                noClasses().that().resideInAPackage("..usecases..")
                        .should().dependOnClassesThat()
                        .resideInAnyPackage("..interfaceadapters..", "..frameworksdrivers..");

        @ArchTest
        static final ArchRule interface_adapters_do_not_know_frameworks =
                noClasses().that().resideInAPackage("..interfaceadapters..")
                        .should().dependOnClassesThat().resideInAPackage("..frameworksdrivers..");


        @ArchTest
        static final ArchRule entities_have_no_framework_dependencies =
        noClasses().that().resideInAPackage("..entities..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("org.springframework..", "org.bson..", "com.mongodb..");

        @ArchTest
        static final ArchRule use_cases_have_no_framework_dependencies =
                noClasses().that().resideInAPackage("..usecases..")
                        .should().dependOnClassesThat()
                        .resideInAnyPackage(
                                "org.springframework..",
                                "jakarta.persistence..",
                                "jakarta.validation..",
                                "org.bson..",
                                "com.mongodb..");

        @ArchTest
        static final ArchRule output_ports_are_interfaces =
                classes().that(com.tngtech.archunit.base.DescribedPredicate.describe(
                                "reside in ..usecases.port.out.. and are named *Port or *Repository",
                                clazz -> clazz.getPackageName().contains(".usecases.port.out")
                                        && (clazz.getSimpleName().endsWith("Port")
                                                || clazz.getSimpleName().endsWith("Repository"))))
                        .should().beInterfaces();

        @ArchTest
        static final ArchRule controllers_live_in_interface_adapters =
                classes().that().haveSimpleNameEndingWith("Controller")
                        .should().resideInAPackage("..interfaceadapters.in.rest..");

        @ArchTest
        static final ArchRule entities_have_no_repository_package =
                noClasses().should().resideInAPackage("..entities.repository..");

}
