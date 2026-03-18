plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

rootProject.name = "interview-manager"

include(":libs:common")
include(":clients:common")
project(":clients:common").name = "client-common"
include(":clients:world-client")
include(":services:common")
project(":services:common").name = "service-common"
include(":services:interview-service")
include(":databases:interview-manager-db")
