#!/usr/bin/env python3
"""Minimal SVG -> Android VectorDrawable converter for StudIN design assets.

Supports: <path>, <circle>, <ellipse>, <rect rx>, <line>, <g transform="translate/scale/rotate">,
fill / stroke / stroke-width / stroke-linecap / stroke-linejoin / opacity / fill-opacity / stroke-opacity,
and linearGradient / radialGradient (userSpaceOnUse) fills via aapt:attr.
Usage: svg2vd.py in.svg out.xml [--width DP] [--height DP] [--tint-free]
"""
import re, sys, xml.etree.ElementTree as ET

NS = '{http://www.w3.org/2000/svg}'

def num(v, d=0.0):
    if v is None: return d
    return float(re.sub(r'[a-z%]+$', '', v.strip()))

def color(c, opacity=1.0):
    c = c.strip()
    if c.startswith('#'):
        h = c[1:]
        if len(h) == 3: h = ''.join(ch * 2 for ch in h)
        a = round(max(0, min(1, opacity)) * 255)
        return '#%02X%s' % (a, h.upper()) if a < 255 else '#' + h.upper()
    named = {'white': '#FFFFFF', 'black': '#000000'}
    if c in named: return color(named[c], opacity)
    raise ValueError('unsupported colour ' + c)

def style_attrs(el, inherited):
    a = dict(inherited)
    for k in ['fill', 'stroke', 'stroke-width', 'stroke-linecap', 'stroke-linejoin', 'opacity', 'fill-opacity', 'stroke-opacity', 'fill-rule']:
        if el.get(k) is not None: a[k] = el.get(k)
    if el.get('style'):
        for part in el.get('style').split(';'):
            if ':' in part:
                k, v = part.split(':', 1); a[k.strip()] = v.strip()
    return a

def circle_path(cx, cy, rx, ry):
    return (f'M{cx - rx:g},{cy:g} a{rx:g},{ry:g} 0 1,0 {2 * rx:g},0 a{rx:g},{ry:g} 0 1,0 {-2 * rx:g},0 Z')

def rect_path(x, y, w, h, rx, ry):
    if rx <= 0 and ry <= 0:
        return f'M{x:g},{y:g} h{w:g} v{h:g} h{-w:g} Z'
    rx = min(rx or ry, w / 2); ry = min(ry or rx, h / 2)
    return (f'M{x + rx:g},{y:g} H{x + w - rx:g} A{rx:g},{ry:g} 0 0 1 {x + w:g},{y + ry:g} V{y + h - ry:g} '
            f'A{rx:g},{ry:g} 0 0 1 {x + w - rx:g},{y + h:g} H{x + rx:g} A{rx:g},{ry:g} 0 0 1 {x:g},{y + h - ry:g} '
            f'V{y + ry:g} A{rx:g},{ry:g} 0 0 1 {x + rx:g},{y:g} Z')

