FilterBank {
	*ar { arg input, freqs, rq=1.0;
		var output = BLowPass4.ar(input, freqs, rq);
		for(1, output.size - 1, {arg i;
			var prev = output[(0..(i-1))].sum;
			output[i] = output[i] - prev;
		});
		//		output[output.size - 1] = input - output[(0..(output.size - 2))].sum;
		^(output ++ [input - output.sum])
	}

}




FollowEnv {
	*kr { arg input, followMode = 0, lagTime = 8, followThresh=0.002;
		var env = input.abs.lag(0.1);
		var longTerm = env.lag(lagTime);
		var mini = RunningMin.kr(longTerm).lag(1) + 0.0001;
		var maxi = RunningMax.kr(longTerm, Impulse.kr(1/lagTime)).lag(lagTime);
		var range = (maxi - mini);
		var playing = (maxi > followThresh).lag2ud(0.5, 2);
		var normalEnv = ((env - mini) / range).clip(0,1) * playing;
		var invEnv = 1 - normalEnv;
		var initialEnv = EnvGen.kr(Env([0,0,1], [lagTime, 0.1]), 1);
		var followEnv = SelectX.kr(followMode.lag(0.5), [normalEnv, 1, invEnv]).lag2(lagTime);
		^([normalEnv, followEnv] * initialEnv).lag2ud(0.0, 3)
	}

}


VoiceAnalysis {
	var <>playingBus, <>envBus, analysisData, <>analysisSynth,
	analysisReceiver, <name, inputBus, predictionResponder,
	parameterDistance, collectedParameters;

	*new { | name, input |
        ^super.new.init(name, input)
    }

    init { | nameIn, input |
		name = nameIn;
		inputBus = input;
		collectedParameters = [];


		parameterDistance = {arg p1, p2;
			(p1.isEmpty.not && p2.isEmpty.not).if({
				var f1 = (p1[0].explin(30, 3000, 0, 1) - p2[0].explin(30, 3000, 0, 1)).squared;
				var f2 = (p1[1].explin(30, 3000, 0, 1) - p2[1].explin(30, 3000, 0, 1)).squared;
				var f3 = (p1[2].explin(30, 3000, 0, 1) - p2[2].explin(30, 3000, 0, 1)).squared;
				var a = (p1[5].explin(1, 20, 0, 1) - p2[5].explin(1, 20, 0, 1)).squared;
				var rq = (p1[6].explin(0.1, 4, 0, 1) - p2[6].explin(0.1, 4, 0, 1)).squared;
				var a2 = (p1[7].explin(1, 30, 0, 1) - p2[7].explin(1, 30, 0, 1)).squared;
				((f1 * 20) + (f2 * 10) + (f3 * 10) + a + rq + a2).sqrt
			}, {
				"paramterDistance: warning! empty input".postln;
				0.0
			})
		}
	}

	start {
		Server.default.options.numInputBusChannels = 6;
		Server.default.options.numOutputBusChannels = 6;
		Server.default.waitForBoot({
			var commandName = ("/analysis" ++ name).asSymbol;
			var playingCmd = ("/playing" ++ name).asSymbol;
			var ampCmd = ("/amp" ++ name).asSymbol;

			playingBus = Bus.control(Server.default, 1);
			envBus = Bus.control(Server.default, 1);
			
			analysisReceiver = OSCFunc({ arg msg; analysisData = msg[3..]; }, commandName);

			// todo delay
			analysisSynth = { | followMode = 0, hpf = 20, lpf = 500, focus = 1, playingThresh=0.3, followThresh=0.01 |
				var inpRaw = SelectXFocus.ar(inputBus, SoundIn.ar([0,1,2,3,4,5]), focus, true);
				var inp = Compander.ar(inpRaw*1.5, inpRaw*1.5, 0.2, 1, 0.5) * 1.5;
				var inpAmp = inp.abs.lag(0.25);
				var follow = FollowEnv.kr(inp, followMode, 8, followThresh);
				var playing = follow[0].lag(2) > playingThresh;

				var in = BHiPass.ar(BLowPass.ar(inp, lpf), hpf);
				var analysisInput = Compander.ar(in*2, in*2, 0.2, 1, 0.4) * 1.5;
				var mfccs = FluidMelBands.kr(analysisInput, 50, minFreq: 80, maxFreq: 6000, windowSize: 2048);
				var derivative = (mfccs - Delay1.kr(mfccs)).abs;
				var statsMFCCS = FluidStats.kr(mfccs, ControlRate.ir*0.5);
				var statsDerivative = FluidStats.kr(derivative, ControlRate.ir*0.5);
				var data = statsMFCCS[0] ++ statsMFCCS[1] ++ statsDerivative[0] ++ statsDerivative[1];
				var trig = Impulse.kr(10*playing);
				var trigGui = Impulse.kr(5);
				
				Out.kr(playingBus,  playing);
				Out.kr(envBus,  follow[1]);
				SendReply.kr(trig: trig, cmdName: commandName, values: data);
				
				SendReply.kr(trig: trigGui, cmdName: ampCmd, values: inpAmp);
				SendReply.kr(trig: trigGui, cmdName: playingCmd, values: playing);
				

			}.play;
		})
	}

	predict { | nn, cb |
		var commandName = ("/pred" ++ name).asSymbol;
		predictionResponder.isNil.not.if({ predictionResponder.free });
		predictionResponder = OSCFunc({ arg msg;
			var parameters = msg[1..].postln;
			cb.(parameters);
		}, commandName);
		nn.sendMsg("/nn/predcb", *([commandName] ++ analysisData));
	}

	



	predictClosest { | nn, cb, parameters, candidates = 5 |

		var idx;
		var commandName = ("/predcollect" ++ name).asSymbol;
		collectedParameters = [];
		predictionResponder.isNil.not.if({ predictionResponder.free });
		predictionResponder = OSCFunc({ | msg |
			collectedParameters = collectedParameters.add(msg[1..]);},
			commandName);
		Task({
			candidates.do({
				nn.sendMsg("/nn/predcb", *([commandName] ++ analysisData));
				0.1.wait;
			});
			0.1.wait;
			idx = collectedParameters.collect({arg p; parameterDistance.(parameters, p)}).minIndex;
			cb.(collectedParameters[idx]);
		}).start;
	}
	
}


