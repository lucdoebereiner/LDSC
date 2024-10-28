Harmonic {
	var <partial;
	var <octave;
	var <fundamental;
	var <midi;

	*new { |partial, octave, fundamental|
		^super.newCopyArgs(partial, octave, fundamental).init
	}

	*rndInRange {|maxPartial, octaves, fundamental|
		^Harmonic.new(1.rrand(maxPartial.asInteger), octaves.choose, fundamental)
	}

	*isEvent {|h|
		^(h.isRest.not && h.isNil.not)
	}

	init {
		var pitchClass = (fundamental*partial).cpsmidi.mod(12.0);
		midi = (((octave+1)*12) + pitchClass);
	}

	interval {|h2|
		var p1 = this.midi;
		var p2 = h2.midi;
		^(p1.max(p2) - p1.min(p2))
	}

	isFifth {|h2|
		var p1 = this.midi;
		var p2 = h2.midi;
		var interv = (p1.max(p2) - p1.min(p2)).mod(12);
		//interv.postln;
		^((interv - 7).abs < 0.1)
	}

	isOctave {|h2|
		var p1 = this.midi;
		var p2 = h2.midi;
		var interv = (p1.max(p2) - p1.min(p2)).mod(12);
		^(interv.abs < 0.1)
	}

	print {
		format("<P %, M: %>", this.partial, this.midi.round(0.1)).post
	}

	cps {
		^this.midi.midicps
	}

	*primeFactors { |n|
		var factors = [];
		var divisor = 2;

		// Handle 1 and negative numbers
		if (n < 2) {
			^[n]
		} {
			while ({n > 1}) {
				if (n % divisor == 0) {
					factors = factors.add(divisor);
					n = n.div(divisor);
				} {
					divisor = divisor + 1;
				}
			};
			^factors
		}
	}

	// gradus suavis
	*gradusRatio {|a,b|
		var factors = Harmonic.primeFactors(a.lcm(b));
		var counts = factors.as(Set).as(Array).collect{|e| [e, factors.occurrencesOf(e)]};
		^(1 + counts.collect{|c| c[1] * (c[0] - 1) }.sum)
	}

	*prIntoSameOct {|a,b|
		var smaller = a.min(b);
		var bigger = a.max(b);
		while ({ (bigger / smaller) > 2 }, {
			smaller = smaller * 2;
		})
		^[smaller, bigger]
	}

	gradus {|h2|
		var gcd;
		var freq1 = (this.partial*this.fundamental).asInteger;
		var freq2 = (h2.partial*h2.fundamental).asInteger;
		var freqs = Harmonic.prIntoSameOct(freq1, freq2);
		freq1 = freqs[0];
		freq2 = freqs[1];
		gcd = freq1.gcd(freq2);
		freq1 = freq1 / gcd;
		freq2 = freq2 / gcd;
		^Harmonic.gradusRatio(freq1.asInteger, freq2.asInteger)
	}


	*gradusChord {|chord|
		^chord.collectUniquePairs{|h1, h2| h1.gradus(h2)}
	}

	isRest {
		^false
	}

}


HarmonicInDomain : Harmonic {
	var <idx;

	prSetIdx {|domainIdx|
		idx = domainIdx
	}


	set {|domainIdx, domain|
		var nextH = domain.at(domainIdx);
		partial = nextH.partial;
		fundamental = nextH.fundamental;
		octave = nextH.octave;
		idx = domainIdx;
	}

	next {|domain|
		(idx >= domain.maxIdx).if({
			^nil
		}, {
			var nextH = domain.at(idx+1);
			partial = nextH.partial;
			fundamental = nextH.fundamental;
			octave = nextH.octave;
			idx = idx + 1;
		})
	}

	reset {|domain|
		this.set(0, domain)
	}
}


