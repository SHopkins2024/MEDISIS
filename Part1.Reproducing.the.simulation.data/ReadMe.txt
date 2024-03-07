The ISIS-Fish software version 4.4.8.0 can be downloaded here:
https://forge.codelutin.com/projects/isis-fish/files

Once installed, select the .bat file to run locally.
The first page will have as you to define a region. Please navigate to the "MEDISIS.zip" file
listed in this foler.

For further information on all the features present in isis-fish, you can go to https://isis-fish.org.

After the region is set (a diagram is shown on the first slide in the Simulation.setup.pptx found in the 
"QUICK.Start.Guide" folder), copy over the corresponding rules found in the "ISIS.Fish.Rules" folder
and exports files found in the "ISIS.Fish.Exports" folder into the 
"isis-fish-4\isis-community-database\rules" and "isis-fish-4\isis-community-database\exports" folders respectively.

It will also be nescessary to copy over the simulation plan "MEDISIS_with_28_scenarios.java" into the folder 
"isis-fish-4\isis-community-database\simulationplans" as well.

Once these files are copied, you can begin investigating their content.

You will need to update the beginning of the directory lines ""C:/Users/shopkins/Documents/"
in the simulation plan MEDISIS_with_28_scenarios.java" on lines 106, 112, and 113
as well as in the rule "Effort_2015_2017_GDL_local_t.java" on line 58.

After completed, you can start the simulation process.

To do this, you can look at the settings in the "Simulation.setup.pptx" file located 
'in the "QUICK.Start.Guide" folder.

Be sure to select MEDISIS as the region, the number of months to simulate is set to 120, all 
strategies are highlighted, hake is selected, and you have imported the initial abundance values 
taken from the stock assessment prior to simulation: which can be found here:
"ISIS.Fish.Input.Tables/InitialStockLevels.csv"
You should name the simulation MEDISIS or what-so-ever your chosing, but do not use "_". This will cause 
a bug when you index the simulations in Part II.

- To upload the initial abundance values directly:
1) Click on the left tab (Simulation). 
2) In the bottom left-hand corner right-click on the empty table under the tab (Hake's effectives).
3) Select "Import/Export file CSV", the click "Import from file".
4) Add the "./ISIS.Fish.Input.Tables/InitialStockLevels.csv" file mentioned above.

Select "Distri_Rec_variable_t" and "Effort_2015_2017_GDL_local_t", the simulation plan, and Results exports files, as well as the advanced 
parameterization settings described in the "Simulation.setup.pptx" file.

There will be 504 simulations that will start in tandum, which will result in approximately 60 GB of data. You can check the status of these simulations by
selecting the fish icon on the left menu bar to check the status of them. 

You can the access the data by selecting the calculator icon and selecting the simulation from the list.
More tips and tricks can be found in the "Simulation.setup.pptx" file.

The code used to generate the simulation design table, which is called in the simulation plan can be the folder "RMarkdown". However, the table is already provided under the "ISIS.Fish.Input.Tables" folder.

Lastly, an empty folder will be created with the following if naming the simulation "MEDISIS": "simu_MEDISIS_YYYY-MM-DD-hh-mm".
Move the corresponding simulations into this folder for Part II." 



