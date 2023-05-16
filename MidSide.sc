MidSide {
    *ar { arg source, width, freq = 500;
		var left = source[0];
		var right = source[1];
		var mid = (left + right) / 2;
		var side = left - right;
		var midLow = LPF.ar(mid, freq.lag(0.5)) * (1-width.lag2(0.2));
		var sideHigh = HPF.ar(side, freq.lag(0.5)) * width.lag2(0.2);
		^[midLow+sideHigh, midLow-sideHigh]
	}
}

MidSideMono {
    *ar { arg source, width, freq = 500;

		var mid = LPF.ar(source, freq.lag(0.5));
		var side = HPF.ar(source, freq.lag(0.5));

		var midLow = mid * (1-width.lag2(0.2));
		var sideHigh = side * width.lag2(0.2);
		^[midLow+sideHigh, midLow-sideHigh]
	}
}


MidSidePan {
    *ar { arg source, width = 1, pan = 0, freq = 500;

		var sourceStereo = MidSideMono.ar(source, width, freq);
		^Balance2.ar(sourceStereo[0], sourceStereo[1], pan);
		
	}
}