plugins {
	java
	id("jacoco")
	id("checkstyle")
	id("com.github.spotbugs") version "6.0.23"
	id("org.springframework.boot") version "3.3.4"
	id("io.spring.dependency-management") version "1.1.6"
}

group = "neu.cs6510.shared"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

dependencies {
//	implementation("org.springframework.boot:spring-boot-starter-data-redis")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.postgresql:postgresql")
	implementation("com.github.docker-java:docker-java:3.2.13")
	implementation("org.glassfish.jersey.core:jersey-common:2.34")
	implementation("javax.ws.rs:javax.ws.rs-api:2.1.1")
	implementation("javax.xml.bind:jaxb-api:2.3.1")
	compileOnly("org.projectlombok:lombok")
	runtimeOnly("org.postgresql:postgresql")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testImplementation("org.mockito:mockito-core:3.11.2")
	testImplementation("org.mock-server:mockserver-netty:5.13.2")
	testImplementation("org.mock-server:mockserver-client-java:5.13.2")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
	enabled = false
}

configurations.all {
	resolutionStrategy {
		force("javax.ws.rs:javax.ws.rs-api:2.1")
	}
}

// Test Configuration
tasks.withType<Test> {
	useJUnitPlatform()
	finalizedBy(tasks.jacocoTestReport) // Update JaCoCo report upon test run
}

// SpotBugs Configuration
spotbugs {
	ignoreFailures.set(true)  // Do not fail the build on SpotBugs errors
	effort.set(com.github.spotbugs.snom.Effort.MIN)
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask> {
	reports {
		create("html") { enabled = true }
	}
}

// Jacoco Configuration
jacoco {
	toolVersion = "0.8.12"
}

tasks.jacocoTestReport {
	dependsOn(tasks.test) // Tests should run before generating the report

	reports {
		xml.required.set(true)  // Generates the XML report (useful for CI tools)
		html.required.set(true) // Generates the HTML report (viewable in a browser)
	}

	classDirectories.setFrom(
		files(classDirectories.files.map {
			fileTree(it) {
				exclude("neu/cs6510/shared/constants/**") // Exclude constants directory
				exclude("neu/cs6510/shared/utils/**")
			}
		})
	)
}

tasks.jacocoTestCoverageVerification {
	dependsOn(tasks.jacocoTestReport) // Ensure that report is generated before verification

	violationRules {
		rule {
			isEnabled = true
			element = "CLASS"

			limit {
				counter = "LINE"
				minimum = "0.8".toBigDecimal() // Minimum threshold for coverage is 80%
			}
		}
	}

	classDirectories.setFrom(
		files(classDirectories.files.map {
			fileTree(it) {
				exclude("neu/cs6510/shared/constants/**") // Exclude constants directory
				exclude("neu/cs6510/shared/utils/**")
			}
		})
	)
}

tasks.check {
	dependsOn(tasks.jacocoTestCoverageVerification)
}

tasks.withType<JavaCompile> {
	options.encoding = "UTF-8"
}

// Javadoc Configuration
tasks.javadoc {
	options {
		encoding = "UTF-8"
		(this as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
		isFailOnError = false // Set to true if Javadoc warnings should fail the build
	}
}

// CheckStyle Configuration
checkstyle {
	toolVersion = "10.18.0"
	configFile = file("${rootDir}/config/checkstyle/checkstyle.xml")
}

tasks.checkstyleMain {
	source = sourceSets["main"].allJava // Set the source files for CheckStyle
}

tasks.checkstyleTest {
	source = sourceSets["test"].allJava // Optionally enforce CheckStyle on test files
}

tasks.withType<Checkstyle> {
	reports {
		xml.required.set(true)  // Generate XML reports (useful for CI)
		html.required.set(true) // Generate HTML reports for viewing
	}
}

// Custom task to run all tasks in sequence
tasks.register("shared-doAll") {
	description = "Runs build, unit tests, JaCoCo, Checkstyle, SpotBugs, and Javadoc for the shared subproject only."
	dependsOn(
		tasks.getByName("build"),
		tasks.getByName("test"),
		tasks.getByName("jacocoTestReport"),
		tasks.getByName("checkstyleMain"),
		tasks.getByName("spotbugsMain"),
		tasks.getByName("javadoc")
	)
	doLast {
		println("All tasks completed for the shared subproject!")
	}
}