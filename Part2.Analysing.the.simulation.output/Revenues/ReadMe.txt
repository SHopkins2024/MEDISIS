For Revenues:

* Other species are not explicit in the model and we assume that the species populations are otherwise 
unaffected by external factors. This is an avoidable assumption given the lack of information available for
the region and is meant more as an worst case scenario.

* For hake, we will use the average mean monthly price from 2017 * simulated catchweight

* For all other species, we use VPUE in 2017 * simulated effort. While this value is used for all
simulations going from 2017 onward, we need to adjust the VPUE when a spatial closure exists to match units. 
This is done by subtracting spatial points that coicide with the spatial temporal closure conditions.
	
Therefore, the order to procede are as follows:

	1) Compute VPUE from logbook and onboard observer data (i.e., "MEDISIS.VPUE.Rmd"): 
	To protect fisher confidentiality, we provided only the final data and the code used to calculate VPUE.
	The procedure followed can be summarised into three steps:
		- For years 2017 to 2019 and non-spatial closure scenarios, use the normal VPUE
		- For all other scenarios and years, cacluate the adjusted VPUE by substracting
		  out spatial closures as described above
		- Repeat the simulated year values for the remaining years
		
	2) Merge VPUE and effort computation files (i.e., "MEDISIS.Revenue.Values.Rmd"):
		- Essentially this is a data wrangling manipulation file that creates and combines
		  metier/scenario values 
		- Species are also renamed and hake dropped from the the final version to compute separately.
		  Recall that for hake derived revenues, effort is multipled by price whereas for other species
		 2017 VPUE (calculated in step 1) is multiplied by effort.
		
	3) Compute Revenues for all species and hake:
	This can be found in the "MEDISIS.Revenue.bySpecies.Rmd".
		