plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

rootProject.name = "camper"

include(":libs:common")
include(":clients:common")
project(":clients:common").name = "client-common"
include(":clients:world-client")
include(":clients:user-client")
include(":clients:plan-client")
include(":clients:item-client")
include(":clients:itinerary-client")
include(":clients:assignment-client")
include(":services:common")
project(":services:common").name = "service-common"
include(":services:camper-service")
include(":databases:camper-db")
