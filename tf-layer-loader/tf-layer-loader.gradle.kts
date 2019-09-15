description = "Loads layers from TF Graphs."

dependencies {
    api(project(":tf-data"))

    implementation(group = "io.jhdf", name = "jhdf", version = property("jhdf.version") as String)
    implementation(group = "com.beust", name = "klaxon", version = property("klaxon.version") as String)
    implementation(project(":util"))
    implementation(project(":logging"))
}
