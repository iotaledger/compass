load("@io_bazel_rules_docker//container:container.bzl", "container_image")

container_image(
    name = "address_generator",
    base = "@java_base//image",
    cmd = ["address_generator_deploy.jar"],
    files = ["//:address_generator_deploy.jar"],
)

container_image(
    name = "merkle_tree_calculator",
    base = "@java_base//image",
    cmd = ["merkle_tree_calculator_deploy.jar"],
    files = ["//:merkle_tree_calculator_deploy.jar"],
)

container_image(
    name = "coordinator",
    base = "@java_base//image",
    cmd = ["coordinator_deploy.jar"],
    files = ["//:coordinator_deploy.jar"],
)

container_image(
    name = "shadowing_coordinator",
    base = "@java_base//image",
    cmd = ["shadowing_coordinator_deploy.jar"],
    files = ["//:shadowing_coordinator_deploy.jar"],
)
