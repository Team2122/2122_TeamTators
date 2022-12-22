#pragma once

#ifdef __linux__
	#include <arpa/inet.h>
#else
	#include <winsock2.h>
	#include <WS2tcpip.h>
#endif

#define MAX_STRING_LENGTH 255
#define WEBSOCKET_KEY   "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"
#define MAXLINE 4096
#define SA struct sockaddr
//#define SERVER_PORT 8051
#define SERVER_PORT 5800

class PipelineConfig;

#define MAX_JSON_ELEMENTS 50

struct JSON_element
{
	char* key[MAX_STRING_LENGTH] = { 0 };
	char* value[MAX_STRING_LENGTH] = { 0 };
};

class SocketServer
{
public:

	SocketServer(int serverPort);							// Default Constructor
	SocketServer(int serverPort, PipelineConfig* config );	// Constructor
	~SocketServer();										// Default Destructor

	void runServer();
	void stopServer();

private:

	bool			   debug_mode = false;
	bool			   dump_mode = false;
	int				   serverPort;
	int				   ListenSocket;
	int				   ClientSocket;   // socket information
	struct sockaddr_in serveraddr ;
	PipelineConfig*    visionConfig ;

	void initalizeServer(int serverPort);

	void decodeWebSocketResponse(char* receiveBuf, char* clent_msg, size_t* msg_length);
	void generateWebSocketResponse(char* sendsBuf, size_t* sendBuf_length, char* clent_msg, size_t msg_length);

	void updateConfigClass( JSON_element* jsonElements, int numElements ) ;
	char* bin2hex(const char* input, size_t len);
	void err_n_die(const char* fmt, ...);

};

