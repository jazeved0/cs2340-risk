import os
import re
from xml.dom.minidom import parse

# Ingests raw svg data and converts it to json for injection on the back and
# front ends. To ensure successful parses, the svg must be formatted in a
# specific way:
#
#   - For each territory, there must be:
#       1. a <path> or <polygon> defining the shape
#       2. a <text> with the 0-based numeric ID as its content
#       3. a <circle> with its center being the vertex
#       4. the above four shapes in a single group (<g>) tag
#
#   - For the edges, there must be a group with one or more <line>
#
#   - For the water connections, there can be an optional group of <line> or
#     <polyline> tags (*with their stroke width greater than the edges*)

INGEST_PATH = 'source/'
OUTPUT_PATH = 'maps/'
TOLERANCE = 15
STYLE_REGEX = '^[.](\\S+){.+stroke-width:([0-9.])+;.+}$'
PATH_TAGS = ['polygon', 'path']


def main():
    ingest_files = [os.path.join(INGEST_PATH, f) for f in os.listdir(INGEST_PATH)
                    if not (os.path.isdir(os.path.join(INGEST_PATH, f)))]

    for source_path in ingest_files:
        print('Parsing {}'.format(source_path))
        with open(source_path, 'r') as source_file:
            source_dom = parse(source_file)
            groups = source_dom.getElementsByTagName('g')

            # parse style elements and look for ones that specify stroke width
            style_elements = source_dom.getElementsByTagName('style')
            line_styles = parse_styles(style_elements)
            if not line_styles:
                print('    - Cannot find definition of <style>! This map will '
                      'not support any water connections')

            # parse all group tags
            territories = []
            edge_groups = []
            for group in groups:
                children = list(filter(lambda n: n.nodeType != n.TEXT_NODE, group.childNodes))
                if children:
                    text_elements = list(filter(lambda t: t.nodeName == 'text', children))
                    if text_elements:
                        territory = parse_territory(children)
                        if territory:
                            territories.append(territory)
                    else:
                        edge_groups.append(group)

            if not edge_groups:
                print('    - Cannot find any <g> tags containing only lines! '
                      'This map will not have any graph edges or water connections')
            else:
                edge_group_styles = []
                for edge_group in edge_groups:
                    # TODO map to class, then flatten to unique, then ensure only one, build map to children,
                    #  find maximum, merge together other sublists, interpret merged as edges, maxiumum list
                    #  as water connections
                    group_styles = list(map(lambda s: 0,
                                            filter(lambda s: s.attribute['class'], edge_group.childNodes)))


def parse_styles(style_elements):
    style_map = {}
    if style_elements:
        for style_element in style_elements:
            style_content = style_element.firstChild.nodeValue
            if style_content:
                styles = list(
                    map(lambda s: s.strip(), style_content.splitlines()))
                for style in styles:
                    style_search = re.search(STYLE_REGEX, style)
                    if style_search:
                        style_map[style_search.group(1)] = style_search.group(2)
    return style_map


def parse_territory(children):
    # parse data
    data = None
    path_tag = next(filter(lambda t: t.tagName in PATH_TAGS, children), None)
    if path_tag:
        if path_tag.tagName == 'polygon':
            if path_tag.attributes['points']:
                data = polygon_to_path(path_tag.attributes['points'].value)
        else:
            if path_tag.attributes['d']:
                data = path_tag.attributes['d'].value

    # parse center
    center = None
    circle_tag = next(filter(lambda t: t.tagName == 'circle', children), None)
    if circle_tag:
        if circle_tag.attributes['cx'] and circle_tag.attributes['cy']:
            center = (float(circle_tag.attributes['cx'].value),
                      float(circle_tag.attributes['cy'].value))

    # parse text
    number = None
    text_tag = next(filter(lambda t: t.tagName == 'text', children), None)
    if text_tag:
        if text_tag.firstChild and text_tag.firstChild.nodeValue:
            number = int(text_tag.firstChild.nodeValue)

    if number and center and data:
        return number, center, data
    else:
        return None


