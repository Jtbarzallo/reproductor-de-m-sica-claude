import json
from playwright.sync_api import sync_playwright
HOOK = """
(() => {
  const oc = AudioNode.prototype.connect;
  AudioNode.prototype.connect = function(d, ...a){
    if(d === this.context.destination){
      if(!this.context.__sp){
        const sp = this.context.__sp = this.context.createChannelSplitter(2);
        window.aL = this.context.createAnalyser(); window.aR = this.context.createAnalyser();
        aL.fftSize = aR.fftSize = 8192; oc.call(sp, aL, 0); oc.call(sp, aR, 1);
      }
      const j = this.context.createGain(); j.channelCountMode = 'explicit'; j.channelCount = 2; oc.call(this, j); oc.call(j, this.context.__sp);
    }
    return oc.call(this, d, ...a);
  };
  window.medir = async (n = 6) => {
    if(!window.aL) return {sinGrafo: true};
    let dd = 0, ss = 0, lr = 0, ll = 0, rr = 0, pk = 0;
    const N = 8192, l = new Float32Array(N), r = new Float32Array(N);
    for(let k = 0; k < n; k++){
      aL.getFloatTimeDomainData(l); aR.getFloatTimeDomainData(r);
      for(let i = 0; i < N; i++){ dd += (l[i]-r[i])**2; ss += (l[i]+r[i])**2; lr += l[i]*r[i]; ll += l[i]*l[i]; rr += r[i]*r[i]; pk = Math.max(pk, Math.abs(l[i]), Math.abs(r[i])); }
      await new Promise(x => setTimeout(x, 180));
    }
    return { ancho: +(Math.sqrt(dd/ss) || 0).toFixed(3), corr: +(lr/Math.sqrt(ll*rr) || 0).toFixed(3),
             nivel: +Math.sqrt((ll+rr)/(2*N*n)).toFixed(4), pico: +pk.toFixed(3), izq: +Math.sqrt(ll/(N*n)).toFixed(4), der: +Math.sqrt(rr/(N*n)).toFixed(4) };
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
