Moebel {
	var <>name, <>gewicht, <>farbe;
	*new { arg name, gewicht, farbe;
		^super.newCopyArgs(name, gewicht, farbe)
	}

	paint { arg farbeIn;
		farbe = farbeIn
	}

}

