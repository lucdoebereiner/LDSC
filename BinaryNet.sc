
/*

// 1 input
NOT {
	*ar {|in|
		^(1-in)
	}
}


DEL {
	*ar {|in|
		^Delay1.ar(in)
	}

}
*/

BinaryNet {
	//var <adjacencyList;
	var inputConnections;
	var n;
	var twoClasses;

	*new {|n|
		^super.new.init(n)
	}

	init {|nIn|
		var nodesToConnect;

		var inputs = nIn.collect{|i| (i-1).mod(nIn)}; // circular connections

		var remainingInputs = nIn.collect{|i| i};
		var additionalInputs = [];


		twoClasses = [XOR, XOR, XOR, OR, AND, NAND, NOR];
		n = nIn;

		{ additionalInputs.size < n }.while{
			var k = 0;
			var keepSearching = true;
			remainingInputs = n.collect{|i| i};
			additionalInputs = [];
			{ remainingInputs.isEmpty.not && keepSearching }.while{
				var possibleConnections = remainingInputs.select{|e| (e != k) && (e != (k - 1).mod(n)) && (e != (k + 1).mod(n))};
				//"remaining: ".post; remainingInputs.postln;
				possibleConnections.isEmpty.if({
					keepSearching = false;
				},{
					var connection = possibleConnections.choose;
					additionalInputs = additionalInputs.add(connection);
					remainingInputs.remove(connection);
					k = k + 1;
				})
			};
			//"additional: ".post; additionalInputs.postln;
		};
		inputConnections = [ inputs, additionalInputs ].flop;

/*		inputConnections = n.collect{[]};
		adjacencyList = Set();
		1.for(n-1, {|i|
			adjacencyList.add([i-1,i]);
			inputConnections[i] = inputConnections[i].add(i-1);
		});


		nodesToConnect = (0..(n-1)).scramble;  // Shuffle nodes to randomize the process

		nodesToConnect.do { |node|
			while { inputConnections[node].size < 2 } {
				// Randomly select a distinct node as an input node
				var randomNode = rrand(0, n-1);

				// Ensure the randomNode is not the node itself and is not already an input
				if ((randomNode != node) && adjacencyList.includes([randomNode, node]).not ) {
					// Add the input connection to the adjacency set and input connections list
					adjacencyList.add([randomNode, node]);
					inputConnections[node] = inputConnections[node] ++ [randomNode];
				};
			};
		};

		n.do{|node|
			var outputs = adjacencyList.select{|edge| edge[0] == node};
			(outputs.size == 0).if({
				var outputNode = 0.rrand(n-1);
				{ outputNode == node }.while{
					outputNode = 0.rrand(n-1);
				};
				adjacencyList.add([node,outputNode]);
			});
		};

		adjacencyList = adjacencyList.asArray;*/
	}
	//
	// *edgeSelect {|probs, input|
	// 	var edgeProbs = [nothingProb, delayProb, lpfProb, hpfProb].normalizeSum;
	// 	^switch ([0,1,2,3].wchoose(edgeProbs),
	// 		0, { input },
	// 		1, { DelayC.ar(input, 1000/SampleRate.ir, (1/SampleRate.ir).exprand(1000/SampleRate.ir))  },
	// 		2, { LPF.ar(input, 20.0.exprand(1000)) },
	// 		3, { HPF.ar(input, 1.0.exprand(100))}
	// 	);
	// }
	//
	// synth {|nothingProb, delayProb, lpfProb, hpfProb| // nothing, delay, lpf, hpf
	// 	^{
	// 		var fb, gens;
	// 		fb = LocalIn.ar(n);
	// 		gens = n.collect{|i|
	// 			var from = inputConnections[i];
	// 			var edge1 = BinaryNet.edgeSelect([nothingProb, delayProb, lpfProb, hpfProb], fb[from[0]]);
	// 			var edge2 = BinaryNet.edgeSelect([nothingProb, delayProb, lpfProb, hpfProb], fb[from[1]]);
	//
	// 			//				var from = adjacencyList.select{|a| a[1] == i}.collect{|lst| lst[0]}.flat.postln;
	// 			//var out = twoClasses.choose.ar(fb[from[0]],fb[from[1]]);
	//
	// 			//0.2.coin.if({ 0.5.coin.if({LPF.ar(out, 20.0.exprand(1000))}, { HPF.ar(out, 1.0.exprand(100))}) },{
	// 			//0.2.coin.if({ DelayC.ar(out, 0.1, (1/SampleRate.ir).exprand(1000/SampleRate.ir)) }, { out })})
	// 		};
	// 		gens[0] = gens[0] + Impulse.ar(1/4);
	// 		//			LocalOut.ar(LeakDC.ar(gens));
	// 		LocalOut.ar(gens);
	// 		//LocalOut.ar(gens - OnePole.ar(gens, 0.99999));
	// 		//Out.ar(0, gens > 0.5);
	// 		Splay.ar(LeakDC.ar(gens));
	// 	}
	// }

}






// todo
// open input to outside
// make pseudo ugen
// decide on filters
// probabilities on construction
// make operations controllable/changeable

// DONE
// improve network construction algo


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