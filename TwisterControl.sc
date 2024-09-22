TwisterControl {
	var speed = 0, ctrlVal = 0.5, lastN, <mappedVal = 0.5,
	buttonDown = false, midiFunc, midiButton, routine, buttonDownSend = false,
	spec, <>baseInc = 0.02, <>minSpeed = 0.2, <>maxSpeed = 5, controlNumber, mOut;


    *new { | controlN, chan = 0, midiOut, spec, callback, buttonReleaseCallback, buttonDownCallback, bdSend = false, initVal = 0.5 |
        ^super.new.init(controlN, chan, midiOut, spec, callback, buttonReleaseCallback, buttonDownCallback, bdSend, initVal)
    }

    init { | controlN, chan, midiOut, specIn, callback, buttonReleaseCallback, buttonDownCallback, bdSend, initVal |
		lastN = [0, 0, 0, 0];
		ctrlVal = initVal;
		mOut = midiOut;
		spec = specIn;
		controlNumber = controlN;
		buttonDownSend = bdSend;

		midiFunc = MIDIFunc.cc({arg ...args;
			var inc = baseInc * speed.abs.linexp(0,3.5,minSpeed,maxSpeed);
			if (args[0] == 65) {
				ctrlVal = (ctrlVal + inc).fold(0,1);
			} {
				ctrlVal = (ctrlVal - inc).fold(0,1);
			};
			mappedVal = spec.map(ctrlVal);
			callback.value(mappedVal);
		}, controlN, chan);

		midiButton = MIDIFunc.cc({arg ...args;
			if (args[0] == 127) {
				buttonDown = true;
				if (buttonDownCallback.isNil.not) {
					buttonDownCallback.value(mappedVal);
				}
			} {
				buttonDown = false;
				if (buttonReleaseCallback.isNil.not) {
					buttonReleaseCallback.value(mappedVal);
				}
			}
		}, controlN, chan + 1);

		routine = Routine({
			inf.do({
				speed = ctrlVal - lastN.mean;
				lastN = lastN.drop(1).add(ctrlVal);
				// if (buttonDown.not || buttonDownSend) {
				// 	mappedVal = spec.map(ctrlVal);
				// 	callback.value(mappedVal);
				// };
				0.1.wait;
				midiOut.control(0, controlN, spec.unmap(mappedVal).linlin(0,1,0,127).round);
			})
		}).play;
    }

	set { | val |
		mappedVal = val;
		ctrlVal = spec.unmap(val);
		mOut.control(0, controlNumber, ctrlVal.linlin(0,1,0,127).round);
	}

	stop {
		routine.stop
	}
}

TwisterDef {
	classvar <>defDict;

	 *initClass {
        defDict = Dictionary();
    }

	*new { | name, control |
        ^super.new.init(name, control)
    }

    init { | name, control |
		var prev = defDict[name];
		if (prev.isNil.not) {
			prev.stop;
		};
		defDict.put(name, control);
	}

	*setVal { | name, val |
		defDict[name].set(val);
	}

	*parameterState {
		var parameters = [];
		defDict.keysValuesDo { |key, value|
			parameters = parameters.add(key);
			parameters = parameters.add(value.mappedVal);
		}
		^parameters
	}

}