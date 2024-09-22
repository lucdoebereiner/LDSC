// 2 inputs

XOR {
	*ar {|in1, in2|
		var in1B = in1 > 0.5;
		var in2B = in2 > 0.5;
		^(in1B - in2B).abs.clip(0,1)
	}

}

OR {
	*ar {|in1, in2|
		var in1B = in1 > 0.5;
		var in2B = in2 > 0.5;
		^(in1B.round + in2B.round).clip(0,1)
	}

}

AND {
	*ar {|in1, in2|
		var in1B = in1 > 0.5;
		var in2B = in2 > 0.5;
		^(in1B.round * in2B.round).clip(0,1)

	}
}

NAND {
	*ar{|in1, in2|
		^NOT.ar(AND.ar(in1, in2))
	}
}


NOR {
	*ar{|in1, in2|
		^NOT.ar(OR.ar(in1, in2))
	}
}

BooleanOp {
	*ar{|select, in1, in2|
		^Select.ar(select, [AND.ar(in1, in2), OR.ar(in1, in2), XOR.ar(in1, in2), NAND.ar(in1, in2), NOR.ar(in1, in2)])
	}

}


// 1 input
NOT {
	*ar {|in|
		^(1-in)
	}
}



BinNet {
	var <inputs1;
	var <inputs2;
	var n;

	*new {|n|
		^super.new.init(n)
	}

	init {|nIn|
		var nodesToConnect, remainingInputs, additionalInputs;

		n = nIn;
		inputs1 = n.collect{|i| (i-1).mod(n)};

		remainingInputs = n.collect{|i| i};
		inputs2 = [];

		{ inputs2.size < n }.while{
			var k = 0;
			var keepSearching = true;
			remainingInputs = n.collect{|i| i};
			inputs2 = [];
			{ remainingInputs.isEmpty.not
				&& keepSearching }.while{
				var possibleConnections = remainingInputs.select{|e|
					(e != k) &&
					(e != (k - 1).mod(n)) &&
					(e != (k + 1).mod(n))};
				possibleConnections.isEmpty.if({
					keepSearching = false;
				},{
					var connection = possibleConnections.choose;
					inputs2 = inputs2.add(connection);
					remainingInputs.remove(connection);
					k = k + 1;
				})
			};
		};

	}

	*ar {|n, input, audioBus, operationsBus, additionalConnectionsBus,
		nothingProb, delayProb, lpfProb, hpfProb, lagTime|
		var fb, gens, operations, additionalConnections;

		var object = super.new.init(n);

		additionalConnectionsBus.setn(object.inputs2);

		fb = InFeedback.ar(audioBus, n);
		operations = In.kr(operationsBus, n);
		additionalConnections = In.kr(additionalConnectionsBus, n);
		gens = n.collect{|i|
			//var from = object.inputConnections[i];
			var edge1 = BinNet.edgeSelect(
				[nothingProb, delayProb, lpfProb, hpfProb],
				fb[object.inputs1[i]]);
			var edge2 = BinNet.edgeSelect(
				[nothingProb, delayProb, lpfProb, hpfProb],
				SelectX.ar(additionalConnections[i].lag(lagTime),
					fb));
			BooleanOp.ar(operations[i], edge1, edge2);
		};
		gens = gens + input;
		Out.ar(audioBus, gens);
		^gens
	}

	*edgeSelect {|probs, input|
		var edgeProbs = probs.normalizeSum;
		^switch ([0,1,2,3].wchoose(edgeProbs),
			0, { input },
			1, { DelayC.ar(input, 1000/SampleRate.ir,
				(1/SampleRate.ir).exprand(1000/SampleRate.ir)) },
			2, { LPF.ar(input, 20.0.exprand(1000)) },
			3, { HPF.ar(input, 1.0.exprand(100))}
		);
	}

	/*
	synth {|operationsBus, nothingProb, delayProb, lpfProb, hpfProb| // nothing, delay, lpf, hpf
		^{
			var fb, gens, operations;
			fb = LocalIn.ar(n);
			operations = In.kr(operationsBus, n);
			gens = n.collect{|i|
				var from = inputConnections[i];
				var edge1 = BinNet.edgeSelect([nothingProb, delayProb, lpfProb, hpfProb], fb[from[0]]);
				var edge2 = BinNet.edgeSelect([nothingProb, delayProb, lpfProb, hpfProb], fb[from[1]]);
				BooleanOp.ar(operations[i], edge1, edge2);
			};
			gens = gens + Impulse.ar(1/4);
//			LocalOut.ar(LeakDC.ar(gens));
			LocalOut.ar(gens);
			//LocalOut.ar(gens - OnePole.ar(gens, 0.99999));
			//Out.ar(0, gens > 0.5);
			Splay.ar(LeakDC.ar(gens));
		}
	}
*/


}







// todo
// open input to outside
// make pseudo ugen

// DONE
// improve network construction algo
// decide on filters
// probabilities on construction
// make operations controllable/changeable


//[XOR, AND, NOR].choose.ar(SinOsc.ar(23), SinOsc.ar(433))

/*
function createConnectedRandomNetwork(N):
    # Initialize an empty adjacency list to store the graph
    adjacencyList = []

    # Initialize a list to keep track of each node's input connections
    inputConnections = [[] for _ in range(N)]

    # Step 1: Create a connected chain to ensure all nodes are connected
    for i in range(1, N):
        # Connect node i-1 to node i (guarantees at least one path through all nodes)
        adjacencyList.append((i-1, i))
        inputConnections[i].append(i-1)

    # Step 2: Add additional random edges to meet the two inputs per node requirement
    for node in range(N):
        while len(inputConnections[node]) < 2:
            # Randomly select a distinct node as an input node
            inputNode = random.randint(0, N-1)

            # Ensure the inputNode is not the node itself and is not already an input
            if inputNode != node and inputNode not in inputConnections[node]:
                # Add the input connection to the adjacency list
                adjacencyList.append((inputNode, node))
                inputConnections[node].append(inputNode)

    # Step 3: Ensure each node has exactly one output
    for node in range(N):
        outputs = [edge for edge in adjacencyList if edge[0] == node]
        if len(outputs) == 0:
            # Randomly assign an output for nodes without any outputs
            outputNode = random.randint(0, N-1)
            while outputNode == node:
                outputNode = random.randint(0, N-1)
            adjacencyList.append((node, outputNode))

    # Output: adjacencyList is a list of directed edges (inputNode, outputNode)
    return adjacencyList

# Example usage:
N = 5  # Number of nodes
network = createConnectedRandomNetwork(N)
print(network)
*/