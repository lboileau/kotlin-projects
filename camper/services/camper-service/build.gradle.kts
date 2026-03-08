plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    implementation(project(":clients:client-common"))
    implementation(project(":clients:world-client"))
    implementation(project(":clients:user-client"))
    implementation(project(":clients:plan-client"))
    implementation(project(":clients:item-client"))
    implementation(project(":clients:itinerary-client"))
    implementation(project(":services:service-common"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-jdbc")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:postgresql:1.20.4")
    testImplementation("org.testcontainers:junit-jupiter:1.20.4")
    testImplementation("org.postgresql:postgresql:42.7.4")
    testImplementation(testFixtures(project(":clients:world-client")))
    testImplementation(testFixtures(project(":clients:user-client")))
    testImplementation(testFixtures(project(":clients:plan-client")))
    testImplementation(testFixtures(project(":clients:item-client")))
    testImplementation(testFixtures(project(":clients:itinerary-client")))
}

tasks.withType<Test> {
    systemProperty("project.root", rootProject.projectDir.absolutePath)
    systemProperty("spring.profiles.active", "test")
}
