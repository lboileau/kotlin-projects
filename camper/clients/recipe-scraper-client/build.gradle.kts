plugins {
    kotlin("jvm")
    `java-test-fixtures`
}

dependencies {
    api(project(":clients:client-common"))

    implementation("com.anthropic:anthropic-java:2.15.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.2")
    implementation("org.jsoup:jsoup:1.18.3")
    implementation("org.slf4j:slf4j-api:2.0.16")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.16")
}

// Manual scrape harness: ./gradlew :clients:recipe-scraper-client:test --tests "*ScrapeHarnessTest*" \
//   -Pscrape.dir=<dir> -Pscrape.input=raw|jsonld -Pscrape.apiKey=<key> [-Pscrape.model=<model>]
tasks.test {
    listOf("scrape.dir", "scrape.input", "scrape.apiKey", "scrape.model", "scrape.offline").forEach { name ->
        project.findProperty(name)?.let { systemProperty(name, it) }
    }
}
