cmake_minimum_required(VERSION 3.22.1)
project("aashuterminal")

add_library(aashuterminal SHARED
    native-lib.h
    terminal.cpp
    pty_manager.cpp
    renderer.cpp
    glyph_atlas.cpp
)

find_library(log-lib log)
find_library(android-lib android)

target_link_libraries(aashuterminal ${log-lib} ${android-lib})
target_compile_options(aashuterminal PRIVATE -Wall -Wextra -std=c++17)

# LD_PRELOAD exec-redirect shim -- see exec_shim.cpp. Built by CMake so
# AGP automatically packages it into the APK's jniLibs (native library)
# directory, the one place Android grants execute permission.
add_library(aashuexec SHARED exec_shim.cpp)
target_link_libraries(aashuexec ${log-lib} dl)
target_compile_options(aashuexec PRIVATE -Wall -Wextra -std=c++17)
