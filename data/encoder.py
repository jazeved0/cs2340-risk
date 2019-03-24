import os
import re
import math
import json
import decimal
from itertools import groupby
from svgpathtools import parse_path
from svgpathtools.path import translate, scale
from scour.scour import cleanPath, sanitizeOptions
from xml.dom.minidom import parse, Node

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
INGEST_EXTENSION = '.svg'
OUTPUT_PATH = 'maps/'
OUTPUT_EXTENSION = '.json'
TOLERANCE = 15
STYLE_REGEX = '^[.](\\S+){.+stroke-width:([0-9.])+;.+}$'
PATH_TAGS = ['polygon', 'path']
LINE_ATTRIBUTES = ['x1', 'x2', 'y1', 'y2']
CONNECTION_TAGS = ['polyline', 'line']
POLYLINE_ATTRIBUTE = 'points'
CURVE_TENSION = 0.3
COORD_REGEX = '^(-?[0-9]+[.]?[0-9]*),(-?[0-9]+[.]?[0-9]*)$'
PREVIEW_SIZE = 100
DECIMAL_REGEX = re.compile(r"\d*\.\d+")
MIDDLE_LINE_START_REGEX = '( M [0-9]+(?:[.][0-9]+),[0-9]+(?:[.][0-9]+))'
EXP_FIX_REGEX = '([0-9]+(?:[.][0-9]+)(?:e-([0-9]+))+)'
ICON_ACCURACY = 2
# Important because of zooming in
MAP_ACCURACY = 4
SIZE_ACCURACY = 2


def main():
    ingest_files = [os.path.join(INGEST_PATH, f) for f in os.listdir(INGEST_PATH)
                    if not (os.path.isdir(os.path.join(INGEST_PATH, f)))]

    for source_path in ingest_files:
        if INGEST_EXTENSION not in source_path:
            continue
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
            territory_count = 0
            for group in groups:
                children = list(filter(lambda n: n.nodeType != n.TEXT_NODE, group.childNodes))
                if children:
                    text_elements = list(filter(lambda text: text.nodeName == 'text', children))
                    if text_elements:
                        territory_count += 1
                        territory = parse_territory(children)
                        if territory:
                            territories.append(territory)
                    else:
                        edge_groups.append(group)
            if territories:
                print('    - Successfully parsed {} out of {} territories'.format(
                    len(territories), territory_count))

            castle_count = 0
            for t in territories:
                if t[5] is not None:
                    castle_count += 1
            if castle_count > 0:
                print('    - Successfully parsed {} castles'.format(castle_count))

            edges = []
            water_connections = []
            edge_count = 0
            water_connection_count = 0
            if not edge_groups:
                print('    - Cannot find any <g> tags containing only lines! '
                      'This map will not have any graph edges or water connections')
            else:
                edge_group_styles_map = {}
                edge_group_styles = []
                for edge_group in edge_groups:
                    valid_nodes = list(filter(lambda s: s.nodeType != s.TEXT_NODE
                                              and 'class' in s.attributes, edge_group.childNodes))
                    group_styles = list(set(
                        map(lambda n: n.attributes['class'].value, valid_nodes)))
                    if len(group_styles) > 1:
                        print('    - Found more than one class in a single group: {}'
                              .format(group_styles))
                    elif len(group_styles) == 1:
                        edge_group_styles_map[group_styles[0]] = valid_nodes
                        edge_group_styles.append(group_styles[0])

                matching_styles = {style: line_styles[style] for style in line_styles
                                   if style in edge_group_styles}
                water_class = max(matching_styles, key=lambda k: matching_styles[k])
                water_connection_nodes = edge_group_styles_map[water_class]
                water_connection_count = len(water_connection_nodes)
                connection_styles = {style: edge_group_styles_map[style] for style
                                     in edge_group_styles_map if style != water_class}
                edge_nodes = [item for sublist in connection_styles.values() for item in sublist]
                edge_count = len(edge_nodes)
                centers = dict(list(map(lambda ter: (ter[0], ter[1]), territories)))

                # parse edges
                for edge_node in edge_nodes:
                    parsed = parse_edge(edge_node, centers)
                    if parsed and parsed[0] != -1 and parsed[1] != -1:
                        edges.append(parsed)

                # parse water connections
                for water_connection_node in water_connection_nodes:
                    parsed = parse_water_connection(water_connection_node, centers)
                    if parsed and parsed[0] != -1 and parsed[1] != -1:
                        water_connections.append(parsed)

            if edges:
                print('    - Successfully parsed {} out of {} edges'.format(
                    len(edges), edge_count))
            if water_connections:
                print('    - Successfully parsed {} out of {} water connections'.format(
                    len(water_connections), water_connection_count))

            # parse size
            size = parse_size(source_dom.getElementsByTagName('svg')[0])
            if size[0] != 0 and size[1] != 0:
                size = list(map(lambda f: round_numbers(f, SIZE_ACCURACY), size))
                print('    - Successfully parsed map bounds [{} x {}]'.format(
                    size[0], size[1]))

            # parse regions
            regions = parse_regions(territories)
            if len(regions) != 0:
                print('    - Successfully parsed {} regions from territory data'.format(
                    len(regions)))

            # serialize to JSON
            if len(territories) > 0 and len(edges) > 0:
                destination_path = os.path.join(OUTPUT_PATH, os.path.relpath(source_path, INGEST_PATH)
                                                .replace(INGEST_EXTENSION, OUTPUT_EXTENSION))
                write_to_file(destination_path, territories, edges, water_connections, size, regions)


