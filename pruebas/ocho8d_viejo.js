function armarViejo(c, fuente){
  const g = v => { const n = c.createGain(); n.gain.value = v; return n; };
  const uno = g(1); uno.channelCountMode = 'explicit'; uno.channelCount = 1; uno.channelInterpretation = 'speakers'; fuente.connect(uno);
  const pan = c.createPanner(); pan.panningModel = 'HRTF'; pan.distanceModel = 'linear'; pan.rolloffFactor = 0;
  pan.positionX.value = 0; pan.positionY.value = 0; pan.positionZ.value = 0;
  const X = c.createOscillator(), Z = c.createOscillator();
  Z.setPeriodicWave(c.createPeriodicWave(new Float32Array([0, 1]), new Float32Array([0, 0])));
  X.frequency.value = Z.frequency.value = 1/9;
  const rx = g(1), rz = g(-1); X.connect(rx); rx.connect(pan.positionX); Z.connect(rz); rz.connect(pan.positionZ); X.start(); Z.start();
  const spO = c.createChannelSplitter(2), mgO = c.createChannelMerger(2), gI = g(1), gD = g(1);
  const aI = g(-0.45), aD = g(0.45); X.connect(aI); aI.connect(gI.gain); X.connect(aD); aD.connect(gD.gain);
  const atras = c.createBiquadFilter(); atras.type='lowpass'; atras.Q.value=0.5; atras.frequency.value=6000;
  const ab = g(1700); Z.connect(ab); ab.connect(atras.detune);
  uno.connect(pan); pan.connect(atras); atras.connect(spO); spO.connect(gI,0); spO.connect(gD,1); gI.connect(mgO,0,0); gD.connect(mgO,0,1);
  const gira = g(0); mgO.connect(gira);
  return {gira, eco: g(0), x: X, z: Z};
}
