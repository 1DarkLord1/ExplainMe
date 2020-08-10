import sys, os

from servsrc import server


def gen_path(paths):
    full_path = os.path.dirname(os.path.abspath(__file__))
    for path in paths:
        full_path = os.path.join(full_path, path)
    return full_path

sys.path.append(gen_path(['servsrc']))

if __name__ == '__main__':
    server.run(8000)
