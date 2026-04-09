plugins {
    kotlin("jvm")
    `java-test-fixtures`
}

dependencies {
    api(project(":clients:client-common"))

    testFixturesApi(project(":clients:client-common"))
}