def write_to_file(path, territories, edges, water_connections, size, regions):
    directory = os.path.dirname(path)
    if not os.path.exists(directory):
        os.makedirs(directory)
    with open(path, 'w+') as output_file:
        print('--------------------------------------------------------')
        print('Writing output to {}'.format(path))
        print()
        data = {
            'nodes': list(map(lambda t: serialize_territory(t), territories)),
            'edges': list(map(lambda e: {
                'a': e[0],
                'b': e[1]
            }, edges)),
            'size': {
                'a': size[0],
                'b': size[1]
            },
            'regions': list(map(lambda r: {
                'a': r[0],
                'b': r[1]
            }, regions)),
            'waterConnections': list(map(serialize_water_connection, water_connections))
        }
        json.dump(data, output_file, indent=2)


def serialize_territory(territory):
    base = {
        'node': territory[0],
        'center': {
            'x': territory[1][0],
            'y': territory[1][1]
        },
        'data': territory[2],
        'iconData': territory[4]
    }
    add = {}
    if territory[5] is not None:
        add = {
            'castle': territory[5]
        }
    return {**base, **add}


def serialize_water_connection(water_connection):
    base = {
        'a': water_connection[0],
        'b': water_connection[1]
    }
    add = {}
    if len(water_connection[2]) != 0:
        add = {
            'midpoints': water_connection[2],
            'tension': CURVE_TENSION
        }
    return {**base, **add}


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
    # parse data and class
    data = None
    class_value = None
    path_tag = next(filter(lambda t: t.tagName in PATH_TAGS, children), None)
    if path_tag:
        if 'class' in path_tag.attributes:
            class_value = path_tag.attributes['class'].value
        if path_tag.tagName == 'polygon':
            if path_tag.attributes['points']:
                data = clean_path(polygon_to_path(path_tag.attributes['points'].value), MAP_ACCURACY)
        else:
            if path_tag.attributes['d']:
                data = clean_path(path_tag.attributes['d'].value, MAP_ACCURACY)

    # parse icon data
    icon_data = None
    if data is not None:
        path = parse_path(data)
        x_min, x_max, y_min, y_max = path.bbox()
        w = x_max - x_min
        h = y_max - y_min
        kx = PREVIEW_SIZE / w
        ky = PREVIEW_SIZE / h
        k = min(kx, ky)
        origin = translate(path, complex(-x_min, -y_min))
        normalized = scale(origin, k)
        offset = ((PREVIEW_SIZE - (w * k)) / 2,
                  (PREVIEW_SIZE - (h * k)) / 2)
        centered = translate(normalized, complex(*offset))
        icon_data = clean_path(centered.d(), ICON_ACCURACY)

    # parse center
    center = None
    circle_tag = next(filter(lambda t: t.tagName == 'circle', children), None)
    if circle_tag:
        if circle_tag.attributes['cx'] and circle_tag.attributes['cy']:
            center = (round_numbers(circle_tag.attributes['cx'].value, MAP_ACCURACY),
                      round_numbers(circle_tag.attributes['cy'].value, MAP_ACCURACY))

    # parse castle
    castle = None
    rect_tag = next(filter(lambda t: t.tagName == 'rect', children), None)
    if rect_tag:
        if rect_tag.attributes['x'] and rect_tag.attributes['y']:
            castle = (round_numbers(rect_tag.attributes['x'].value, MAP_ACCURACY),
                      round_numbers(rect_tag.attributes['y'].value, MAP_ACCURACY))

    # parse text
    number = None
    text_tag = next(filter(lambda t: t.tagName == 'text', children), None)
    if text_tag:
        if text_tag.firstChild and text_tag.firstChild.nodeValue:
            number = int(text_tag.firstChild.nodeValue)

    if number is not None and center is not None and data is not None:
        return number, center, data, class_value, icon_data, castle
    else:
        print('    - Failed to parse node {} with center at ({}, {}) and path data [{}]'
              .format(number, center[0], center[1], data))
        return None


