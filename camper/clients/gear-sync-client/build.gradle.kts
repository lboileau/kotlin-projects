plugins {
    kotlin("jvm")
    `java-test-fixtures`
}

dependencies {
    api(project(":clients:client-common"))
    implementation(project(":clients:item-client"))
    implementation(project(":clients:assignment-client"))
    implementation(project(":clients:plan-client"))

    testFixturesApi(project(":clients:client-common"))
}
