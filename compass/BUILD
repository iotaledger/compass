COORDINATOR_RUNTIME_DEPS = [
    "@org_slf4j_slf4j_simple//jar",
    "@com_squareup_okhttp3_okhttp//jar",
    "@com_squareup_retrofit2_converter_gson//jar",
    "@com_squareup_retrofit2_retrofit//jar",
    "@com_squareup_okio_okio//jar",
    "@io_netty_netty_tcnative_boringssl_static//jar",
]

java_binary(
    name = "layers_calculator",
    srcs = ["LayersCalculator.java"],
    main_class = "org.iota.compass.LayersCalculator",
    visibility = ["//visibility:public"],
    runtime_deps = COORDINATOR_RUNTIME_DEPS,
    deps = [
        "//compass/conf",
        "//compass/milestone",
        "//compass/sign:common",
        "//compass/sign:helper",
        "@com_beust_jcommander//jar",
        "@com_google_guava_guava//jar",
        "@org_iota_jota//jar",
        "@org_slf4j_slf4j_api//jar",
    ],
)

java_binary(
    name = "shadowing_coordinator",
    srcs = ["ShadowingCoordinator.java"],
    main_class = "org.iota.compass.ShadowingCoordinator",
    visibility = ["//visibility:public"],
    runtime_deps = COORDINATOR_RUNTIME_DEPS,
    deps = [
        "//compass/conf",
        "//compass/crypto",
        "//compass/milestone",
        "//compass/sign:common",
        "//compass/sign:helper",
        "@com_beust_jcommander//jar",
        "@org_apache_commons_commons_lang3//jar",
        "@org_iota_jota//jar",
        "@org_slf4j_slf4j_api//jar",
    ],
)

java_binary(
    name = "coordinator",
    srcs = ["Coordinator.java"],
    main_class = "org.iota.compass.Coordinator",
    visibility = ["//visibility:public"],
    runtime_deps = COORDINATOR_RUNTIME_DEPS,
    deps = [
        "//compass/conf",
        "//compass/milestone",
        "//compass/sign:common",
        "//compass/sign:helper",
        "@com_beust_jcommander//jar",
        "@org_iota_jota//jar",
        "@org_slf4j_slf4j_api//jar",
    ],
)
