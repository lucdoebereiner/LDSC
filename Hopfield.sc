Hopfield {

	var <size = 4; // n neurons, must be power of two
	var <weights; // = (0!size!size);
	var <state;// = 0!size;
	var <targetPatterns;
	var <nRows;

	 *new { |n|
        ^super.new.init(n)
    }

    init { |n|
        size = n;
		weights = 0!size!size;
		state = 0!size;
		targetPatterns = [];
		nRows = size.sqrt.asInteger;
		nRows.postln;
	}

	addTarget {|t|
		targetPatterns = targetPatterns.add(t);
	}

	train {
		targetPatterns.do { |pattern|
			pattern.do { |xi, i|
				pattern.do { |xj, j|
					if (i != j) {
						weights[i][j] = weights[i][j] + (xi * xj);
					};
				};
			};
		};
		"Trained".postln;
	}

	setState {|newState|
		state = newState;
	}

	randomizeState {
		state = Array.fill(size, { [-1,1].choose });
	}

	update {
		state = state.collect { |x, i|
			weights[i].collect{ |w, j|
				w * state[j] + -0.2.rrand(0.2);
			}.sum.sign;
		};
		"Updated State".postln;
	}

}

HopfieldContinuous : Hopfield {
	var bias;


	*sigmoid { |x, gain=1.0|
		^(1 / (1 + exp(-1 * gain * x)))
	}

	randomizeState {
		state = Array.rand(size, 0.0, 1.0);
	}

	randomizeWeights {
		weights.size.do{|i|
			weights[i].size.do{|j|
				weights[i][j] = -0.05.rrand(0.05);
			}
		}
	}

	train {|lr=0.1|
		var maxWeight;
		bias = 0!size;
		targetPatterns.do { |pattern|
			pattern.do { |xi, i|
				pattern.do { |xj, j|
					if (i != j) {
						weights[i][j] = weights[i][j] + (xi * xj * lr);
					};
				};
				bias = bias + (xi * lr);
			};
		};
		weights = weights * (1/size);
		bias = bias * (1/size);
		//maxWeight = weights.flatten.abs.maxItem;
	//	if (maxWeight > 1.0) {
		//	weights = weights * (1/maxWeight);
		//};
		weights.postln;
		bias.postln;
	}

	update {
		state = state.collect { |x, i|
			HopfieldContinuous.sigmoid(weights[i].collect{ |w, j|
				w * state[j];
			}.sum + bias[i]);
		};
		state.postln;
	}

}


