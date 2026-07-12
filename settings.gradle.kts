rootProject.name = "Kammich"

pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        maven {
            url = uri("https://reposilite.iktdev.no/releases")
            // Hvis du trenger autentisering her også, må du legge til:
            /*
            credentials {
                username = System.getenv("reposiliteUsername")
                password = System.getenv("reposilitePassword")
            }
            */
        }
    }
    /*resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "no.iktdev.ts-gen") {
                useModule("no.iktdev:ts-gen:1.0-SNAPSHOT")
            }
        }
    }*/
}