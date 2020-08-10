import http.server, ssl
import socketserver
import logging
import json
import sqlite3
from database import Dbase
import requests
from datetime import datetime

dbase = Dbase()

class Serv(http.server.BaseHTTPRequestHandler):
    def handle_add_user(self, message):
        dbase.add_user(message)
        self._set_headers()
        logging.info('User {0} added'.format(message['google_mail']))


    def handle_add_lecture(self, message):
        zoom_url = self.zoom_create_meeting(message)
        message['zoom_url'] = zoom_url
        dbase.add_lecture(message)
        logging.info('Lecture {0} added'.format(message['title']))
        self._set_headers()


    def handle_get_lectures(self, message):
        lectures = dbase.get_lectures(message['begin'], message['end'])
        self._set_headers()
        response = json.dumps(lectures).encode('utf-8')
        self.wfile.write(response)
        logging.info(response)


    def handle_get_user_by_mail(self, message):
        user = dbase.get_user_by_mail(message['google_mail'])
        if user == None:
            self.send_response(400)
            logging.info('User {0} not found'.format(message['google_mail']))
            return
        self._set_headers()
        response = json.dumps(user).encode('utf-8')
        self.wfile.write(response)
        logging.info(response)


    def zoom_create_meeting(self, lecture):
        jwt_token = '<JWT token>'
        heads = {'Authorization': jwt_token, 'Content-type': 'application/json'}
        body = {
            'topic': lecture['title'],
            'type': 2,
            'start_time': datetime.utcfromtimestamp(lecture['time']).strftime('%Y-%m-%dT%H:%M:%S'),
            'timezone': 'UTC',
            'agenda': lecture['description'],
            "settings": {
                'join_before_host': True
            }
        }
        logging.info(body)
        user_id = '<userId>'
        url = 'https://api.zoom.us/v2/users/' + user_id + '/meetings'
        response = requests.post(url, headers = heads, json = body)
        json_response = response.json()
        logging.info(json_response)
        return json_response['start_url']


    def _set_headers(self):
        self.send_response(200)
        self.send_header('Content-type', 'application/json')
        self.end_headers()


    def do_HEAD(self):
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

        handlers = {
            'add_user': self.handle_add_user,
            'add_lecture': self.handle_add_lecture,
            'get_lectures': self.handle_get_lectures,
            'get_user_by_mail': self.handle_get_user_by_mail
        }

        handlers.get(message.get('type'))(message)


def run(log_path, server_class = http.server.HTTPServer, handler_class = Serv, port = 8000):
    logging.basicConfig(level = logging.INFO)
    #logging.basicConfig(filename = log_path, filemode = 'w', level = logging.INFO)
    serv_ip = '0.0.0.0'
    httpd = server_class((serv_ip, port), handler_class)

    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        logging.info('Server interrupted')

    self.dbase.close_db()
    httpd.server_close()
    logging.info('Stopping server...')