HarmonicDomain {
	var <domain, <maxIdx;

	*new {|maxPartial, octaves, fundamental|
		^super.new.init(maxPartial, octaves, fundamental)
	}

	init {|maxPartial, octaves, fundamental|
		var i = 0;
		maxPartial.do{|p|
			var partial = p + 1;
			octaves.do{|oct|
				domain = domain.add(HarmonicInDomain(partial, oct, fundamental));
				i = i + 1;
			}
		};
		domain = domain.scramble;
		this.prReIndexDomain;
	}

	prReIndexDomain {
		maxIdx = domain.size - 1;
		domain.do{|h,i|
			h.prSetIdx(i)
		};
	}

	at {|idx|
		^domain[idx]
	}

	similarWithinMidiRange {|h, maxDiff|
		^domain.select{|h2| (h.midi - h2.midi).abs <= maxDiff}
	}

	// closer pitches more likely
	chooseWithinMidiRange {|h, maxDiff, steepness=2|
		var possibilities = this.similarWithinMidiRange(h, maxDiff);
		var sorted = possibilities.sort{|p1, p2| (p1.midi - h.midi).abs < (p2.midi - h.midi).abs};
		var idx = Array.fill(steepness, {sorted.size.linrand}).minItem;
	//	h.print; sorted[0].print;
		//" ".postln;
		^sorted[idx]
	}

	choose {
		^domain.choose
	}

	selectRegisters {|registers|
		domain = domain.select{|h|
			registers.any{|r|
				var mini = r[0].min(r[1]);
				var maxi = r[0].max(r[1]);
				(h.midi <= maxi) && (h.midi >= mini)
			}
		};
		this.prReIndexDomain;
	}

	closestHarmToFreq {|freq|
		var diffs = domain.collect{|h| (h.cps - freq).abs};
		^domain[diffs.minIndex]
	}
}

Voice {
	var <events;

	*new {
		^super.newCopyArgs([])
	}

	at {|i|
		^events[i]
	}

	print {
		"Voice: ".post;
		events.do{|e| e.isRest.if({"Rest".post}, {e.print}); " ".post}
	}

	size {
		^events.size
	}

	add {|e|
		events = events.add(e.deepCopy)
	}

	addRest {
		events = events.add(Rest())
	}

	dropLast {
		events = events.drop(-1)
	}

	last {
		^events.last
	}

	lastN {|n|
		^events.keep(n * (-1))
	}

	lastNNoRests {|n|
		^events.keep(n * (-1)).select{|e| e.isRest.not && e.isNil.not}
	}

	testLastN {|n,pred,default=true|
		var lastNEvents = this.lastNNoRests(n);
	//	"events are ".post; events.postln;
	//	"last n events are ".post; lastNEvents.postln;
		(lastNEvents.size >= n).if({
			^pred.(lastNEvents)
		}, {
			^default
		})
	}
}


