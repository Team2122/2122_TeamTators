#pragma once

#define VISIONLIB_VERSION 20220328
#define VISIONLIB_VERSION_STR "VisionLib, Version 1.5.1 2022-03-28"
#define VISIONLIB_VERSION_COPYRIGHT "Copyright (c) 2022 TeamTators"

const char* check_VISIONLIB_version();

/*
 * This define can be used in code that requires
 * compilation-related definitions specific to a
 * version or versions of the library.  Runtime
 * version checking should be done based on the
 * string returned by check_VISIONLIB_version.
 */