def clean_path(path, accuracy):
    path_obj = parse_path(path)
    no_midpoints = round_numbers(re.sub(MIDDLE_LINE_START_REGEX, '', path_obj.d()), accuracy)
    data = clean_path_data(remove_exp(no_midpoints), accuracy)
    return data


def clean_path_data(path_data, accuracy):
    # noinspection PyPep8Naming, PyMethodMayBeStatic
    class ElementWrapper:
        def __init__(self, data):
            self.data = data

        def getAttribute(self, attr):
            if attr == 'd':
                return self.data
            else:
                return ''

        def hasAttribute(self, attr):
            if attr == 'd':
                return True
            else:
                return False

        def setAttribute(self, attr, value):
            if attr == 'd':
                self.data = value
            else:
                return

        def nodeType(self):
            return Node.ELEMENT_NODE

    import scour
    scour.scour._num_path_segments_removed = 0
    scour.scour._num_bytes_saved_in_path_data = 0
    context = decimal.Context(prec=accuracy)
    scour.scour.scouringContext = context
    scour.scour.scouringContextC = context
    wrapper = ElementWrapper(path_data)
    cleanPath(wrapper, sanitizeOptions(None))
    return wrapper.data


def parse_edge(edge_node, centers):
    attributes = edge_node.attributes
    center_list = [centers[i] for i in sorted(centers.keys())]
    if attributes and all(a in attributes for a in LINE_ATTRIBUTES):
        start, end = parse_line(edge_node)
        start_node = find_rounded_tuple(center_list, start)
        end_node = find_rounded_tuple(center_list, end)
        return start_node, end_node
    else:
        return None


def parse_water_connection(water_connection_node, centers):
    center_list = [centers[i] for i in sorted(centers.keys())]
    if water_connection_node.nodeName in CONNECTION_TAGS:
        if water_connection_node.nodeName == 'polyline':
            # polyline
            if POLYLINE_ATTRIBUTE in water_connection_node.attributes:
                point_strings = water_connection_node.attributes[POLYLINE_ATTRIBUTE]\
                    .value.strip().split()
                points = list(map(to_point, point_strings))
                if len(points) >= 2:
                    start_node = find_rounded_tuple(center_list, points[0])
                    end_node = find_rounded_tuple(center_list, points[-1])
                    midpoints = points[1:-1] if len(points) > 2 else []
                    return start_node, end_node, midpoints
                else:
                    print('   - Failed parsing water connection node {} with points {}'
                          .format(water_connection_node.nodeName, points))
            else:
                print('   - Failed parsing water connection node {} with attributes {}'
                      .format(water_connection_node.nodeName,
                              list(map(lambda a: a.value, water_connection_node.attributes))))
        else:
            # line
            start, end = parse_line(water_connection_node)
            start_node = find_rounded_tuple(center_list, start)
            end_node = find_rounded_tuple(center_list, end)
            return start_node, end_node, []
    else:
        print('    - Failed parsing water connection node {}: unknown tag type')\
            .format(water_connection_node.nodeName)
    return None