Satz {
	var <voices, routine, synths;

	*initClass {
		StartUp.add {
			SynthDef(\satzVoice, {|out = 0, freq = 440, gate = 1, pan = 0|
				var snd = LFTri.ar(freq*LFNoise1.kr(0.07).range(1/1.012,1.012));
				var env = EnvGen.ar(Env.asr(0.075,1,6), gate, doneAction: 2);
				snd = LPF.ar(RLPF.ar(snd, freq*LFNoise1.kr(0.07).range(1/1.012,1.012), 0.4, mul: 1.75).tanh, freq*3)
				+ (LPF.ar(HPF.ar(snd, freq), freq*3, mul: 0.45));
				snd = AllpassC.ar(snd, 0.2, LFNoise1.kr(0.05).range(0.001,0.1), mul: 0.1*Line.kr(0,1,1)) + (snd*0.9);
				snd = snd * SinOsc.ar(Rand(0.01,0.06), Rand(0,6)).range(0.5,1);
				snd = LPF.ar(snd, 2000, mul: 0.3) + (snd * 0.8);
				snd = Pan2.ar(snd, pan, -12.dbamp);
				Out.ar(out, snd * env);
			}).add;
		};

		StartUp.add {
			SynthDef(\satzVoiceBus, {|out = 0, freq = 440, gate = 1|
				var snd = LFPulse.ar(freq);
				var env = EnvGen.ar(Env.asr(0.075,1,6), gate, doneAction: 2);
				Out.ar(out, snd * env);
			}).add;
		}


	}

	*new {|n, firstChord=nil|
		^super.new.init(n, firstChord)
	}

	init {|n, firstChordIn|
		voices = Array.fill(n, { Voice() });
		(firstChordIn.isNil.not).if({
			n.do{|i|
				(firstChordIn.size > i).if({
					voices[i].add(firstChordIn[i]);
				}, {
					voices[i].addRest;
				});
			}
		});
		voices = voices.scramble;
	}


	lastChord {
		//^voices.collect{|v| v.last}
		^this.currentHarm
	}

	firstChord {
		^voices.select{|v| v[0].isRest.not && v[0].isNil.not}.collect{|v| v[0]}
	}

	nFrames {
		^voices[0].size
	}

	lastActive {
		^voices.select{|v| v.last.isRest.not && v.last.isNil.not}
	}

	currentHarm {
		^this.lastActive.collect{|v| v.last}
	}


	lastTwoHarm {
		^voices.collect{|v| v.events.keep(-2)}.select{|v| v.every{|h| Harmonic.isEvent(h)}}
	}


	nActive {
		^this.lastActive.size
	}

	testCurrentHarm {|pred,default=true|
		var active = this.lastActive;
		active.isEmpty.if({
			^false
		}, {
			^pred.(active)
		})
	}

	testLastNMelodic {|n,pred,default=true|
		^voices.collect{|v| v.testLastN(n,pred,default)}
	}


	testLastNMelodicEvery {|n,pred,default=true|
		^this.testLastNMelodic(n,pred,default).every{|e| e}
	}


	// predicate is for two voices, but will be applyed to all unique pairs
	testLastNMelodicHarmonicPairs {|n,pred,default=true|
		var voicesWithoutRests = voices.collect{|v| v.lastN(n)}.select{|events| events.every{|e| e.isRest.not} };
		(voicesWithoutRests[0].size < n).if({
			^default
		}, {
			^voicesWithoutRests.collectUniquePairs(pred).every{|e| e}
		})
	}



	// adding event frames

	addRndEventFrame {|n, domains|
		var toRemove = [];
		var toAdd = [];
		var diff = n - this.nActive;
		var active = this.voices.collect{|v, i| [i, v.last.isRest.not] }.select{|e| e[1]}.collect{|e| e[0]};
		var notActive = this.voices.collect{|v, i| [i, v.last.isRest] }.select{|e| e[1]}.collect{|e| e[0]};

	//	"diff ".post;diff.postln;
		//"active ".post;active.postln;
		//"notActive ".post;notActive.postln;
		(diff > 0).if({
			// we need to add
			toAdd = notActive.scramble.keep(diff);
		});
		(diff < 0).if({
			// we need to remove
			toRemove = active.scramble.keep(diff.abs);
		});
		voices.do{|v,i|
			toAdd.includes(i).if({
				var h = domains[i].at(0); // Harmonic.rndInRange(maxPartial, octaves, fundamental)
				v.add(h)
			}, {
				toRemove.includes(i).if({
					v.addRest;
				}, {

					v.last.isRest.if({
						v.addRest;
					},{
						var h = domains[i].at(0); // Harmonic.rndInRange(maxPartial, octaves, fundamental)
						v.add(h)
					})
				})
			})

		}
	}

	// for genetic algo solver
	createNextFramePopulation {|n, domain, nCandidates, maxDiff,steepness|
		^nCandidates.collect{
			var satz = this.deepCopy;
			var toRemove = [];
			var toAdd = [];
			var diff = n - satz.nActive;
			var active = satz.voices.collect{|v, i| [i, v.last.isRest.not && v.last.isNil.not ] }
			.select{|e| e[1]}.collect{|e| e[0]};
			var notActive = satz.voices.collect{|v, i| [i, v.last.isRest || v.last.isNil] }
			.select{|e| e[1]}.collect{|e| e[0]};

			(diff > 0).if({
				// we need to add
				toAdd = notActive.scramble.keep(diff);
			});
			(diff < 0).if({
				// we need to remove
				toRemove = active.scramble.keep(diff.abs);
			});
	//		("To add: %s\n").postf(toAdd);
		//	("To remove: %s\n").postf(toRemove);
			satz.voices.do{|v,i|
				toAdd.includes(i).if({
					// add new random
					var h = domain.choose;
				//	"chose h :".post;
					//h.postln;
					v.add(h)
				}, {
					toRemove.includes(i).if({
						v.addRest;
					}, {

						(v.last.isRest || v.last.isNil).if({
							v.addRest;
						},{
						//	var pr = v.last.postln;
							var h = domain.chooseWithinMidiRange(v.last, maxDiff,steepness);
							v.add(h)
						})
					})
				})
			};
		//	"Satz candidate printing ".post;
			satz;
		}
	}

	dropLastEventFrame {
		this.voices.do{|v| v.dropLast}
	}

	// dropping last event frame
	// activating a new voice
	// resting an active voice

	print {
		voices.do{|v|
			v.print; "".postln;
		}
	}

	nextEventFrame {|domains|
		var active = this.voices.collect{|v, i| [i, v.last.isRest.not] }.select{|e| e[1]}.collect{|e| e[0]};
		var currentIndices = active.collect{|i|
			this.voices[i].last.idx
		};
		var nextIndices = currentIndices.incrementList(domains[0].maxIdx);
	//	"current indices".post; currentIndices.postln;
		//"next indices".post; nextIndices.postln;
		if (nextIndices.isNil) {
			^false
		} {
			active.do{|i,k|
			//	"i ".post; i.postln;
				//"k ".post; nextIndices[k].postln;
				this.voices[i].last.set(nextIndices[k], domains[i])
			};
			^true
		}
	}

	incVoices {|indices, domains|
		indices.do{|i|
			var nextIdx = this.voices[i].last.idx + 1;
			this.voices[i].last.set(nextIdx.mod(domains[i].maxIdx + 1), domains[i])
		}
	}

	// todo amplitude
	play {|bus, waitTime|
		routine = Routine{
			this.nFrames.do{|i|
				var freqs = this.voices.collect{|v| v[i].cps};
				bus.setn(freqs);
				waitTime.wait;
			}
		}.play
	}


	playSynths {|bus, waitTimes|
		routine = Routine{
			var previousFreq = 0!this.voices.size;
			synths = nil!this.voices.size;
			this.nFrames.do{|i|
				var dur = waitTimes.next;
				"Playing % of %\n".postf(i+1, this.nFrames);
				this.voices.do{|v, vi|
					var pan = vi.linlin(0, this.voices.size - 1, -1, 1);
					var useBuses = bus.isCollection;
					var thisBus = bus;
					useBuses.if({
						thisBus = bus[vi]
					});
					Harmonic.isEvent(v[i]).if({
						(previousFreq[vi] == 0).if({
							// from rest to play
							useBuses.if({
								synths[vi] = Synth(\satzVoiceBus, [\freq, v[i].cps, \gate, 1, \out, thisBus]);
							}, {
								synths[vi] = Synth(\satzVoice, [\freq, v[i].cps, \gate, 1, \out, bus, \pan, pan]);
							});
							previousFreq[vi] = v[i].cps;
						}, {
							(previousFreq[vi] != v[i].cps).if({
								// change of pitch
								//"Different pitches % and %".postf(previousFreq[vi], v[i].cps);
								synths[vi].set(\gate, 0);

								useBuses.if({
									synths[vi] = Synth(\satzVoiceBus, [\freq, v[i].cps, \gate, 1, \out, thisBus]);
								}, {
									synths[vi] = Synth(\satzVoice, [\freq, v[i].cps, \gate, 1, \out, bus, \pan, pan]);
								});

								previousFreq[vi] = v[i].cps;
							})
						})
					}, {
						// from playing to rest
						(previousFreq[vi] != 0).if({
							synths[vi].set(\gate, 0);
							previousFreq[vi] = 0;
						});
					});
				};
			//	previousFreq.cpsmidi.postln;
				dur.wait;
			}
		}.play
	}

	stop {
		routine.stop;
		synths.do{|sy| {sy.set(\gate, 0)}.defer(0.05.exprand(0.1))};
	}

}


