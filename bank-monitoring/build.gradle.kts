dependencies {
    implementation("org.springframework:spring-context")

    // metric 수집, Prometheus와 연동 가능한 표준화된 API 제공
    implementation("io.micrometer:micrometer-core")
}

tasks.register("prepareKotlinBuildScriptModel"){}