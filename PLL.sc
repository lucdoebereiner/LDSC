
PLL {
	*ar {arg inputSignal, bus, lpf=10000, inLPF=5000, errFac=0.1, errLeak=0.5, filter=10000, reso=1, drop=0;
		var ph = InFeedback.ar(bus);

		var input = BLowPass4.ar(inputSignal.sign, inLPF.lag(0.2));
		var phase = BLowPass4.ar(Integrator.ar(ph,1).mod(2*pi), lpf.lag(0.2));
		var signal = WaveLoss.ar(RLPF.ar(phase.sin.sign, filter, reso), drop, 100);
		var comp = (((input >= 0) * (signal >= 0)) + ((input < 0) * (signal < 0)));
		var withSign = (1-comp) * (signal > 0).if( K2A.ar(1), K2A.ar(-1));

		var err =  Integrator.ar(withSign, errLeak.lag(0.5));

		Out.ar(bus, 0.0001+(err*errFac));
		^signal
	}
}



PLL2 {
// xor gate phase detector
	*ar { arg source, bus, errFac=0.1, errLeak=0.8, baseFreq=0.0, divIn=1, divSelf=1, width=0.5,
		sourceMin = 0.005, sourceMax=1.0;

		var ph = InFeedback.ar(bus);

		var sourceLarger = ((source.abs > sourceMin) * (source.abs < sourceMax)) * source;
		var inputSign = sourceLarger.sign;
		var input = (ToggleFF.ar(PulseDivider.ar(inputSign, divIn)) * 2) - 1;

		var phase = Integrator.ar(ph,1).mod(2*pi);
		var signal = ((phase < (width * 2 * pi)) * 2) - 1;
		var compSignal = (ToggleFF.ar(PulseDivider.ar(phase.sin.sign, divSelf)) * 2) - 1;

		var comp = (((input >= 0) * (compSignal >= 0)) + ((input < 0) * (compSignal < 0)));
		//var withSign = (1-comp) * (compSignal > 0).if( K2A.ar(-1), K2A.ar(1));
		var withSign = (1-comp);

		var err =  Integrator.ar(withSign, errLeak); // lower for lower freqs, higher for higher

		Out.ar(bus, ((2 * pi * baseFreq)/SampleRate.ir) +(err*K2A.ar(errFac))); // div higher for lower freq, higher for higher
		^signal
	}
}


PLL3 {
// jk flip flop freq/phase detector
	*ar { arg source, bus, errFac=0.1, errLeak=0.0, baseFreq=0.0, divIn=1, width=0.5, polarity=0;

		var o = InFeedback.ar(bus);


	//	var sourceLarger = ((source.abs > sourceMin) * (source.abs < sourceMax)) * source;
		var inputSign = source.sign;
		var input = (ToggleFF.ar(PulseDivider.ar(inputSign, divIn)) * 2) - 1;


		var freq =  Integrator.ar((o*K2A.ar(errFac)), K2A.ar(errLeak)) + baseFreq;

		var j = input.clip(0,1);
		var k = LFPulse.ar(freq, width: width);
		var case1 = (j <= 0) * (k <= 0);
		var case2 = (j <= 0) * (k > 0);
		var case3 = (j > 0) * (k <= 0);
		var case4 = (j > 0) * (k > 0);

		var case4Res = Select.ar(Changed.ar(case4), [o, 1-o]);
		var o1 = (case1 * o) + (case2 * DC.ar(0)) + (case3 * DC.ar(1)) + (case4 * case4Res);

		Out.ar(bus, o1); // div higher for lower freq, higher for higher
		^Select.ar(polarity, [k, (k*2-1)])
	}
}