SatzCosts {
	var <fun;

	*new {|fun, pruneFun=nil|
		^super.newCopyArgs(fun, pruneFun)
	}

	*minMaxDistance {|minD, maxD, factor=2|
		^SatzCosts({|satz, unused_percSatz, thisRegisters|
			var pitches = satz.currentHarm.collect{|h| h.midi}.sort;
			var groupedPitches = thisRegisters.collect { |range|
				var mini = range[0].min(range[1]);
				var maxi = range[0].max(range[1]);
				pitches.select { |num|
					(num >= mini) && (num <= maxi)
				}
			};
			var intervals = groupedPitches.collect{|g| g.differentiate.drop(1)}.flatten;
			var intervalDiffs = intervals.collect{|i|
				if ((i < minD), {
					(i - minD).abs
				}, {
					if ((i > maxD), {
						(i - maxD).abs
					}, {
						0
					})
			})};
			if (intervals.size > 0) {
				((intervalDiffs.sum / intervals.size) * factor)
			} {
				0
			}
		})
	}

	*noSamePitch {|factor=2|
		^SatzCosts({|satz|
			var pitches = satz.currentHarm.collect{|h| h.midi.mod(12)};
			var classes = pitches.as(Set).as(Array);
			((pitches.size - classes.size) / satz.currentHarm.size) * factor;
		})
	}

	*gradusOutside {|minGStart, maxGStart, minGEnd, maxGEnd, factor=2|
		^SatzCosts({|satz, percSatz|
			var minG = percSatz.linlin(0,1,minGStart,minGEnd);
			var maxG = percSatz.linlin(0,1,maxGStart,maxGEnd);
			var grades = Harmonic.gradusChord(satz.currentHarm);
			var gradeDiffs = grades.collect{|g|
				if ((g < minG), {
					(g - minG).abs

				}, {
					if ((g > maxG), {
						(g - maxG).abs
					}, {
						0
					})
			})};
			if (grades.size > 0) {
				((gradeDiffs.sum / grades.size) * factor)
			} {
				0
			}
		})
	}

	*bigIntervals {|fac|
		^SatzCosts({|satz|
			var result = satz.testLastNMelodic(2, {|harms|
				harms[0].interval(harms[1]) >= 9;
			}, false);
			result.occurrencesOf(true) * fac
		})
	}


	// todo as percentage of total
	*change {|fac|
		^SatzCosts({|satz|
			var result = satz.testLastNMelodic(2, {|harms|
				harms[0].interval(harms[1]) > 0;
			}, false);
			result.occurrencesOf(true) * fac
		})
	}

	*changePerc {|perc, fac|
		var func = {|v1, v2|
			var parallel5th = v1[0].isFifth(v2[0]) && v1[1].isFifth(v2[1]);
			var parallelOct = v1[0].isOctave(v2[0]) && v1[1].isOctave(v2[1]);
			var movement = v1[0].midi != v1[1].midi;
			(movement && (parallel5th || parallelOct))
		};
		^SatzCosts({|satz|
			var voices = satz.lastTwoHarm;
			var n = voices.size;
			((n > 0) && (voices[0].size > 1)).if({
				var nChanges = voices.count{|v| v[0].midi != v[1].midi};
				var percCh = (nChanges/n);
				//"changes: ".post; percCh.postln;
				if ((percCh <= 0.0), {
					10*fac
				}, {
					((perc-percCh).abs)*fac
				})
			}, {
				0
			})
		})
	}


	*noSteps {|fac|
		^SatzCosts({|satz|
			satz.testLastNMelodic(5, {|harms|
				var intervals;
				harms.doAdjacentPairs{|h1, h2| intervals = intervals.add(h1.interval(h2))};
				intervals.any{arg i; (i < 6).and(i > 0)}.not
			}, false).occurrencesOf(true) * fac
		})
	}


	*successiveLeapsSameDir {|fac|
		var func = {arg lastThree;
			var interval1 = lastThree[1].interval(lastThree[0]);
			var interval2 = lastThree[2].interval(lastThree[1]);
			((interval1.abs >= 7) && (interval2.abs >= 7) &&
				(interval1.sign == interval2.sign))
		};
		^SatzCosts({|satz|
			satz.testLastNMelodic(3, func, false).occurrencesOf(true) * fac
		})
	}

	*parallel5thOrOct {|fac|
		var func = {|v1, v2|
			var parallel5th = v1[0].isFifth(v2[0]) && v1[1].isFifth(v2[1]);
			var parallelOct = v1[0].isOctave(v2[0]) && v1[1].isOctave(v2[1]);
			var movement = v1[0].midi != v1[1].midi;
			(movement && (parallel5th || parallelOct))
		};
		^SatzCosts({|satz|
			satz.testLastNMelodicHarmonicPairs(2, func, false).asInteger * fac
		})
	}


	*noSimulLeapsSameDir {|fac|
		var func = {|v1, v2|
			var interval1 = v1[0].interval(v1[0]);
			var interval2 = v2[0].interval(v2[0]);
			((interval1.abs >= 7) && (interval2.abs >= 7) &&
				(interval1.sign == interval2.sign))
		};
		^SatzCosts({|satz|
			satz.testLastNMelodicHarmonicPairs(2, func, false).asInteger * fac
		})
	}



	apply {|satz, perc=nil, registers=nil|
		^this.fun.(satz, perc, registers)
	}


}

