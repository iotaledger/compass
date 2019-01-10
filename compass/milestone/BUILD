MAIN_BASE_PATH = "src/main/java/org/iota/compass/%s"

java_library(
    name = "milestone",
    srcs = [
        "MilestoneDatabase.java",
        "MilestoneSource.java",
    ],
    visibility = ["//visibility:public"],
    deps = [
        "//compass/crypto",
        "//compass/sign:common",
        "@com_google_guava_guava//jar",
        "@org_bouncycastle_bcprov_jdk15on//jar",
        "@org_iota_jota//jar",
        "@org_slf4j_slf4j_api//jar",
    ],
)

java_test(
    name = "test_milestone",
    srcs = ["MilestoneTest.java"],
    flaky = True,
    test_class = "org.iota.compass.MilestoneTest",
    deps = [
        ":milestone",
        "//compass:layers_calculator",
        "//compass/conf",
        "//compass/crypto",
        "//compass/sign:common",
        "//compass/sign:inmemory",
        "//compass/sign:remote",
        "//compass/sign:server",
        "//compass/test",
        "@com_google_guava_guava//jar",
        "@junit_junit//jar",
        "@org_iota_jota//jar",
    ],
)
