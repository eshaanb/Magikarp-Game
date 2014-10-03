import tornado.ioloop
import tornado.web
from pusher import Config, Pusher

pusher = Pusher(config=Config(app_id=u'91495', key=u'69534632919c7bdab408', secret=u'0b8865f2f0e391031ff5'))

ips = set()

#p['test_channel'].trigger('my_event', {'message': 'hello world'})

class LoginHandler(tornado.web.RequestHandler):
    def post(self):
        ips.add(self.request.remote_ip)
        print(repr(self.request))
        self.finish()


class StartGameHandler(tornado.web.RequestHandler):
    def post(self):
        if self.get_argument("opponentip", None):
            pass

class GetDevicesHandler(tornado.web.RequestHandler):
    def get(self):
        tempIpList = ips.copy()
        myIp = self.request.remote_ip
        if myIp in tempIpList:
            ips.remove(myIp)
            ips.add(myIp+" (ME)")
        self.write(",".join(ips))
        self.finish()

application = tornado.web.Application([
    (r"/login", LoginHandler),
    (r"/getdevices", GetDevicesHandler),
])

if __name__ == "__main__":
    application.listen(8888)
    tornado.ioloop.IOLoop.instance().start()