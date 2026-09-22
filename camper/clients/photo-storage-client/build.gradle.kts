plugins {
    kotlin("jvm")
    `java-test-fixtures`
}

dependencies {
    api(project(":clients:client-common"))

    implementation(platform("software.amazon.awssdk:bom:2.29.52"))
    implementation("software.amazon.awssdk:s3")
    implementation("org.slf4j:slf4j-api:2.0.16")

    testFixturesApi(project(":clients:client-common"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("org.testcontainers:minio:1.20.4")
    testImplementation("org.testcontainers:junit-jupiter:1.20.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("ch.qos.logback:logback-classic:1.5.16")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
