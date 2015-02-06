import tornado.ioloop, tornado.web, tornado.websocket, json, tornado.tcpserver, tornado.ioloop, tornado.iostream, \
    socket, threading
from pusher import Config, Pusher

pusher = Pusher(config=Config(app_id=u'91495', key=u'69534632919c7bdab408', secret=u'0b8865f2f0e391031ff5'))

PORT = 7011
HOST = '10.1.10.13'
games = {}
socketDict = {}

# p['test_channel'].trigger('my_event', {'message': 'hello world'})
#
# class LoginHandler(tornado.web.RequestHandler):
# def post(self):
# ipToAdd = self.request.remote_ip
#         if (HOST == ipToAdd):
#             if (dummyip is None):
#                 ipToAdd = '1.2.3.4.5'
#             else:
#                 ipToAdd = '55.55.55.55'
#         ips.add(ipToAdd)
#         print("logging in", repr(ipToAdd))
#         self.finish()
#
#
# class StartGameHandler(tornado.web.RequestHandler):
#     def post(self):
#         if self.get_argument("opponentip", None):
#             games[self.request.remote_ip] = self.get_argument("opponentip")
#             self.finish()

# class GetDevicesHandler(tornado.web.RequestHandler):
#     def get(self):
#         tempIpList = ips.copy()
#         myIp = self.request.remote_ip
#         if myIp in tempIpList:
#             ips.remove(myIp)
#             ips.add(myIp+" (ME)")
#         self.write(",".join(ips))
#         self.finish()

# application = tornado.web.Application([
#     (r"/login", LoginHandler),
#     (r"/getdevices", GetDevicesHandler),
# ])

def sendJsonToSocket(socket, jsonToSend):
    print('sending action: ', json.dumps(jsonToSend), 'to: ' + repr(socket.getpeername()))
    socket.send(bytes(json.dumps(jsonToSend), 'UTF-8'))


def sendIpListToAll(ip):
    for socket in socketDict.values():
        ipList = list(socketDict.keys())
        socketip = socket.getpeername()
        for index, item in enumerate(ipList):
            print('checking: ', item, ' against: ', socketip[0] + '/' + str(socketip[1]))
            if item == (socketip[0] + '/' + str(socketip[1])):
                ipList[index] = item + ' -- ME'
                break
        jsonToSend = {"action": 'ipList', "ipList": ipList}
        sendJsonToSocket(socket, jsonToSend)


def handle_data(socket_, ip):
    while 1:
        message = socket_.recv(1024)
        print('got message from: ' + repr(ip) + ' -- ' + repr(message))
        message = message.decode("utf-8").strip()
        if not message:
            disconnectedIp = ip[0] + '/' + str(ip[1])
            socketDict.pop(disconnectedIp, None)
            foundKey = None
            for key, value in games.items():
                if key == disconnectedIp or value == disconnectedIp:
                    foundKey = key
            if foundKey is not None:
                games.pop(foundKey)
            sendIpListToAll(ip)
            socket_.close()
            break
        messageMap = json.loads(message)
        action = messageMap["action"]
        if action == 'connect':
            oppIp = messageMap['iptochallenge']
            games[ip[0] + '/' + str(ip[1])] = oppIp
            challengeJson = {'action': 'challenge', 'from': ip[0] + '/' + str(ip[1])}
            sendJsonToSocket(socketDict[oppIp], challengeJson)
        elif action == 'game_ended':
            pass
            #TODO forward game ended to opponent
        elif action == 'fall':
            jsonToSend = {'action' : 'fall'}
            sendJsonToSocket(socketDict[games[ip[0] + '/' + str(ip[1])]], jsonToSend)
        elif action == 'jump_fast':
            jsonToSend = {'action' : 'jump_fast'}
            sendJsonToSocket(socketDict[games[ip[0] + '/' + str(ip[1])]], jsonToSend)
        elif action == 'jump_slow':
            jsonToSend = {'action' : 'jump_slow'}
            sendJsonToSocket(socketDict[games[ip[0] + '/' + str(ip[1])]], jsonToSend)
        elif action == 'ipList':
            sendIpListToAll(ip)
        else:
            forwardIp = None
            sendingIp = ip[0] + '/' + str(ip[1])
            for key, value in games.items():
                if key == sendingIp:
                    forwardIp = value
                    break
                elif value == sendingIp:
                    forwardIp = key
                    break
            if forwardIp is None:
                return
            elif action == 'sync_opp':
                jsonToSend = {'action': 'jump_fast'}
                sendJsonToSocket(socketDict[forwardIp], jsonToSend)
            elif action == 'add_point':
                jsonToSend = {'action': 'jump_slow'}
                sendJsonToSocket(socketDict[forwardIp], jsonToSend)


def game_listen():
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.bind((HOST, PORT))
    s.listen(5)
    while 1:
        client_socket, address = s.accept()
        print("connected with: ", address)
        socketDict[address[0] + '/' + str(address[1])] = client_socket
        # if myIp == HOST:
        #     if dummyip is None:
        threading.Thread(target=handle_data, args=(client_socket, address)).start()


if __name__ == "__main__":
    game_listen()
    # threading.Thread(target=game_listen).start()
    # application.listen(PORT)
    # tornado.ioloop.IOLoop.instance().start()