COMPILE_DEPS = CORE_DEPS + JACKSON + REST + [
    "//core/common:onos-core-common",
    "//core/net:onos-core-net",
    "//apps/annotationx/app:onos-apps-annotationx-app",
]

osgi_jar_with_tests(
    api_description = "API for Annotation Extend",
    api_package = "com.ict.annotationx.rest",
    api_title = "Annotation Extend API",
    api_version = "1.0",
    web_context = "/onos/v1/annotationx",
    deps = COMPILE_DEPS,
)
