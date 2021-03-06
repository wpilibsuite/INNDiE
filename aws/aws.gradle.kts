description = "Interfaces with AWS using their Java SDK."

fun DependencyHandler.arrow(name: String) =
    create(group = "io.arrow-kt", name = name, version = property("arrow.version") as String)

fun DependencyHandler.koin(name: String) =
    create(group = "org.koin", name = name, version = property("koin.version") as String)

dependencies {
    api(arrow("arrow-core"))
    api(arrow("arrow-core-data"))
    api(arrow("arrow-optics"))
    api(arrow("arrow-fx"))
    api(arrow("arrow-syntax"))
    api(arrow("arrow-free"))
    api(arrow("arrow-free-data"))
    api(arrow("arrow-recursion"))
    api(arrow("arrow-recursion-data"))

    api(koin("koin-core"))

    api(project(":tf-data"))
    api(project(":db"))
    api(project(":training"))
    api(project(":plugin"))

    api(
        group = "software.amazon.awssdk",
        name = "aws-sdk-java",
        version = "2.10.12"
    )

    implementation(
        group = "com.beust",
        name = "klaxon",
        version = property("klaxon.version") as String
    )

    implementation(
        group = "mysql",
        name = "mysql-connector-java",
        version = property("mysql-connector-java.version") as String
    )

    implementation(project(":logging"))
    implementation(project(":util"))

    testImplementation(project(":test-util"))
    testImplementation(project(":db-test-util"))
}
