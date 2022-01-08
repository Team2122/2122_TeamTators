#include "SocketServer.h"
#include "sha1.h"
#include <stdio.h>
#include <unistd.h>
#include <string>
#include <stdarg.h>
#include "PipelineConfig.hpp"
#include <iostream>

using namespace std;

#define countof(array) (sizeof(array) / sizeof(array[0]))

struct _websocket_header
{
    unsigned char opcode : 4;

    unsigned char rsv3 : 1;
    unsigned char rsv2 : 1;
    unsigned char rsv1 : 1;
    unsigned char fin : 1;

    unsigned char len : 7;
    unsigned char mask : 1;
};

struct _extended_16
{
    unsigned char value[2];
};

struct _extended_64
{
    unsigned char value[8];
};

struct _mask_key
{
    unsigned char value[4];
};

SocketServer::SocketServer( int serverPort )
{
    initalizeServer(serverPort);
}

SocketServer::SocketServer( int serverPort, PipelineConfig* config )
{
    // Configure the server
    if (config != NULL) {
        visionConfig = config;
    } else {
        visionConfig = NULL;
    }

    // Run the initalization
    initalizeServer( serverPort );
}

SocketServer::~SocketServer(void)
{
    // Default Destructor - cleans up
    stopServer();
}

void SocketServer::initalizeServer( int serverPort )
{
    // Default Constructor
    // Set-Up the Server Port number
    this->serverPort = serverPort ;

    // Make sure that the socket is open
    if ((ListenSocket = socket(AF_INET, SOCK_STREAM, 0)) < 0)
        err_n_die("Error while creating the socket!");

    int setTrue = 1;
    setsockopt(ListenSocket, SOL_SOCKET, SO_REUSEADDR, &setTrue, sizeof(int));

    bzero(&serveraddr, sizeof(serveraddr));
    serveraddr.sin_family = AF_INET;
    serveraddr.sin_port = htons(serverPort);
    serveraddr.sin_addr.s_addr = htonl(INADDR_ANY);

    // Bind to the open socket 
    if ((bind(ListenSocket, (SA*)&serveraddr, sizeof(serveraddr))) < 0) {
        err_n_die("bind error");
    }

    // Now start listening on the socket 
    if ((listen(ListenSocket, 10)) < 0) {
        err_n_die("listen error");
    }
}

