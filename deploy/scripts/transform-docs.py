from pathlib import Path
import re
import os
import sys

DOCS_ROOT = sys.argv[1]
TITLE_REGEX = r'<title>[ ]*<[/]title>'
TITLE_REPLACE = '''<title>CS 2340 Risk | API Docs - {0}</title>
<link rel="shortcut icon" type="image/png" href="/static/images/favicon.png">
<meta name="theme-color" content="#103a51">
<meta name="description" content="CS 2340 Risk API docs for {0}">
<meta name="robots" content="index, follow">
<meta property="og:type" content="website"/>
<meta property="og:title" content="CS 2340 Risk | API Docs - {0}"/>
<meta property="og:description" content="CS 2340 Risk API docs for {0}"/>
<meta property="og:image" content="/static/images/logo.png"/>
<meta property="og:url" content="https://riskgame.ga"/>
<meta property="og:site_name" content="CS 2340 Risk"/>
<link rel="stylesheet" href="/static/docs/api.css">'''
REMOVE = '<meta name="description" content="" />'
LINK_REGEX = r'(<a.*href=".+)([.]html)(.*".*?>)'
LINK_REPLACE_REGEX = r'\g<1>\g<3>'


def main():
    print('                 - Searching {}'.format(DOCS_ROOT))
    file_path_list = Path(DOCS_ROOT).glob('**/*.html')
    for path in file_path_list:
        # because path is object not string
        path_in_str = str(path)
        title = resolve_title(path_in_str)
        print("                 - Transforming {}".format(path_in_str))
        with open(path_in_str, 'r+') as file:
            html = file.read()
            replace = TITLE_REPLACE.format(title)
            html = re.sub(TITLE_REGEX, replace, html)
            html = re.sub(LINK_REGEX, LINK_REPLACE_REGEX, html)
            file.seek(0)
            file.write(html)
            file.truncate()


def resolve_title(path):
    filename = os.path.splitext(os.path.basename(path))[0]
    title = filename.replace('$', '')
    if title == 'index':
        parent = os.path.abspath(os.path.join(path, os.pardir))
        return os.path.basename(os.path.normpath(parent)).capitalize()
    else:
        return title



# Run script
if __name__ == '__main__':
    main()
