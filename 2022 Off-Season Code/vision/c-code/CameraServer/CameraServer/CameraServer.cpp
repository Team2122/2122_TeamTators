#include <iostream>

#include "networktables/NetworkTableInstance.h"
#include "networktables/NetworkTableEntry.h"
#include "Utilities.h"
#include "signal.h"

#define SERVER_ADDR "10.21.22.2"

// Declare function prototypes
int legacyCodeView(bool debugInfoState);


void segfault_sigaction(int signal, siginfo_t* si, void* arg)
{
    printf("Caught segfault at address %p\n", si->si_addr);
    printf( "Exiting Vision\n" );
    exit(0);
}


int main(int argc, char* argv[])
{
    bool debug_info_state = false;
    int  input_arg_counter;
    char networkServer_IP[256] = SERVER_ADDR;

    if (argc >= 2)
    {
        for (input_arg_counter = 0; input_arg_counter < argc; input_arg_counter++)
        {
            if (strcmp(argv[input_arg_counter], "-s") == 0)
            {
                printf("\tSimulation Environment Enabled\n");
                input_arg_counter++;
                sprintf(networkServer_IP, argv[input_arg_counter]);
            }

            if (strcmp(argv[input_arg_counter], "-d") == 0)
            {
                printf("\tDebugging Info Enabled\n");
                debug_info_state = true;
            }
        }
        printf("\n");
    }

    std::cout << "Running Vision\n";    
    std::cout << "Configuring NetworkTables\n";

    nt::NetworkTableInstance ntinst = nt::NetworkTableInstance::GetDefault();
    ntinst.SetServer(networkServer_IP);
    ntinst.StartClient();

    std::cout << "Unable to connect to Network Tables at IP : " << networkServer_IP << "\n";
    std::cout << "Continuing Anyway...\n";

    // First, Make sure that we are using the correction version of
    //Verify the version of VISIONLIB
    test_VisionLib_Version();

    // Start by adding segmentation fault tracking
    struct sigaction sa;

    memset(&sa, 0, sizeof(struct sigaction));
    sigemptyset(&sa.sa_mask);
    sa.sa_sigaction = segfault_sigaction;
    sa.sa_flags = SA_SIGINFO;

    sigaction(SIGSEGV, &sa, NULL);


    // Call into the vision pipeline
    legacyCodeView( debug_info_state );

    // Notify termination
    std::cout << "Exiting Vision\n";

}