void SocketServer::runServer()
{
    // Set up temporary variable
    int n;

    // Start a loop such that the server resets when the client disconnects
    for (; ; )
    {
        // Initalize the WebSocket parameters
        struct sockaddr_in addr;
        socklen_t addr_len;

        // Accepts blocks until an incoming connection arrives
        // returning a file descriptor to the connection
        printf("waiting for a connection on port %d\n", this->serverPort );
        fflush(stdout);

        // Now we open up a connection to talk to the client 
        ClientSocket = accept(ListenSocket, (SA*)NULL, NULL);

        int linecount = 0;

        // Now read the clients message
        while (true)
        {

            char receiveBuffer[MAXLINE] = { 0 };
            char sendbuf[1024];
            size_t sendbuf_size = 0;

            // Read from the stream
            n = read(ClientSocket, receiveBuffer, MAXLINE);

            if (n == 0) {
                // The stream has been closed  
                printf("Client Socket Connection closing ...");
                close(ClientSocket);
                break;
            }
            else if (n < 0) {
                shutdown(ClientSocket, SHUT_RDWR);
                close(ClientSocket);
                shutdown(ListenSocket, SHUT_RDWR);
                close(ListenSocket);
                err_n_die("Client Socket receive failed with error ...");
            }

            // see if it's requesting a key
            char* pKey = strstr(receiveBuffer, "Sec-WebSocket-Key:");

            if (pKey)
            {
                // parse just the key part
                pKey = strchr(pKey, ' ') + 1;
                char* pEnd = strchr(pKey, '\r');
                *pEnd = 0;

                char key[256];
                snprintf(key, countof(key), "%s%s", pKey, WEBSOCKET_KEY);

                unsigned char result[20];
                const unsigned char* pSha1Key = sha1(key);

                // endian swap each of the 5 ints
                for (int i = 0; i < 5; i++)
                {
                    for (int c = 0; c < 4; c++)
                        result[i * 4 + c] = pSha1Key[i * 4 + (4 - c - 1)];
                }

                pKey = base64_encode(result, 20);

                const char* pTemplateResponse = "HTTP/1.1 101 Switching Protocols\r\n"
                    "Upgrade: websocket\r\n"
                    "Connection: Upgrade\r\n"
                    "Sec-WebSocket-Accept: %s\r\n"
                    "Sec-WebSocket-Protocol: tatorvision\r\n\r\n";

                printf("Server : Responding to Client WebSocket Request\n");

                snprintf(sendbuf, countof(sendbuf), pTemplateResponse, pKey);
                sendbuf_size = strlen(sendbuf);

                if (debug_mode) {
                    fprintf(stdout, "\nLogging Information Start\n~~~~~~~~~~~~~~~~~~~~~~~~~\n");
                    fprintf(stdout, "\nBinary Representation : %d\n%s\n\n", bin2hex(receiveBuffer, n), receiveBuffer);
                    fprintf(stdout, "Linecount : %d\nResponse returned :\n%s", linecount, sendbuf);
                    fprintf(stdout, "Logging Information End\n~~~~~~~~~~~~~~~~~~~~~~~\n\n");
                }
            }
            else
            {

                char* pHTTP = strstr(receiveBuffer, "GET / HTTP/1.1");

                /*
                if (pHTTP)
                {
                    fprintf(stdout, "\nRecevied an HTTP Request\n");

                    //    // fprintf(stdout, "\nLogging Information Start\n~~~~~~~~~~~~~~~~~~~~~~~~~\n");
                    //    // fprintf(stdout, "\nBinary Representation : %s\n\n", receiveBuffer);
                    //    // fprintf(stdout, "Logging Information End\n~~~~~~~~~~~~~~~~~~~~~~~\n\n");
                }
                else
                */

                {
                    // else read, print the response, and echo it back to the server
                    _websocket_header* h = (_websocket_header*)receiveBuffer;

                    _mask_key* mask_key;

                    unsigned long long length;

                    if (h->len < 126)
                    {
                        length = h->len;
                        mask_key = (_mask_key*)(receiveBuffer + sizeof(_websocket_header));
                    }
                    else if (h->len == 126)
                    {
                        _extended_16* extended = (_extended_16*)(receiveBuffer + sizeof(_websocket_header));

                        length = (extended->value[0] << 8) | extended->value[1];
                        mask_key = (_mask_key*)(receiveBuffer + sizeof(_websocket_header) + sizeof(_extended_16));
                    }
                    else
                    {
                        _extended_64* extended = (_extended_64*)(receiveBuffer + sizeof(_websocket_header));

                        length = (((unsigned long long) extended->value[0]) << 56) | (((unsigned long long) extended->value[1]) << 48) | (((unsigned long long) extended->value[2]) << 40) |
                            (((unsigned long long) extended->value[3]) << 32) | (((unsigned long long) extended->value[4]) << 24) | (((unsigned long long) extended->value[5]) << 16) |
                            (((unsigned long long) extended->value[6]) << 8) | (((unsigned long long) extended->value[7]) << 0);

                        mask_key = (_mask_key*)(receiveBuffer + sizeof(_websocket_header) + sizeof(_extended_64));
                    }

                    char* client_msg = ((char*)mask_key) + sizeof(_mask_key);

                    if (h->mask)
                    {
                        for (int i = 0; i < length; i++)
                            client_msg[i] = client_msg[i] ^ mask_key->value[i % 4];
                    }

                    // Take a look at the message and start to parse the inputs
                    parseJSONCommand( client_msg );

                    // This code defines what the server code returns to the client
                    {
                        char* pData;

                        h = (_websocket_header*)sendbuf;
                        *h = _websocket_header{};

                        h->opcode = 0x1; //0x1 = text, 0x2 = blob
                        h->fin = 1;

                        // Write the Command that has been received from the client to the server screen
                        char text[MAXLINE];
                        snprintf(text, countof(text), "Server Echo: %s", client_msg);

                        unsigned long long msg_length = strlen(text);

                        sendbuf_size = sizeof(_websocket_header);

                        if (msg_length <= 125)
                        {
                            pData = sendbuf + sizeof(_websocket_header);
                            h->len = msg_length;
                        }
                        else if (msg_length <= 0xffff)
                        {
                            h->len = 126;

                            _extended_16* extended = (_extended_16*)(sendbuf + sendbuf_size);
                            sendbuf_size += sizeof(_extended_16);

                            extended->value[0] = (msg_length >> 8) & 0xff;
                            extended->value[1] = msg_length & 0xff;
                        }
                        else
                        {
                            h->len = 127;

                            _extended_64* extended = (_extended_64*)(sendbuf + sendbuf_size);
                            sendbuf_size += sizeof(_extended_64);

                            extended->value[0] = ((msg_length >> 56) & 0xff);
                            extended->value[1] = ((msg_length >> 48) & 0xff);
                            extended->value[2] = ((msg_length >> 40) & 0xff);
                            extended->value[3] = ((msg_length >> 32) & 0xff);
                            extended->value[4] = ((msg_length >> 24) & 0xff);
                            extended->value[5] = ((msg_length >> 16) & 0xff);
                            extended->value[6] = ((msg_length >> 8) & 0xff);
                            extended->value[7] = ((msg_length >> 0) & 0xff);
                        }

                        pData = sendbuf + sendbuf_size;

                        memcpy(pData, text, (size_t)msg_length);
                        sendbuf_size += (size_t)msg_length;
                    }

                    if (debug_mode) {
                        fprintf(stdout, "\nLogging Information Start\n~~~~~~~~~~~~~~~~~~~~~~~~~\n");
                        fprintf(stdout, "\nBinary Representation : %d\n%s\n\n", bin2hex(receiveBuffer, n), receiveBuffer);
                        fprintf(stdout, "Linecount : %d\nResponse to be returned :\n%s", linecount, sendbuf);
                        fprintf(stdout, "Logging Information End\n~~~~~~~~~~~~~~~~~~~~~~~\n\n");
                    }
                }
            }

            int iSendResult = send(ClientSocket, sendbuf, (int)sendbuf_size, 0);

            // Check to make sure that te socket did not error
            if (iSendResult == SO_ERROR) {
                shutdown(ClientSocket, SHUT_RDWR);
                close(ClientSocket);
                shutdown(ListenSocket, SHUT_RDWR);
                close(ListenSocket);
                err_n_die("Client Socket send failed with error ...");
            }

            // Go on to the next line if needed
            linecount++;

        } while (n > 0);

        // Clean up
        shutdown(ClientSocket, SHUT_RDWR);
        close(ClientSocket);
    }

}

