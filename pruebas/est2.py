import sys; sys.path.insert(0,'/tmp')
import est_lib
from est_lib import *
import json
REF = json.load(open('/tmp/ref.json'))
fallas = []
def ok(c, txt, extra=''):
    print(('  ✔ ' if c else '  ✘ ') + txt, extra)
    if not c: fallas.append(txt)
with sync_playwright() as p:
    for app in (False, True):
        tag = 'APP' if app else 'WEB'; print('=====', tag)
        b, pg = nueva(p, app)
        pg.click('#bPlay'); pg.wait_for_timeout(1200)
        res = {}
        for idx, nom in ((0,'est'),(1,'estB'),(2,'mono')):
            pg.evaluate(f'cargar({idx})'); pg.wait_for_timeout(1300)
            for m in ['normal','amplio','maximo','mono']:
                modo(pg, m); res[nom, m] = med(pg)
        for k, v in res.items(): print('   ', k, v)
        ok(abs(res['est','normal']['ancho'] - REF['st_a']) < 0.03 and abs(res['estB','normal']['ancho'] - REF['st_b']) < 0.03 and res['mono','normal']['ancho'] == 0, 'T1 Normal deja el sonido original (igual al archivo)', (res['est','normal']['ancho'], REF['st_a'], res['estB','normal']['ancho'], REF['st_b']))
        ok(res['est','normal']['ancho'] + .1 < res['est','amplio']['ancho'] < res['est','maximo']['ancho'] - .1, 'T2 estéreo: normal < ampliado < máximo')
        ok(res['estB','normal']['ancho'] + .1 < res['estB','amplio']['ancho'] < res['estB','maximo']['ancho'] - .1, 'T2 estéreo B: normal < ampliado < máximo')
        ok(res['mono','mono']['ancho'] > 0.65 and res['mono','mono']['ancho'] > res['mono','maximo']['ancho'] > res['mono','amplio']['ancho'] > 0.3 and res['mono','maximo']['ancho'] > 0.5, 'T2 canción mono: se crea estéreo claro en los 3 modos', (res['mono','amplio']['ancho'], res['mono','maximo']['ancho'], res['mono','mono']['ancho']))
        ok(all(v['corr'] > 0.05 for v in res.values()), 'T2 nunca queda "hueco" (lados opuestos)', min(v['corr'] for v in res.values()))
        ok(all(abs(v['izq'] - v['der']) / max(v['izq'], v['der']) < 0.05 for v in res.values()), 'T2 izquierda y derecha equilibradas', max(round(abs(v['izq'] - v['der']) / max(v['izq'], v['der']),3) for v in res.values()))
        niv = [v['nivel'] for k, v in res.items() if k[0] == 'est']
        ok(max(niv) / min(niv) < 1.3, 'T2 el volumen casi no cambia entre modos', round(max(niv)/min(niv),2))
        # T7 estado visible
        modo(pg, 'maximo'); pg.wait_for_timeout(1200); txt = pg.inner_text('#estEstado')
        ok('Máximo activo' in txt and 'separación' in txt, 'T7 se ve el estado y la medición', txt)
        ok(pg.evaluate("parseFloat(document.getElementById('mL').style.width) > 3 && parseFloat(document.getElementById('mR').style.width) > 3"), 'T7 barras izquierda/derecha se mueven')
        # T6 prueba izquierda / derecha
        pg.click('#bProbar'); pg.wait_for_timeout(450); a = pg.evaluate('medir(3)'); t1 = pg.inner_text('#estEstado')
        pg.wait_for_timeout(750); bb = pg.evaluate('medir(3)'); t2 = pg.inner_text('#estEstado')
        ok(a['izq'] > 4 * a['der'] and 'IZQUIERDA' in t1, 'T6 pitido izquierdo sale solo por la izquierda', (a['izq'], a['der']))
        ok(bb['der'] > 4 * bb['izq'] and 'DERECHA' in t2, 'T6 pitido derecho sale solo por la derecha', (bb['izq'], bb['der']))
        pg.wait_for_timeout(8500); c2 = med(pg)
        ok(abs(c2['nivel'] - res['mono','maximo']['nivel']) / res['mono','maximo']['nivel'] < 0.2 and 'Máximo' in pg.inner_text('#estEstado'), 'T6 la música vuelve a su volumen tras la prueba', c2['nivel'])
        ok(not pg.errs, 'sin errores', pg.errs); b.close()

        # T3 escenarios
        b, pg = nueva(p, app); modo(pg, 'maximo'); pg.click('#bPlay'); pg.wait_for_timeout(2500); m = med(pg)
        ok(m['ancho'] > 0.5, 'T3 elegir modo ANTES de reproducir', m['ancho'])
        pg.click('#bPlay'); pg.wait_for_timeout(400); modo(pg, 'normal'); pg.click('#bPlay'); pg.wait_for_timeout(1200); m = med(pg)
        ok(abs(m['ancho'] - REF['st_a']) < 0.03, 'T3 pausar, volver a Normal, seguir', m['ancho'])
        modo(pg, 'amplio'); pg.wait_for_timeout(300); pg.reload(); pg.wait_for_timeout(2500)
        ok(pg.eval_on_selector('#est button.on', 'e=>e.dataset.m') == 'amplio', 'T3 el modo queda guardado al cerrar y abrir')
        pg.click('#bPlay'); pg.wait_for_timeout(1500); m = med(pg)
        pg.wait_for_timeout(1500); m = med(pg); ok(0.25 < m['ancho'] < 0.5, 'T3 tras abrir de nuevo el efecto suena', m['ancho'])
        # T4 llegan canciones nuevas: no se cambia el modo ni se corta la canción
        modo(pg, 'maximo'); pg.evaluate("document.getElementById('vol').value = 0.5; document.getElementById('vol').dispatchEvent(new Event('input'))")
        t_antes = pg.evaluate('audio.currentTime'); tit = pg.inner_text('#titulo')
        if app: pg.evaluate("__lista.push({id:1,ruta:'Music/otra/nueva.mp3'}); sincronizarApp()")
        else: pg.set_input_files('#archivos', ['/tmp/fuerte.mp3'])
        pg.wait_for_timeout(1500)
        ok(pg.eval_on_selector('#est button.on', 'e=>e.dataset.m') == 'maximo' and pg.evaluate('modoEst') == 'maximo', 'T4 canción nueva: el modo estéreo NO se cambia')
        ok(pg.evaluate('!audio.paused && audio.currentTime') > t_antes + 1 and pg.inner_text('#titulo') == tit, 'T4 canción nueva: la música NO se corta')
        ok(abs(pg.evaluate('audio.volume') - 0.5) < 0.01, 'T4 canción nueva: el volumen NO se cambia')
        pg.wait_for_timeout(1500); m = med(pg); ok(m['ancho'] > 0.5, 'T4 el efecto sigue tras llegar canciones', m['ancho'])
        # T3 DJ
        pg.click('#djChips button[data-s="4"]'); pg.wait_for_timeout(500)
        pg.click('#bSig'); pg.wait_for_function('!mezclando', timeout=60000); pg.wait_for_timeout(3000); m = med(pg)
        ok(m['ancho'] > 0.55 and m['corr'] > 0.05 and pg.evaluate('!audio.paused'), 'T3 el efecto sigue después de una mezcla DJ', (pg.inner_text('#titulo'), m['ancho'], m['corr']))
        pg.click('#bSig'); pg.wait_for_function('!mezclando', timeout=60000); pg.wait_for_timeout(3000); m = med(pg)
        ok(pg.evaluate('!audio.paused') and m['nivel'] > 0.02 and m['ancho'] > 0.5, 'T3 segunda mezcla DJ (a canción mono): suena y con estéreo', (pg.inner_text('#titulo'), m['ancho']))
        ok(not pg.errs, 'sin errores', pg.errs); b.close()

    # T8 canciones a todo volumen: sin saturar
    print('===== VOLUMEN FUERTE'); est_lib.FILES[:] = ['/tmp/fuerte.mp3', '/tmp/solo_izq.mp3', '/tmp/mono_c.mp3']
    b, pg = nueva(p, False); pg.click('#bPlay'); pg.wait_for_timeout(1200)
    for idx in (0, 1):
        pg.evaluate(f'cargar({idx})'); pg.wait_for_timeout(1300)
        for m in ['normal','amplio','maximo','mono']:
            modo(pg, m); pg.wait_for_timeout(500); v = pg.evaluate('medir(14)'); print('   ', idx, m, v)
            ok(v['pico'] <= 1.0, f'T8 canción {idx} {m}: no satura', v['pico'])
            if idx == 1: ok(v['corr'] > -0.1, f'T8 canción solo-izquierda {m}: no queda hueca', v['corr'])
    ok(not pg.errs, 'sin errores', pg.errs); b.close()
    est_lib.FILES[:] = ['/tmp/st_a.mp3','/tmp/st_b.mp3','/tmp/mono_c.mp3']

    # T5 celular que no deja activar el efecto: la música no se pierde
    print('===== FALLA SIMULADA')
    b, pg = nueva(p, False)
    pg.evaluate("AudioContext.prototype.createMediaElementSource = function(){ if(this.__n){ throw new Error('bloqueado'); } this.__n = 1; return AudioContext.prototype.__orig.call(this, ...arguments); }") if False else None
    pg.evaluate("(() => { const o = AudioContext.prototype.createMediaElementSource; let n = 0; AudioContext.prototype.createMediaElementSource = function(el){ if(++n >= 2) throw new Error('bloqueado'); return o.call(this, el); }; })()")
    pg.click('#bPlay'); pg.wait_for_timeout(1500); m = med(pg)
    ok(pg.evaluate('!audio.paused && audio.currentTime > 0.8') and m.get('nivel', 0) > 0.02, 'T5 si falla el efecto, la música sigue sonando', m)
    modo(pg, 'maximo'); ok('no permitió' in pg.inner_text('#estEstado'), 'T5 avisa en pantalla', pg.inner_text('#estEstado'))
    pg.click('#bSig'); pg.wait_for_timeout(1500)
    ok(pg.evaluate('!audio.paused && audio.currentTime > 0.5') and pg.inner_text('#titulo') == 'Estereo B', 'T5 se puede cambiar de canción')
    ok(not pg.errs, 'sin errores', pg.errs); b.close()
print('\nFALLAS:', fallas if fallas else 'ninguna')