class Conv:
    def __init__(self, root):
        self.grads = {}
        for g in root.iter():
            tag = g.tag.replace(NS, '')
            if tag in ('linearGradient', 'radialGradient'):
                stops = [(num(s.get('offset')), s.get('stop-color'), num(s.get('stop-opacity'), 1.0)) for s in g if s.tag.replace(NS, '') == 'stop']
                self.grads[g.get('id')] = (tag, dict(g.attrib), stops)
        self.out = []

    def gradient_xml(self, attr, gid, opacity, indent):
        tag, at, stops = self.grads[gid]
        pad = ' ' * indent
        if tag == 'linearGradient':
            head = (f'{pad}<gradient android:type="linear" android:startX="{num(at.get("x1")):g}" android:startY="{num(at.get("y1")):g}" '
                    f'android:endX="{num(at.get("x2")):g}" android:endY="{num(at.get("y2")):g}">')
        else:
            head = (f'{pad}<gradient android:type="radial" android:centerX="{num(at.get("cx")):g}" android:centerY="{num(at.get("cy")):g}" '
                    f'android:gradientRadius="{num(at.get("r")):g}">')
        items = '\n'.join(f'{pad}    <item android:offset="{o:g}" android:color="{color(c, so * opacity)}"/>' for o, c, so in stops)
        return (f'{pad[:-4]}<aapt:attr name="{attr}">\n{head}\n{items}\n{pad}</gradient>\n{pad[:-4]}</aapt:attr>')

    def emit_path(self, d, a, indent):
        pad = ' ' * indent
        op = num(a.get('opacity'), 1.0)
        fill = a.get('fill', '#000000'); stroke = a.get('stroke', 'none')
        attrs = [f'android:pathData="{" ".join(d.split())}"']
        children = []
        if fill != 'none':
            fo = op * num(a.get('fill-opacity'), 1.0)
            m = re.match(r'url\(#(.+)\)', fill)
            if m: children.append(self.gradient_xml('android:fillColor', m.group(1), fo, indent + 8))
            else: attrs.append(f'android:fillColor="{color(fill, fo)}"')
            if a.get('fill-rule') == 'evenodd': attrs.append('android:fillType="evenOdd"')
        if stroke != 'none':
            so = op * num(a.get('stroke-opacity'), 1.0)
            m = re.match(r'url\(#(.+)\)', stroke)
            if m: children.append(self.gradient_xml('android:strokeColor', m.group(1), so, indent + 8))
            else: attrs.append(f'android:strokeColor="{color(stroke, so)}"')
            attrs.append(f'android:strokeWidth="{num(a.get("stroke-width"), 1):g}"')
            if a.get('stroke-linecap'): attrs.append(f'android:strokeLineCap="{a["stroke-linecap"]}"')
            if a.get('stroke-linejoin'): attrs.append(f'android:strokeLineJoin="{a["stroke-linejoin"]}"')
        sep = '\n' + pad + '    '
        if children:
            self.out.append(f'{pad}<path{sep}{sep.join(attrs)}>\n' + '\n'.join(children) + f'\n{pad}</path>')
        else:
            self.out.append(f'{pad}<path{sep}{sep.join(attrs)} />')

    def walk(self, el, inherited, indent):
        for ch in el:
            tag = ch.tag.replace(NS, '')
            if tag in ('defs', 'title', 'desc', 'linearGradient', 'radialGradient', 'text') or callable(ch.tag): continue
            a = style_attrs(ch, inherited)
            tr = ch.get('transform')
            opened = False
            if tr:
                g = []
                for fn, args in re.findall(r'(\w+)\(([^)]*)\)', tr):
                    v = [float(x) for x in re.split(r'[ ,]+', args.strip()) if x]
                    if fn == 'translate': g += [f'android:translateX="{v[0]:g}"', f'android:translateY="{(v[1] if len(v) > 1 else 0):g}"']
                    elif fn == 'scale': g += [f'android:scaleX="{v[0]:g}"', f'android:scaleY="{(v[1] if len(v) > 1 else v[0]):g}"']
                    elif fn == 'rotate':
                        g.append(f'android:rotation="{v[0]:g}"')
                        if len(v) == 3: g += [f'android:pivotX="{v[1]:g}"', f'android:pivotY="{v[2]:g}"']
                    else: raise ValueError('unsupported transform ' + fn)
                self.out.append(' ' * indent + '<group ' + ' '.join(g) + '>'); opened = True; indent += 4
            if tag == 'g':
                self.walk(ch, a, indent)
            elif tag == 'path':
                self.emit_path(ch.get('d'), a, indent)
            elif tag == 'circle':
                r = num(ch.get('r')); self.emit_path(circle_path(num(ch.get('cx')), num(ch.get('cy')), r, r), a, indent)
            elif tag == 'ellipse':
                self.emit_path(circle_path(num(ch.get('cx')), num(ch.get('cy')), num(ch.get('rx')), num(ch.get('ry'))), a, indent)
            elif tag == 'rect':
                self.emit_path(rect_path(num(ch.get('x')), num(ch.get('y')), num(ch.get('width')), num(ch.get('height')),
                                         num(ch.get('rx')), num(ch.get('ry'), num(ch.get('rx')))), a, indent)
            elif tag == 'line':
                self.emit_path(f'M{num(ch.get("x1")):g},{num(ch.get("y1")):g} L{num(ch.get("x2")):g},{num(ch.get("y2")):g}', a, indent)
            else:
                raise ValueError('unsupported element ' + tag)
            if opened:
                indent -= 4; self.out.append(' ' * indent + '</group>')

def convert(src, dst, width=None, height=None):
    root = ET.parse(src).getroot()
    vb = [float(x) for x in root.get('viewBox').split()]
    vw, vh = vb[2], vb[3]
    w = width or vw; h = height or (vh * w / vw)
    c = Conv(root)
    c.walk(root, {}, 4)
    body = '\n'.join(c.out)
    needs_aapt = 'aapt:attr' in body
    ns = ' xmlns:aapt="http://schemas.android.com/aapt"' if needs_aapt else ''
    xml = (f'<?xml version="1.0" encoding="utf-8"?>\n<!-- Generated from design/{src.split("design/")[-1]} by design/svg2vd.py – edit the SVG, not this file. -->\n'
           f'<vector xmlns:android="http://schemas.android.com/apk/res/android"{ns}\n'
           f'    android:width="{w:g}dp"\n    android:height="{h:g}dp"\n'
           f'    android:viewportWidth="{vw:g}"\n    android:viewportHeight="{vh:g}">\n{body}\n</vector>\n')
    open(dst, 'w').write(xml)

if __name__ == '__main__':
    args = sys.argv[1:]
    w = h = None
    if '--width' in args: i = args.index('--width'); w = float(args[i + 1]); del args[i:i + 2]
    if '--height' in args: i = args.index('--height'); h = float(args[i + 1]); del args[i:i + 2]
    convert(args[0], args[1], w, h)
