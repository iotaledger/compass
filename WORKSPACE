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
PROTOBUF_REV = "66dc42d891a4fc8e9190c524fd67961688a37bbe"

PROTOBUF_URL = "https://github.com/protocolbuffers/protobuf/archive/%s.zip" % PROTOBUF_REV

PROTOBUF_PREFIX = "protobuf-%s" % PROTOBUF_REV

PROTOBUF_SHA = "23dcfef4adf06e1db7f1d391c035896a92eec546fff5ef9244b6e7b9ee24aa66"

http_archive(
    name = "com_google_protobuf",
    sha256 = PROTOBUF_SHA,
    strip_prefix = PROTOBUF_PREFIX,
    urls = [PROTOBUF_URL],
)

http_archive(
    name = "com_google_protobuf_java",
    sha256 = PROTOBUF_SHA,
    strip_prefix = PROTOBUF_PREFIX,
    urls = [PROTOBUF_URL],
)

http_archive(
    name = "io_grpc_grpc_java",
    strip_prefix = "grpc-java-fe7f043504d66e1b3f674c0514ce794c8a56884e",
    urls = ["https://github.com/grpc/grpc-java/archive/fe7f043504d66e1b3f674c0514ce794c8a56884e.zip"],
)

load("@io_grpc_grpc_java//:repositories.bzl", "grpc_java_repositories")

grpc_java_repositories(
    omit_com_google_code_findbugs_jsr305 = True,
    omit_com_google_code_gson = True,
    omit_com_google_errorprone_error_prone_annotations = True,
    omit_com_google_guava = True,
    omit_com_google_j2objc_j2objc_annotations = True,
    omit_com_google_protobuf = True,
    omit_com_squareup_okio = True,
    omit_junit_junit = True,
    omit_org_apache_commons_lang3 = True,
    omit_org_codehaus_mojo_animal_sniffer_annotations = True,
)
