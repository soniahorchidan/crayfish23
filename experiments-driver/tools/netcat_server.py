import socketserver
import sys


class TCPHandler(socketserver.BaseRequestHandler):
    def handle(self):
        data = self.request.recv(1024)
        print(data.decode())


class MyServer(socketserver.TCPServer):
    allow_reuse_address = True


if __name__ == "__main__":
    host, port = "localhost", int(sys.argv[1])
    server = MyServer((host, port), TCPHandler)
    server.handle_request()