VoiceSynth {
	var parameters, nn, <>synth, id, envBus, <>outBus, dswitch, <>currentAmp, <>stateOn,
	<>continuousTime, <>continuousTransPerc, continuousTask, eventTask, <>analysis;

	*initClass {

		 StartUp.add {
			 SynthDef(\voiceSynth, { arg freq1 = 80, freq2 = 212, freq3 = 1321, id = 0, 
				 ratio1 = 1, ratio2 = 1, ratio3 = 1, a = 2.0, rq = 3, dswitch = 0, 
				 lagTime = 0.1, bus = 0, amp = 0.25, on = 0, envLag = 10, envBus, envType=0,
				 a2 = 1, selNL = 0, currentAmp, factors = #[1,1,1,1,1,1,1,1], neighborBus1=0, neighborBus2=0, neighborCoupling=0;
				 var ampMod, del1, del2, del3, del12, del22, del32, filAmps, filTrig,
				 del1a, del2a, del3a, del1b, del2b, del3b, env, envOut, switchEnv,
				 gateA, gateB, d1, d2, d3, input, output, fil, fb, filTrigFB,
				 //				 localIn = LocalIn.ar(3+8, 0.8);
				 //				 fb = localIn[(0..2)];
				 //				 filTrigFB = localIn[3..];
				 neighbor = (InFeedback.ar(neighborBus1) + InFeedback.ar(neighborBus2)) * neighborCoupling;
				 factors = A2K.kr(FilterBank.ar(neighbor, Array.geom(8, 120, 1.7)).abs.lag2(0.2));
				 factors = (1 - factors).clip(0,1) * (factors.sum/factors.size).linlin(0,0.5,1,2);
				 fb = LocalIn.ar(3, 0.8);
				 input = (((a.lag2(lagTime) * (fb.sum)) + 0.5).sin  * 0.5) + neighbor; 
				 fb = Compander.ar(fb*1.2, fb*1.2, 0.08, 0.9, 0.4, 0.01, 0.1) * 2;
				 d1 = (1/freq1) - ControlDur.ir;
				 d2 = (1/freq2) - ControlDur.ir;
				 d3 = (1/freq3) - ControlDur.ir;
				 
				 switchEnv = EnvGen.ar(Env.asr(1, 1.0, 1, \sine), dswitch, timeScale: lagTime);
				 gateA = (switchEnv <= 0) + Impulse.kr(0);
				 gateB = (switchEnv >= 1) + Impulse.kr(0);
	
				 del1a = DelayC.ar(BBandPass.ar(input*0.5+fb[0],
					 Gate.kr(freq1*ratio1, gateA).lag2(0.01).clip(20,10000),
					 Gate.kr(rq, gateA).lag2(0.01)),
					 0.2, Gate.kr(d1, gateA).lag2(0.01));	
				 del2a = DelayC.ar(BBandPass.ar(input*0.5+fb[1],
					 Gate.kr(freq2*ratio2, gateA).lag2(0.01).clip(20,10000),
					 Gate.kr(rq, gateA).lag2(0.01)),
					 0.2, Gate.kr(d2, gateA).lag2(0.01));	
				 del3a = DelayC.ar(BBandPass.ar(input*0.5+fb[2],
					 Gate.kr(freq3*ratio3, gateA).lag2(0.01).clip(20,10000),
					 Gate.kr(rq, gateA).lag2(0.01)),
					 0.2, Gate.kr(d3, gateA).lag2(0.01));	

				 del1b = DelayC.ar(BBandPass.ar(input*0.5+fb[0],
					 Gate.kr(freq1*ratio1, gateB).lag2(0.01).clip(20,10000),
					 Gate.kr(rq, gateB).lag2(0.01)),
					 0.2, Gate.kr(d1, gateB).lag2(0.01));	
				 del2b = DelayC.ar(BBandPass.ar(input*0.5+fb[1],
					 Gate.kr(freq2*ratio2, gateB).lag2(0.01).clip(20,10000),
					 Gate.kr(rq, gateB).lag2(0.01)),
					 0.2, Gate.kr(d2, gateB).lag2(0.01));	
				 del3b = DelayC.ar(BBandPass.ar(input*0.5+fb[2],
					 Gate.kr(freq3*ratio3, gateB).lag2(0.01).clip(20,10000),
					 Gate.kr(rq, gateB).lag2(0.01)),
					 0.2, Gate.kr(d3, gateB).lag2(0.01));	
				 
				 del1 = SelectX.ar(DelayC.ar(switchEnv,0.1,0.1), [del1b, del1a]);
				 del2 = SelectX.ar(DelayC.ar(switchEnv,0.1,0.1), [del2b, del2a]);
				 del3 = SelectX.ar(DelayC.ar(switchEnv,0.1,0.1), [del3b, del3a]);
	
				 del1 = SelectX.ar(selNL.lag2(lagTime), [((a2.lag2(lagTime) * del1) + 0.5).sin,
					 (del1 * a2.lag2(lagTime)).fold(-0.3, 0.8)]);
				 del1 = LeakDC.ar(del1);

				 amp = amp.lag3(0.6);
				 
				 factors = factors.lag2(5);
				 fil = FilterBank.ar(del1, Array.geom(8, 120, 1.7)) * factors.pow(3); //* filTrigFB.lag2(6).clip(0.2,1);
				 // filAmps = A2K.kr((fil*amp).abs.lag(2));
				 // filTrig = 1 - Schmidt.kr(Integrator.kr(filAmps, 0.9999), 50, (200*factors).clip(100,2000));
				 // SendReply.kr(trig: Impulse.kr(1), cmdName: '/filters', values: filAmps, replyID: id);
				 del1 = fil.sum;

				 env = SelectX.kr(envType.lag2(4), [EnvGen.kr(Env.asr(envLag*0.66,1,envLag,[3,-3]), DelayN.kr(on, 0.1, 0.1)), In.kr(envBus).lag2ud(0.0,6)]);

				 LocalOut.ar(([del1, del2, del3] * (-1) * ((env*0.2)+0.8)));// ++ K2A.ar(filTrig));
				 output = fb.sum*amp*env;
				 Out.kr(currentAmp, A2K.kr(output.abs.lag(0.1)));
				 Out.ar(bus, Limiter.ar(LPF.ar(output, 10000), 0.8));
			 }).add;
        }
		
	}
	
	*new { | nn, id, voiceAnalysis |
        ^super.new.init(nn, id, voiceAnalysis)
    }

    init { | nnIn, idIn, voiceAnalysis |
		id = idIn;
		parameters = Array.fill(9, { 0 });
		analysis = voiceAnalysis;
		nn = nnIn;
		continuousTime = 10;
		continuousTransPerc = 0.9;
		dswitch = 1;
		stateOn = false;
		parameters = [];
    }

	start {
		Server.default.options.numInputBusChannels = 6;
		Server.default.options.numOutputBusChannels = 6;
		Server.default.waitForBoot({
			outBus.isNil.if({ outBus = Bus.audio(Server.default, 1) });
			currentAmp = Bus.control(Server.default, 1);
			synth = Synth(\voiceSynth, [\id, id, \bus, outBus, \amp, 0.0, \envBus, analysis.envBus, \currentAmp, currentAmp]);
		})

	} 
	
	set { | parametersIn |
		parametersIn.postln;
		parameters = parametersIn;
		synth.set(
			\dswitch, dswitch,
			\freq1, parameters[0], \freq2, parameters[1],
			\freq3, parameters[2], \ratio2, parameters[3],
			\ratio3, parameters[4], \a, parameters[5],
			\rq, parameters[6], \a2, parameters[7],
			\selNL, parameters[8]);
		dswitch = (dswitch + 1).mod(2);
	}

	on {
		synth.set(\on, 1);
		stateOn = true;
	}

	off {
		synth.set(\on, 0);
		stateOn = false;
	}


	continuousOn {
		var thisSynth = this;
		this.continuousOff;
		this.eventOff;
		synth.set(\envType, 1);
		continuousTask = Task({
			inf.do({
				synth.set(\lagTime, continuousTransPerc * continuousTime);
				(analysis.playingBus.getSynchronous > 0.5).if({
					analysis.predictClosest(nn, { |p| thisSynth.set(p) }, parameters);
				});
				continuousTime.wait;
			})
		});
		continuousTask.start;
	}

	continuousOff {
		continuousTask.isNil.not.if({ continuousTask.stop });
	}

	eventOn {
		var thisSynth = this;
		this.continuousOff;
		this.eventOff;
		synth.set(\envType, 0);
		eventTask = Task({
			inf.do({
				((currentAmp.getSynchronous < 0.0001) && (analysis.playingBus.getSynchronous > 0.5) && stateOn.not).if({
					analysis.predict(nn, { |p| thisSynth.set(p) });
					"turning on".postln;
					thisSynth.on;
				});

				((currentAmp.getSynchronous > 0.01) && (analysis.playingBus.getSynchronous < 0.01)).if({
					"turning off".postln;
					thisSynth.off;
				});

				0.3.wait;
			});
		});
		eventTask.start;
	}

	eventOff {
		eventTask.isNil.not.if({ eventTask.stop });
	}
	
}



