load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

http_archive(
    name = "io_bazel_rules_docker",
    sha256 = "29d109605e0d6f9c892584f07275b8c9260803bf0c6fcb7de2623b2bedc910bd",
    strip_prefix = "rules_docker-0.5.1",
    url = "https://github.com/bazelbuild/rules_docker/archive/v0.5.1.tar.gz",
)

load(
    "@io_bazel_rules_docker//container:container.bzl",
    "container_pull",
    container_repositories = "repositories",
)

container_repositories()

container_pull(
    name = "java_base",
    digest =
        "sha256:bb1c9179c2263733f235291998cb849d52fb730743125420cf4f97a362d6a6dd",
    registry = "gcr.io",
    repository = "distroless/java",
)

# Java dependencies

maven_jar(
    name = "com_google_guava_guava",
    artifact = "com.google.guava:guava:26.0-jre",
)

maven_jar(
    name = "com_beust_jcommander",
    artifact = "com.beust:jcommander:1.72",
)

maven_jar(
    name = "org_slf4j_slf4j_api",
    artifact = "org.slf4j:slf4j-api:1.7.25",
)

maven_jar(
    name = "org_slf4j_slf4j_simple",
    artifact = "org.slf4j:slf4j-simple:1.7.25",
)

maven_jar(
    name = "org_apache_commons_commons_lang3",
    artifact = "org.apache.commons:commons-lang3:3.8.1",
)

maven_jar(
    name = "org_bouncycastle_bcprov_jdk15on",
    artifact = "org.bouncycastle:bcprov-jdk15on:1.60",
)

maven_jar(
    name = "com_squareup_retrofit2_retrofit2",
    artifact = "com.squareup.retrofit2:retrofit:2.4.0",
)

maven_jar(
    name = "com_squareup_retrofit2_converter_gson",
    artifact = "com.squareup.retrofit2:converter_gson:2.4.0",
)

maven_jar(
    name = "com_squareup_okhttp3_okhttp",
    artifact = "com.squareup.okhttp3:okhttp:3.11.0",
)

maven_jar(
    name = "junit_junit",
    artifact = "junit:junit:4.12",
)
