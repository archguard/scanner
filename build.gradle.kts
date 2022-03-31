plugins {
    base
    java
    id("jacoco-report-aggregation")
}

jacoco {
    toolVersion = "0.8.7"
}

allprojects {
    apply(plugin = "java")
    apply(plugin = "jacoco")

    group = "com.thoughtworks.archguard"
    version = "1.4.1"
    java.sourceCompatibility = JavaVersion.VERSION_1_8

    repositories {
        mavenCentral()
        mavenLocal()
    }

    tasks.test {
        finalizedBy(tasks.jacocoTestReport) // report is always generated after tests run
    }

    tasks.jacocoTestReport {
        dependsOn(tasks.test) // tests are required to run before generating the report
    }

    tasks.jacocoTestReport {
        reports {
            xml.required.set(true)
            csv.required.set(false)
            html.outputLocation.set(layout.buildDirectory.dir("jacocoHtml"))
        }
    }

    tasks.withType<JacocoReport> {
        afterEvaluate {
            classDirectories.setFrom(files(classDirectories.files.map {
                fileTree(it).apply {
                    exclude("dev.evolution")
                }
            }))
        }
    }
}

dependencies {
    jacocoAggregation(project(":source_repository"))
    jacocoAggregation(project(":scan_git"))
    jacocoAggregation(project(":scan_jacoco"))
    jacocoAggregation(project(":scan_sourcecode"))
    jacocoAggregation(project(":scan_mysql"))
    jacocoAggregation(project(":scan_test_badsmell"))
    jacocoAggregation(project(":scan_bytecode"))
}

reporting {
    reports {
        val jacocoRootReport by creating(JacocoCoverageReport::class) {
            testType.set(TestSuiteType.UNIT_TEST)
        }
    }
}

tasks.check {
    dependsOn(tasks.named<JacocoReport>("jacocoRootReport"))
}
