import json, urllib.parse
from playwright.sync_api import sync_playwright
BD = [  # lo que "tiene" el servicio de letras
 {"trackName":"Io So Che Tu","artistName":"Davide Esposito","duration":30,"syncedLyrics":"[00:01.00] linea esposito","plainLyrics":"linea esposito"},
 {"trackName":"Me Tienes","artistName":"Eduardo Capetillo","duration":31,"syncedLyrics":"","plainLyrics":"letra capetillo"},
 {"trackName":"Miénteme","artistName":"Camila","duration":29,"syncedLyrics":"[00:01.00] letra mienteme","plainLyrics":"letra mienteme"},
 {"trackName":"Bien Etiquetada","artistName":"Grupo Bueno","duration":30,"syncedLyrics":"[00:01.00] letra buena","plainLyrics":"letra buena"},
 {"trackName":"Otra Cosa Popular","artistName":"Famoso","duration":200,"syncedLyrics":"[00:01.00] NO DEBE SALIR","plainLyrics":"NO DEBE SALIR"},
 {"trackName":"Me Tienes","artistName":"Otro Cantante","duration":240,"syncedLyrics":"[00:01.00] VERSION EQUIVOCADA","plainLyrics":"VERSION EQUIVOCADA"},
]
import unicodedata, re
def n(t): return re.sub(r'[^a-z0-9ñ]+',' ',''.join(ch for ch in unicodedata.normalize('NFD',t.lower()) if unicodedata.category(ch)!='Mn')).strip()
llamadas=[]
def lrc(route):
    u=urllib.parse.urlparse(route.request.url); q=dict(urllib.parse.parse_qsl(u.query)); llamadas.append((u.path.split('/')[-1], q))
    h={'Access-Control-Allow-Origin':'*'}
    if u.path.endswith('/get'):
        for x in BD:
            if n(x['trackName'])==n(q.get('track_name','')) and n(x['artistName'])==n(q.get('artist_name','')):
                return route.fulfill(status=200,content_type='application/json',headers=h,body=json.dumps(x))
        return route.fulfill(status=404,content_type='application/json',headers=h,body='{}')
    if 'q' in q:   # búsqueda libre: devuelve basura popular + lo que coincida por alguna palabra
        pal=set(n(q['q']).split()); res=[BD[4]]+[x for x in BD if pal & set(n(x['trackName']).split())]
    else: res=[x for x in BD if n(q.get('track_name','')) in n(x['trackName'])]
    route.fulfill(status=200,content_type='application/json',headers=h,body=json.dumps(res))
with sync_playwright() as p:
    b=p.chromium.launch(args=['--autoplay-policy=no-user-gesture-required']); pg=b.new_page(viewport={'width':400,'height':800}); errs=[]; pg.on('pageerror',lambda e:errs.append(str(e)))
    pg.route('https://lrclib.net/**',lrc)
    pg.goto('http://localhost:8765/index.html')
    pg.set_input_files('#archivos',['/tmp/l1.mp3','/tmp/l2.mp3','/tmp/03 - Miénteme (Versión Gala) [Video Oficial].mp3','/tmp/l4.mp3','/tmp/l5.mp3']); pg.wait_for_timeout(2000)
    esperado=['linea esposito','letra capetillo','letra mienteme','No se encontró','letra buena']; fallas=0
    pg.click('#bLetra')
    for i,e in enumerate(esperado):
        llamadas.clear(); pg.evaluate(f'cargar({i}, false)'); pg.wait_for_function("!document.getElementById('flotTxt').textContent.includes('Buscando')",timeout=20000); pg.wait_for_timeout(300)
        t=pg.inner_text('#flotTxt').strip().replace('\n',' / '); bien = e in t and 'NO DEBE' not in t and 'EQUIVOCADA' not in t; fallas += (not bien)
        print(('  ✔ ' if bien else '  ✘ '), pg.evaluate(f'lista[{i}].nombre'), '→', t[:50], '| consultas:', len(llamadas))
    # una que antes quedó marcada "no encontrada" se vuelve a buscar
    pg.evaluate("lista[0].letra=''; lista[0].letraSync=''; lista[0].letraNo=true; delete lista[0].letraV; cargar(0,false)"); pg.wait_for_timeout(1500)
    t=pg.inner_text('#flotTxt'); bien='linea esposito' in t; fallas += (not bien); print(('  ✔ ' if bien else '  ✘ '),'marcada antes como no encontrada: se busca de nuevo')
    r=pg.evaluate("(()=>{const f=document.getElementById('flot').getBoundingClientRect(), p=document.getElementById('flotPie').getBoundingClientRect(); return [Math.round(f.bottom), Math.round(p.bottom)]})()")
    bien = r[1] <= r[0]; fallas += (not bien); print(('  ✔ ' if bien else '  ✘ '),'el pie "Letras: lrclib.net" queda dentro de la ventana', r)
    pg.screenshot(path='/tmp/letra.png'); print('FALLAS:', fallas, errs); b.close()
