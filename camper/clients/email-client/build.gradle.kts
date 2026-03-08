plugins {
    kotlin("jvm")
    `java-test-fixtures`
}

dependencies {
    api(project(":clients:client-common"))

    implementation("com.resend:resend-java:3.1.0")
    implementation("org.slf4j:slf4j-api:2.0.16")

    testFixturesApi(project(":clients:client-common"))
}
