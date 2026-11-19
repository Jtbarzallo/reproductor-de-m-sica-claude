// Audio 8D (versión 2): el sonido da vueltas alrededor de la cabeza usando las mismas pistas que usa el oído
//  - retraso entre oídos (el oído lejano escucha ~0,6 ms después): es lo que más ubica el sonido a un lado
//  - sombra de la cabeza: al oído lejano le llegan menos agudos (no se baja todo el volumen)
//  - detrás suena más opaco y con más eco de sala (se siente lejos); adelante, más claro y cercano
//  - los graves profundos se quedan al centro: el "golpe" no sube y baja con el giro
function armar8D(c, fuente){
  const g = v => { const n = c.createGain(); n.gain.value = v; return n; };
  const filtro = (tipo, hz, q = 0.7071) => { const f = c.createBiquadFilter(); f.type = tipo; f.frequency.value = hz; f.Q.value = q; return f; };
  const uno = g(1); uno.channelCountMode = 'explicit'; uno.channelCount = 1; uno.channelInterpretation = 'speakers';
  fuente.connect(uno);
  // posición: x = de izquierda (-1) a derecha (+1); z = adelante (+1) / atrás (-1)
  const x = c.createOscillator(), z = c.createOscillator();
  z.setPeriodicWave(c.createPeriodicWave(new Float32Array([0, 1]), new Float32Array([0, 0])));   // coseno
  x.frequency.value = z.frequency.value = 1 / 9;
  x.start(); z.start();
  const zInv = g(-1); z.connect(zInv);                                   // +1 cuando está atrás
  // graves al centro (cruce de 4.º orden: al sumar las dos partes no se pierde nada)
  const lp1 = filtro('lowpass', 140), lp2 = filtro('lowpass', 140), hp1 = filtro('highpass', 140), hp2 = filtro('highpass', 140);
  uno.connect(lp1); lp1.connect(lp2); uno.connect(hp1); hp1.connect(hp2);
  // adelante / atrás: detrás pierde brillo; adelante tiene más "presencia"
  const brillo = filtro('highshelf', 3500); brillo.gain.value = -1;
  const gz1 = g(2); z.connect(gz1); gz1.connect(brillo.gain);              // adelante +1 dB, atrás −3 dB
  const pres = filtro('peaking', 2800, 1); pres.gain.value = 0;
  const gz2 = g(2); z.connect(gz2); gz2.connect(pres.gain);                // adelante +2 dB, atrás −2 dB
  const cerca = g(1); const gz3 = g(0.05); z.connect(gz3); gz3.connect(cerca.gain);   // atrás un poquito más lejos (−0,9 dB)
  hp2.connect(brillo); brillo.connect(pres); pres.connect(cerca);
  // sombra de la cabeza: curva que vale −11 dB si el sonido está del otro lado y +2 dB si está de este lado
  const N = 1025, curva = new Float32Array(N);
  for(let i = 0; i < N; i++){ const p = (i / (N - 1)) * 2 - 1; curva[i] = p > 0 ? -11 * p : 2 * -p; }
  const oido = (signo) => {                                               // signo: −1 oído izquierdo, +1 derecho
    const lado = g(-signo); x.connect(lado);                               // > 0 cuando el sonido está del lado contrario
    const forma = c.createWaveShaper(); forma.curve = curva; lado.connect(forma);
    const ret = c.createDelay(0.01); ret.delayTime.value = 0.00035;
    const itd = g(-signo * 0.00032); x.connect(itd); itd.connect(ret.delayTime);   // oído lejano: hasta 0,67 ms tarde
    const sombra = filtro('highshelf', 1600); sombra.gain.value = 0; forma.connect(sombra.gain);
    const nivel = g(1); const ild = g(signo * 0.18); x.connect(ild); ild.connect(nivel.gain);   // ±1,5 dB en todo el sonido
    cerca.connect(ret); ret.connect(sombra); sombra.connect(nivel);
    return nivel;
  };
  const mg = c.createChannelMerger(2);
  oido(-1).connect(mg, 0, 0); oido(1).connect(mg, 0, 1);
  const centro = g(1); lp2.connect(centro); centro.connect(mg, 0, 0); centro.connect(mg, 0, 1);
  const gira = g(0); mg.connect(gira);
  // sala: eco distinto en cada oído (se siente afuera de la cabeza), más eco cuando pasa por detrás
  const sr = c.sampleRate, largo = Math.floor(sr * 1.6), ir = c.createBuffer(2, largo, sr), pre = Math.floor(sr * 0.018);
  for(let ch = 0; ch < 2; ch++){
    const d = ir.getChannelData(ch); let lp = 0;
    for(let i = pre; i < largo; i++){
      const t = (i - pre) / sr, k = Math.min(0.92, 0.25 + t * 0.9);     // con el tiempo el eco se vuelve más opaco
      lp = lp * k + (Math.random() * 2 - 1) * (1 - k);
      d[i] = lp * Math.exp(-t * 4.2) * 3;
    }
    [0.007, 0.013, 0.021, 0.029].forEach((r, j) => { const i = pre + Math.floor((r + ch * 0.0023 * (j + 1)) * sr); if(i < largo) d[i] += 0.5 / (j + 1); });   // primeros rebotes
  }
  const sala = c.createConvolver(); sala.normalize = true; sala.buffer = ir;
  const envio = g(1); const gz4 = g(0.6); zInv.connect(gz4); gz4.connect(envio.gain);   // atrás 1,6x de eco, adelante 0,4x
  hp2.connect(envio); envio.connect(sala);
  const eco = g(0); sala.connect(eco);
  return {gira, eco, x, z};
}
if(typeof module !== 'undefined') module.exports = {armar8D};