void SocketServer::stopServer()
{
    // Ensure that the client socket has been closed
    shutdown(ClientSocket, SHUT_RDWR);
    close(ClientSocket);

    // Since we are done with the server socket, we can close it out
    shutdown(ListenSocket, SHUT_RDWR);
    close(ListenSocket);
}


char* SocketServer::bin2hex(const char* input, size_t len)
{
    char* result;
    const char* hexits = "0123456789ABCDEF";

    if (input == NULL || len <= 0)
        return NULL;

    int resultlength = (len * 3) + 1;

    // MEMORY LEAK !!!!
    result = (char*)malloc(resultlength);

    bzero(result, resultlength);

    for (int i = 0; i < len; i++)
    {
        result[i * 3] = hexits[input[i] >> 4];
        result[(i * 3) + 1] = hexits[input[i] & 0x0F];
        result[(i * 3) + 2] = ' ';
    }

    return result;
}


void SocketServer::err_n_die(const char* fmt, ...)
{
    int errno_save;
    va_list ap;

    errno_save = errno;

    // Print out the fmt and args 
    va_start(ap, fmt);
    vfprintf(stdout, fmt, ap);
    fprintf(stdout, "\n");
    fflush(stdout);

    // Print out the error message if errno was set
    if (errno_save != 0)
    {
        fprintf(stdout, "(errno = %d) : %s\n", errno_save, strerror(errno_save));
        fprintf(stdout, "\n");
        fflush(stdout);
    }
    va_end(ap);

    // Exit and die
    exit(1);
}