//
// SatzPredicate {
// 	var <fun, <pruneFun;
//
// 	*new {|fun, pruneFun=nil|
// 		^super.newCopyArgs(fun, pruneFun)
// 	}
//
// 	*melodicInterval {
// 		^SatzPredicate({|satz|
// 			satz.testLastNMelodic(2, {|harms|
// 				harms[0].interval(harms[1]) <= 9;
// 			})
// 		})
// 	}
//
// 	*containsSteps {
// 		^SatzPredicate({|satz|
// 			satz.testLastNMelodic(5, {|harms|
// 				var intervals;
// 				harms.doAdjacentPairs{|h1, h2| intervals = intervals.add(h1.interval(h2))};
// 				intervals.any{arg i; (i < 6).and(i > 0)}
// 			})
// 		})
// 	}
//
//
// 	*noSuccessiveLeapsSameDir {
// 		var func = {arg lastThree;
// 			var interval1 = lastThree[1].interval(lastThree[0]);
// 			var interval2 = lastThree[2].interval(lastThree[1]);
// 			if ((interval1.abs >= 7) && (interval2.abs >= 7)) {
// 				interval1.sign != interval2.sign
// 			} {
// 				true
// 			}
// 		};
// 		^SatzPredicate({|satz|
// 			satz.testLastNMelodic(3, func)
// 		})
// 	}
//
//
// 	*noParallel5thOrOct {
// 		var func = {|v1, v2|
// 			//var nothing = [v1.postln, v2.postln];
// 			var parallel5th = v1[0].isFifth(v2[0]) && v1[1].isFifth(v2[1]);
// 			var parallelOct = v1[0].isOctave(v2[0]) && v1[1].isOctave(v2[1]);
// 			var movement = v1[0].midi != v1[1].midi;
// 			(movement && (parallel5th || parallelOct)).not
// 		};
// 		^SatzPredicate({|satz|
// 			satz.testLastNMelodicHarmonicPairs(2, func)
// 		})
// 	}
//
// 	apply {|satz|
// 		^this.fun.(satz)
// 	}
//
// 	// todo
// 	// no direct 5th or Oct
// 	// no simul leaps in same dir
//
// }


