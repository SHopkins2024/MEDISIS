The two sets of program script files written in java and R programming languages correspond to the Fisheries Research publication "Trade-offs between spatio-temporal closures and effort reduction measures to ensure fisheries sustainability"

1Stephanie C. Hopkins, 2Sigrid Lehuta, 1Stephanie Mahevas, and 1Sandrine.Vaz

1 UMR 9190 MARBEC, University of Montpellier-IRD-Ifremer-CNRS, Av. Jean Monnet, CS 30171, Sète Cedex 34203, France

2 DECOD (Ecosystem Dynamics and Sustainability), IFREMER, Institut Agro, INRAE, Nantes 44980, France

The following software components are needed:
Java Development Toolkit 20.02 or later, RStudio 2023.12.1 or later, R 4.3.2 or later, ISIS-Fish 4.4.8.1.

In order to reproduce the simulation output and simulation settings in ISIS-Fish, please follow the instructions listed in the ReadMe.txt file located in the folder "Part1.Reproducing.the.simulation.data". You will find all nescessary rules, export scripts, region defintions, simulation plan (i.e., "MEDISIS_with_28_scenarios.java"), source code for the simulation design table, and tutorial in this folder.

Once you have successfuly reproduced the simulation output results, you will find the data aggregation methods in the sub folder "InitialDataProcessing" located in folder "Part2.Analysing.the.simulation.output". The order of the steps needed and description of the following files are presented both in RMarkdowns and in the ReadMe.txt files located within each sub folder presented. Please note that the spider charts and uncertainty analyses must be done last. All file are automatically set to read and write to the "Data" sub folder in "Part2.Analysing.the.simulation.output"