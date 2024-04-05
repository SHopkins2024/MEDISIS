To compile:

Once the simulations have ran and you have moved them into the header folder detailed in Part I, you are ready for
InitialDataProcessing. Keep in mind though that when compiling the data you will need approximate 30 GB of space for the 
output files.

To begin, start with the "MEDISIS.Data.Formation.Rmd" file located in 
"~\MEDISIS\Part2.Analysing.the.simulation.output\InitialDataProcessing" folder.
This will store the data into the "~\MEDISIS\Part2.Analysing.the.simulation.output\Data" folder.

Then run the "MEDISIS.Zeros.Rmd" file, which is also located in the 
"~\MEDISIS\Part2.Analysing.the.simulation.output\InitialDataProcessing" folder.

To process:

A series of analyses are available to explore. 

For convience, we begin with catch weight (i.e., folder: "CatchWeight").

1) The global catch patterns (i.e., not by coutnry, métier, or strategy), you will find the analyses in 
"MEDISIS.Catch.Weight.Global.Exploration.Rmd". 
- This code is nescessary to obtain the catch maitanance indices, the inital loss of catch, and juvenile 
avoidance indices (Fig. 4), and Appendix F, Fig. F.8.
- other pannel figures comparing all scenarios evaluated are also shown.


2) The comparision between the simulated catch weight in tonnes derived from the MEDISIS model and the 2019 
FAO report values, as well as code for Fig. 3 can be found here ("MEDISIS.Differences.In.Catch.Weight.Rmd"). 
-Values are compared against 2015-2018 total catch presented in section 3.2.7.2, subsection A, pp. 80-81 (FAO, 2019). 

FAO, 2019. GFCM: Working Group on Stock Assessment of Demersal Species (WGSAD) 
Benchmark Session for the Assessment of European Hake in GSAs 1,3,4,5,6,7,8,9,10,11,12,13,14,15,16,19,20,22,23 and 26.

- Note that 2019 was assumed to be 2018 values and 2018 values assumed to be 2017 values based on the presented figure on page 80 and the 1994-2018 stock assessment report from Certain et al., 2018. Values compared also included only the summed reported values per count (e.g., landings in 2015 for the French fleet equals the sum of landings for French OTB, French GNS, and French GTR in 2015 divided by 1000 (to get kgs).

3) In "MEDISIS.Catch.Weight.by.Metier.Exploration.spider.Rmd", we regroup the fleet segments so that the general patterns observed could be summarised more readily.
- The code provided returns the catch indices by fleet and population zone used in Fig. 4.

3) In "MEDISIS.Catch.Weight.by.Metier.Exploration.spider.Rmd", we regroup the fleet segments so that the general 
patterns observed could be summarised more readily.
- The code provided returns the catch indices by fleet and population zone used in Figs. 5 and 6.

--------------------------------------------------------------------------------------------------------------------------

Next we move to the biomass analyses (i.e., folder: "Biomass").

1)For global biomass, you will find the nescessary code here: "MEDISIS.Global.Biomass.Exploration.Rmd". This code is nescessary for Fig. 4.

2) Similiarly for biomass by population zone, you will find the code here: "MEDISIS Biomass byZone Exploration.Rmd". The code nescessary to reproduce Figs. 5 and 6 as well as Appendix F, Fig. F.9 is also present in this file.

--------------------------------------------------------------------------------------------------------------------------

Moving to revenue analyses (i.e., Folder: "Revenues"), there is a specific order that must be followed. 
Be sure to read the ReadMe.text file as well.

1) Begin with the "MEDISIS.VPUE.Rmd" - to view and extend vpue data. This code also produced Appendix A, Fig. A.1 and Appendix E, Fig. E.2.

2) Merge VPUE and effort computation files in "MEDISIS.Revenue.Values.Rmd". 

3) Calculate revenues in the "MEDISIS.Revenues.bySpecies.Rmd" file.
The nescessary code to reproduce the indices assessed in Fig. 4 as well as Appendix F. Fig. F.2 is found here.

--------------------------------------------------------------------------------------------------------------------------

For effort redistribution analyses, it is also import to read the scripts in a specified order (i.e., Folder: "EffortRedistributionMaps")
For more detailed instructions be sure to read the ReadMe.text file.

1) Begin by prepairing the data in the R Markdown "MEDISIS_Fishing_Effort_Distribution_DataPrep_andZoneDefs". 
This will return all zone definition files and furture cell definition work needed one data becomes available in the region producing Appendix B, Figs. B.1-B.23 and Appendix C, Figs. C.1 and C.2.

2) Then load the MEDISIS_Fishing_Effort_Distribution.Rmd file and execute the effort redistribution code. This code will render Appendix Figs. F.3-F.7.

--------------------------------------------------------------------------------------------------------------------------

To summarise the data, open the folder: "Spider.charts".

1) "MEDISIS.Spider.plots.main.14.Rmd" will give you the main idea of the scenario effects with respects to catch maintanance, 
initial loss of revenues, projected revenue changes, juvenile catch avoidance, and population biomass recovery as well as produce Fig. 4.

2) Meanwhile "MEDISIS.Spider.plots.main.14.fleets.yr1.Rmd" and "MEDISIS.Spider.plots.main.14.fleets.yr5.Rmd" will return
the fleet specific dynamics and differences between population zones producing Figs. 5 and 6.

--------------------------------------------------------------------------------------------------------------------------

Lastly for uncertainty, scenario comparision was made (i.e., Folder: "UncertaintyAnalyses")

1) For the full scenario evaluation, open "MEDISIS.Uncertainty.Rmd". 
- Note that it is nescessary to have first compiled at least the revenues code and ideally revenues, biomass, and catch weight
codes prior to running the uncertainty code. This also returns Appendix F, Fig. F.1.

2) For a selection of representative sceanrio comparisions, run the "MEDISIS.Uncertainty.Fisheries.Research.Rmd" file. This will return Fig. 7.



 




