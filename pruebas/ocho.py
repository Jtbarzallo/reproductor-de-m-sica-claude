import sys, math; sys.path.insert(0,'/tmp')
import est_lib
from est_lib import *
fallas=[]
def ok(c,t,e=''):
    print(('  ✔ ' if c else '  ✘ ')+t,e)
    if not c: fallas.append(t)
AGUDOS = """async () => { aL.smoothingTimeConstant = 0; const fd = new Float32Array(aL.frequencyBinCount), fr = new Float32Array(aL.frequencyBinCount); let a = 0, g = 0;
  const hz = aL.context.sampleRate / aL.fftSize;
  for(let n = 0; n < 4; n++){ aL.getFloatFrequencyData(fd); aR.getFloatFrequencyData(fr);
    for(let b = Math.round(6000/hz); b < Math.round(12000/hz); b++) a += Math.pow(10, fd[b]/10) + Math.pow(10, fr[b]/10);
    for(let b = Math.round(300/hz); b < Math.round(1500/hz); b++) g += Math.pow(10, fd[b]/10) + Math.pow(10, fr[b]/10);
    await new Promise(x => setTimeout(x, 60)); }
  return 10 * Math.log10(a / g); }"""
def vuelta(pg, seg, pasos=20):
    out=[]; pg.evaluate('if(audio.currentTime > 12) audio.currentTime = 2'); pg.wait_for_timeout(400)
    for k in range(pasos):
        v = pg.evaluate('medir(1)'); out.append((v['izq'], v['der'])); pg.wait_for_timeout(int(seg*1000/pasos) - 180)
    return out
with sync_playwright() as p:
    for app in (False, True):
        print('=====', 'APP' if app else 'WEB')
        est_lib.FILES[:] = ['/tmp/mono_c.mp3','/tmp/st_b.mp3','/tmp/fuerte.mp3']
        b, pg = nueva(p, app)
        if app: pg.evaluate("__lista=[{id:1,ruta:'Music/mono_c.mp3'},{id:2,ruta:'Music/st_b.mp3'},{id:3,ruta:'Music/fuerte.mp3'}]")
        pg.click('#bPlay'); pg.wait_for_timeout(1500)
        pg.evaluate('cargar(lista.findIndex(c=>c.nombre.includes("mono_c")))'); pg.wait_for_timeout(1500)
        base = med(pg); pg.evaluate('audio.currentTime = 2')
        pg.click('#ochoChips button[data-s="5"]'); pg.wait_for_timeout(1500)
        v = vuelta(pg, 5); r = [i/max(d,1e-6) for i,d in v]
        print('   izq/der en una vuelta:', [round(x,2) for x in r])
        ok(max(r) > 2 and min(r) < 0.5, '8D rápido: el sonido pasa de un oído al otro', (round(max(r),2), round(min(r),2)))
        cruces = sum(1 for k in range(1,len(r)) if (r[k]-1)*(r[k-1]-1) < 0)
        ok(1 <= cruces <= 3, '8D rápido: una vuelta completa en ~5 s', cruces)
        niv = [math.sqrt((i*i+d*d)/2) for i,d in v]
        ok(max(niv)/min(niv) < 2.2, 'el volumen no da saltos durante la vuelta', round(max(niv)/min(niv),2))
        # adelante contra atrás: se mide el brillo cuando está centrado (izq≈der) en dos momentos de la vuelta
        pg.click('#ochoChips button[data-s="14"]'); pg.wait_for_timeout(2500)
        pg.evaluate('audio.currentTime = 2'); pg.wait_for_timeout(400)
        serie=[]
        for k in range(60):
            m = pg.evaluate('medir(1)'); serie.append((m['izq']/max(m['der'],1e-6), pg.evaluate(AGUDOS)))
        cr = [k for k in range(1,len(serie)) if (serie[k][0]-1)*(serie[k-1][0]-1) < 0]
        brillo = [round((serie[k][1]+serie[k-1][1])/2,1) for k in cr[:2]]
        ok(len(brillo) == 2 and abs(brillo[0]-brillo[1]) > 1.5, 'adelante y atrás suenan distinto (atrás más apagado)', brillo)
        v = vuelta(pg, 14, 28); r = [i/max(d,1e-6) for i,d in v]; cruces = sum(1 for k in range(1,len(r)) if (r[k]-1)*(r[k-1]-1) < 0)
        ok(max(r) > 2 and min(r) < 0.5 and 1 <= cruces <= 3, '8D lento: una vuelta en ~14 s', (cruces, round(max(r),2), round(min(r),2)))
        # apagar: sonido original
        pg.evaluate('audio.currentTime = 2'); pg.click('#ochoChips button[data-s="0"]'); pg.wait_for_timeout(1500); m = med(pg)
        ok(abs(m['ancho'] - base['ancho']) < 0.01 and abs(m['nivel'] - base['nivel'])/base['nivel'] < 0.08, 'Apagado: vuelve el sonido original', (m['ancho'], m['nivel'], base['nivel']))
        # guardado
        pg.evaluate('audio.currentTime = 2'); pg.click('#ochoChips button[data-s="9"]'); pg.wait_for_timeout(3500); pg.reload(); pg.wait_for_timeout(2500)
        ok(pg.evaluate('ochoSeg') == 9 and pg.eval_on_selector('#ochoChips button.on','e=>e.dataset.s') == '9', 'queda guardado al cerrar y abrir')
        pg.click('#bPlay'); pg.wait_for_timeout(2000); v = vuelta(pg, 9, 18); r = [i/max(d,1e-6) for i,d in v]
        ok(max(r) > 2 and min(r) < 0.5, 'tras abrir de nuevo sigue girando', (round(max(r),2), round(min(r),2)))
        # canción a todo volumen + estéreo máximo + ecualizador: no satura
        pg.evaluate('cargar(lista.findIndex(c=>c.nombre.includes("fuerte")))'); pg.wait_for_timeout(1500)
        pg.click('#ochoChips button[data-s="5"]')
        for nombre, js in (('solo 8D', "ponerEQ([0,0,0,0,0]); ponerEstereo('normal')"), ('8D + Máximo + Más bajos', "ponerEQ([7,4,0,0,0]); ponerEstereo('maximo')"), ('8D + todo +12', "ponerEQ([12,12,12,12,12])")):
            pg.evaluate('audio.currentTime = 2'); pg.evaluate(js); pg.wait_for_timeout(1200); pk = max(pg.evaluate('medir(4)')['pico'] for _ in range(6))
            ok(0.2 < pk <= 1.0, f'canción fuerte, {nombre}: suena y no satura', pk)
        # modo DJ con 8D
        pg.evaluate("ponerEQ([0,0,0,0,0]); ponerEstereo('normal')"); pg.click('#djChips button[data-s="4"]'); pg.wait_for_timeout(400)
        pg.click('#bSig'); pg.wait_for_function('!mezclando', timeout=60000); pg.wait_for_timeout(1500)
        v = vuelta(pg, 5); r = [i/max(d,1e-6) for i,d in v]
        ok(pg.evaluate('!audio.paused') and max(r) > 2 and min(r) < 0.5, 'tras una mezcla DJ sigue sonando y girando', (pg.inner_text('#titulo'), round(max(r),2), round(min(r),2)))
        ok(not pg.errs, 'sin errores', pg.errs); b.close()
print('FALLAS:', fallas or 'ninguna')
