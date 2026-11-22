plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation(project(":core"))
}

application { mainClass.set("com.splitcalc.cli.MainKt") }

tasks.named<JavaExec>("run") { standardInput = System.`in` }
