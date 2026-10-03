plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.anthropic.java)
}

application {
    mainClass.set("dev.abhinav.reviewagent.MainKt")
}
