SelectDelay {
	*ar {arg input, delTime=0.1, lagTime=1, maxDelTime=0.5;
		var delTimes;
		var timeChanged = Changed.kr(delTime);
		var times = LocalBuf.newFrom([delTime,delTime]);
		var idx = ToggleFF.kr(timeChanged);
		BufWr.kr(delTime, times, idx);
		delTimes = BufRd.kr(1, times, [0,1]);
		^SelectX.ar(idx.lag2(lagTime), DelayC.ar(input, maxDelTime, delTimes))
	}
}


SelectDelayBlocking {
	*ar {arg input, delTime=0.1, lagTime=1, maxDelTime=0.5;
		var delTimes;
		var timeChangedRaw = Changed.kr(delTime);
		var timeChanged = Gate.kr(timeChangedRaw, 1-Trig1.kr(timeChangedRaw, lagTime));
		var times = LocalBuf.newFrom([delTime,delTime]);
		var idx = ToggleFF.kr(timeChanged);
		BufWr.kr(delTime, times, idx);
		delTimes = BufRd.kr(1, times, [0,1]);
		^SelectX.ar(idx.lag2(lagTime), DelayC.ar(input, maxDelTime, delTimes))
	}
}
