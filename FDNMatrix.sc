// Define a Matrix class
FDNMatrix {
    var <n, <dataArray, <bus;

    *new { |n, dataArray, withBus=true|
        ^super.new.init(n, dataArray, withBus)
    }

    init { |nIn, dataArrayIn, withBus=true|
        n = nIn;
        dataArray = dataArrayIn;
		if (withBus) {
			bus = Bus.control(Server.default, n.squared);
		}
    }

	matMult { |b|
        var rowsA = this.n, colsA = this.n, rowsB = b.n, colsB = b.n;
        var resultData = Array.fill(rowsA * colsB, 0);
        var result = FDNMatrix.new(rowsA, resultData, false);

        rowsA.do { |i|
            colsB.do { |j|
                var sumProduct = 0;
                colsA.do { |k|
                    sumProduct = sumProduct + (this.at(i, k) * this.at(k, j));
                };
                result.put(i, j, sumProduct);
            };
        };

        ^result
    }

	setData {|newData|
		dataArray = newData;
	}

    at { |row, col|
        ^dataArray[row * n + col]
    }

    put { |row, col, value|
        dataArray[row * n + col] = value;
    }

    row { |r|
        ^dataArray.copyRange(r * n, (r + 1) * n - 1)
    }

    col { |c|
        ^Array.fill(n, { |i| this.at(i, c) })
    }

    dot { |v1, v2|
        ^v1.collect({ |val, i| val * v2[i] }).sum
    }

	setBus {
		bus.setn(dataArray);
	}

    norm { |v|
        ^(this.dot(v, v)).sqrt
    }

    project { |u, a|
        var scalar = this.dot(a, u) / this.dot(u, u);
        ^u.collect({ |val| val * scalar })
    }


	powerIteration { |numIterations|

		var eigenvalue;
		var b_k = (1!n);  // Start with an initial vector (can be random)

		numIterations.do{
			var b_k1 = n.collect{|i|
				n.collect{|j|
					this.at(i,j) * b_k.at(j)
				}.sum
			};
			var b_k1_norm = b_k1.squared.sum.sqrt;
			b_k1 = b_k1 / b_k1_norm;
			b_k = b_k1;
		};

		eigenvalue = n.collect{ |i| b_k[i] * n.collect{ |j| this.at(i, j) * b_k[j] }.sum }.sum;
		^eigenvalue.abs
	}



    spectralRadius {
        ^this.powerIteration(300)
    }

	setSpectralRadius { |target|
		var fac = target / this.spectralRadius;
		this.setData(this.dataArray * fac);
		this.setBus
	}

/*
	   // Householder reflection function
    *householderReflection { |x|
		var normV;
        var v = x.copy;
        var normX = (x.collect { |val| val.squared }).sum.sqrt;
        var sign = if (x[0] >= 0, 1, -1);
        v[0] = v[0] + (sign * normX);
        normV = (v.collect { |val| val.squared }).sum.sqrt;
        v = v.collect { |val| val / normV };
        ^v
    }


    // QR decomposition function
    qrDecomposition {
        var n = this.n;
        var m = this.n;  // Assuming a is a square matrix
        var r = FDNMatrix.new(n, this.dataArray.copy, false);  // Make a copy of the matrix
        var qData = Array.fill(n * n, { |i| if (i % (n + 1) == 0) { 1 } { 0 } });
        var q = FDNMatrix.new(n, qData, false);  // Identity matrix

        m.do { |k|
			var h, hData, v;
            // Extract the vector to reflect
            var x = Array.newClear(n - k);
			(k .. n - 1).do { |i|
                x[i - k] = r.at(i, k);
            };

            // Compute the Householder vector
            v = FDNMatrix.householderReflection(x);
		//	v.postln;

            // Create the Householder matrix
            hData = Array.fill(n * n, { |i| if (i % (n + 1) == 0) { 1 } { 0 } });
            h = FDNMatrix.new(n, hData, false);
            (k .. n - 1).do { |i|
                (k .. n - 1).do { |j|
                    h.put(i, j, h.at(i, j) - (2 * v[i - k] * v[j - k]));
                };
            };

            // Apply the Householder transformation
			"h ".post; h.dataArray.postln;
            r = h.matMult(r);
            q = q.matMult(h);
        };

        ^[q, r]
    }*/


/*
    gramSchmidt {
        var q = [];
        var r = Array.fill(n, { Array.fill(n, 0) });
        var a, u, e;

        n.do { |i|
            a = this.row(i);
            u = a;
            (0..(i-1)).do { |j|
                e = q[j];
                u = u - this.project(e, a);
            };
            q = q.add(u);
            q[i] = q[i] / this.norm(u);
        };

        q.do { |qi, i|
            r[i] = qi;
        };

        ^FDNMatrix.new(n, r.flat)
    }
	*/
	gramSchmidt {


		var q = FDNMatrix(n, Array.fill(n.squared {0.0}), false);
		var r = FDNMatrix(n, Array.fill(n.squared {0.0}), false);
		var qi, v;

		n.do{|j|
			v = this.col(j).copy;

			j.do{|i|
				qi = q.col(i).copy;

				r.put(i, j, (qi * v).sum); //dot
				v = v - r.at(i, j) * qi;
			};
			r.put(j, j, v.squared.sum.sqrt);

			n.do{|row|
				q.put(row,j, v[row] / r.at(j,j));
			}
		}
		^q
	}

	gramSchmidtSelfBus {
		var new = this.gramSchmidt;
		dataArray = new.dataArray;
		this.setBus;

	}

}



