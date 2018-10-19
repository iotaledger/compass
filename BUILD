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
            "SignatureSource.java",
            "KerlPoW.java",
        ]
    ],
    deps = [
        ":jota",
        "@com_google_guava_guava//jar",
        "@org_bouncycastle_bcprov_jdk15on//jar",
        "@org_slf4j_slf4j_api//jar",
    ],
)

java_library(
    name = "inmemory_signature_source",
    srcs = [MAIN_BASE_PATH % "InMemorySignatureSource.java"],
    deps = [
        ":common",
        ":jota",
    ],
)

java_library(
    name = "conf",
    srcs = [
        (MAIN_BASE_PATH % x)
        for x in [
            "conf/BaseConfiguration.java",
            "conf/Configuration.java",
            "conf/ShadowingConfiguration.java",
            "conf/POWModeValidator.java",
        ]
    ],
    deps = [
        ":common",
        ":jota",
        "@com_beust_jcommander//jar",
    ],
)

java_binary(
    name = "address_generator",
    srcs = [MAIN_BASE_PATH % "util/AddressGenerator.java"],
    main_class = "coo.util.AddressGenerator",
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
    main_class = "coo.util.MerkleTreeCalculator",
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
    srcs = [MAIN_BASE_PATH % "shadow/ShadowingCoordinator.java"],
    main_class = "coo.shadow.ShadowingCoordinator",
    visibility = ["//visibility:public"],
    runtime_deps = COORDINATOR_RUNTIME_DEPS,
    deps = [
        ":common",
        ":conf",
        ":inmemory_signature_source",
        ":jota",
        "@com_beust_jcommander//jar",
        "@org_apache_commons_commons_lang3//jar",
        "@org_slf4j_slf4j_api//jar",
    ],
)

java_binary(
    name = "coordinator",
    srcs = [MAIN_BASE_PATH % "Coordinator.java"],
    main_class = "coo.Coordinator",
    visibility = ["//visibility:public"],
    runtime_deps = COORDINATOR_RUNTIME_DEPS,
    deps = [
        ":common",
        ":conf",
        ":inmemory_signature_source",
        ":jota",
        "@com_beust_jcommander//jar",
        "@org_slf4j_slf4j_api//jar",
    ],
)

java_test(
    name = "test_milestone",
    srcs = glob(["src/test/java/**/*.java"]),
    test_class = "MilestoneTest",
    deps = [
        ":address_generator",
        ":common",
        ":inmemory_signature_source",
        ":jota",
        ":merkle_tree_calculator",
        "@com_google_guava_guava//jar",
        "@junit_junit//jar",
    ],
)
