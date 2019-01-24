def maven_jars():
    # com.google.guava:guava:bundle:26.0-jre
    native.maven_jar(
        name = "com_google_code_findbugs_jsr305",
        artifact = "com.google.code.findbugs:jsr305:3.0.2",
        repository = "https://jcenter.bintray.com/",
        sha1 = "25ea2e8b0c338a877313bd4672d3fe056ea78f0d",
    )

    # com.google.guava:guava:bundle:26.0-jre
    native.maven_jar(
        name = "org_codehaus_mojo_animal_sniffer_annotations",
        artifact = "org.codehaus.mojo:animal-sniffer-annotations:1.14",
        repository = "https://jcenter.bintray.com/",
        sha1 = "775b7e22fb10026eed3f86e8dc556dfafe35f2d5",
    )

    # org.slf4j:slf4j-simple:jar:1.7.25
    native.maven_jar(
        name = "org_slf4j_slf4j_api",
        artifact = "org.slf4j:slf4j-api:1.7.25",
        repository = "https://jcenter.bintray.com/",
        sha1 = "da76ca59f6a57ee3102f8f9bd9cee742973efa8a",
    )

    # junit:junit:jar:4.12
    native.maven_jar(
        name = "org_hamcrest_hamcrest_core",
        artifact = "org.hamcrest:hamcrest-core:1.3",
        repository = "https://jcenter.bintray.com/",
        sha1 = "42a25dc3219429f0e5d060061f71acb49bf010a0",
    )

    native.maven_jar(
        name = "com_squareup_retrofit2_converter_gson",
        artifact = "com.squareup.retrofit2:converter-gson:2.4.0",
        repository = "https://jcenter.bintray.com/",
        sha1 = "15d7790ee311d961379c51b00aba12d5967cb7ea",
    )

    # com.squareup.retrofit2:converter-gson:jar:2.4.0
    native.maven_jar(
        name = "com_google_code_gson_gson",
        artifact = "com.google.code.gson:gson:2.8.2",
        repository = "https://jcenter.bintray.com/",
        sha1 = "3edcfe49d2c6053a70a2a47e4e1c2f94998a49cf",
    )

    native.maven_jar(
        name = "com_squareup_retrofit2_retrofit",
        artifact = "com.squareup.retrofit2:retrofit:2.4.0",
        repository = "https://jcenter.bintray.com/",
        sha1 = "fc4aa382632bfaa7be7b41579efba41d5a71ecf3",
    )

    # com.google.guava:guava:bundle:26.0-jre
    native.maven_jar(
        name = "org_checkerframework_checker_qual",
        artifact = "org.checkerframework:checker-qual:2.5.2",
        repository = "https://jcenter.bintray.com/",
        sha1 = "cea74543d5904a30861a61b4643a5f2bb372efc4",
    )

    # com.squareup.retrofit2:retrofit:jar:2.4.0
    native.maven_jar(
        name = "com_squareup_okhttp3_okhttp",
        artifact = "com.squareup.okhttp3:okhttp:3.10.0",
        repository = "https://jcenter.bintray.com/",
        sha1 = "7ef0f1d95bf4c0b3ba30bbae25e0e562b05cf75e",
    )

    # com.google.guava:guava:bundle:26.0-jre
    native.maven_jar(
        name = "com_google_errorprone_error_prone_annotations",
        artifact = "com.google.errorprone:error_prone_annotations:2.1.3",
        repository = "https://jcenter.bintray.com/",
        sha1 = "39b109f2cd352b2d71b52a3b5a1a9850e1dc304b",
    )

    native.maven_jar(
        name = "org_apache_commons_commons_lang3",
        artifact = "org.apache.commons:commons-lang3:3.8.1",
        repository = "https://jcenter.bintray.com/",
        sha1 = "6505a72a097d9270f7a9e7bf42c4238283247755",
    )

    # com.squareup.okhttp3:okhttp:jar:3.10.0
    native.maven_jar(
        name = "com_squareup_okio_okio",
        artifact = "com.squareup.okio:okio:1.14.0",
        repository = "https://jcenter.bintray.com/",
        sha1 = "102d7be47241d781ef95f1581d414b0943053130",
    )

    native.maven_jar(
        name = "com_google_guava_guava",
        artifact = "com.google.guava:guava:26.0-jre",
        repository = "https://jcenter.bintray.com/",
        sha1 = "6a806eff209f36f635f943e16d97491f00f6bfab",
    )

    native.maven_jar(
        name = "org_bouncycastle_bcprov_jdk15on",
        artifact = "org.bouncycastle:bcprov-jdk15on:1.60",
        repository = "https://jcenter.bintray.com/",
        sha1 = "bd47ad3bd14b8e82595c7adaa143501e60842a84",
    )

    native.maven_jar(
        name = "com_beust_jcommander",
        artifact = "com.beust:jcommander:1.72",
        repository = "https://jcenter.bintray.com/",
        sha1 = "6375e521c1e11d6563d4f25a07ce124ccf8cd171",
    )

    native.maven_jar(
        name = "org_slf4j_slf4j_simple",
        artifact = "org.slf4j:slf4j-simple:1.7.25",
        repository = "https://jcenter.bintray.com/",
        sha1 = "8dacf9514f0c707cbbcdd6fd699e8940d42fb54e",
    )

    native.maven_jar(
        name = "junit_junit",
        artifact = "junit:junit:4.12",
        repository = "https://jcenter.bintray.com/",
        sha1 = "2973d150c0dc1fefe998f834810d68f278ea58ec",
    )

    # com.google.guava:guava:bundle:26.0-jre
    native.maven_jar(
        name = "com_google_j2objc_j2objc_annotations",
        artifact = "com.google.j2objc:j2objc-annotations:1.1",
        repository = "https://jcenter.bintray.com/",
        sha1 = "976d8d30bebc251db406f2bdb3eb01962b5685b3",
    )

    # org.iota:jota:1.0.0-beta3
    native.maven_jar(
        name = "org_iota_jota",
        artifact = "org.iota:jota:1.0.0-beta3",
        repository = "https://jcenter.bintray.com/",
        sha1 = "ffda9cd4c25bd92ea23503887bfe49115cb5f6b7",
    )

