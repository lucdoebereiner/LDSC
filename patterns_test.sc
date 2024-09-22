PwhiteTest : Pattern {
	var <>lo, <>hi, <>length;
	*new { arg lo=0.0, hi=1.0, length=inf;
		^super.newCopyArgs(lo, hi, length)
	}
	storeArgs { ^[lo,hi,length] }
	embedInStream { arg inval;
		var loStr = lo.asStream;
		var hiStr = hi.asStream;
		var hiVal, loVal;
		"Inval at first: ".post; inval.postln;

		length.value(inval).do({
			"Inval: ".post; inval.postln;

			hiVal = hiStr.next(inval);
			loVal = loStr.next(inval);
			if(hiVal.isNil or: { loVal.isNil }) { ^inval };
			inval = rrand(loVal, hiVal).yield;
		});
		^inval;
	}
}