def parse_line(node):
    start = (node.attributes['x1'].value, node.attributes['y1'].value)
    end = (node.attributes['x2'].value, node.attributes['y2'].value)
    return start, end


def parse_size(svg_node):
    if 'viewBox' in svg_node.attributes:
        view_box = svg_node.attributes['viewBox'].value
        coords = list(map(lambda s: float(s.strip()), view_box.strip().split()))
        if len(coords) == 4:
            min_bounds = (coords[0], coords[1])
            max_bounds = (coords[2], coords[3])
            width = max_bounds[0] - min_bounds[0]
            height = max_bounds[1] - min_bounds[1]
            return width, height
        else:
            print('    - Failed parsing viewBox: invalid value {}'.format(view_box))
    return 0, 0


def parse_regions(territories):
    num_class_dict = dict(map(lambda t: (t[0], t[3]), territories))
    class_list = [num_class_dict[i] for i in sorted(num_class_dict.keys())]
    class_runs = [x[0] for x in groupby(class_list)]
    if len(list(set(class_list))) != len(class_runs):
        print('    - Failed parsing region data: territory numbers are not contiguous '
              '(parsed list: {})'.format(class_list))
        return []
    else:
        full_runs = map(lambda c: list(
            {num: num_class_dict[num] for num in sorted(num_class_dict.keys()) if num_class_dict[num] == c}
            .keys()), class_runs)
        ranges = list(map(lambda l: (l[0], l[-1]), full_runs))
        return ranges


def to_point(string):
    match = re.search(COORD_REGEX, string.strip())
    if match:
        return match.group(1), match.group(2)
    else:
        return 0, 0


def find_rounded_tuple(source, target):
    tup1 = float(target[0])
    tup2 = float(target[1])
    nearest_dist = 0
    nearest = -1
    i = 0
    for node in source:
        node1 = float(node[0])
        node2 = float(node[1])
        dist = distance(tup1, tup2, node1, node2)
        if dist <= TOLERANCE:
            if nearest == -1 or dist < nearest_dist:
                nearest = i
                nearest_dist = dist
        i += 1
    return nearest


def distance(x1, y1, x2, y2):
    return math.sqrt((x1 - x2)**2 + (y1 - y2)**2)


def polygon_to_path(polygon_data):
    return 'M{}z'.format(polygon_data)


def round_numbers(text, places):
    if isinstance(text, float):
        text = str(text)

    def round_match(match):
        return pretty_float(("{:." + str(places) + "f}").format(float(match.group())))
    return re.sub(DECIMAL_REGEX, round_match, text)


def pretty_float(num):
    try:
        dec = decimal.Decimal(num)
    except decimal.InvalidOperation:
        return 'bad'
    tup = dec.as_tuple()
    delta = len(tup.digits) + tup.exponent
    digits = ''.join(str(d) for d in tup.digits)
    if delta <= 0:
        zeros = abs(tup.exponent) - len(tup.digits)
        val = '0.' + ('0'*zeros) + digits
    else:
        val = digits[:delta] + ('0'*tup.exponent) + '.' + digits[delta:]
    val = val.rstrip('0')
    if val[-1] == '.':
        val = val[:-1]
    if tup.sign:
        return '-' + val
    return val


def remove_exp(string):
    return re.sub(EXP_FIX_REGEX, '0', string)


# Run script
if __name__ == "__main__":
    main()