VocalMain {
	var <>analyses, <>synths, win, amps, playing, knobs, labels, ampFuncs, playingFuncs;
	
	*new { | nn |
        ^super.new.init(nn);
    }

    init { | nn |

		analyses = 6.collect({ | i |
			var name = "analysis" ++ i.asString;
			VoiceAnalysis.new(name, i);
		});
		synths = analyses.collect({ | vAn, id |
			VoiceSynth.new(nn, id, vAn);
		});

	}

	createGUI { arg names;
		var playingCmds = names.collect({arg n;  ("/playing" ++ n).asSymbol});
		var ampCmds = names.collect({arg n;  ("/amp" ++ n).asSymbol});

		amps = Array.fill(6, { LevelIndicator().style_(\continuous).value_(0) }); 
		playing = Array.fill(6, { Button()
			.states_([["", Color.white, Color.white], ["Playing", Color.white, Color.red]])
			.value_(0) });
		//		knobs = Array.fill(6, {Knob().value_(0.5)});
		labels = Array.fill(6, {arg i; StaticText().string_(names[i]).align_(\center).maxHeight_(50)});
		win = Window("Manifold", 1000@800).front.layout_(VLayout(HLayout(*labels),HLayout(*amps),HLayout(*playing)));	

		names.do({arg n, i; labels[i].string_(n)});
		
		
		ampFuncs = ampCmds.collect({arg cmd, i;
			OSCFunc({ arg msg;
				var val = msg[3];
				{ amps[i].value_(val) }.defer;
			}, cmd);
		});
		
		playingFuncs = playingCmds.collect({arg cmd, i;
			OSCFunc({ arg msg;
				var val = msg[3];
				{ playing[i].value_(val) }.defer;
			}, cmd);
		});
		
	}

	
	start {
		Task({
			analyses.do(_.start);
			4.wait;
			{ this.createGUI(analyses.collect(_.name)) }.fork(AppClock);


			synths.do(_.start);
			4.wait;
			synths.do({|sy, i|
				sy.synth.set(\neighborBus1, synths[(i+1).mod(6)].outBus.index);
				sy.synth.set(\neighborBus2, synths[(i+2).mod(6)].outBus.index);
				sy.continuousOn;
			});
			"Started".postln;
		}).start;
	}

}


// 1. Continuous S&H with transition
// continuous very slowly follow with lagged transitions, Sample and Transition
// - controllable is sample interval and lag ratio (perc of sample interval)
  
// 2. "event"
// "events" fading in and out (and possibly transitioning slightly)
// with lengths (freezing)

// 3 spawing
// "spawning" following, sung input leads to several (quasi) simultaneous electronic outgrowths
// multiple events with slight difference (one mic -> multiple models)


// - amplitude for each model
// - low pass/high pass bias of analysis input, one knob per model
// - delay of analysis/amp env following, one knob per model
// - amp mode (for all)
// - "freezing" (duration of events)
// - mode: spawn, follow, event

// it looks like this implements all but spawining...??

