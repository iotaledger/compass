MAIN_BASE_PATH = "src/main/java/org/iota/compass/%s"

java_library(
    name = "inmemory",
    srcs = ["InMemorySignatureSource.java"],
    visibility = ["//visibility:public"],
    deps = [
        ":common",
        "//compass/crypto",
        "@org_iota_jota//jar",
        "@org_slf4j_slf4j_api//jar",
    ],
)

java_library(
    name = "remote",
    srcs = ["RemoteSignatureSource.java"],
    visibility = ["//visibility:public"],
    deps = [
        ":common",
        "//proto:signature_source_java_grpc",
        "//proto:signature_source_java_proto",
        "@com_google_api_grpc_proto_google_common_protos//jar",
        "@com_google_code_findbugs_jsr305//jar",
        "@com_google_guava_guava//jar",
        "@com_google_protobuf//:protobuf_java",
        "@com_google_protobuf//:protobuf_java_util",
        "@io_grpc_grpc_java//alts",
        "@io_grpc_grpc_java//core",
        "@io_grpc_grpc_java//netty",
        "@io_grpc_grpc_java//protobuf",
        "@io_grpc_grpc_java//stub",
        "@io_netty_netty_handler//jar",
        "@org_iota_jota//jar",
        "@org_slf4j_slf4j_api//jar",
    ],
)

java_library(
    name = "common",
    srcs = [
        "SignatureSource.java",
        "SignatureSourceType.java",
    ],
    visibility = ["//visibility:public"],
    deps = [
        "@org_iota_jota//jar",
    ],
)

java_library(
    name = "helper",
    srcs = [
        "SignatureSourceHelper.java",
    ],
    visibility = ["//visibility:public"],
    deps = [
        ":common",
        ":inmemory",
        ":remote",
        "//compass/conf",
        "@com_beust_jcommander//jar",
    ],
)

java_binary(
    name = "server",
    srcs = ["SignatureSourceServer.java"],
    main_class = "org.iota.compass.SignatureSourceServer",
    visibility = ["//visibility:public"],
    runtime_deps = ["@org_slf4j_slf4j_simple//jar"],
    deps = [
        ":common",
        ":inmemory",
        "//compass/conf",
        "//compass/crypto",
        "//proto:signature_source_java_grpc",
        "//proto:signature_source_java_proto",
        "@com_beust_jcommander//jar",
        "@com_google_api_grpc_proto_google_common_protos//jar",
        "@com_google_code_findbugs_jsr305//jar",
        "@com_google_guava_guava//jar",
        "@com_google_protobuf//:protobuf_java",
        "@com_google_protobuf//:protobuf_java_util",
        "@io_grpc_grpc_java//alts",
        "@io_grpc_grpc_java//core",
        "@io_grpc_grpc_java//netty",
        "@io_grpc_grpc_java//protobuf",
        "@io_grpc_grpc_java//stub",
        "@io_netty_netty_handler//jar",
        "@org_iota_jota//jar",
        "@org_slf4j_slf4j_api//jar",
    ],
)
