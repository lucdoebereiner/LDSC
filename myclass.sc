MyClass {

	 *new {
		^super.new.init()
    }

	init {
		this.postln;
		{ this.postln; }.();
	}

}


MyInt {
	var i;

	*new { |i|
		^super.new.init(i)

	}

	init { |iIn|
		i = iIn;

	}

	collectAs { arg function, class;
		var res = (class ? Array).new(i);
		res.class.postln;
		this.do {|c| var val = function.value(c); res.add(val); }
		^res;
	}

	collect { arg function;
		^this.collectAs(function, Array)
	}

	do { arg function;
		// iterates function from 0 to this-1
		// special byte codes inserted by compiler for this method
		var counter = 0;
		while ({ counter < i }, { function.value(counter, counter).postln; counter = counter + 1; });
	}

}