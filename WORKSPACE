workspace(name = "org_iota_compass")

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

load("//third-party:maven_deps.bzl", "maven_jars")

maven_jars()

# Protobuf
PROTOBUF_URL = "https://github.com/protocolbuffers/protobuf/archive/48cb18e5c419ddd23d9badcfe4e9df7bde1979b2.zip"

http_archive(
    name = "com_google_protobuf",
    sha256 = "b6b42f90c60b54732f764ae875623a9b05e6eede064173c36c6fea12dd376cdd",
    strip_prefix = "protobuf-48cb18e5c419ddd23d9badcfe4e9df7bde1979b2/",
    urls = [PROTOBUF_URL],
)

http_archive(
    name = "com_google_protobuf_java",
    sha256 = "b6b42f90c60b54732f764ae875623a9b05e6eede064173c36c6fea12dd376cdd",
    strip_prefix = "protobuf-48cb18e5c419ddd23d9badcfe4e9df7bde1979b2/",
    urls = [PROTOBUF_URL],
)

http_archive(
    name = "io_grpc_grpc_java",
    strip_prefix = "grpc-java-6657c310947bc61fcc579e31de2c3c113bc238b3/",
    urls = ["https://github.com/grpc/grpc-java/archive/6657c310947bc61fcc579e31de2c3c113bc238b3.zip"],
)

load("@io_grpc_grpc_java//:repositories.bzl", "grpc_java_repositories")

grpc_java_repositories(
    omit_com_google_code_findbugs_jsr305 = True,
    omit_com_google_code_gson = True,
    omit_com_google_errorprone_error_prone_annotations = True,
    omit_com_google_guava = True,
    omit_com_google_protobuf = True,
    omit_com_squareup_okio = True,
    omit_junit_junit = True,
    omit_org_apache_commons_lang3 = True,
    omit_org_codehaus_mojo_animal_sniffer_annotations = True,
)