void SocketServer::parseJSONCommand( char* client_msg )
{
    // Prepare maximum of 10 JSON Object
    JSON_element jsonCommands[MAX_JSON_ELEMENTS];
    int jsonCount = 0;

    // Copy the pointer for reference
    char* base_offset = client_msg;
    int current_offset = 0;

    // printf("Message from Client: %s\r\n", (base_offset + current_offset ) );

    char* openPos = strstr(base_offset, "{");
    char* closePos = strstr(base_offset, "}");
    char* commaPos = strstr(base_offset, ",");
    char* colonPos = strstr(base_offset, ":");

    int commaLength = commaPos - base_offset;
    int endLength = closePos - base_offset;

    //printf( "Comma Position : %d\n", commaLength);
    //printf( "End Brace Position : %d\n", endLength);

    // Itterate through the pairs
    while ( colonPos > 0 )
    {
        char* openQ = openPos + 1;
        int str_length = colonPos - openQ ;

        //printf("OpenPos  : %x\n", openQ);
        //printf("ColonPos : %x\n", colonPos);

        // Extract the first key value 
        memcpy(jsonCommands[jsonCount].key, openQ, str_length);
        // printf("String 1 : %s\n", jsonCommands[jsonCount].key);

        // Extract the second key value 
        openQ = colonPos + 1;

        if ( commaPos < openQ ) {
            str_length = closePos - openQ ;
        } else {
            str_length = commaPos - openQ ;
        }
        
        //printf("StrLen : %d\n", str_length);
        memcpy(jsonCommands[jsonCount].value, openQ, str_length);
        //printf("String 2 : %s\n", jsonCommands[jsonCount].value);

        //printf("Current Position : %d\n", ( openQ + str_length ) - client_msg );
        //printf("CommaPos : %d\n", commaPos - client_msg);
        jsonCount++;

        if ( ( jsonCount >= (MAX_JSON_ELEMENTS-1)) || (commaPos == NULL )) {
            break;
        }
        else {

            //printf("OpenPos : %x\n", openPos);
            //printf("ClosePos : %x\n", closePos);
            //printf("CommaPos : %x\n", commaPos);

            //Reset the pointer
            openPos = commaPos ;
            // Look for another delimeter
            commaPos = strstr(openPos+1, ",");
            colonPos = strstr(openPos+1, ":");
        }
    }

    // Now act on the JSON Command
    updateConfigClass( jsonCommands, jsonCount );
}

void SocketServer::updateConfigClass( JSON_element* jsonElements, int numElements )
{
    // Test the pointer for the pipelineConfig object
    // printf("Pointer in Memory : %x\n", visionConfig );

    // Now start to work with the elements
    //for (int i = 0; i < numElements; i++) {
    //    printf("Key   : %s\n", jsonElements[i].key );
    //    printf("Value : %s\n", jsonElements[i].value );
    //}

    printf("Key   : %s\n", jsonElements[0].key );
    printf("Value : %s\n", jsonElements[0].value );

    if ( strcmp( (const char*) jsonElements[0].key, "type" ) ) 
    {
        if ( strstr((const char*) jsonElements[0].value, "tatorVisionState") )
        {
            if (debug_mode) {
                printf("Vision Config Connection\n");
            }
            updateStatusConfig( jsonElements, numElements );
        }
        else if (strstr((const char*)jsonElements[0].value, "systemControl"))
        {
            if (debug_mode) {
                printf("Connection : %s\n", jsonElements[0].value);
            }
            updateSystemConfig( jsonElements, numElements );
        }
        else if (strstr((const char*)jsonElements[0].value, "pipelineControl"))
        {
            if (debug_mode) {
                printf("Connection : %s\n", jsonElements[0].value);
            }
            updatePipelineConfig( jsonElements, numElements );
        }
        else if (strstr((const char*)jsonElements[0].value, "cameraControl"))
        {
            if (debug_mode) {
                printf("Connection : %s\n", jsonElements[0].value);
            }
            updateCameraConfig( jsonElements, numElements );
        }
    }
}


