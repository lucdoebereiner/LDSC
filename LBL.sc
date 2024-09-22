LBLControl {
	classvar id = 0;
	var <key, spec, <currentVal, <>synthID, synthPars, <>updateSynth, respPath, oscId, oscdef, <>synthName, <>func;

	*new { arg key, spec, currentVal, synthName, synthPars, respPath, func;
		^super.new.init(key, spec, currentVal, synthName, synthPars, respPath, func)
    }

	init { arg keyIn, specIn, currentValIn, synthNameIn, synthParsIn, respPathIn, funcIn;
		oscId = (keyIn ++ id).asSymbol;
		func = funcIn;
		synthName = synthNameIn;
		id = id + 1;
		spec = specIn;
		updateSynth=false;
		currentVal = currentValIn;
		synthPars = synthParsIn;
		respPath = respPathIn;
		key = keyIn;
	}

	listen { arg addr;
		oscdef = OSCdef(oscId, {arg msg;
			currentVal = spec.map(msg[1]);
			if (func.isNil.not) { func.(msg.drop(1), synthID) };
			this.send(addr);
		}, key, addr);
	}

	stopListening {
		oscdef.free;
	}

	setSynth {
		synthID.set(synthPars, currentVal)
	}

	send { arg addr;
		if (updateSynth) {
			this.setSynth();
		};
		addr.sendMsg(key, spec.unmap(currentVal));
		addr.sendMsg(respPath, spec.units ++ ": " ++ currentVal.round(0.0001));
	}

	set { arg addr, val;
		currentVal = val;
		this.send(addr);
	}

	setSpec {arg specIn, addr;
		spec = specIn;
		addr.sendMsg(key, spec.unmap(currentVal));
	}

	// includesParameter {arg p;
	// 	if (synthPars.isArray) {
	// 		^synthPars.includes(p)
	// 	} {
	// 		^s(synthPars == p)
	// 	}
	// }

	// checks if parameter exists in synthPars
	setParameter {arg addr, p, v;
		if (synthPars.isArray) {
			if (synthPars.includes(p)) {
				var idx = synthPars.indexOf(p);
				currentVal[idx] = v;
				this.send(addr);
			}
		} {
			if (synthPars == p) {
				this.set(addr, v);
			}
		}
	}

}



LBLControlXYMLP : LBLControl {
	var xybuf, mlp, <>paramsbuf, toggleFun;

	*new { arg key, synthName, toggleFun, path;
		^super.new.initMLP(key, synthName, toggleFun, path)
    }

	initMLP { arg keyIn, synthNameIn, toggleFunIn, path;
		oscId = (keyIn ++ id).asSymbol;
		synthName = synthNameIn;
		id = id + 1;
		updateSynth=false;
		key = keyIn;
		toggleFun = toggleFunIn;
		{
			xybuf = Buffer.alloc(Server.default, 2);

			mlp = FluidMLPRegressor(Server.default,
				[10,10],
				activation:FluidMLPRegressor.sigmoid,
				outputActivation:FluidMLPRegressor.sigmoid,
				maxIter: 1000,
				learnRate:0.1,
				batchSize:1,
				validation:0
			);
			Server.default.sync;
			mlp.read(path);
			paramsbuf = Buffer.alloc(Server.default, 9);
		}.fork;
	}

	listen { arg addr;

		oscdef = OSCdef(oscId, {arg msg;

			xybuf.setn(0,[msg[1],msg[2]]);
			{ toggleFun.(msg[3], synthID) }.fork;

			mlp.predictPoint(xybuf,paramsbuf,{
				/*paramsbuf.getn(0,9,{
					arg prediction;
					currentVal = prediction;
					this.send(addr);*/
				});

		}, key, addr);
	}

// setSynth {
// 	synthID.set( *([ synthPars, currentVal].lace) )
// }
//
//
	send { arg addr;
	/*	if (updateSynth) {
			this.setSynth();
		};*/
	}

	setSpec {arg specIn, addr;
		//spec = specIn;
		//addr.sendMsg(key, spec.unmap(currentVal));
	}

}


