dependencies {
    implementation(project(":bank-domain"))
    implementation(project(":bank-core"))
    implementation(project(":bank-monitoring"))

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.retry:spring-retry")
}

tasks.register("prepareKotlinBuildScriptModel"){}