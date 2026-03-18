plugins {
    kotlin("jvm")
    `java-test-fixtures`
}

dependencies {
    api(project(":clients:client-common"))  // directory: clients/common/

    implementation("org.jdbi:jdbi3-core:3.47.0")
    implementation("org.slf4j:slf4j-api:2.0.16")
    runtimeOnly("org.postgresql:postgresql:42.7.4")

    testFixturesApi(project(":clients:client-common"))  // directory: clients/common/
    testFixturesImplementation(project(":databases:interview-manager-db"))

    testRuntimeOnly(project(":databases:interview-manager-db"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("org.testcontainers:postgresql:1.20.4")
    testImplementation("org.testcontainers:junit-jupiter:1.20.4")
    testImplementation("org.postgresql:postgresql:42.7.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("ch.qos.logback:logback-classic:1.5.16")
}

tasks.withType<Test> {
    systemProperty("project.root", rootProject.projectDir.absolutePath)
}
