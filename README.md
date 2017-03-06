# StatisticalInductiveMinerInfrequent

This is the program code for the Bachelor's thesis "A Statistical Variant of the Inductive Miner".

It is designed as a plugin for the ProM-Framework (see http://www.promtools.org/doku.php?id=start).

##Installation##

In order to use the plugin you need eclipse (see https://eclipse.org/) 

and the Ivy-plugin for eclipse (see https://ant.apache.org/ivy/ivyde/).




You then resolve all dependencies using the ivy.xml-file in in the projects main directory inside of eclipse. It will prompt for a login, in which case you just proceed.

##Using the plugins##

You can start ProM with the plugin using the appropriate launch-file in eclipse.

In ProM under the actions tab you can find the two plugins called "Mine Process Tree with statistical Inductive Miner" and 
"Analyze performance of statistical Inductive Miner".

In it's current version the variables have to be set prior to starting ProM in src/org/processmining/statisticalInductiveMiner/plugins/StatisticalInductiveMiner.java there are a few variables at the top of the file:

	"//for use in the algorithm
	double probabilityThreshold=0.05;
	double confidenceLevel=0.99;
	boolean eventTimeAnalysis=false;
	double epsilonInMinutes=5;


	//for use in performance analysis
	double deltaModelIncrement=1;
	double deltaEventIncrement=1;
	int NoOfExperiments=5;
	int NoOfMeasurementsPerExperiment=100;"
  
  
The first 4 are the delta, alpha, epsilon and boolean flag defined as user input in the work.
 
The lower 4 are used to automate the performance analysis for multiple epsilon. 

You can specifiy how many experiments per epsilon you want to conduct (NoOfMeasurementsPerExperiment),
 
how many different epsilon values you want to analyze (NoOfExperiments) 

and how much epsilon should be incremented or decremented after each analysis (deltaIncrement)
