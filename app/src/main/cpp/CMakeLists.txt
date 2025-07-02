cmake_minimum_required(VERSION 3.18.1)

project(kzmusic-native)

# Your native source
add_library(native-lib SHARED
        native-lib.cpp)

# Include the Superpowered headers
include_directories(Superpowered)


# Import Superpowered static library
add_library(Superpowered STATIC IMPORTED)
set_target_properties(Superpowered PROPERTIES
        IMPORTED_LOCATION
        ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libSuperpoweredAudioAndroid.a
)

# Link native-lib with Superpowered and Android system libs
find_library(log-lib log)
find_library(android-lib android)
find_library(OpenSLES-lib OpenSLES)

target_link_libraries(native-lib
        Superpowered
        ${log-lib}
        ${android-lib}
        ${OpenSLES-lib})