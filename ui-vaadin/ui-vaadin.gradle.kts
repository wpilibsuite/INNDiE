plugins {
    id("org.gretty") version "2.3.1"
    id("com.devsoap.vaadin-flow") version "1.2"
}

dependencies {
    fun jetty(
        group: String = "org.eclipse.jetty",
        name: String,
        version: String = "9.4.20.v20190813"
    ) = create(group = group, name = name, version = version)

    implementation(jetty(name = "jetty-server"))
    implementation(jetty(name = "jetty-webapp"))
    implementation(jetty(group = "org.eclipse.jetty.websocket", name = "websocket-server"))

    testImplementation("com.github.mvysny.kaributesting:karibu-testing-v10:1.1.11")
}

gretty {
    // https://akhikhl.github.io/gretty-doc/Gretty-configuration.html
    host = "localhost"
    httpPort = 8080

    contextPath = "axon"
}

node {
    download = true
}

vaadin {
    version = "14.0.5"
    isProductionMode = false
    isSubmitStatistics = false

    autoconfigure()
}
