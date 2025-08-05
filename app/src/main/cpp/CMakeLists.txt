cmake_minimum_required(VERSION 3.18.1)

project(kzmusic-native)

# Define path to Superpowered SDK headers and sources
set(SUPERPOWERED_DIR ${CMAKE_CURRENT_SOURCE_DIR}/Superpowered)

# Include the Superpowered headers
include_directories(${SUPERPOWERED_DIR})

# Add your native source files, including the missing implementation
add_library(native-lib SHARED
        AdvancedAudioPlayer.cpp
        ${SUPERPOWERED_DIR}/SuperpoweredAndroidAudioIO.cpp
)

# Import Superpowered static library
add_library(Superpowered STATIC IMPORTED)
set_target_properties(Superpowered PROPERTIES
        IMPORTED_LOCATION
        ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libSuperpoweredAudioAndroid.a
)

# Find Android system libraries
find_library(log-lib log)
find_library(android-lib android)
find_library(OpenSLES-lib OpenSLES)

# Link native-lib with Superpowered and Android system libs
target_link_libraries(native-lib
        Superpowered
        ${log-lib}
        ${android-lib}
        ${OpenSLES-lib}
)