void SocketServer::updateStatusConfig(JSON_element* jsonElements, int numElements)
{

    // Now start to work with the elements
    for (int i = 1; i < numElements; i++)
    {
        char test[MAX_STRING_LENGTH];
        sprintf(test, "%s", jsonElements[i].value);

        //printf("%s\n", jsonElements[i].key);
        //printf("%s\n", test );

        if (strstr((const char*)jsonElements[i].key, "tatorVisionState")) {
            //printf("I am here : %s\n", jsonElements[i].key);
        }
        else {
            printf("Unknown Command : %s , %s\n", jsonElements[i].key, jsonElements[i].value);
        }

    }

}

void SocketServer::updateSystemConfig(JSON_element* jsonElements, int numElements)
{

    // Now start to work with the elements
    for (int i = 1; i < numElements; i++)
    {
        char test[MAX_STRING_LENGTH];
        sprintf(test, "%s", jsonElements[i].value);

        //printf("%s\n", jsonElements[i].key);
        //printf("%s\n", test );

        if (strstr((const char*)jsonElements[i].key, "debugMode")) {

            if (debug_mode) {
                printf("I am here : %s\n", jsonElements[i].key);
            }
            if (strstr((const char*)jsonElements[i].key, "on")) {
                visionConfig->setVisionLibDebugMode(true);
            } else {
                visionConfig->setVisionLibDebugMode(false);
            }
        }
        else if (strstr((const char*)jsonElements[i].key, "captureMode")) {
            
            if (debug_mode) {
                printf("I am here : %s\n", jsonElements[i].key);
            }
            if (strstr((const char*)jsonElements[i].key, "on")) {
                visionConfig->setVisionLibCaptureMode(true);
            }
            else {
                visionConfig->setVisionLibCaptureMode(false);
            }
        }
        else if (strstr((const char*)jsonElements[i].key, "captureIncrement")) {
            if (debug_mode) {
                printf("I am here : %s\n", jsonElements[i].key);
            }
            visionConfig->setVisionLibCaptureInc(stoi(test));
        }
        else if (strstr((const char*)jsonElements[i].key, "debugFilename")) {
            if (debug_mode) {
                printf("I am here : %s\n", jsonElements[i].key);
            }
            visionConfig->setVisionLibDebugStr(test);
        }
        else if (strstr((const char*)jsonElements[i].key, "multi-thread")) {
            if (debug_mode) {
                printf("I am here : %s\n", jsonElements[i].key);
            }
            if (strstr((const char*)jsonElements[i].key, "on")) {
                visionConfig->setMultiThreaded(true);
            }
            else {
                visionConfig->setMultiThreaded(false);
            }
        }
        else if (strstr((const char*)jsonElements[i].key, "executor-service")) {
            if (debug_mode) {
                printf("I am here : %s\n", jsonElements[i].key);
            }
            if (strstr((const char*)jsonElements[i].key, "on")) {
                visionConfig->setExecutorService(true);
            }
            else {
                visionConfig->setExecutorService(false);
            }
        }
        else if (strstr((const char*)jsonElements[i].key, "numberOfThreads")) {
            if (debug_mode) {
                printf("I am here : %s\n", jsonElements[i].key);
            }
            visionConfig->setNumberOfThreads(stoi(test));
        }
        else if (strstr((const char*)jsonElements[i].key, "visionLEDstatus")) {
            if (debug_mode) {
                printf("I am here : %s\n", jsonElements[i].key);
            }
            if (strstr((const char*)jsonElements[i].key, "on")) {
                visionConfig->setVisionLEDEnabled(true);
            }
            else {
                visionConfig->setVisionLEDEnabled(false);
            }
        }
        else if (strstr((const char*)jsonElements[i].key, "frameRate")) {
            if (debug_mode) {
                printf("I am here : %s\n", jsonElements[i].key);
            }
            visionConfig->setVisionNTFrameRate(stod(test));
        }
        else if (strstr((const char*)jsonElements[i].key, "visionLEDbrightness")) {
            if (debug_mode) {
                printf("I am here : %s\n", jsonElements[i].key);
            }
            visionConfig->setVisionLEDBrightness(stod(test));
        }
        else {
            printf("Unknown Command : %s , %s\n", jsonElements[i].key, jsonElements[i].value );
        }

    }

}

