plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

rootProject.name = "hello-world"

include(":libs:common")
include(":clients:common")
project(":clients:common").name = "client-common"
include(":clients:world-client")
include(":services:common")
project(":services:common").name = "service-common"
include(":services:hello-service")
include(":databases:hello-world-db")
