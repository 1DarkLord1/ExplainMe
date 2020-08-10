import sqlite3, os
import pandas as pd

def gen_path(paths):
    full_path = os.path.dirname(os.path.abspath(__file__))
    for path in paths:
        full_path = os.path.join(full_path, path)
    return full_path

class Dbase:
    def __init__(self):
        self.db = self.connect_db()
        self.curs = self.db.cursor()
        self.create_users_table()
        self.create_lectures_table()
        #self.curs.execute('SELECT * FROM users')
        #print(self.curs.fetchall())

    def connect_db(self):
        sqlite_db = sqlite3.connect(gen_path(['..', 'data', 'app_data.db']))
        return sqlite_db


    def create_users_table(self):
        self.curs.execute('''CREATE TABLE IF NOT EXISTS users (google_mail TEXT PRIMARY KEY, photo_url TEXT, display_name TEXT)''')
        self.db.commit()


    def add_user(self, user_info):
        self.curs.execute('INSERT OR IGNORE INTO users (google_mail, photo_url, display_name) VALUES (?, ?, ?)',
        [user_info['google_mail'], user_info['photo_url'], user_info['display_name']])
        self.db.commit()


    def create_lectures_table(self):
        self.curs.execute('''CREATE TABLE IF NOT EXISTS lectures (title TEXT, description TEXT,
        author TEXT, time BIGINT, zoom_url TEXT)''')
        self.db.commit()


    def add_lecture(self, lecture_info):
        self.curs.execute('INSERT OR IGNORE INTO lectures (title, description, author, time, zoom_url) VALUES (?, ?, ?, ?, ?)',
        [lecture_info['title'], lecture_info['description'], lecture_info['author'], lecture_info['time'], lecture_info['zoom_url']])
        self.db.commit()


    def get_lectures(self, i, j):
        self.curs.execute('SELECT * FROM lectures WHERE time > ? ORDER BY time LIMIT ? OFFSET ?', [1, j - i, i])
        raw_data = self.curs.fetchall()
        return [{'title': note[0], 'description': note[1], 'author': note[2], 'time': note[3], 'zoom_url': note[4]} for note in raw_data]


    def get_user_by_mail(self, google_mail):
        self.curs.execute('SELECT * FROM users WHERE google_mail = ?', [google_mail])
        raw_data = self.curs.fetchone()
        if raw_data == None:
            return None
        return {'google_mail': raw_data[0], 'photo_url': raw_data[1], 'display_name': raw_data[2]}


    def close_db(self):
        self.db.close()
