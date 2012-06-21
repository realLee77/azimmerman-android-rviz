import string, cgi, time
import rospkg

from os import curdir, sep, path
from BaseHTTPServer import BaseHTTPRequestHandler, HTTPServer
    
rp = rospkg.RosPack()
class MyHandler( BaseHTTPRequestHandler):            
    def do_GET(self):
        try:
            if self.path.startswith("/PKG"):
                splitPath = self.path.split("/")
                if len(splitPath) > 2:
                    splitPath.remove('');
                    try:
                        pkg_path = rp.get_path(splitPath[1])
                        subdir = self.path.replace('/PKG/' + splitPath[1], '')
                        filePath = pkg_path + subdir
                        f = open(filePath) 
                        self.send_response(200)
                        self.send_header('Content-Type', "type/html")
                        self.send_header('Content-Length', path.getsize(filePath))
                        self.end_headers()
                        self.wfile.write(f.read())
                        f.close()
                        return
                    except:
                        self.send_error(404,'Requested file not found: %s' % self.path)
                else:
                    self.send_error(404,'Not enough information supplied: %s' % self.path)
            return
        except IOError:
            self.send_error(404,'File Not Found: %s' % self.path)

def main():
    try:
        server = HTTPServer(('', 44644), MyHandler)
        print 'started httpserver on port 44644'
        server.serve_forever()
    except KeyboardInterrupt:
        print 'Control-C received, shutting down server'
        server.socket.close()

if __name__ == '__main__':
    main()