void SocketServer::updatePipelineConfig(JSON_element* jsonElements, int numElements)
{

    // Now start to work with the elements
    for (int i = 1; i < numElements; i++)
    {
        char test[MAX_STRING_LENGTH];
        char low_value[MAX_STRING_LENGTH];
        char high_value[MAX_STRING_LENGTH];

        sprintf(test, "%s", jsonElements[i].value);

        //printf("%s\n", jsonElements[i].key);
        //printf("%s\n", test );

        if (strstr((const char*)jsonElements[i].key, "scaleFactor")) {
            if (debug_mode) {
                printf("I am here : %s\n", jsonElements[i].key);
            }
            visionConfig->setScaleFactor(stod(test));
        }
        else if (strstr((const char*)jsonElements[i].key, "rgb_clipping")) {
            if (debug_mode) {
                printf("I am here : %s\n", jsonElements[i].key);
            }
            char* startPos = strstr(test, "[") + 2;
            char* spacePos = strstr(startPos, " ")+1;
            char* endPos = strstr(test, "]");
            memcpy(low_value, startPos, (spacePos - startPos) );
            memcpy(high_value, spacePos, (endPos - spacePos) );

            visionConfig->setRGB_ClippingLUT( (double) stoi(low_value), (double) stoi(high_value));
        }
        else if (strstr((const char*)jsonElements[i].key, "lightness_clipping")) {
            if (debug_mode) {
                printf("I am here : %s\n", jsonElements[i].key);
            }
            char* startPos = strstr(test, "[") + 2;
            char* spacePos = strstr(startPos, " ")+1;
            char* endPos = strstr(test, "]");
            memcpy(low_value, startPos, (spacePos - startPos));
            memcpy(high_value, spacePos, (endPos - spacePos));

            visionConfig->setL_ClippingLUT((double)stoi(low_value), (double)stoi(high_value));
        }
        else if (strstr((const char*)jsonElements[i].key, "chroma_range")) {
            if (debug_mode) {
                printf("I am here : %s\n", jsonElements[i].key);
            }
            char* startPos = strstr(test, "[") + 2;
            char* spacePos = strstr(startPos, " ")+1;
            char* endPos = strstr(test, "]");
            memcpy(low_value, startPos, (spacePos - startPos));
            memcpy(high_value, spacePos, (endPos - spacePos));

            visionConfig->setChroma_ClippingLUT((double)stoi(low_value), (double)stoi(high_value));
        }
        else if (strstr((const char*)jsonElements[i].key, "hue_range")) {
            if (debug_mode) {
                printf("I am here : %s\n", jsonElements[i].key);
            }
            char* startPos = strstr(test, "[") + 2;
            char* spacePos = strstr(startPos, " ")+1;
            char* endPos = strstr(test, "]");
            memcpy(low_value, startPos, (spacePos - startPos));
            memcpy(high_value, spacePos, (endPos - spacePos));

            visionConfig->setHue_ClippingLUT((double)stoi(low_value), (double)stoi(high_value));
        }
        else if (strstr((const char*)jsonElements[i].key, "L_Ref")) {
            if (debug_mode) {
                printf("I am here : %s\n", jsonElements[i].key);
            }
            visionConfig->setTargetL(stod(test));
        }
        else if (strstr((const char*)jsonElements[i].key, "a_Ref")) {
            if (debug_mode) {
                printf("I am here : %s\n", jsonElements[i].key);
            }
            visionConfig->setTargeta(stod(test));
        }
        else if (strstr((const char*)jsonElements[i].key, "b_Ref")) {
            if (debug_mode) {
                printf("I am here : %s\n", jsonElements[i].key);
            }
            visionConfig->setTargetb(stod(test));
        }
        else if (strstr((const char*)jsonElements[i].key, "threshold")) {
            if (debug_mode) {
                printf("I am here : %s\n", jsonElements[i].key);
            }
            visionConfig->setThreshold(stod(test));
        }
        else if (strstr((const char*)jsonElements[i].key, "morphology")) {
            if (debug_mode) {
                printf("I am here : %s\n", jsonElements[i].key);
            }
            visionConfig->setMorphologyMethod(test);
        }
        else if (strstr((const char*)jsonElements[i].key, "conn_comp")) {
            if (debug_mode) {
                printf("I am here : %s\n", jsonElements[i].key);
            }
            visionConfig->setConnectComponentsConnectivity(test);
        }
        else if (strstr((const char*)jsonElements[i].key, "boundary_detect")) {
            if (debug_mode) {
                printf("I am here : %s\n", jsonElements[i].key);
            }
            visionConfig->setBoundaryDetectionMethod(test);
        }
        else if (strstr((const char*)jsonElements[i].key, "target_detect")) {
            if (debug_mode) {
                printf("I am here : %s\n", jsonElements[i].key);
            }
            visionConfig->setTargetType(test);
        }
        else {
            printf("Unknown Command : %s , %s\n", jsonElements[i].key, jsonElements[i].value);
        }

    }

}

