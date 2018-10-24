load("@io_grpc_grpc_java//:java_grpc_library.bzl", "java_grpc_library")

java_proto_library(
    name = "signature_source_java_proto",
    visibility = ["//visibility:public"],
    deps = [":signature_source_proto"],
)

java_grpc_library(
    name = "signature_source_java_grpc",
    srcs = [":signature_source_proto"],
    visibility = ["//visibility:public"],
    deps = [":signature_source_java_proto"],
)

proto_library(
    name = "signature_source_proto",
    srcs = ["signature_source.proto"],
    visibility = ["//visibility:public"],
)