LBLControlXY : LBLControl {
	/*var <key, specX, specY, <currentVal, synthID, synthPars, respPathX, respPathY, oscdef;

	*new { arg key, specX, specY, currentVals, synthID, synthPars, respPathX="", respPathY;
		^super.new.init(key, specX, specY, currentVals, synthID, synthPars, respPathX, respPathY)
    }

	init { arg keyIn, specXIn, specYIn, currentValsIn, synthIDIn, synthParsIn, respPathXIn, respPathYIn;
		specX = specXIn;
		specY = specYIn;
		specX.default = currentValsIn[0];
		specY.default = currentValsIn[1];
		currentVal = currentValsIn;
		synthID = synthIDIn;
		synthPars = synthParsIn;
		respPathX = respPathXIn;
		respPathY = respPathYIn;
		key = keyIn;
	}*/

	listen { arg addr;
		//key.post; " is listening".postln;

		oscdef = OSCdef(oscId, {arg msg;
//			msg.postln;
//			currentVal = [specX.map(msg[1]), specY.map(msg[2])];
			currentVal = [spec[0].map(msg[1]), spec[1].map(msg[2])];
			if (func.isNil.not) { func.(msg[3], synthID) };
			this.send(addr);
		}, key, addr);
	}

	setSynth {
	//	[synthPars[0], currentVal[0], synthPars[1], currentVal[1]].postln;
		synthID.set(synthPars[0], currentVal[0], synthPars[1], currentVal[1])
	}

	send { arg addr;
		if (updateSynth) {
			this.setSynth();
		};
		addr.sendMsg(key, spec[0].unmap(currentVal[0]), spec[1].unmap(currentVal[1]));
		addr.sendMsg(respPath[0], spec[0].units ++ ": " ++ currentVal[0].round(0.0001));
		addr.sendMsg(respPath[1], spec[1].units ++ ": " ++ currentVal[1].round(0.0001));
	}

	// set { arg addr, vals;
	// 	currentVal = vals;
	// 	this.send(addr);
	// }
	//
	// setSpec {arg specs;
	// 	specX = specs[0];
	// 	specY = specs[1];
	// }
	setSpec {arg specIn, addr;
		spec = specIn;
		addr.sendMsg(key, spec[0].unmap(currentVal[0]), spec[1].unmap(currentVal[1]));
	}


}

LBLControlOptions : LBLControl {
	var callback, units;

	*new { arg key, options, units, currentVal, synthName, synthPars, callback=nil, respPath="";
		^super.new.initControlOptions(key, options, units, currentVal, synthName, synthPars, callback, respPath)
    }

	initControlOptions { arg keyIn, optionsIn, unitsIn, currentValIn, synthNameIn, synthParsIn, callbackIn, respPathIn;
		oscId = (keyIn ++ id).asSymbol;
		synthName = synthNameIn;
		id = id + 1;
		spec = optionsIn;
		units = unitsIn;
		currentVal = currentValIn;
		synthPars = synthParsIn;
		respPath = respPathIn;
		key = keyIn;
		callback = callbackIn;
		updateSynth = false;
	}

	listen { arg addr;
		oscdef = OSCdef(oscId, {arg msg;
			currentVal = spec[msg[1]];
			this.send(addr);
		}, key, addr);
	}

	/*setSynth {
		synthID.set(synthPars, currentVal)
	}*/

	send { arg addr;
	//	if (updateSynth) {
		//	this.setSynth();
	//	};
		callback.(currentVal, synthID);
		addr.sendMsg(key, spec.indexOf(currentVal));
		addr.sendMsg(respPath, units ++ ": " ++ currentVal.round(0.0001));
	}

	/*set { arg addr, val;
		currentVal = val;
		this.send(addr);
	}

	setSpec {arg newOptions;
		options = newOptions;
	}

*/

	setSpec {arg specIn, addr;
		spec = specIn;
		addr.sendMsg(key, spec.indexOf(currentVal));
	}

}

