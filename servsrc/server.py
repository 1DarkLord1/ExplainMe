import http.server, ssl
import socketserver
import logging
import json
import sqlite3
from database import Dbase
import requests

dbase = Dbase()

class Serv(http.server.BaseHTTPRequestHandler):
    def handle_add_user(self, message):
        dbase.add_user(message)
        self._set_headers()


    def handle_add_lecture(self, message):
        dbase.add_lecture(message)
        self._set_headers()


    def handle_get_lectures(self, message):
        lectures = dbase.get_lectures(message['begin'], message['end'])
        self._set_headers()
        response = json.dumps(lectures).encode('utf-8')
        logging.info(response)
        self.wfile.write(response)


    def zoom_create_meeting_request(self, lecture):
        jwt_token = 'Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhdWQiOm51bGwsImlzcyI6Im5WZlZ1WUNfVE51X09OQTRzTC13ZlEiLCJleHAiOjE1OTc1NDMwODQsImlhdCI6MTU5NjkzODI4NH0._x0QFJsokMDorcrlrh0B1SssP7dn4uJrpR3NwV6b6yY'
        heads = {'Authorization': jwt_token, 'Content-type': 'application/json'}
        body = {
            'topic': lecture['title'],
            'type': 2,
            'timezone': 'UTC',
            'agenda': lecture['description']
        }
        user_id = '3akLmEOxSaaFh6v_F9t4zQ'
        url = 'https://api.zoom.us/v2/users/' + user_id + '/meetings'
        response = requests.post(url, headers = heads, json = body)
        json_response = response.json()
        logging.info(json_response)


    def _set_headers(self):
        self.send_response(200)
        self.send_header('Content-type', 'application/json')
        self.end_headers()


    def do_HEAD(self):
        lect = {'title': 'aaaa', 'description': 'bbbb', 'author': 'cccc', 'time': 213134}
        self.zoom_create_meeting_request(lect)
        self._set_headers()


    def do_POST(self):
        logging.info('POST request')
        message = None

        try:
            length = int(self.headers.get('content-length'))
            message = json.loads(self.rfile.read(length))
        except Exception:
            self.send_response(400)
            logging.error('Bad request format')
            return

        logging.info(message)

        if message.get('type') == 'add_user':
            self.handle_add_user(message)
        elif message.get('type') == 'add_lecture':
            self.handle_add_lecture(message)
        elif message.get('type') == 'get_lectures':
            self.handle_get_lectures(message)



def run(log_path, server_class = http.server.HTTPServer, handler_class = Serv, port = 8000):
    logging.basicConfig(level = logging.INFO)
    #logging.basicConfig(filename = log_path, filemode = 'w', level = logging.INFO)
    serv_ip = '0.0.0.0' #'145.255.11.21'
    httpd = server_class((serv_ip, port), handler_class)

    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        logging.info('Server interrupted')

    self.dbase.close_db()
    httpd.server_close()
    logging.info('Stopping server...')