GenControl {
	var <nVoices, <registers, <maxHarm;

	// registers are a list of lists containing min and max of pitch register areas
	*new {|nVoices, registers, maxHarm|
		^super.newCopyArgs(nVoices, registers, maxHarm);
	}

	// percStart and percEnd determine how many unchanging control frames
	// will be produced at the beginning and at the ending
	*fromTo {|nInterpolation, nStart, nEnd, nVoicesStart, nVoicesEnd, registersStart, registersEnd, maxHarmStart, maxHarmEnd|

		var output = [];
		nStart.do{
			var g = GenControl(nVoicesStart, registersStart, maxHarmStart);
			output = output.add(g);
		};
		nInterpolation.do{|i|
			var gN = i.linlin(0, nInterpolation-1, nVoicesStart, nVoicesEnd).round.asInteger;
			var gRegisters = registersStart.size.collect{|k|
				var r1 = i.linlin(0, nInterpolation-1, registersStart[k][0], registersEnd[k][0]);
				var r2 = i.linlin(0, nInterpolation-1, registersStart[k][1], registersEnd[k][1]);
				[r1.min(r2), r1.max(r2)]
			};
			var gMaxHarm = i.linlin(0, nInterpolation-1, maxHarmStart, maxHarmEnd).round.asInteger;
			var g = GenControl(gN, gRegisters, gMaxHarm);
			output = output.add(g);

		};
		nEnd.do{
			var g = GenControl(nVoicesEnd, registersEnd, maxHarmEnd);
			output = output.add(g);
		};
		^output
	}

	*static {|n, nVoices, registers, maxHarm|
		^n.collect{GenControl(nVoices, registers, maxHarm)}
	}

}


