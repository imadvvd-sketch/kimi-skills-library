import sys, re, xml.etree.ElementTree as ET
NS='{http://www.w3.org/2000/svg}'
def f(e,k,d=0.0): return float(e.get(k,d))
def conv(e):
    t=e.tag.replace(NS,'')
    if t=='path': return e.get('d')
    if t=='line': return f"M{e.get('x1')},{e.get('y1')}L{e.get('x2')},{e.get('y2')}"
    if t in('circle','ellipse'):
        cx,cy=f(e,'cx'),f(e,'cy'); rx=f(e,'r') if t=='circle' else f(e,'rx'); ry=f(e,'r') if t=='circle' else f(e,'ry')
        return f"M{cx-rx},{cy}a{rx},{ry} 0 1,0 {2*rx},0a{rx},{ry} 0 1,0 {-2*rx},0z"
    if t=='rect':
        x,y,w,h=f(e,'x'),f(e,'y'),f(e,'width'),f(e,'height'); rx=f(e,'rx',e.get('ry',0)); ry=f(e,'ry',rx)
        if rx==0: return f"M{x},{y}h{w}v{h}h{-w}z"
        return (f"M{x+rx},{y}h{w-2*rx}a{rx},{ry} 0 0 1 {rx},{ry}v{h-2*ry}a{rx},{ry} 0 0 1 {-rx},{ry}"
                f"h{-(w-2*rx)}a{rx},{ry} 0 0 1 {-rx},{-ry}v{-(h-2*ry)}a{rx},{ry} 0 0 1 {rx},{-ry}z")
    if t in('polyline','polygon'):
        p=re.split(r'[\s,]+',e.get('points').strip()); d='M'+' L'.join(f"{p[i]},{p[i+1]}" for i in range(0,len(p),2))
        return d+('z' if t=='polygon' else '')
    raise SystemExit('unsupported '+t)
src,dst,mirror=sys.argv[1],sys.argv[2],len(sys.argv)>3
out=['<?xml version="1.0" encoding="utf-8"?>','<!-- Lucide (ISC). Generated from lucide-static; do not edit by hand. -->',
 '<vector xmlns:android="http://schemas.android.com/apk/res/android"','    android:width="24dp" android:height="24dp"',
 '    android:viewportWidth="24" android:viewportHeight="24"'+(' android:autoMirrored="true"' if mirror else '')+'>']
for e in ET.parse(src).getroot().iter():
    if e.tag==NS+'svg': continue
    out.append(f'    <path android:pathData="{conv(e)}" android:strokeColor="#FF000000" android:strokeWidth="2"\n        android:strokeLineCap="round" android:strokeLineJoin="round" />')
out.append('</vector>'); open(dst,'w').write('\n'.join(out)+'\n')
