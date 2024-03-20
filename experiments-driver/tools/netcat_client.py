import socket
import sys

host, port = sys.argv[1], int(sys.argv[2])
data = sys.argv[3]

with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
    sock.connect((host, port))
    sock.sendall(bytes(data, "utf-8"))

sock.close()