LBLControlCollection {
	var <>controls, addr;

	*new { arg addr;
		^super.new.init(addr);
	}


	init { arg addrIn;
		addr = addrIn;
		controls = Dictionary();

		CmdPeriod.add( {
			//"cmdp".postln;
			controls.do({arg c; c.updateSynth = false;})
		})
	}

	add {arg control;
		controls.put(control.key, control);
	}

	listen {
		controls.do({arg c; c.listen(addr)});
		controls.do({arg c; c.send(addr)});
	}

	get {arg k;
		^controls.at(k)
	}

	getAndListen {arg k;
		this.get(k).listen(addr);

	}

	getVal {arg k;
		^controls.at(k).currentVal
	}

	set {arg key, val;
		controls.at(key).set(addr, val)
	}

	setParameter {arg par, v;
		controls.do({arg c;
			c.setParameter(addr, par, v);
		})
	}

	setParameterList {arg lst;
		lst.clump(2).do({arg pair;
			this.setParameter(pair[0], pair[1])
		})
	}

	setSpec {arg key, specs;
		controls.at(key).setSpec(specs, addr)
	}

	replace { arg control;
		controls.at(control.key).stopListening;
		this.add(control);
	}

	setSynthUpdate {arg synthDic, on=true;
		controls.do({arg c; c.synthID = synthDic.at(c.synthName); c.updateSynth = on;})
	}
}

