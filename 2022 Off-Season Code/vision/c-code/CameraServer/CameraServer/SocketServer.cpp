#include "SocketServer.h"
#include "sha1.h"
#include <stdio.h>
#include <unistd.h>
#include <string>
#include <stdarg.h>
#include "PipelineConfig.hpp"
#include <iostream>
#include "LED_Control.h"

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
    }
    else {
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
    int numBytes;

    // Start a loop such that the server resets when the client disconnects
    for ( ; ; )
    {

        // Accepts blocks until an incoming connection arrives
        // returning a file descriptor to the connection
        printf("waiting for a connection on port %d\n", this->serverPort);
        fflush(stdout);

        // Now we open up a connection to talk to the client 
        ClientSocket = accept(ListenSocket, (SA*)NULL, NULL);

        int linecount = 0;

        // Now read the clients message
        while (true)
        {

            char receiveBuffer[MAXLINE] = { 0 };
            char sendbuf[MAXLINE] = { 0 };
            size_t sendbuf_size = 0;

            // Read from the stream
            numBytes = read( ClientSocket, receiveBuffer, MAXLINE );

            if (numBytes <= 0) {
                // The stream has been closed  
                printf("Client Socket Connection closing ...\n");
                shutdown(ClientSocket, SHUT_RDWR);
                close(ClientSocket);
                break;
            }
            //else if (numBytes < 0) {
            //    shutdown(ClientSocket, SHUT_RDWR);
            //    close(ClientSocket);
            //    shutdown(ListenSocket, SHUT_RDWR);
            //    close(ListenSocket);
            //    err_n_die("Client Socket receive failed with error ...");
            //}

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

                if (dump_mode) {
                    fprintf(stdout, "\nLogging Information Start\n~~~~~~~~~~~~~~~~~~~~~~~~~\n");
                    fprintf(stdout, "\nBinary Representation : %d\n%s\n\n", bin2hex(receiveBuffer, numBytes), receiveBuffer);
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

                    // fprintf(stdout, "\nLogging Information Start\n~~~~~~~~~~~~~~~~~~~~~~~~~\n");
                    // fprintf(stdout, "\nBinary Representation : %s\n\n", receiveBuffer);
                    // fprintf(stdout, "Logging Information End\n~~~~~~~~~~~~~~~~~~~~~~~\n\n");
                }
                else
                */

                {
                    // Allocate the memory for the return buffer
                    size_t msg_length;
                    char client_msg[MAXLINE] = { 0 };

                    // Decode the message from the web-socket
                    decodeWebSocketResponse(receiveBuffer, client_msg, &msg_length);

                    // Take a look at the message and start to parse the inputs
                    visionConfig->parseJSONCommand( client_msg, msg_length );

                    // This code defines what the server code returns to the client
                    generateWebSocketResponse( sendbuf, &sendbuf_size, client_msg, msg_length);

                    if (dump_mode) {
                        fprintf(stdout, "\nLogging Information Start\n~~~~~~~~~~~~~~~~~~~~~~~~~\n");
                        fprintf(stdout, "Binary Representation : %d\n%s\n", bin2hex(receiveBuffer, numBytes), receiveBuffer);
                        fprintf(stdout, "Linecount : %d\nLength :%d\nResponse to be returned :\n%s\n", linecount, sendbuf_size, sendbuf);
                        fprintf(stdout, "Logging Information End\n~~~~~~~~~~~~~~~~~~~~~~~\n\n");
                    }
                }
            }

            int iSendResult = send(ClientSocket, sendbuf, (int)sendbuf_size, 0);

            // Check to make sure that the socket did not error
            if (iSendResult == SO_ERROR) {
                shutdown(ClientSocket, SHUT_RDWR);
                close(ClientSocket);
                shutdown(ListenSocket, SHUT_RDWR);
                close(ListenSocket);
                err_n_die("Client Socket send failed with error ...");
            }

            // Go on to the next line if needed
            linecount++;

        } while (numBytes > 0);

        // Clean up
        shutdown(ClientSocket, SHUT_RDWR);
        close(ClientSocket);

        printf("Closing the Socket\n");
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


void SocketServer::generateWebSocketResponse(char* sendbuf, size_t* sendbuf_size, char* client_msg, size_t msg_length)
{
    // Set up char ptr to point to data stream
    char* pData;

    // Initialize a web socket header
    _websocket_header* h = (_websocket_header*)sendbuf;
    *h = _websocket_header{};

    h->opcode = 0x1; //0x1 = text, 0x2 = blob
    h->fin = 1;

    // Write the Command that has been received from the client to the server screen
    char text[MAXLINE];
    snprintf(text, countof(text), "%s%c", client_msg, 0x00);

    msg_length = strlen(text);

    sendbuf_size[0] = sizeof(_websocket_header);

    if (msg_length <= 125)
    {
        pData = sendbuf + sizeof(_websocket_header);
        h->len = msg_length;
    }
    else if (msg_length <= 0xffff)
    {
        h->len = 126;

        _extended_16* extended = (_extended_16*)(sendbuf + sendbuf_size[0]);
        sendbuf_size[0] += sizeof(_extended_16);

        extended->value[0] = (msg_length >> 8) & 0xff;
        extended->value[1] = msg_length & 0xff;
    }
    else
    {
        h->len = 127;

        _extended_64* extended = (_extended_64*)(sendbuf + sendbuf_size[0]);
        sendbuf_size[0] += sizeof(_extended_64);

        extended->value[0] = ((msg_length >> 56) & 0xff);
        extended->value[1] = ((msg_length >> 48) & 0xff);
        extended->value[2] = ((msg_length >> 40) & 0xff);
        extended->value[3] = ((msg_length >> 32) & 0xff);
        extended->value[4] = ((msg_length >> 24) & 0xff);
        extended->value[5] = ((msg_length >> 16) & 0xff);
        extended->value[6] = ((msg_length >> 8) & 0xff);
        extended->value[7] = ((msg_length >> 0) & 0xff);
    }

    pData = sendbuf + sendbuf_size[0];

    memcpy(pData, text, (size_t)msg_length);
    sendbuf_size[0] += (size_t)msg_length;

}


void SocketServer::decodeWebSocketResponse(char* receiveBuffer, char* client_msg, size_t* msg_length)
{
    // else read, print the response, and echo it back to the server
    _websocket_header* h = (_websocket_header*) receiveBuffer;

    // Defined on the stack
    _mask_key* mask_key;

    if (h->len < 126)
    {
        msg_length[0] = h->len;
        mask_key = (_mask_key*)( receiveBuffer + sizeof(_websocket_header));
    }
    else if (h->len == 126)
    {
        _extended_16* extended = (_extended_16*)(receiveBuffer + sizeof(_websocket_header));

        msg_length[0] = (extended->value[0] << 8) | extended->value[1];
        mask_key = (_mask_key*)(receiveBuffer + sizeof(_websocket_header) + sizeof(_extended_16));
    }
    else
    {
        _extended_64* extended = (_extended_64*)(receiveBuffer + sizeof(_websocket_header));

        msg_length[0] = (((unsigned long long) extended->value[0]) << 56) | (((unsigned long long) extended->value[1]) << 48) | (((unsigned long long) extended->value[2]) << 40) |
            (((unsigned long long) extended->value[3]) << 32) | (((unsigned long long) extended->value[4]) << 24) | (((unsigned long long) extended->value[5]) << 16) |
            (((unsigned long long) extended->value[6]) << 8) | (((unsigned long long) extended->value[7]) << 0);

        mask_key = (_mask_key*)(receiveBuffer + sizeof(_websocket_header) + sizeof(_extended_64));
    }

    // Make a copy of the base pointer for reference 
    char* output_buffer = client_msg;

    // This is where we are re-defining the pointer
    memcpy(output_buffer, mask_key, sizeof(_mask_key) + msg_length[0] ) ;
    output_buffer += sizeof(_mask_key);

    if (h->mask)
    {
        //Convert the ASCII
        for (int i = 0; i < msg_length[0]; i++)
            client_msg[i] = output_buffer[i] ^ mask_key->value[i % 4];
        
        // Zero out the extra
        for (int i = 0; i< sizeof(_mask_key); i++ )
            client_msg[msg_length[0]+i] = 0;
    }

}


