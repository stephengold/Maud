// global build settings for the Maud project

rootProject.name = "Maud"

dependencyResolutionManagement {
    repositories {
        //mavenLocal() // to find libraries installed locally
        mavenCentral() // to find libraries released to the Maven Central repository
        maven {
            name = "Central Portal Snapshots"
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
        }
    }
}

// no subprojects
