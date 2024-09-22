Palea : Pattern {
	var elements;
	*new { arg elements;
		^super.newCopyArgs(elements)
	}
	embedInStream {arg inval;
		inf.do({
			elements.choose.yield
		})
		^inval
	}

}
·