def maven_libraries():
    native.java_library(
        name = "com_google_code_findbugs_jsr305",
        visibility = ["//visibility:public"],
        exports = ["@com_google_code_findbugs_jsr305//jar"],
    )

    native.java_library(
        name = "org_codehaus_mojo_animal_sniffer_annotations",
        visibility = ["//visibility:public"],
        exports = ["@org_codehaus_mojo_animal_sniffer_annotations//jar"],
    )

    native.java_library(
        name = "org_slf4j_slf4j_api",
        visibility = ["//visibility:public"],
        exports = ["@org_slf4j_slf4j_api//jar"],
    )

    native.java_library(
        name = "org_hamcrest_hamcrest_core",
        visibility = ["//visibility:public"],
        exports = ["@org_hamcrest_hamcrest_core//jar"],
    )

    native.java_library(
        name = "com_squareup_retrofit2_converter_gson",
        visibility = ["//visibility:public"],
        exports = ["@com_squareup_retrofit2_converter_gson//jar"],
        runtime_deps = [
            ":com_google_code_gson_gson",
            ":com_squareup_retrofit2_retrofit",
        ],
    )

    native.java_library(
        name = "com_google_code_gson_gson",
        visibility = ["//visibility:public"],
        exports = ["@com_google_code_gson_gson//jar"],
    )

    native.java_library(
        name = "com_squareup_retrofit2_retrofit",
        visibility = ["//visibility:public"],
        exports = ["@com_squareup_retrofit2_retrofit//jar"],
        runtime_deps = [
            ":com_squareup_okhttp3_okhttp",
            ":com_squareup_okio_okio",
        ],
    )

    native.java_library(
        name = "org_checkerframework_checker_qual",
        visibility = ["//visibility:public"],
        exports = ["@org_checkerframework_checker_qual//jar"],
    )

    native.java_library(
        name = "com_squareup_okhttp3_okhttp",
        visibility = ["//visibility:public"],
        exports = ["@com_squareup_okhttp3_okhttp//jar"],
        runtime_deps = [
            ":com_squareup_okio_okio",
        ],
    )

    native.java_library(
        name = "com_google_errorprone_error_prone_annotations",
        visibility = ["//visibility:public"],
        exports = ["@com_google_errorprone_error_prone_annotations//jar"],
    )

    native.java_library(
        name = "org_apache_commons_commons_lang3",
        visibility = ["//visibility:public"],
        exports = ["@org_apache_commons_commons_lang3//jar"],
    )

    native.java_library(
        name = "com_squareup_okio_okio",
        visibility = ["//visibility:public"],
        exports = ["@com_squareup_okio_okio//jar"],
    )

    native.java_library(
        name = "com_google_guava_guava",
        visibility = ["//visibility:public"],
        exports = ["@com_google_guava_guava//jar"],
        runtime_deps = [
            ":com_google_code_findbugs_jsr305",
            ":com_google_errorprone_error_prone_annotations",
            ":com_google_j2objc_j2objc_annotations",
            ":org_checkerframework_checker_qual",
            ":org_codehaus_mojo_animal_sniffer_annotations",
        ],
    )

    native.java_library(
        name = "org_bouncycastle_bcprov_jdk15on",
        visibility = ["//visibility:public"],
        exports = ["@org_bouncycastle_bcprov_jdk15on//jar"],
    )

    native.java_library(
        name = "com_beust_jcommander",
        visibility = ["//visibility:public"],
        exports = ["@com_beust_jcommander//jar"],
    )

    native.java_library(
        name = "org_slf4j_slf4j_simple",
        visibility = ["//visibility:public"],
        exports = ["@org_slf4j_slf4j_simple//jar"],
        runtime_deps = [
            ":org_slf4j_slf4j_api",
        ],
    )

    native.java_library(
        name = "junit_junit",
        visibility = ["//visibility:public"],
        exports = ["@junit_junit//jar"],
        runtime_deps = [
            ":org_hamcrest_hamcrest_core",
        ],
    )

    native.java_library(
        name = "com_google_j2objc_j2objc_annotations",
        visibility = ["//visibility:public"],
        exports = ["@com_google_j2objc_j2objc_annotations//jar"],
    )

    native.java_library(
        name = "org_iota_jota",
        visibility = ["//visibility:public"],
        exports = ["@org_iota_jota//jar"],
    )
