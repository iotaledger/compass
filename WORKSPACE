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
    sha1 = "6a806eff209f36f635f943e16d97491f00f6bfab",
)

maven_jar(
    name = "com_beust_jcommander",
    artifact = "com.beust:jcommander:1.72",
    sha1 = "6375e521c1e11d6563d4f25a07ce124ccf8cd171",
)

maven_jar(
    name = "org_slf4j_slf4j_api",
    artifact = "org.slf4j:slf4j-api:1.7.25",
    sha1 = "da76ca59f6a57ee3102f8f9bd9cee742973efa8a",
)

maven_jar(
    name = "org_slf4j_slf4j_simple",
    artifact = "org.slf4j:slf4j-simple:1.7.25",
    sha1 = "8dacf9514f0c707cbbcdd6fd699e8940d42fb54e",
)

maven_jar(
    name = "org_apache_commons_commons_lang3",
    artifact = "org.apache.commons:commons-lang3:3.8.1",
    sha1 = "6505a72a097d9270f7a9e7bf42c4238283247755",
)

maven_jar(
    name = "org_bouncycastle_bcprov_jdk15on",
    artifact = "org.bouncycastle:bcprov-jdk15on:1.60",
    sha1 = "bd47ad3bd14b8e82595c7adaa143501e60842a84",
)

maven_jar(
    name = "com_squareup_retrofit2_retrofit2",
    artifact = "com.squareup.retrofit2:retrofit:2.4.0",
    sha1 = "fc4aa382632bfaa7be7b41579efba41d5a71ecf3",
)

maven_jar(
    name = "com_squareup_retrofit2_converter_gson",
    artifact = "com.squareup.retrofit2:converter-gson:2.4.0",
    sha1 = "15d7790ee311d961379c51b00aba12d5967cb7ea",
)

maven_jar(
    name = "com_squareup_okhttp3_okhttp",
    artifact = "com.squareup.okhttp3:okhttp:3.11.0",
    sha1 = "75966e05a49046ca2ae734e5626f28837a8d1e82",
)

maven_jar(
    name = "commons_io_commons_io",
    artifact = "commons-io:commons-io:2.6",
    sha1 = "815893df5f31da2ece4040fe0a12fd44b577afaf",
)

maven_jar(
    name = "junit_junit",
    artifact = "junit:junit:4.12",
    sha1 = "2973d150c0dc1fefe998f834810d68f278ea58ec",
)
