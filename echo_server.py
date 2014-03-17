import argparse
import time
import sys
import zmq

VERSION = "0.1"
DATE= "2014-03-15"

DEBUG = 10

def main():
    """
    zmqdump command line script
    """
    
    global DEBUG

    parser = setup_parser()
    conf = parser.parse_args()

    if (conf.verbose):
        DEBUG = 10

    es = EchoServer(conf)
    es.loop()

    es.destroy()

class EchoServer:
    conf = None
    context = None
    socket = None
    socket_type = None

    def __init__(self, conf):
        """
        Constructor

        Configuration options are translated into socket
        configuration.
        """

        self.setup_socket()

        method = "bind" if conf.bind else "connect"
        self.init_connection(method, conf.endpoint)


    def setup_socket(self):
        """
        Create context and socket obejcts.
        """

        if (DEBUG): print "Setting up socket" 

        self.context = zmq.Context()
	self.socket_type = "REP"
        self.socket = self.context.socket(zmq.REP)



    def init_connection(self, method, endpoint):
        """
        Bind/Connect socket
        """
        assert(method in ["bind", "connect"])
        assert(type(endpoint) is str and "://" in endpoint)
        if (DEBUG): print "%s socket to %s" % (method, endpoint)

        if (method == "bind"):
            self.socket.bind(endpoint)
        elif (method == "connect"):
            self.socket.connect(endpoint)


    def loop(self):
        """
        Start listning/writing to socket.
        """
	self.rep_loop()


    def rep_loop(self):
        if (DEBUG): print "Starting reply loop."
        while (True):
            try:
		lines = self.socket.recv_multipart()
                print time.strftime("%Y-%m-%d %H:%M:%S") + ": ", lines
                self.socket.send_multipart(lines)
            except KeyboardInterrupt:
                break
                
    
    def destroy(self):
        self.socket.close()
        self.context.term()
        self.context.destroy()

        
def setup_parser():
    parser = argparse.ArgumentParser(
        prog = "echoserver",
        description = "zmq echo server that replies the request"
    )

    parser.add_argument(
        "endpoint", 
        help="endpoint to listen on messages, e.g. 'tcp://127.0.0.1:5000'",
        type = str
    )
    
    parser.add_argument(
        "-b", "--bind",
        help="bind socket instead of connect",
        dest="bind", default = False,
        action = "store_true"
    )

    parser.add_argument(
        "-v", "--verbose",
        help="print additional logging information",
        dest="verbose", default = False,
        action = "store_true"
    )

    parser.add_argument(
        "--version",
        action='version',
        version='%(prog)s v.' + VERSION
    )

    return parser
   
if __name__ == "__main__":
    main()


