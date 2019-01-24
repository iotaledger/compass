MAIN_BASE_PATH = "src/main/java/org/iota/compass/%s"

java_library(
    name = "crypto",
    srcs = [
        "Hasher.java",
        "ISS.java",
        "ISSInPlace.java",
        "KerlPoW.java",
    ],
    visibility = ["//visibility:public"],
    deps = [
        "@com_google_guava_guava//jar",
        "@org_bouncycastle_bcprov_jdk15on//jar",
        "@org_iota_jota//jar",
        "@org_slf4j_slf4j_api//jar",
    ],
)
