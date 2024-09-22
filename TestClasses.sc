Trossingen {
	var <counter;
	var <name;


	*new { arg initCounter, initName;
        ^super.new.init(initCounter, initName)
    }

    init { arg initCounter, initName;
		counter = initCounter;
		name = initName;
	}


	hello { arg n;
		n.do({
			"Hello".postln;
			counter = counter + 1;
		})
    }

	counterString {
		^counter.asString
	}

}