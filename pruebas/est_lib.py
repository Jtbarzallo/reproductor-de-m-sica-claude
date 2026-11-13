import json
from playwright.sync_api import sync_playwright
HOOK = """
(() => {
  const oc = AudioNode.prototype.connect;
  AudioNode.prototype.connect = function(d, ...a){
    if(d === this.context.destination && !(this.context instanceof OfflineAudioContext)){
      if(!this.context.__sp){
        const sp = this.context.__sp = this.context.createChannelSplitter(2);
        window.aL = this.context.createAnalyser(); window.aR = this.context.createAnalyser();
        aL.fftSize = aR.fftSize = 8192; oc.call(sp, aL, 0); oc.call(sp, aR, 1);
        // los dos canales se capturan EN EL MISMO instante (dos analizadores por separado pueden leerse desfasados)
        const proc = this.context.createScriptProcessor(2048, 2, 1), mudo = this.context.createGain(); mudo.gain.value = 0;
        window.__acc = {dd:0, ss:0, lr:0, ll:0, rr:0, pk:0, n:0};
        proc.onaudioprocess = e => { const l = e.inputBuffer.getChannelData(0), r = e.inputBuffer.getChannelData(1), a = window.__acc;
          for(let i = 0; i < l.length; i++){ a.dd += (l[i]-r[i])**2; a.ss += (l[i]+r[i])**2; a.lr += l[i]*r[i]; a.ll += l[i]*l[i]; a.rr += r[i]*r[i]; const m = Math.max(Math.abs(l[i]), Math.abs(r[i])); if(m > a.pk) a.pk = m; }
          a.n += l.length; };
        this.context.__proc = proc; this.context.__mudo = mudo; oc.call(proc, mudo); oc.call(mudo, this.context.destination);
      }
      const j = this.context.createGain(); j.channelCountMode = 'explicit'; j.channelCount = 2; oc.call(this, j); oc.call(j, this.context.__sp); oc.call(j, this.context.__proc);
    }
    return oc.call(this, d, ...a);
  };
  window.medir = async (n = 6) => {
    if(!window.__acc) return {sinGrafo: true};
    const a = window.__acc; a.dd = a.ss = a.lr = a.ll = a.rr = a.pk = a.n = 0;
    await new Promise(x => setTimeout(x, n * 180));
    return { ancho: +(Math.sqrt(a.dd/a.ss) || 0).toFixed(3), corr: +(a.lr/Math.sqrt(a.ll*a.rr) || 0).toFixed(3),
             nivel: +Math.sqrt((a.ll+a.rr)/(2*a.n || 1)).toFixed(4), pico: +a.pk.toFixed(3), izq: +Math.sqrt(a.ll/(a.n||1)).toFixed(4), der: +Math.sqrt(a.rr/(a.n||1)).toFixed(4) };
  };
})();
"""
APP_HOOK = """
window.__lista=[{id:1,ruta:'Music/st_a.mp3'},{id:2,ruta:'Music/st_b.mp3'},{id:3,ruta:'Music/mono_c.mp3'}];
window.AndroidJAT={ canciones:()=>JSON.stringify(window.__lista), pedirPermiso:()=>{}, sincronizar:()=>{}, guardarRespaldo:()=>{}, estado:()=>{}, abrirAjustes:()=>{} };
"""
FILES = ['/tmp/st_a.mp3','/tmp/st_b.mp3','/tmp/mono_c.mp3']
def nueva(p, app=False, ctx=None):
    b = p.chromium.launch(args=['--autoplay-policy=no-user-gesture-required'])
    c = ctx or b.new_context()
    pg = c.new_page(); pg.errs = []
    pg.on('pageerror', lambda e: pg.errs.append(str(e)))
    pg.on('console', lambda m: m.type == 'error' and pg.errs.append(m.text))
    pg.add_init_script(HOOK)
    if app:
        pg.add_init_script(APP_HOOK)
        pg.route('**/musica/*', lambda r: r.fulfill(status=200, content_type='audio/mpeg', body=open(FILES[int(r.request.url.split('/')[-1])-1],'rb').read()))
    pg.goto('http://localhost:8765/index.html')
    if not app:
        pg.set_input_files('#archivos', FILES)
    pg.wait_for_timeout(1500)
    return b, pg
def modo(pg, m): pg.click(f'#est button[data-m="{m}"]'); pg.wait_for_timeout(2200)
def med(pg): return pg.evaluate('medir()')
