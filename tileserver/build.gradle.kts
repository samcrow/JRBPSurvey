plugins {
    id("com.android.library")
    id("kotlin-android")
}
android {
    compileSdk = 34
    namespace = "org.samcrow.ridgesurvey.tileserver"
    defaultConfig {
        minSdk = 26
    }
}
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlin {
    jvmToolchain(17)
}
dependencies {
    implementation("org.eclipse.jetty:jetty-server:12.1.4")
    implementation("uk.uuid.slf4j:slf4j-android:2.0.17-0")
}