LBL {
	var schedule, time, addresses, routine, callback;

	*new {arg schedule, addresses, callback;
		^super.new.init(schedule, addresses, callback)
	}

	init {arg scheduleIn, addressesIn, callbackIn;
		schedule = scheduleIn.collect({arg event; [event[0], Routine({ event[1].() })]});
		time = -5;
		addresses = addressesIn;
		callback = callbackIn;
	}




	boot {
		Server.default.options.blockSize = 1;
		Server.default.options.numInputBusChannels = 3;
		Server.default.options.numOutputBusChannels = 6;

		Server.default.waitForBoot({
			~recBuf1 = Buffer.alloc(Server.default, 48000);
			~recBuf2 = Buffer.alloc(Server.default, 48000);
			~recBuf3 = Buffer.alloc(Server.default, 48000);

			~pll1 = Bus.audio(Server.default, 1);
			~pll2 = Bus.audio(Server.default, 1);
			~pll3 = Bus.audio(Server.default, 1);
			~pll4 = Bus.audio(Server.default, 1);
			~pll5 = Bus.audio(Server.default, 1);
			~pll6 = Bus.audio(Server.default, 1);
			~pll7 = Bus.audio(Server.default, 1);
			~pll8 = Bus.audio(Server.default, 1);
			~pll9 = Bus.audio(Server.default, 1);
			~pll10 = Bus.audio(Server.default, 1);
			~pll11 = Bus.audio(Server.default, 1);
			~pll12 = Bus.audio(Server.default, 1);
			~pll13 = Bus.audio(Server.default, 1);
			~pll14 = Bus.audio(Server.default, 1);
			Server.default.sync;

			SynthDef(\pll, {arg out1=0, out2=3, amp=1, errFreq1=0, errFreq2=0, errFreq3=0,
				leak1=0.0, leak2=0.0, leak3=0.0, freq1=60, freq2=370, freq3=1060,
				amp1=0.5,amp2=0.5,amp3=0.5,ampLag1=0.2,ampLag2=0.2,ampLag3=0.2,
				combRate1=0.08,combAmp1=0,combRate2=0.08,combAmp2=0,combRate3=0.08,combAmp3=0,
				combOnOff1=0,combOnOff2=0,combOnOff3=0, filLag = 0.05, pllParLag = 0.05,
				filSel=0,fil1min=80, fil1max=8000, fil2min=80, fil2max=8000,fil3min=80,fil3max=8000,
				filModSpeed1=0.07,filModSpeed2=0.07,filModSpeed3=0.07,
				fil1=300,fil2=400,fil3=500,rq1=0.1,rq2=0.1,rq3=0.1,
				hpf1amp=0,hpf2amp=0,hpf3amp=0,hpfFreq1=200, hpfFreq2=2320, hpfFreq3=2000,
				del1=0,del2=0,del3=0,combOffWeight=1,
				div1=1, div2=1, div3=1, lpfHpf1=0.2, lpfHpf2=0.2, lpfHpf3=0.2, lpfHpfMode=0, combHighAmp=0.3,
				b1,b2,b3;
				var combOn,rev;
				var in = LocalIn.ar(3);
				var pulse1 = PLL3.ar(DelayC.ar(in[1], 3, del1.lag2(0.3)), b1, errFreq1.lag2(pllParLag), leak1.lag2(pllParLag), freq1.lag2(0.05), div1);
				var pulse2 = PLL3.ar(DelayC.ar(in[2], 3, del2.lag2(0.3)), b2, errFreq2.lag2(pllParLag), leak2.lag2(pllParLag), freq2.lag2(0.05), div2);
				var pulse3 = PLL3.ar(DelayC.ar(in[0], 3, del3.lag2(0.3)), b3, errFreq3.lag2(pllParLag), leak3.lag2(pllParLag), freq3.lag2(0.05), div3);
				var pulse = [pulse1, pulse2, pulse3];
				var lpf = RLPF.ar(pulse,
					Select.ar(filSel, [LFNoise1.ar([filModSpeed1, filModSpeed2, filModSpeed3].lag2(0.03)).exprange([fil1min,fil2min,fil3min].lag2(1), [fil1max,fil2max,fil3max].lag2(1)), //TExpRand.ar([fil1min,fil2min,fil3min], [fil1max,fil2max,fil3max], pulse).lag2(0.02)
						TChoose.ar(pulse, [ TExpRand.ar([30,30,30],[120,120],pulse), TExpRand.ar([800,800,800],[3000,3000,3000],pulse)]).lag2(0.02)

						, K2A.ar([fil1,fil2,fil3]).lag2(filLag)]),
					Select.ar(filSel, [LFNoise1.ar([filModSpeed1, filModSpeed2, filModSpeed3].lag2(0.03)).range(0.02,0.2), TExpRand.ar(0.02,0.2,pulse).lag2(0.02), K2A.ar([rq1, rq2, rq3]).lag2(filLag)]));
				var snd = SelectX.ar(Select.ar(lpfHpfMode, [ K2A.ar([lpfHpf1, lpfHpf2, lpfHpf3]).lag2(filLag),
					TChoose.ar(pulse, K2A.ar([0.0, 1.0, 0.3, 0.8])) ]),
				[lpf, RHPF.ar(pulse, [hpfFreq1, hpfFreq2, hpfFreq3].lag2(filLag), 0.15, [hpf1amp, hpf2amp, hpf3amp].lag2(1))]);

//				Out.ar(out2, snd * ([amp1,amp2,amp3] > 0.01));
				Out.ar(out2, Gate.ar(snd, [amp1,amp2,amp3] > 0.01));


				snd = snd * [amp1,amp2,amp3].lag2([ampLag1, ampLag2, ampLag3]);

				snd = snd + CombC.ar(LeakDC.ar(snd)*combHighAmp.lag2(0.3), 0.2, LFNoise1.kr(0.08!3).exprange(0.0001,0.0002), 20);


				combOn = Select.ar([combOnOff1,combOnOff2,combOnOff3], [DC.ar(1!3), TWChoose.ar(pulse, [DC.ar(0), DC.ar(1)], [K2A.ar(combOffWeight), DC.ar(1)], 1)]);
				snd = snd + CombC.ar(LeakDC.ar(snd)*[combAmp1,combAmp2,combAmp3].lag2(3)*combOn, 0.2, LFNoise1.kr([combRate1,combRate2,combRate3].lag2(0.3)).exprange(0.05,0.09), 18);

				snd = Compander.ar(snd, snd, 0.4, 1, 0.6);

				LocalOut.ar(snd);

				snd = snd.tanh;
				rev = JPverb.ar(Splay.ar(snd), t60: 2, damp: 0.2, size: 2, earlyDiff: 0.01);
				snd = snd + ([rev[0], (rev[0]+rev[1])*0.7, rev[1]] * 0.055);
				Out.ar(out1, snd * amp.lag2(0.3));
			}).send(Server.default);




			SynthDef(\fbpll, {arg out1=0, out2=3, amp=0, ampLag=0.01, paramsbuf, recBuf, pllBus1, pllBus2, extIn, extIn2, extAmount=0, loop=0, freqMax=100, amp2=1;

				var val = FluidBufToKr.kr(paramsbuf, 0, 9);
				var in = LocalIn.ar(2);
				var in1 = SelectX.ar(extAmount.lag2(40), [in[1], (InFeedback.ar(extIn) + InFeedback.ar(extIn2))]);

				var freqL, phaseL, looped, selected;

				var pll1 = PLL3.ar(in1, pllBus1, in1.linexp(-1,1, val[0].linexp(0,1,10,1800), val[1].linexp(0,1,50,1900)), val[2].linlin(0,1,0.4,0.989).lag(0.01), val[3].linexp(0,1,8,1000), 1, 1);
				var pll2 = PLL3.ar(in[0], pllBus2, in[0].linexp(-1,1, val[4].linexp(0,1,10,1800), val[5].linexp(0,1,50,1900)), val[6].linlin(0,1,0.4,0.989).lag(0.01), val[7].linexp(0,1,8,1000), 1, 1);
				pll1 = (BPF.ar(pll1, val[8].linexp(0,1,40,8000).lag2(0.2), 0.7) * 0.6) + (pll1 * 0.8);
			//	loop = K2A.ar(loop).poll;
				//loop = MouseX.kr() > 0.5;

				RecordBuf.ar(pll1, recBuf, run: (1-loop));

				LocalOut.ar([pll1, pll2]);
				freqL = Gate.kr(ZeroCrossing.ar(pll1), (1-loop));
				freqL = freqL.fold(1, freqMax.linexp(0,1,3,2000));
				phaseL = Phasor.ar(0, 1, 0, SampleRate.ir/freqL);
				looped = BufRd.ar(1, recBuf, phaseL.fold(0, BufFrames.ir(recBuf)));
				selected = Select.ar(loop, [pll1, looped]);

				selected = LeakDC.ar((selected-0.5) * 2);
				selected = BPeakEQ.ar(selected, 70, 1, 4);
				selected = BPeakEQ.ar(selected, 200, 1, 4);
				selected = Compander.ar(selected*2, selected*2, 0.7, 1, 0.6) * 1.5;
				//selected = selected + CombC.ar(LeakDC.ar(selected)*0.1, 0.2, LFNoise1.kr(0.08).exprange(0.0001,0.0002), 20);

				Out.ar(out1, selected.tanh * amp.lag2(ampLag) * amp2);
				Out.ar(out2, selected * amp.lag2(ampLag));


			}).send(Server.default);

			Server.default.sync;

			callback.();

		});
	}

	start {arg start= (-5), stop=200;
	/*	var startEvent = 0, found = false;
		var fastEvents = [], scheduledEvents = [];
		var fastRoutine, scheduleRoutine;*/

		var eventIdx = 0;

		time = -5;

/*		while ({found.not && ((startEvent + 1) < schedule.size)}) {
			if (start >= schedule[startEvent+1][0]) {
				found = true;
			} {
				startEvent = startEvent + 1;
			}
		};*/

/*		if (startEvent > 0) {
			fastEvents = schedule[(0..(startEvent-1))];
			scheduledEvents = schedule[(startEvent..(schedule.size - 1))];
		} {
			scheduledEvents = schedule;
		}*/

		routine = Routine({
			"starting routine".postln;
			stop.do({
				var timeInc = 0.03;
				addresses.do({ arg addr; addr.sendMsg(\time, time.asString) });

				if ((time >= (start-5)) || (start < 0)) {
					timeInc = 1;
				};

				if (schedule.size > eventIdx) {
					if (time == schedule[eventIdx][0]) {
						schedule[eventIdx][1].play;
						eventIdx = eventIdx + 1;
					};
				};

				time = time + 1;
				timeInc.wait;
			})


		}).play;
	}

	stop {
		routine.stop;

	}

	reset {
		routine.reset;

	}

}

