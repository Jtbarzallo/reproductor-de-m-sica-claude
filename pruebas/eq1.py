import sys; sys.path.insert(0,'/tmp')
from est_lib import *
ESP = """async (hz) => { aL.smoothingTimeConstant = 0; if(audio.currentTime > 25) audio.currentTime = 2; await new Promise(x=>setTimeout(x,600));
  const fd = new Float32Array(aL.frequencyBinCount); let o = hz.map(()=>0); const N = 50;
  for(let n=0;n<N;n++){ aL.getFloatFrequencyData(fd); hz.forEach((h,i)=>{ const b=Math.round(h/(aL.context.sampleRate/aL.fftSize)); const w=Math.max(2,Math.round(b*0.25)); let m=0; for(let k=-w;k<=w;k++) m+=Math.pow(10,fd[b+k]/10); o[i]+=m/(2*w+1); }); await new Promise(x=>setTimeout(x,90)); }
  return o.map(v=>+(10*Math.log10(v/N)).toFixed(1)); }"""
HZ=[50,250,1000,3500,12000]
fallas=[]
def ok(c,t,e=''):
    print(('  ✔ ' if c else '  ✘ ')+t,e)
    if not c: fallas.append(t)
with sync_playwright() as p:
    import est_lib; est_lib.FILES[:] = ['/tmp/mono_c.mp3','/tmp/fuerte.mp3','/tmp/st_b.mp3']
    b, pg = nueva(p, False); pg.click('#bPlay'); pg.wait_for_timeout(1500)
    base = pg.evaluate(ESP, HZ); m0 = med(pg); print('plano', base)
    for i in range(5):
        pg.evaluate(f"ponerEQ([0,0,0,0,0].map((v,k)=>k==={i}?9:0))"); pg.wait_for_timeout(500)
        e = pg.evaluate(ESP, HZ); d=[round(e[k]-base[k],1) for k in range(5)]
        ok(abs(d[i]) < 2.5 and all(d[k] < -6 for k in range(5) if abs(k-i)>1), f'barra {i+1} sube solo su zona (el resto baja para no saturar)', d)
    pg.evaluate("ponerEQ([-9,0,0,0,0])"); pg.wait_for_timeout(500); e = pg.evaluate(ESP, HZ)
    ok(e[0]-base[0] < -6 and all(abs(e[k]-base[k]) < 1.5 for k in (2,3,4)), 'bajar graves no toca voces ni agudos', [round(e[k]-base[k],1) for k in range(5)])
    pg.click('#eqChips button:has-text("Rock")'); pg.wait_for_timeout(400)
    ok(pg.evaluate('JSON.stringify(eqVals)')=='[5,2,-2,3,5]' and pg.eval_on_selector('#eqChips button.on','e=>e.textContent')=='Rock', 'ajuste Rock pone las barras')
    pg.evaluate("const i=document.querySelectorAll('#eq input')[2]; i.value=6; i.dispatchEvent(new Event('input'))"); pg.wait_for_timeout(300)
    ok(pg.evaluate('eqVals[2]')==6 and pg.evaluate("document.querySelectorAll('#eqChips button.on').length")==0 and pg.evaluate("document.querySelectorAll('#eq b')[2].textContent")=='+6', 'mover una barra a mano')
    pg.wait_for_timeout(300); pg.reload(); pg.wait_for_timeout(2500)
    ok(pg.evaluate('JSON.stringify(eqVals)')=='[5,2,6,3,5]' and pg.evaluate("document.querySelectorAll('#eq input')[2].value")=='6', 'queda guardado al cerrar y abrir')
    pg.click('#bPlay'); pg.wait_for_timeout(1500); e = pg.evaluate(ESP, HZ)
    ok((e[2]-base[2]) - (e[1]-base[1]) > 2.5 and (e[2]-base[2]) - (e[3]-base[3]) > 1.5, 'tras abrir de nuevo el ecualizador suena', [round(e[k]-base[k],1) for k in range(5)])
    # sin saturar con canción a todo volumen y todo al máximo
    pg.evaluate('cargar(1)'); pg.wait_for_timeout(1500)
    for nombre, vals in (('todo +12',[12]*5),('Más bajos',[7,4,0,0,0]),('Parlante de celular',[8,5,0,1,2])):
        for m in ('normal','maximo'):
            pg.evaluate(f"ponerEQ({vals})"); modo(pg, m); v = pg.evaluate('medir(10)')
            ok(v['pico'] <= 1.0, f'canción fuerte, {nombre}, estéreo {m}: no satura', v['pico'])
    pg.evaluate("ponerEQ([0,0,0,0,0])"); modo(pg,'normal'); pg.evaluate('cargar(2)'); pg.wait_for_timeout(1800); v = med(pg)
    ok(abs(v['ancho']-0.401) < 0.03, 'plano + normal = sonido original', v['ancho'])
    ok(not pg.errs, 'sin errores', pg.errs); b.close()
print('FALLAS:', fallas or 'ninguna')
