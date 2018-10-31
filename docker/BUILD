load("@io_bazel_rules_docker//container:container.bzl", "container_image")

container_image(
    name = "layers_calculator",
    base = "@java_base//image",
    cmd = ["layers_calculator_deploy.jar"],
    files = ["//:layers_calculator_deploy.jar"],
    repository = "iota/compass",
)

container_image(
    name = "coordinator",
    base = "@java_base//image",
    cmd = ["coordinator_deploy.jar"],
    files = ["//:coordinator_deploy.jar"],
    repository = "iota/compass",
)

container_image(
    name = "shadowing_coordinator",
    base = "@java_base//image",
    cmd = ["shadowing_coordinator_deploy.jar"],
    files = ["//:shadowing_coordinator_deploy.jar"],
    repository = "iota/compass",
)

container_image(
    name = "signature_source_server",
    base = "@java_base//image",
    cmd = ["signature_source_server_deploy.jar"],
    files = ["//:signature_source_server_deploy.jar"],
    repository = "iota/compass",
)
