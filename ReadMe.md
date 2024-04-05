Shield: [![CC BY 4.0][cc-by-shield]][cc-by]

This work is licensed under a
[Creative Commons Attribution 4.0 International License][cc-by].

[![CC BY 4.0][cc-by-image]][cc-by]

[cc-by]: http://creativecommons.org/licenses/by/4.0/
[cc-by-image]: https://i.creativecommons.org/l/by/4.0/88x31.png
[cc-by-shield]: https://img.shields.io/badge/License-CC%20BY%204.0-lightgrey.svg

-------------------------------------------------------------------------------------------------------------------------------------------------------------
Description
------------

This is the working repository for the fishereis dynamic model MEDISIS, which is written in java and R programming (RMarkdown) languages. The data and codes provided here correspond to the Fisheries Research publication:

Hopkins Stephanie C., Lehuta Sigrid, Mahevas Stephanie, Vaz Sandrine (2024). Trade-offs between spatio-temporal closures and effort reduction measures to ensure fisheries sustainability. Fish. Res.

and the SEANOE data record: 

Hopkins Stephanie, Lehuta Sigrid, Mahevas Stephanie, Vaz Sandrine (2024). MEDISIS Hake Fisheries Dynamics Model for the Gulf of Lion. SEANOE. https://doi.org/10.17882/99221

-------------------------------------------------------------------------------------------------------------------------------------------------------------
Software requirements
---------------------

The following software components are needed:
Java Development Toolkit 20.02 or later
RStudio 2023.12.1 or later
R 4.3.2 or later
ISIS-Fish 4.4.8.1 (available at https://isis-fish.org/)

-------------------------------------------------------------------------------------------------------------------------------------------------------------
Instructions
------------

In order to reproduce the simulation output and simulation settings in ISIS-Fish, please follow the instructions listed in the ReadMe.txt file located in the folder "Part1.Reproducing.the.simulation.data". You will find all nescessary rules, export scripts, region defintions, simulation plan (i.e., "MEDISIS_with_28_scenarios.java"), source code for the simulation design table, and tutorial in this folder.

Once you have successfuly reproduced the simulation output results, you will find the data aggregation methods in the sub folder "InitialDataProcessing" located in folder "Part2.Analysing.the.simulation.output". The order of the steps needed and description of the following files are presented both in RMarkdowns and in the ReadMe.txt files located within each sub folder presented. Please note that the spider charts and uncertainty analyses must be done last. All file are automatically set to read and write to the "Data" sub folder in "Part2.Analysing.the.simulation.output"