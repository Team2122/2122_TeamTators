#pragma once

#define VISIONLIB_VERSION 20211218
#define VISIONLIB_VERSION_STR "VisionLib, Version 1.0.17 2021-12-18"
#define VISIONLIB_VERSION_COPYRIGHT "Copyright (c) 2021 TeamTators"

const char* check_VISIONLIB_version();

/*
 * This define can be used in code that requires
 * compilation-related definitions specific to a
 * version or versions of the library.  Runtime
 * version checking should be done based on the
 * string returned by check_VISIONLIB_version.
 */
