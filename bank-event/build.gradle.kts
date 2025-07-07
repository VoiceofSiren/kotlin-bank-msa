dependencies {
    implementation(project(":bank-domain"))
    implementation(project(":bank-core"))

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.retry:spring-retry")
}

tasks.register("prepareKotlinBuildScriptModel"){}