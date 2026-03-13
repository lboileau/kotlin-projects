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
    implementation(project(":clients:assignment-client"))
    implementation(project(":clients:email-client"))
    implementation(project(":clients:invitation-client"))
    implementation(project(":clients:gear-sync-client"))
    implementation(project(":clients:ingredient-client"))
    implementation(project(":clients:recipe-client"))
    implementation(project(":clients:recipe-scraper-client"))
    implementation(project(":clients:meal-plan-client"))
    implementation(project(":clients:log-book-client"))
    implementation(project(":libs:meal-plan-calculator"))
    implementation(project(":services:service-common"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    runtimeOnly("org.postgresql:postgresql:42.7.4")

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
    testImplementation(testFixtures(project(":clients:assignment-client")))
    testImplementation(testFixtures(project(":clients:email-client")))
    testImplementation(testFixtures(project(":clients:invitation-client")))
    testImplementation(testFixtures(project(":clients:gear-sync-client")))
    testImplementation(testFixtures(project(":clients:ingredient-client")))
    testImplementation(testFixtures(project(":clients:recipe-client")))
    testImplementation(testFixtures(project(":clients:recipe-scraper-client")))
    testImplementation(testFixtures(project(":clients:meal-plan-client")))
    testImplementation(testFixtures(project(":clients:log-book-client")))
}

tasks.named<Copy>("processResources") {
    from("${rootProject.projectDir}/databases/camper-db/migrations") {
        into("db/migration")
    }
}

tasks.withType<Test> {
    systemProperty("project.root", rootProject.projectDir.absolutePath)
    systemProperty("spring.profiles.active", "test")
}