void SocketServer::updateCameraConfig(JSON_element* jsonElements, int numElements)
{

    // Now start to work with the elements
    for (int i = 1; i < numElements; i++) 
    {
        char test[MAX_STRING_LENGTH] ;
        sprintf( test, "%s", jsonElements[i].value );

        //printf("%s\n", jsonElements[i].key);
        //printf("%s\n", test );

        if (strstr((const char*)jsonElements[i].key, "tatorVisionCamera")){
            if (debug_mode) {
                printf("I am here : %s\n", jsonElements[i].key);
            }
        }
        else if (strstr((const char*)jsonElements[i].key, "tatorVisionResolution")) {
            if (debug_mode) {
                printf("I am here : %s\n", jsonElements[i].key);
            }
        }
        else if (strstr((const char*)jsonElements[i].key, "tatorVisionframeRate")) {
            if (debug_mode) {
                printf("I am here : %s\n", jsonElements[i].key);
            }
            visionConfig->setCameraFramesPerSecond(stoi(test));
        }
        else if (strstr((const char*)jsonElements[i].key, "orientation")) {
            if (debug_mode) {
                printf("I am here : %s\n", jsonElements[i].key);
            }
            visionConfig->setSensorOrientation(stoi(test));
        }
        else if (strstr((const char*)jsonElements[i].key, "exposure_value")) {
            if (debug_mode) {
                printf("I am here : %s\n", jsonElements[i].key);
            }
            visionConfig->setCameraManualExposure(stod(test));
        }
        else if (strstr((const char*)jsonElements[i].key, "brightness_value")) {
            if (debug_mode) {
                printf("I am here : %s\n", jsonElements[i].key);
            }
            visionConfig->setCameraBrightness(stod(test));
        }
        else if (strstr((const char*)jsonElements[i].key, "saturation_value")) {
            if (debug_mode) {
                printf("I am here : %s\n", jsonElements[i].key);
            }
            visionConfig->setCameraSaturation(stod(test));
        }
        else if (strstr((const char*)jsonElements[i].key, "contrast_value")) {
            if (debug_mode) {
                printf("I am here : %s\n", jsonElements[i].key);
            }
            visionConfig->setCameraContrast(stod(test));
        }
        else if (strstr((const char*)jsonElements[i].key, "sharpness_value")) {
            if (debug_mode) {
                printf("I am here : %s\n", jsonElements[i].key);
            }
            visionConfig->setCameraSharpness(stod(test));
        }
        else if (strstr((const char*)jsonElements[i].key, "colorbalanceR_value")) {
            if (debug_mode) {
                printf("I am here : %s\n", jsonElements[i].key);
            }
            visionConfig->setCameraColorBalanceR(stoi(test));
        }
        else if (strstr((const char*)jsonElements[i].key, "colorbalanceB_value")) {
            if (debug_mode) {
                printf("I am here : %s\n", jsonElements[i].key);
            }
            visionConfig->setCameraColorBalanceB(stoi(test));
        }
        else {
            printf("Unknown Command : %s , %s\n", jsonElements[i].key, jsonElements[i].value);
        }

    }

    printf("I am here\n");

    // Force the camera to use manual exposure control
    visionConfig->setCameraAutoExposure(false);

    // Force a refresh on the camera configuration
    visionConfig->setCameraVisionSettings();
    visionConfig->printCameraSupportSettings();

}


