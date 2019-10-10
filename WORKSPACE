workspace(name = "org_iota_compass")

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

http_archive(
    name = "io_bazel_rules_docker",
    sha256 = "e513c0ac6534810eb7a14bf025a0f159726753f97f74ab7863c650d26e01d677",
    strip_prefix = "rules_docker-0.9.0",
    urls = ["https://github.com/bazelbuild/rules_docker/releases/download/v0.9.0/rules_docker-v0.9.0.tar.gz"],
)

load(
    "@io_bazel_rules_docker//repositories:repositories.bzl",
    container_repositories = "repositories",
)

container_repositories()

load(
    "@io_bazel_rules_docker//repositories:deps.bzl", 
    container_deps = "deps")

container_deps()

load(
    "@io_bazel_rules_docker//container:container.bzl",
    "container_pull",
)

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
PROTOBUF_REV = "09745575a923640154bcf307fba8aedff47f240a"

PROTOBUF_URL = "https://github.com/protocolbuffers/protobuf/archive/%s.zip" % PROTOBUF_REV

PROTOBUF_PREFIX = "protobuf-%s" % PROTOBUF_REV

PROTOBUF_SHA = "76ee4ba47dec6146872b6cd051ae5bd12897ef0b1523d5aeb56d81a5a4ca885a"

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
    name = "com_google_protobuf_deps",
    sha256 = PROTOBUF_SHA,
    strip_prefix = PROTOBUF_PREFIX,
    urls = [PROTOBUF_URL],
)

load("@com_google_protobuf_deps//:protobuf_deps.bzl", "protobuf_deps")

 # Load common dependencies.
protobuf_deps()

http_archive(
    name = "io_grpc_grpc_java",
    sha256 = "3990d711e801cc61efa4eab6987e4847c264b6fb2bead99b270cbfe4039b7871",
    strip_prefix = "grpc-java-b3ed852c406e4f66f126b7b73a5b3330ac30e6ac",
    urls = ["https://github.com/grpc/grpc-java/archive/b3ed852c406e4f66f126b7b73a5b3330ac30e6ac.zip"],
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