def polygon_to_path(polygon_data):
    polygon_data = re.sub(
        r'(<polygon[\w\W]+?)points=(["\'])([.\d, ]+?)(["\'])',
        '\\g<1>d=\\g<2>M\\g<3>z\\g<4>',
        polygon_data
    )
    polygon_data = re.sub(
        r'(<polyline[\w\W]+?)points=(["\'])([.\d, ]+?)(["\'])',
        '\\g<1>d=\\g<2>M\\g<3>\\g<4>',
        polygon_data
    )
    polygon_data = re.sub(
        r'poly(gon|line)',
        'path',
        polygon_data
    )
    return polygon_data


# def to_path(line):
#     line = re.sub(
#         r'(<polygon[\w\W]+?)points=(["\'])([\.\d, ]+?)(["\'])',
#         '\g<1>d=\g<2>M\g<3>z\g<4>',
#         line
#     )
#     line = re.sub(
#         r'(<polyline[\w\W]+?)points=(["\'])([\.\d, ]+?)(["\'])',
#         '\g<1>d=\g<2>M\g<3>\g<4>',
#         line
#     )
#     line = re.sub(
#         r'poly(gon|line)',
#         'path',
#         line
#     )
#     return line
#
#
# def distance(x1, y1, x2, y2):
#     return math.sqrt((x1 - x2)**2 + (y1 - y2)**2)
#
#
# def find_rounded_tuple(list, tuple):
#     tup1 = float(tuple[0])
#     tup2 = float(tuple[1])
#     nearestDistance = 0
#     nearest = -1
#     i = 0
#     for node in list:
#         node1 = float(node[0])
#         node2 = float(node[1])
#         dist = distance(tup1, tup2, node1, node2)
#         if dist <= tolerance:
#             if (nearest == -1 or dist < nearestDistance):
#                 nearest = i
#                 nearestDistance = dist
#         i += 1
#     return nearest
#
#
# with open(output_path, 'w') as output_file:
#     nodesList = [(-1, -1)]*(nodes)
#     # continent data
#     with open(continent_ingest, 'r') as input_file:
#         first = True
#         output_file.write('{"nodes":[')
#         while True:
#             path_poly = input_file.readline()
#             if not path_poly:
#                 break
#             circle = input_file.readline()
#             text = input_file.readline()
#
#             # path data
#             start = len('<path d="')
#             path_text = to_path(path_poly)
#             data = path_text[start:path_text.index('"', start + 1)]
#
#             # node center
#             center = (-1, -1)
#             center_search = re.search('<circle.+cx="([-0-9.]+)" cy="([-0-9.]+)".+\/>', circle)
#             if center_search:
#                 center = (center_search.group(1), center_search.group(2))
#             else:
#                 raise Exception('invalid node center')
#
#             # node number
#             number = -1
#             number_search = re.search('<text.+>([0-9]+)<\/text>', text)
#             if number_search:
#                 number = int(number_search.group(1))
#             else:
#                 raise Exception('invalid node number')
#             nodesList[number] = center
#
#             if first:
#                 delimiter = ''
#             else:
#                 delimiter = ','
#             output_file.write('%s{"node":%s,"center":{"x":"%s","y":"%s"},"data":"%s"}' %
#                 (delimiter, number, center[0], center[1], data))
#             first = False
#         output_file.write('],"edges":[')
#
#     # edge data
#     with open(graph_ingest, 'r') as input_file:
#         first = True
#         for line in input_file:
#             line_search = re.search(
#                 '<line.+x1="([-0-9.]+)" *y1="([-0-9.]+)" *x2="([-0-9.]+)" *y2="([-0-9.]+)" *\/>',
#                     line)
#             if line_search:
#                 x1 = line_search.group(1)
#                 y1 = line_search.group(2)
#                 x2 = line_search.group(3)
#                 y2 = line_search.group(4)
#                 a = (x1, y1)
#                 b = (x2, y2)
#                 nodeA = find_rounded_tuple(nodesList, a)
#                 nodeB = find_rounded_tuple(nodesList, b)
#                 if first:
#                     delimiter = ''
#                 else:
#                     delimiter = ','
#                 output_file.write('%s{"a":"%s","b":"%s"}' % (delimiter, nodeA, nodeB))
#                 first = False
#         output_file.write(']}')

# Run script
if __name__ == "__main__":
    main()
