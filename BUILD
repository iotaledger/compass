MAIN_BASE_PATH = "src/main/java/org/iota/compass/%s"

java_import(
    name = "jota",
    jars = ["libs/jota-0.9.11-SNAPSHOT.jar"],
    runtime_deps = [
        "@com_google_code_gson_gson//jar",
        "@com_squareup_okhttp3_okhttp//jar",
        "@com_squareup_okio_okio//jar",
        "@com_squareup_retrofit2_converter_gson//jar",
        "@com_squareup_retrofit2_retrofit//jar",
        "@org_bouncycastle_bcprov_jdk15on//jar",
    ],
)

java_library(
    name = "common",
    srcs = [
        (MAIN_BASE_PATH % (x))
        for x in [
            "crypto/Hasher.java",
            "crypto/ISS.java",
            "crypto/ISSInPlace.java",
            "MilestoneSource.java",
            "MilestoneDatabase.java",
            "KerlPoW.java",
        ]
    ],
    deps = [
        ":jota",
        ":signature_source_common",
        "@com_google_guava_guava//jar",
        "@org_bouncycastle_bcprov_jdk15on//jar",
        "@org_slf4j_slf4j_api//jar",
    ],
)

java_library(
    name = "signature_source_inmemory",
    srcs = [MAIN_BASE_PATH % "InMemorySignatureSource.java"],
    deps = [
        ":common",
        ":jota",
        ":signature_source_common",
        "@org_slf4j_slf4j_api//jar",
    ],
)

java_library(
    name = "signature_source_remote",
    srcs = [MAIN_BASE_PATH % "RemoteSignatureSource.java"],
    deps = [
        ":common",
        ":jota",
        ":signature_source_common",
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
        "@org_slf4j_slf4j_api//jar",
    ],
)

java_library(
    name = "signature_source_common",
    srcs = [(MAIN_BASE_PATH % x) for x in [
        "SignatureSource.java",
        "SignatureSourceType.java",
    ]],
    deps = [
        ":jota",
    ],
)

java_library(
    name = "signature_source_helper",
    srcs = [(MAIN_BASE_PATH % x) for x in [
        "SignatureSourceHelper.java",
    ]],
    deps = [
        ":conf",
        ":signature_source_common",
        ":signature_source_inmemory",
        ":signature_source_remote",
        "@com_beust_jcommander//jar",
    ],
)

java_library(
    name = "conf",
    srcs = [
        (MAIN_BASE_PATH % x)
        for x in [
            "conf/InMemorySignatureSourceConfiguration.java",
            "conf/RemoteSignatureSourceConfiguration.java",
            "conf/SignatureSourceServerConfiguration.java",
            "conf/BaseConfiguration.java",
            "conf/CoordinatorConfiguration.java",
            "conf/SpongeModeConverter.java",
            "conf/SignatureSourceTypeConverter.java",
            "conf/AddressGeneratorConfiguration.java",
            "conf/ShadowingCoordinatorConfiguration.java",
            "conf/POWModeValidator.java",
        ]
    ],
    deps = [
        ":common",
        ":jota",
        ":signature_source_common",
        "@com_beust_jcommander//jar",
    ],
)

java_binary(
    name = "signature_source_server",
    srcs = [MAIN_BASE_PATH % "SignatureSourceServer.java"],
    main_class = "org.iota.compass.SignatureSourceServer",
    visibility = ["//visibility:public"],
    runtime_deps = ["@org_slf4j_slf4j_simple//jar"],
    deps = [
        ":common",
        ":conf",
        ":jota",
        ":signature_source_common",
        ":signature_source_inmemory",
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
        "@org_slf4j_slf4j_api//jar",
    ],
)

java_binary(
    name = "address_generator",
    srcs = [MAIN_BASE_PATH % "util/AddressGenerator.java"],
    main_class = "org.iota.compass.util.AddressGenerator",
    visibility = ["//visibility:public"],
    runtime_deps = ["@org_slf4j_slf4j_simple//jar"],
    deps = [
        ":common",
        ":jota",
        "@org_slf4j_slf4j_api//jar",
    ],
)

java_binary(
    name = "merkle_tree_calculator",
    srcs = [MAIN_BASE_PATH % "util/MerkleTreeCalculator.java"],
    main_class = "org.iota.compass.util.MerkleTreeCalculator",
    visibility = ["//visibility:public"],
    runtime_deps = ["@org_slf4j_slf4j_simple//jar"],
    deps = [
        ":common",
        ":jota",
        "@com_google_guava_guava//jar",
        "@org_slf4j_slf4j_api//jar",
    ],
)

COORDINATOR_RUNTIME_DEPS = [
    "@org_slf4j_slf4j_simple//jar",
]

java_binary(
    name = "shadowing_coordinator",
    srcs = [MAIN_BASE_PATH % "ShadowingCoordinator.java"],
    main_class = "org.iota.compass.ShadowingCoordinator",
    visibility = ["//visibility:public"],
    runtime_deps = COORDINATOR_RUNTIME_DEPS,
    deps = [
        ":common",
        ":conf",
        ":jota",
        ":signature_source_common",
        ":signature_source_helper",
        "@com_beust_jcommander//jar",
        "@org_apache_commons_commons_lang3//jar",
        "@org_slf4j_slf4j_api//jar",
    ],
)

java_binary(
    name = "coordinator",
    srcs = [MAIN_BASE_PATH % "Coordinator.java"],
    main_class = "org.iota.compass.Coordinator",
    visibility = ["//visibility:public"],
    runtime_deps = COORDINATOR_RUNTIME_DEPS,
    deps = [
        ":common",
        ":conf",
        ":jota",
        ":signature_source_common",
        ":signature_source_helper",
        "@com_beust_jcommander//jar",
        "@org_slf4j_slf4j_api//jar",
    ],
)

java_test(
    name = "test_milestone",
    srcs = glob(["src/test/java/**/*.java"]),
    flaky = True,
    test_class = "MilestoneTest",
    deps = [
        ":address_generator",
        ":common",
        ":conf",
        ":jota",
        ":merkle_tree_calculator",
        ":signature_source_common",
        ":signature_source_inmemory",
        ":signature_source_remote",
        ":signature_source_server",
        "@com_google_guava_guava//jar",
        "@junit_junit//jar",
    ],
)