SatzGeneticSolver {
	var predicates, n, maxHarm, satz, controls, nCandidates, steepness; // steepness of linrand, higher means more likely smaller intervals

	*new {arg predicates, controls, nCandidates, steepness,firstChord=nil;
		^super.new.init(predicates, controls, nCandidates, steepness,firstChord);
	}

	init {arg predicatesIn, controlsIn, nCandidatesIn, steepnessIn, firstChord;
		predicates = predicatesIn;
		n = controlsIn.size;
		controls = controlsIn;
		maxHarm = controls.collect{|c| c.maxHarm}.maxItem;
		//nVoices = nVoicesIn;
		satz = Satz(controls.collect{|c| c.nVoices}.maxItem, firstChord);
		steepness = steepnessIn;
		nCandidates = nCandidatesIn;
		//domains = Array.fill(nVoices, { HarmonicDomain(maxHarm, [1,2,3,4,5,6,7,8], 50) });
	}

	solve {
		while ({satz.nFrames < n}, {

			// generate candidates
			var thisControl = controls[satz.nFrames];
			var thisRegisters = thisControl.registers;
			var domain = HarmonicDomain(thisControl.maxHarm, [1,2,3,4,5,6,7,8], 50).selectRegisters(thisRegisters);
			var population = satz.createNextFramePopulation(thisControl.nVoices, domain, nCandidates, 12, steepness);
			// apply predicates

			var scores = population.collect{|candidateSatz|
				predicates.collect{|p| p.apply(candidateSatz, satz.nFrames/(n-1), thisRegisters)}.sum
			};
			var bestScore = scores.minItem;
			var selectedIndex = scores.indicesOfEqual(bestScore).choose;
			// pick best and assign to satz
			satz = population[selectedIndex];
	//		"scores ".post; scores.postln;
		//	"selectedIndex ".post; selectedIndex.postln;
			//satz;
			"Finished frame: % of %\n".postf(satz.nFrames, n);
		});
		^satz
	}

}


/*SatzSolver {
	var independentPredicates, dependentPredicates, satz, n, <domains;


	*new {arg independentPredicates, dependentPredicates, nVoices, nFrames;
		^super.new.init(independentPredicates, dependentPredicates, nVoices, nFrames);
	}

	init {arg indiPredicatesIn, depPredIn, nVoices, nFrames;
		independentPredicates = indiPredicatesIn;
		dependentPredicates = depPredIn;
		n = nFrames;
		satz = Satz(nVoices);
		domains = Array.fill(nVoices, { HarmonicDomain(20, [3,4], 50) });
	}

	backtrack {
		var existsNextEventFrame;

		if  (satz.nFrames == 0) {
			satz.addRndEventFrame(satz.voices.size, domains);
		} {
			existsNextEventFrame = satz.nextEventFrame(domains);
			if (existsNextEventFrame.not && (satz.nFrames > 0)) {
				satz.dropLastEventFrame;
				this.backtrack;
			}
		}
	}

	solve {
		var done = false;
		satz.addRndEventFrame(satz.voices.size, domains);

		while { done.not } {
			var predResults;
			var indiPredResults;
			var existsNextEventFrame = true;

			//satz.print;
			indiPredResults = independentPredicates.collect{arg p; p.apply(satz)}.andOnColumns;

			if (indiPredResults.every{|e| e}) {

				predResults = dependentPredicates.collect{arg p; p.apply(satz)};

				if (predResults.every{arg e; e}) {
					if (satz.nFrames >= n) {
						done = true;
					} {
						satz.addRndEventFrame(satz.voices.size, domains);
					}
				} {
					this.backtrack
				}

			} {
				// indis are not all true
				satz.incVoices(indiPredResults.indicesOfEqual(false), domains)
			}
		}
		^satz
	}

}*/


+ SequenceableCollection {

	collectUniquePairs {|func|
		var result = [];
		this.do { |a, i|
			this.do { |b, j|
				if (i < j) {
					result = result.add(func.value(a, b));
				}
			}
		};
		^result;
	}


	incrementList { |maxVal|
		var len = this.size;
		var list = this.copy; // Make a copy to avoid modifying the original list

		if (list.every{|e| e == maxVal}) {
			^nil
		};

		for (len-1, 0, { |i|
			list[i] = list[i] + 1; // Increment the current element
			if (list[i] > maxVal) {
				list[i] = 0; // Reset to 0 if max is exceeded
			} {
				^list; // Return the list if no carry-over
			}
		});
		list[0] = list[0] + 1; // Increment the first element if all others are reset
		^list
	}

	andOnColumns {
		var output = true!this[0].size;
		this.do{|row|
			row.do{|c, i|
				output[i] = output[i].and(c)
			}
		}
		^output
	}

	collect2 {|array2, fun|
		^this.collect{|el, i| fun.(el, array2[i])}
	}

}

