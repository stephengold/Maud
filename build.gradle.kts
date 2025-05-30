// Gradle script to build and run the Maud project

import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

plugins {
    application // to build JVM applications
    checkstyle  // to analyze Java sourcecode for style violations
}

val isMacOS = DefaultNativePlatform.getCurrentOperatingSystem().isMacOsX
val javaVersion = JavaVersion.current()
val enableNativeAccess = javaVersion.isCompatibleWith(JavaVersion.VERSION_17)

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

application {
    mainClass = "maud.Maud"
}
tasks.register<JavaExec>("runForceDialog") {
    args("--forceDialog")
    description = "Runs the editor after displaying the Settings dialog."
    mainClass = application.mainClass
}
tasks.named<Jar>("jar") {
    manifest {
        attributes["Main-Class"] = application.mainClass
    }
}

checkstyle {
    toolVersion = libs.versions.checkstyle.get()
}

tasks.withType<JavaCompile>().all { // Java compile-time options:
    options.compilerArgs.add("-Xdiags:verbose")
    if (javaVersion.isCompatibleWith(JavaVersion.VERSION_20)) {
        // Suppress warnings that source value 8 is obsolete.
        options.compilerArgs.add("-Xlint:-options")
    }
    options.compilerArgs.add("-Xlint:unchecked")
    options.encoding = "UTF-8"
    //options.isDeprecation = true // to provide detailed deprecation warnings
    if (javaVersion.isCompatibleWith(JavaVersion.VERSION_1_10)) {
        options.release = 8
    }
}

tasks.withType<JavaExec>().all { // Java runtime options:
    if (isMacOS) {
        jvmArgs("-XstartOnFirstThread")
    } else {
        //args("--forceDialog")
    }
    //args("--openGL3")
    //args("--openGL33")
    //args("--skipStartup")
    //args("--verbose") // to enable additional log output
    classpath = sourceSets.main.get().runtimeClasspath
    enableAssertions = true
    if (enableNativeAccess) {
        jvmArgs("--enable-native-access=ALL-UNNAMED") // suppress System::load() warning
    }
    //jvmArgs("-verbose:gc")
    jvmArgs("-Xms4g", "-Xmx4g") // to enlarge the Java heap
    //jvmArgs("-XX:+UseG1GC", "-XX:MaxGCPauseMillis=10")
}

configurations.all {
    resolutionStrategy.cacheChangingModulesFor(0, "seconds") // to disable caching of snapshots
}

dependencies {
    // from mavenCentral (or mavenLocal) repositories:
    implementation(libs.acorus)
    implementation(libs.heart)
    implementation(libs.jme.ttf)
    if (!isMacOS) {
        runtimeOnly(libs.jme3.awt.dialogs)
    }
    implementation(libs.jme3.blender)
    implementation(libs.jme3.lwjgl)
    implementation(libs.jme3.plugins)
    implementation(libs.jme3.utilities.nifty)
    implementation(libs.logback.classic)
    implementation(libs.minie)
    implementation(libs.skyControl)
    implementation(libs.wes)
    runtimeOnly(libs.jme3.awt.dialogs)
    runtimeOnly(libs.jme3.desktop)
    //runtimeOnly(libs.jme3.testdata.old)
    runtimeOnly(libs.nashorn.core)
    runtimeOnly(libs.nifty.style.black)

    // from jitpack repositories:
    implementation(libs.jme3.xbuf.loader)
}

// Register cleanup tasks:

tasks.named("clean") {
    dependsOn("cleanDLLs", "cleanDyLibs", "cleanLogs", "cleanSOs")
    /*
     * The Acorus sandbox isn't cleaned by default
     * because it might contain a custom startup script.
     */
}

tasks.register<Delete>("cleanDLLs") { // extracted Windows native libraries
    delete(fileTree(".").matching{ include("*.dll") })
}
tasks.register<Delete>("cleanDyLibs") { // extracted macOS native libraries
    delete(fileTree(".").matching{ include("*.dylib") })
}
tasks.register<Delete>("cleanLogs") { // JVM crash logs
    delete(fileTree(".").matching{ include("hs_err_pid*.log") })
}
tasks.register<Delete>("cleanSandbox") { // Acorus sandbox
    delete("Written Assets")
}
tasks.register<Delete>("cleanSOs") { // extracted Linux and Android native libraries
    delete(fileTree(".").matching{ include("*.so") })
}
