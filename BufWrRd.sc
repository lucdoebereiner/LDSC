BufWrRd {
 *ar{
		arg  input, buffer, writerate = 1, readrate = 1, out = 0;
		var read, write;
		write = BufWr.ar(input, buffer.bufnum, Phasor.ar(0, writerate, 0, BufFrames.ir(buffer.bufnum)-1));
		read = BufRd.ar(1, buffer.bufnum, Phasor.ar(0, readrate, 0, BufFrames.ir(buffer.bufnum)-1),interpolation: 1);
		^read;
	}
}