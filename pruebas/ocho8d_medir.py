import sys, json, numpy as np
from playwright.sync_api import sync_playwright
js = open('pruebas/ocho8d.js').read() + open('pruebas/ocho8d_viejo.js').read()
render = """async ([cual, seg]) => {
  const sr = 44100, dur = 8, c = new OfflineAudioContext(2, sr*dur, sr);
  const b = c.createBuffer(1, sr*dur, sr), d = b.getChannelData(0);
  let b0=0,b1=0,b2=0; for(let i=0;i<d.length;i++){ const w=Math.random()*2-1; b0=.99765*b0+w*.0990460; b1=.963*b1+w*.2965164; b2=.57*b2+w*1.0526913; d[i]=(b0+b1+b2+w*.1848)*0.1; }
  const s = c.createBufferSource(); s.buffer = b;
  const o = cual === 'nuevo' ? armar8D(c, s) : armarViejo(c, s);
  o.x.frequency.value = o.z.frequency.value = 1/seg;
  o.gira.gain.value = 1; o.gira.connect(c.destination); o.eco.gain.value = cual==='nuevo' ? 0.3 : 0; o.eco.connect(c.destination); s.start();
  const r = await c.startRendering();
  return [Array.from(r.getChannelData(0)), Array.from(r.getChannelData(1))];
}"""
def bandas(x, sr, lo, hi):
    X = np.fft.rfft(x); f = np.fft.rfftfreq(len(x), 1/sr); X[(f<lo)|(f>hi)] = 0; return np.fft.irfft(X, len(x))
with sync_playwright() as p:
    br = p.chromium.launch(executable_path='/usr/bin/google-chrome', args=['--autoplay-policy=no-user-gesture-required'])
    pg = br.new_page(); pg.set_content('<html></html>'); pg.add_script_tag(content=js)
    for cual in ('viejo','nuevo'):
        L, R = [np.array(v) for v in pg.evaluate(render, [cual, 4])]
        sr = 44100; w = int(sr*0.1)
        Lh, Rh = bandas(L,sr,2000,16000), bandas(R,sr,2000,16000)
        Lm, Rm = bandas(L,sr,300,1200), bandas(R,sr,300,1200)
        db = lambda a: 20*np.log10(np.sqrt(np.mean(a*a))+1e-12)
        ild_h, total, itd = [], [], []
        for k in range(4, 80, 4):
            s = slice(k*w//2, k*w//2 + w)
            ild_h.append(db(Rh[s]) - db(Lh[s])); total.append(10*np.log10(np.mean(L[s]**2)+np.mean(R[s]**2)))
            cc = np.correlate(Lm[s], Rm[s][40:-40], 'valid'); lag = (np.argmax(cc)-40)/sr*1000; itd.append(lag)
        print(f'== {cual}')
        print('  dif. agudos der-izq dB:', [round(v,1) for v in ild_h])
        print('  retraso entre oídos ms:', [round(v,2) for v in itd])
        print('  volumen total: vaivén', round(max(total)-min(total),1), 'dB')
    br.close()
