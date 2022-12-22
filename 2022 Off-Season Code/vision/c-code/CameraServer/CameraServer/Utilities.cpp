#include "VisionLib.hpp"
#include "Utilities.h"

int test_VisionLib_Version()
{

    //#ifndef __linux__
    //	_CrtSetDbgFlag(_CRTDBG_ALLOC_MEM_DF | _CRTDBG_LEAK_CHECK_DF);
    //#endif

    //Verify the version of VISIONLIB
    const char* result = check_VISIONLIB_version();
    if (strcmp(result, VISIONLIB_VERSION_STR) != 0)
    {
        printf("\nVisionLib Version Missmatch - Fatal Error Cannot Continue\n");
        printf("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n");
        printf("Library Version  : %s\n", VISIONLIB_VERSION_STR);
        printf("Expected Version : %s\n\n", result);
        return EXIT_FAILURE;
    }
    else
        return EXIT_SUCCESS;
}


