FMTrossingen {
    *ar { arg carFr, modFr, modAmount;
		^SinOsc.ar(carFr + SinOsc.ar(modFr, 0, modAmount))
    }

	*nonsense {
		^99
	}
}

