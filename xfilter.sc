XFilter {
	*ar { arg snd, x=0.5, r;
		var low = RLPF.ar(snd, x.clip(0, 0.5).linexp(0, 0.5, 40, 20000), r.linlin(0,1,1,0.001).lag(0.2).poll);
		var output = RHPF.ar(low, x.clip(0.5, 1).linexp(0.5, 1, 20, 10000), r.linlin(0,1,1,0.001).lag(0.2));
		^output
	}

}

Source {
	*ar { arg in, del = 0, freeze=0, filter=0.5, resonanz=0;
		var chain, snd, res;
		snd = DelayC.ar(in, 10, (del.linexp(0,1,0.1,10) - 0.1));
		chain = FFT(LocalBuf(2048), snd);
		chain = PV_MagFreeze(chain, freeze);
		in = IFFT.ar(chain);
		//res = Resonz.ar(in, filter.linexp(0,1,40,10000).lag(0.2), 0.02);
		^XFilter.ar(in, filter.lag2(0.2), resonanz);
		//^SelectX.ar(resonanz.lag(0.2), [XFilter.ar(in, filter.lag2(0.2)), res])
	}
}