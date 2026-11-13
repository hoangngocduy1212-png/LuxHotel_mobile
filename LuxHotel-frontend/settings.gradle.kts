pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Kho lưu trữ bắt buộc để tải MPAndroidChart, đặt gọn gàng ở đây
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "LuxHotel"
include(":app")