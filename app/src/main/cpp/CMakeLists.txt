project(Project)

add_library(native-lib
    SHARED
    native-lib.cpp
)

target_link_libraries(native-lib
    android
    log
)
