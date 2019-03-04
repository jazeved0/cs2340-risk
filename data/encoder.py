import re
import math

# Ingests raw(ish) svg data from illustrator to nice json
# to do this, make circles located at the centers of the continents and
# include a labeled text block there with the number of the continent as its
# content. Next, group all three elements (continent path/polygon, text,
# and circle). Finally, open the generated svg and remove everything but
# the <polygon>/<path>, <circle>, and <text> tags (and ensure they are
# all at the root level).

# For the edge ingest, the svg can be directly used (make sure lines
# are close-ish to the centers of the nodes, adjust tolerance as necessary)

nodes = 48
continent_ingest = 'continent-export.svg'
graph_ingest = 'edges.svg'
output_path = 'skirmish-map.json'
tolerance = 10

def to_path(line):
    line = re.sub(
        r'(<polygon[\w\W]+?)points=(["\'])([\.\d, ]+?)(["\'])',
        '\g<1>d=\g<2>M\g<3>z\g<4>',
        line
    )
    line = re.sub(
        r'(<polyline[\w\W]+?)points=(["\'])([\.\d, ]+?)(["\'])',
        '\g<1>d=\g<2>M\g<3>\g<4>',
        line
    )
    line = re.sub(
        r'poly(gon|line)',
        'path',
        line
    )
    return line

def distance(x1, y1, x2, y2):
    return math.sqrt((x1 - x2)**2 + (y1 - y2)**2)

def find_rounded_tuple(list, tuple):
    tup1 = float(tuple[0])
    tup2 = float(tuple[1])
    nearestDistance = 0
    nearest = -1
    i = 0
    for node in list:
        node1 = float(node[0])
        node2 = float(node[1])
        dist = distance(tup1, tup2, node1, node2)
        if dist <= tolerance:
            if (nearest == -1 or dist < nearestDistance):
                nearest = i
                nearestDistance = dist
        i += 1
    return nearest

with open(output_path, 'w') as output_file:
    nodesList = [(-1, -1)]*(nodes)
    # continent data
    with open(continent_ingest, 'r') as input_file:
        first = True
        output_file.write('{"nodes":[')
        while True:
            path_poly = input_file.readline()
            if not path_poly:
                break
            circle = input_file.readline()
            text = input_file.readline()

            # path data
            start = len('<path d="')
            path_text = to_path(path_poly)
            data = path_text[start:path_text.index('"', start + 1)]

            # node center
            center = (-1, -1)
            center_search = re.search('<circle.+cx="([-0-9.]+)" cy="([-0-9.]+)".+\/>', circle)
            if center_search:
                center = (center_search.group(1), center_search.group(2))
            else:
                raise Exception('invalid node center')
                
            # node number
            number = -1
            number_search = re.search('<text.+>([0-9]+)<\/text>', text)
            if number_search:
                number = int(number_search.group(1))
            else:
                raise Exception('invalid node number')
            nodesList[number] = center
            
            if first:
                delimiter = ''
            else:
                delimiter = ','
            output_file.write('%s{"node":%s,"center":{"x":"%s","y":"%s"},"data":"%s"}' %
                (delimiter, number, center[0], center[1], data))
            first = False
        output_file.write('],"edges":[')

    # edge data
    with open(graph_ingest, 'r') as input_file:
        first = True
        for line in input_file:
            line_search = re.search(
                '<line.+x1="([-0-9.]+)" *y1="([-0-9.]+)" *x2="([-0-9.]+)" *y2="([-0-9.]+)" *\/>',
                    line)
            if line_search:
                x1 = line_search.group(1)
                y1 = line_search.group(2)
                x2 = line_search.group(3)
                y2 = line_search.group(4)
                a = (x1, y1)
                b = (x2, y2)
                nodeA = find_rounded_tuple(nodesList, a)
                nodeB = find_rounded_tuple(nodesList, b)
                if first:
                    delimiter = ''
                else:
                    delimiter = ','
                output_file.write('%s{"a":"%s","b":"%s"}' % (delimiter, nodeA, nodeB))
                first = False
        output_file.write(']}')