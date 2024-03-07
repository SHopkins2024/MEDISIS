/*
 * Copyright (C) 2021 shopkins
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */

package simulationplans;
import fr.ifremer.isisfish.IsisFishDAOHelper;
import fr.ifremer.isisfish.datastore.SimulationStorage;
import fr.ifremer.isisfish.entities.Equation;
import fr.ifremer.isisfish.entities.Gear;
import fr.ifremer.isisfish.entities.Population;
import fr.ifremer.isisfish.entities.PopulationDAO;
import fr.ifremer.isisfish.entities.Zone;
import fr.ifremer.isisfish.rule.Rule;
import fr.ifremer.isisfish.simulator.SimulationPlanContext;
import fr.ifremer.isisfish.simulator.SimulationPlanIndependent;
import fr.ifremer.isisfish.types.Month;
import fr.ifremer.isisfish.types.TimeStep;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuiton.math.matrix.MatrixFactory;
import org.nuiton.math.matrix.MatrixIterator;
import org.nuiton.math.matrix.MatrixND;
import org.nuiton.topia.TopiaContext;
import rules.Effort_2015_2017_GDL_local_t;
import rules.Distri_Rec_variable_t;
import rules.EffortReduction_fixe_t;
import rules.EffortReduction_fixe_AllGears_t;
import rules.EffortReduction_10_20_30_t;
import rules.EffortReduction_10_20_30_AllGears_t;
import rules.EffortReduction_10_17point5_to40percent_t;
import rules.EffortReduction_10_17point5_to40percent_AllGears_t;
import rules.EffortReduction_10_20_30_40_50_t;
import rules.EffortReduction_10_20_30_40_50_AllGears_t;
import rules.Cantonnement;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

//import org.apache.commons.lang3.StringUtils.splitByWholeSeparator;


/**
 * MEDISIS_with_28_scenarios.java
 * <p>
 * Created: 30 July 2021
 *
 * @author shopkins <user.name@vcs.hostName>
 * @version $Revision: 1 $
 * Last update: $Date: 17 December 2021 $
 * by : $Author: shopkins $
 * <p>
 * ATTENTION :
 * This is the first attempt in writing the script, so check the following before launching the plan:
 * -paths leading to the csv containing the initial staffing tables used in the simulation plan
 * -the name of the management rules called in the plan and the begin & end steps
 * -path leading to the csv containing the experimental plan (simulation_design)
 * -in the rules: "Effort_2015_2017_GDL_local_t", "Distri_Rec_variable_t","EffortReduction_fixe_t",
 * "EffortReduction_fixe_AllGears_t", "EffortReduction_10_20_30_t","EffortReduction_10_20_30_AllGears_t",
 * "EffortReduction_10_17point5_to40percent_AllGears_t", "EffortReduction_10_17point5_to40percent_t",
 * "EffortReduction_10_20_30_40_50_t","EffortReduction_10_20_30_40_50_AllGears_t","Cantonnement")
 * -> Effort is now applied to the proportion of the Spanish_Strategy which corresponds to ESP_OTB (a grouped gear)
 * -in the uncertainty analysis part of the plan, take exactly the recruitment equation that appears in the database:
 * eqqs = eqqs.replaceAll ("double recru = 31296 \\; double \\ [\\] recZone = \\ {0.165,0.835 \\} \\;", eqqNew);
 * <p>
 * --------------------------------------------------------------------------------------------------------------------------------------------------------------------
 */

public class MEDISIS_with_28_scenarios implements SimulationPlanIndependent {

    //to use log facility, just put in your code: log.info("...");
    private static Log log = LogFactory.getLog(MEDISIS_with_28_scenarios.class);

    static private String MATRIX = "simulation_design_28"; // corresponding code to formulate this can be found the ./RMarkdown/Simulation.design.table.source.code.Rmd
    // this is a table of all possible combinations (rules from original plan are actually scenarios)
    // its order is irrelevant since you are doing a global analysis and the table can be generated in R
    private MatrixND matrix = null; // create empty matrix in java

    // Matrix columns
    static private final String INIT = "InitialAbundance"; // names of columns from previous table
    static private final String RECRUT = "Recruitment";
    static private final String CONNEC = "Connectivity";
    static private final String SCENARIO = "Scenario";

    static private final int parameterNumber = 4; // number of columns
    static private final int matrix_size = 504; // 2 InitialAbundance * 3 Recruitment * 3 Connectivity * 30 Scenarios (StatuQuo id=0 plus 27 management scenarios)
    public int param_first = 0;
    public int param_simulationNumber = 504; // equals the matrix_size
    public String param_directory = "C:/Users/steph/Documents/MEDISIS/Part1.Reproducing.the.simulation.data/ISIS.Fish.Input.Tables/"; // where the simulation design table is stored
    public String param_directory_rules = "C:/Users/steph/Documents/MEDISIS/Part1.Reproducing.the.simulation.data/ISIS.Fish.Input.Tables/"; // where the supporting tables for the rules are stored

    private MatrixND mateffInit0 = null; // more empty matrices
    private MatrixND mateffInit1 = null;
    // TO DO enter correct matrix path and names
    static private final String effInit0 = "C:/Users/steph/Documents/MEDISIS/Part1.Reproducing.the.simulation.data/ISIS.Fish.Input.Tables/abondInit.csv"; //pre-simulation step so should be close to where the simulation design table is
    static private final String effInit1 = "C:/Users/steph/Documents/MEDISIS/Part1.Reproducing.the.simulation.data/ISIS.Fish.Input.Tables/abond_z1.csv";

    protected String[] necessaryResult = {

    };

    @Override
    public String[] getNecessaryResult() {
        return this.necessaryResult;
    }

/**
* Permet d'afficher a l'utilisateur une aide sur le plan.
* @return L''aide ou la description du plan
* --------------------------------------------------------------------------------------
* Used to display help on the map to the user.
* @return Help or description of the plan
* --------------------------------------------------------------------------------------
*/

    @Override
    public String getDescription() throws Exception {
        return "MEDISIS management scenarios and main uncertainty hypotheses for GDL Hake";
    }

/**
* Called once before {@code beforeSimulation} call.
* @param context plan context
*/

    @Override
    public void init(SimulationPlanContext context) throws Exception { // method or function (init is called only once at beginning of plan)

        System.out.println("etape0");
        // Load simulation plan matrix
        matrix = MatrixFactory.getInstance().create(new int[]{matrix_size, parameterNumber}); // load matrix from above
        matrix.importCSV(new FileReader(new File(param_directory, MATRIX + ".csv")), new int[]{0, 0}); // import csvs into matrix
        List<Integer> dim0 = new ArrayList<Integer>(); // creates a list of integers with 3 lines below...in java this has to be done as a loop
        for (int i = 0; i < matrix_size; i++) { // end result is a list of matrix_Size length
            dim0.add(i);
        }
        System.out.println("etape1: lecture du plan de conception de la simulation"); // reading simulation design table (i.e. simulation plan)
        matrix.setSemantic(0, dim0); //sets names of rows
        matrix.setSemantic(1, Arrays.asList(new String[]{INIT, RECRUT, CONNEC, SCENARIO})); // sets names of columns

// End of importing simulation plan in java

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

// To DO Add necessary rules to simulation context (the supporting tables called in each rule should be stored where the simulations are run)
        context.getParam().addExtraRules("Effort_2015_2017_GDL_local_t"); // efort de peche force sur les annees de calibration // fishing effort based on calibration years
        context.getParam().addExtraRules("Distri_Rec_variable_t"); // distribution du recrutement variable sur les annees de calibration // variable recruitment distribution based on calibration years
        context.getParam().addExtraRules("EffortReduction_fixe_t");
        context.getParam().addExtraRules("EffortReduction_fixe_AllGears_t");
        context.getParam().addExtraRules("EffortReduction_10_20_30_t");
        context.getParam().addExtraRules("EffortReduction_10_20_30_AllGears_t");
        context.getParam().addExtraRules("EffortReduction_10_17point5_to40percent_AllGears_t");
        context.getParam().addExtraRules("EffortReduction_10_17point5_to40percent_t");
        context.getParam().addExtraRules("EffortReduction_10_20_30_40_50_t");
        context.getParam().addExtraRules("EffortReduction_10_20_30_40_50_AllGears_t");
        context.getParam().addExtraRules("Cantonnement");

// distribution du recrutement variable sur les annees de calibration
// variable recruitment distribution based on calibration years

        // Load initial abundance matrices
        mateffInit0 = MatrixFactory.getInstance().create(new int[]{6, 2}); // abundance for dims age and zones
        mateffInit0.importCSV(new FileReader(new File(effInit0)), new int[]{0, 0});
        mateffInit1 = MatrixFactory.getInstance().create(new int[]{6, 2});
        mateffInit1.importCSV(new FileReader(new File(effInit1)), new int[]{0, 0});

        System.out.println("etape2 : lecture plan des abondances initiales");
    }

/**
* @param name -> le nom de l'element a recuperer
* @param simulation -> le numero de la simulation
* ------------------------------------------------
* @param name -> name of the element to retrieve
* @param simulation -> number of the the simulation
 -------------------------------------------------
* /**

    private double getDouble(String name, String colname, int simulation) throws Exception {
        File dir = new File(param_directory);
        Properties prop = new Properties();
        prop.load(new BufferedReader(new FileReader(new File(dir, name + ".txt"))));
        int ligne = simulation + param_first;
        int mod = (int) matrix.getValue(ligne, colname);
        System.out.println("nom " + name + "mod :" + mod);
        double result = Double.parseDouble(prop.getProperty("" + mod));

        return result;
    }
 -------------------------------------------------
/**
* Call before each simulation.
* @param context -> plan context
* @param nextSimulation storage used for next simulation
* @return true if we must do next simulation, false to stop plan
* @throws Exception
*/

    @Override
    // runs matrix_Size times before each simulation (the scenario combination or line of the of the matrix defined above)
    public boolean beforeSimulation(SimulationPlanContext context,
                                    SimulationStorage nextSimulation) throws Exception {
        System.out.println("entre etapes 2 et 3");
        int simNum = nextSimulation.getParameter().getSimulationPlanNumber() + param_first; // gets the number of the current simulation
        System.out.println("etape3 : determiner getDouble valeur simNum : " + simNum);

        TopiaContext db = nextSimulation.getStorage().beginTransaction();
        String populationId = nextSimulation.getParameter().getPopulations().get(0).getTopiaId();
        PopulationDAO populationDAO = IsisFishDAOHelper.getPopulationDAO(db);
        Population pop = populationDAO.findByTopiaId(populationId);

        if (simNum < param_simulationNumber + param_first) {

////////////////////////////////////////////////////////////
// Modif rules

            List<Rule> paramRules = nextSimulation.getParameter().getRules();

            double init = matrix.getValue(simNum, INIT); //retrieve values for the four columns in that line
            double recr = matrix.getValue(simNum, RECRUT);
            double conn = matrix.getValue(simNum, CONNEC);
            double scen = matrix.getValue(simNum, SCENARIO);

            int ruleNum = 0;  // StatusQuo
            System.out.println("etape4 , " + init + recr + conn + scen);

            // Paramètres des règles à modifier pour chaque scénario
            // Rule settings to modify for each scenario

// ------------------------------------------------------
// Fishing Effort Reduction SCENARIOS
// ------------------------------------------------------

            // reduction taille flottille des chalets actuelle (reduction de 30% de l'effort nominal)
            // reduction in the size of the current trawling fleet (30% effort reduction of nominal effort)
            if (scen == 1) {
                setupScenario1(nextSimulation, paramRules);
            }
            // reduction taille flottille des chalets actuelle (reduction de 40% de l'effort nominal)
            // reduction in the size of the current trawling fleet (40% effort reduction of nominal effort)
            if (scen == 2) {
                setupScenario2(nextSimulation, paramRules);
            }
            // reduction taille flottille des chalets actuelle (reduction de 50% de l'effort nominal)
            // reduction in the size of the current trawling fleet (50% effort reduction of nominal effort)
            if (scen == 3) {
                setupScenario3(nextSimulation, paramRules);
            }
            // réduction progressive de la taille de la flotte de chalutiers actuelle (réduction de 10% en 2020, 20% en 2021, 30% en 2022 de l'effort nominal)
            // gradual reduction in the size of the current trawling fleet (10% in 2020, 20% in 2021, 30% in 2022 reduction in nominal effort)
            if (scen == 4) {
                setupScenario4(nextSimulation, paramRules);
            }
            // réduction progressive de la taille de la flotte de chalutiers actuelle (réduction de 10% en 2020, 17.5% en 2021, 25% en 2022, 32.5% en 2023, et 40% en 2024 de l'effort nominal)
            // gradual reduction in the size of the current trawling fleet (reduction of 10% in 2020, 17.5% in 2021, 25% in 2022, 32.5% in 2023, and 40% in 2024 for nominal effort)
            if (scen == 5) {
                setupScenario5(nextSimulation, paramRules);
            }
            // réduction progressive de la taille de la flotte de chalutiers actuelle (réduction de 10% en 2020, 20% en 2021, 30% en 2022, 40% en 2023, et 50% en 2024 de l'effort nominal)
            // gradual reduction in the size of the current trawling fleet (reduction of 10% in 2020, 20% in 2021, 30% in 2022, 40% in 2023, and 50% in 2024 for nominal effort)
            if (scen == 6) {
                setupScenario6(nextSimulation, paramRules);
            }
            // réduction de la taille de la flotte appliquée à tous les engins (reduction de 30% de l'effort nominal)
            // reduction in the size of the fleet applied to all gears (30% effort reduction of nominal effort)
            if (scen == 7) {
                setupScenario7(nextSimulation, paramRules);
            }
            // réduction de la taille de la flotte appliquée à tous les engins (reduction de 40% de l'effort nominal)
            // reduction in the size of the fleet applied to all gears (40% effort reduction of nominal effort)
            if (scen == 8) {
                setupScenario8(nextSimulation, paramRules);
            }
            //réduction de la taille de la flotte appliquée à tous les engins (reduction de 50% de l'effort nominal)
            //reduction in the size of the fleet applied to all gears (50% effort reduction of nominal effort)
            if (scen == 9) {
                setupScenario9(nextSimulation, paramRules);
            }
            // réduction progressive de la taille de la flotte appliquée à tous les engins (réduction de 10% en 2020, 20% en 2021, 30% en 2022 de l'effort nominal)
            // gradual reduction in the size of the fleet applied to all gears (10% in 2020, 20% in 2021, 30% in 2022 reduction in nominal effort)
            if (scen == 10) {
                setupScenario10(nextSimulation, paramRules);
            }
            // réduction progressive de la taille de la flotte appliquée à tous les engins (réduction de 10% en 2020, 17.5% en 2021, 25% en 2022, 32.5% en 2023, et 40% en 2024 de l'effort nominal)
            // gradual reduction in the size of the fleet applied to all gears(reduction of 10% in 2020, 17.5% in 2021, 25% in 2022, 32.5% in 2023, and 40% in 2024 for nominal effort)
            if (scen == 11) {
                setupScenario11(nextSimulation, paramRules);
            }
            // réduction progressive de la taille de la flotte appliquée à tous les engins (réduction de 10% en 2020, 20% en 2021, 30% en 2022, 40% en 2023, et 50% en 2024 de l'effort nominal)
            // gradual reduction in the size of the fleet applied to all gears (reduction of 10% in 2020, 20% in 2021, 30% in 2022, 40% in 2023, and 50% in 2024 for nominal effort)
            if (scen == 12) {
                setupScenario12(nextSimulation, paramRules);
            }

// ------------------------------------------------------
// Spatial Temporal Closure SCENARIOS
// ------------------------------------------------------

            // FRA fermé toute l'année
            // FRA closed all year
            if (scen == 13) {
                setupScenario13(nextSimulation, paramRules, db);
            }
            // Nouveau règlement FRA (fermè novembre à avril)
            // New FRA Regulation (November to April Closure)
            if (scen == 14) {
                setupScenario14(nextSimulation, paramRules, db);
            }
            // Nouveau règlement FRA (fermè toute l'année)
            // New FRA Regulation (closed all year)
            if (scen == 15) {
                setupScenario15(nextSimulation, paramRules, db);
            }
            // Nouveau règlement 90-100 m isobathe (fermé septembre à avril)
            // New 90-100 m isobath closure regulations (September to April closure)
            if (scen == 16) {
                setupScenario16(nextSimulation, paramRules, db);
            }
            // Nouveau règlement 90-100 m isobathe (fermé toute l'année)
            // New 90-100 m isobath closure regulations (closed all year)
            if (scen == 17) {
                setupScenario17(nextSimulation, paramRules, db);
            }
            // Zones de fermeture en mer appliquées à partir du 28 avril 2018 par les pêcheries pour tous les engins de fond
            // Les fermetures offshore comprennent 3 zones fermées définitivement et une zone de fermeture saisonnière appliquée autour de leur connectivité du 15 octobre au 15 décembre.
            //Pour tenir compte de la résolution du modèle, la date de début de la fermeture saisonnière a été déplacée au 1er octobre et la date de fin au 30 novembre.
            // Offshore closure areas enforced as of April 28th, 2018 by the fisheries for all bottom gears.
            // The offshore closures comprise of 3 zones closed permanently and a seasonal closure zone applied around their connectivity from October 15th to December 15th.
            // To adjust for the model resolution, the starting date of the seasonal closure was moved to October 1st and ending date to November 30th.
            if (scen == 18) {
                setupScenario18(nextSimulation, paramRules, db);
            }
            // Les fermetures offshore comprennent 3 zones fermées définitivement et étendent la fermeture de la zone de connectivité à toute l'année.
            // The offshore closures comprise of 3 zones closed permanently and extends closure of the connectivity zone to all year.
            if (scen == 19) {
                setupScenario19(nextSimulation, paramRules, db);
            }

// ------------------------------------------------------
// Combined Spatial Closure  SCENARIOS
// ------------------------------------------------------

            // Nouveau règlement FRA combiné, règlement de fermeture pour 90-100 isobathes et fermetures offshore, (fermé septembre à avril)
            // Combined New FRA regulation, closure regulation for 90-100 isobaths, and offshore closures, (September to April closure)
            if (scen == 20) {
                setupScenario20(nextSimulation, paramRules, db);
            }
            // Nouveau règlement FRA combiné, règlement de fermeture pour 90-100 isobathes et fermetures offshore, (toute l'année)
          	// Combined New FRA regulation, closure regulation for 90-100 isobaths, and offshore closures, (all year)
            if (scen == 21) {
                setupScenario21(nextSimulation, paramRules, db);

// --------------------------------------------------------
// Combined Effort Reduction and Spatial Closure SCENARIOS
// --------------------------------------------------------

            // Nouveau règlement FRA combiné, règlement de fermeture pour 90-100 isobathes et fermetures offshore
            // Avec un réduction progressive de la taille de la flotte de chalutiers actuelle (réduction de 10% en 2020, 17.5% en 2021, 25% en 2022, 32.5% en 2023, et 40% en 2024 de l'effort nominal)
            // Combined New FRA regulation, closure regulation for 90-100 isobathes, and offshore closures
            // With a gradual reduction in the size of the current trawling fleet (reduction of 10% in 2020, 17.5% in 2021, 25% in 2022, 32.5% in 2023, and 40% in 2024 for nominal effort)
            }
            if (scen == 22) {
                setupScenario22(nextSimulation, paramRules, db);
            }
            // Nouveau règlement FRA combiné, règlement de fermeture pour 90-100 isobathes et fermetures offshore
            // Avec un réduction progressive de la taille de la flotte appliquée à tous les engins (réduction de 10% en 2020, 17.5% en 2021, 25% en 2022, 32.5% en 2023, et 40% en 2024 de l'effort nominal)
            // Combined New FRA regulation, closure regulation for 90-100 isobaths, and offshore closures
            // With a gradual reduction in the size of the fleet applied to all gears(reduction of 10% in 2020, 17.5% in 2021, 25% in 2022, 32.5% in 2023, and 40% in 2024 for nominal effort)
            if (scen == 23) {
                setupScenario23(nextSimulation, paramRules, db);
            }

// ----------------------------------------------------------
// Post-hoc Simulation Scenarios Due to progressive Deleterious Combined Effects
// ----------------------------------------------------------

          // Nouveau règlement FRA combiné et fermetures offshore
          // Combined New FRA regulation and offshore closures
          if (scen == 24) {
              setupScenario24(nextSimulation, paramRules, db);
            }
          // Nouveau règlement FRA combiné et fermetures offshore, (toute l'année)
          // Combined New FRA regulation and offshore closures, (all year)
          if (scen == 25) {
              setupScenario25(nextSimulation, paramRules, db);
            }
          // Nouveau règlement FRA combiné et fermetures offshore avec un réduction progressive de la taille de la flotte de chalutiers actuelle (réduction de 10% en 2020, 17.5% en 2021, 25% en 2022, 32.5% en 2023, et 40% en 2024 de l'effort nominal)
          // Combined New FRA regulation and offshore closures with a gradual reduction in the size of the current trawling fleet (reduction of 10% in 2020, 17.5% in 2021, 25% in 2022, 32.5% in 2023, and 40% in 2024 for nominal effort)
          if (scen == 26) {
              setupScenario26(nextSimulation, paramRules, db);
            }
          // Nouveau règlement FRA combiné et fermetures offshore avec un réduction progressive de la taille de la flotte appliquée à tous les engins (réduction de 10% en 2020, 17.5% en 2021, 25% en 2022, 32.5% en 2023, et 40% en 2024 de l'effort nominal)
          // Combined New FRA regulation and offshore closures with a gradual reduction in the size of the fleet applied to all gears(reduction of 10% in 2020, 17.5% in 2021, 25% in 2022, 32.5% in 2023, and 40% in 2024 for nominal effort)
          if (scen == 27) {
              setupScenario27(nextSimulation, paramRules, db);
            }

////////////////////////////////////////////////////////////
// Modif Recrutement //replaces the average of the recruitment values used for 2018 onwards and checks alternative rules

          String recEq_part1 = "double recru = 31296;"; //projection en tenant compte du facteur de correction issu de la calibration (from calibration)
          if (recr == 1) {
              recEq_part1 = "double recru = 47298;"; // 1er quartile de la série (first quartile of the time series)
            } else if (recr == 2) {
              recEq_part1 = "double recru = 64960;"; // moyenne sur la série depuis 1998 (average of time series)
            }

////////////////////////////////////////////////////////////////
// modif connectivite

          String recEq_part2 = "double[] recZone = {0.165,0.835};"; // connectivity
          if (conn == 1) {
              recEq_part2 = "double[] recZone = {0.452,0.548};";
            } else if (conn == 2) {
              recEq_part2 = "double[] recZone = {0.026,0.974};";
            }
          System.out.println("Avant changement recrutement");
          Equation eqq = pop.getReproductionEquation(); // get the equation
          String eqqs = eqq.getContent(); // retreive character string of characters of the equation
          System.out.println("eqqs : " + eqqs);
          String eqqNew = recEq_part1 + recEq_part2;

          eqqs = eqqs.replaceAll("double recru = 31296\\; double\\[\\] recZone = \\{0.165,0.835\\}\\;", eqqNew); /// ATTENTION :: mettre valeur Recrutement de la base !!!!
          // replaces the values of the character string with new values (eqqNew)...replaces both recruitment and connectivity uncertainty
          System.out.println("EqqNew : " + eqqNew);
          eqq.setContent(eqqs);

          System.out.println("EqqNew : " + eqq.getContent());
          System.out.println("TOTO 6");

////////////////////////////////////////////////////////////
// Modif effectifs initiaux //retreive initial abundance and replace with recalculated numbers

          MatrixND mat = nextSimulation.getParameter().getNumberOf(pop); // if the value is 0 or 1, use the corresponding mateffInit
          MatrixND mateffInit = null;
          if (init == 0) {
              mateffInit = mateffInit0;
            } else if (init == 1) {
              mateffInit = mateffInit1;
            }
          for (MatrixIterator i = mat.iterator(); i.hasNext(); ) { // in java the only way to retrieve the values in a matrix is to go case by case
              i.next();
              int[] dim = i.getCoordinates(); // for each row column combination in eff, get value and store in val in the dim table
              double val = mateffInit.getValue(dim);
              i.setValue(val);
              System.out.println("Initial_Abundance" + i.getValue());
            }
          db.commitTransaction();
          db.closeContext();
          System.out.println("etape 5 : Fin de la mise en place des combinaisons");
          return true;
        } else return false;
    }

////////////////////////////////////////////////////////////
// Modif setupSenarios

// ------------------------------------------------------
// Fishing Effort Reduction SCENARIOS
// ------------------------------------------------------

    // reduction taille flottille des chalets actuelle (reduction de 30% de l'effort nominal)
    // reduction in the size of the current trawling fleet (30% effort reduction of nominal effort)
    protected void setupScenario1(SimulationStorage nextSimulation, List<Rule> paramRules) {
        EffortReduction_fixe_t rule = new EffortReduction_fixe_t();
        rule.param_beginStep = new TimeStep(60);
        rule.param_endStep = new TimeStep(120);
        rule.param_PercentReduction = 0.3;
        paramRules.add(rule);
        System.out.println("Mesure de Gestion : 1");
    }
    // reduction taille flottille des chalets actuelle (reduction de 40% de l'effort nominal)
    // reduction in the size of the current trawling fleet (40% effort reduction of nominal effort)
    protected void setupScenario2(SimulationStorage nextSimulation, List<Rule> paramRules) {
        EffortReduction_fixe_t rule = new EffortReduction_fixe_t();
        rule.param_beginStep = new TimeStep(60);
        rule.param_endStep = new TimeStep(120);
        rule.param_PercentReduction = 0.4;
        paramRules.add(rule);
        System.out.println("Mesure de Gestion : 2");
    }
    // reduction taille flottille des chalets actuelle (reduction de 50% de l'effort nominal)
    // reduction in the size of the current trawling fleet (50% effort reduction of nominal effort)
    protected void setupScenario3(SimulationStorage nextSimulation, List<Rule> paramRules) {
        EffortReduction_fixe_t rule = new EffortReduction_fixe_t();
        rule.param_beginStep = new TimeStep(60);
        rule.param_endStep = new TimeStep(120);
        rule.param_PercentReduction = 0.5;
        paramRules.add(rule);
        System.out.println("Mesure de Gestion : 3");
    }
    // réduction progressive de la taille de la flotte de chalutiers actuelle (réduction de 10% en 2020, 20% en 2021, 30% en 2022 de l'effort nominal)
    // gradual reduction in the size of the current trawling fleet (10% in 2020, 20% in 2021, 30% in 2022 reduction in nominal effort)
    protected void setupScenario4(SimulationStorage nextSimulation, List<Rule> paramRules) {
        EffortReduction_10_20_30_t rule = new EffortReduction_10_20_30_t();
        rule.param_beginStep = new TimeStep(60);
        rule.param_endStep = new TimeStep(120);
        paramRules.add(rule);
        System.out.println("Mesure de Gestion : 4");
    }
    // réduction progressive de la taille de la flotte de chalutiers actuelle (réduction de 10% en 2020, 17.5% en 2021, 25% en 2022, 32.5% en 2023, et 40% en 2024 de l'effort nominal)
    // gradual reduction in the size of the current trawling fleet (reduction of 10% in 2020, 17.5% in 2021, 25% in 2022, 32.5% in 2023, and 40% in 2024 for nominal effort)
    protected void setupScenario5(SimulationStorage nextSimulation, List<Rule> paramRules) {
        EffortReduction_10_17point5_to40percent_t rule = new EffortReduction_10_17point5_to40percent_t();
        rule.param_beginStep = new TimeStep(60);
        rule.param_endStep = new TimeStep(120);
        paramRules.add(rule);
        System.out.println("Mesure de Gestion : 5");
    }
    // réduction progressive de la taille de la flotte de chalutiers actuelle (réduction de 10% en 2020, 20% en 2021, 30% en 2022, 40% en 2023, et 50% en 2024 de l'effort nominal)
    // gradual reduction in the size of the current trawling fleet (reduction of 10% in 2020, 20% in 2021, 30% in 2022, 40% in 2023, and 50% in 2024 for nominal effort)
    protected void setupScenario6(SimulationStorage nextSimulation, List<Rule> paramRules) {
        EffortReduction_10_20_30_40_50_t rule = new EffortReduction_10_20_30_40_50_t();
        rule.param_beginStep = new TimeStep(60);
        rule.param_endStep = new TimeStep(120);
        paramRules.add(rule);
        System.out.println("Mesure de Gestion : 6");
    }
    // réduction de la taille de la flotte appliquée à tous les engins (reduction de 30% de l'effort nominal)
    // reduction in the size of the fleet applied to all gears (30% effort reduction of nominal effort)
    protected void setupScenario7(SimulationStorage nextSimulation, List<Rule> paramRules) {
        EffortReduction_fixe_AllGears_t rule = new EffortReduction_fixe_AllGears_t();
        rule.param_beginStep = new TimeStep(60);
        rule.param_endStep = new TimeStep(120);
        rule.param_PercentReduction = 0.3;
        paramRules.add(rule);
        System.out.println("Mesure de Gestion : 7");
    }
    // réduction de la taille de la flotte appliquée à tous les engins (reduction de 40% de l'effort nominal)
    // reduction in the size of the fleet applied to all gears (40% effort reduction of nominal effort)
    protected void setupScenario8(SimulationStorage nextSimulation, List<Rule> paramRules) {
        EffortReduction_fixe_AllGears_t rule = new EffortReduction_fixe_AllGears_t();
        rule.param_beginStep = new TimeStep(60);
        rule.param_endStep = new TimeStep(120);
        rule.param_PercentReduction = 0.4;
        paramRules.add(rule);
        System.out.println("Mesure de Gestion : 8");
    }
    // réduction de la taille de la flotte appliquée à tous les engins (reduction de 50% de l'effort nominal)
    // reduction in the size of the fleet applied to all gears (50% effort reduction of nominal effort)
    protected void setupScenario9(SimulationStorage nextSimulation, List<Rule> paramRules) {
        EffortReduction_fixe_AllGears_t rule = new EffortReduction_fixe_AllGears_t();
        rule.param_beginStep = new TimeStep(60);
        rule.param_endStep = new TimeStep(120);
        rule.param_PercentReduction = 0.5;
        paramRules.add(rule);
        System.out.println("Mesure de Gestion : 9");
    }
    // réduction progressive de la taille de la flotte appliquée à tous les engins (réduction de 10% en 2020, 20% en 2021, 30% en 2022 de l'effort nominal)
    // gradual reduction in the size of the fleet applied to all gears (10% in 2020, 20% in 2021, 30% in 2022 reduction in nominal effort)
    protected void setupScenario10(SimulationStorage nextSimulation, List<Rule> paramRules) {
        EffortReduction_10_20_30_AllGears_t rule = new EffortReduction_10_20_30_AllGears_t();
        rule.param_beginStep = new TimeStep(60);
        rule.param_endStep = new TimeStep(120);
        paramRules.add(rule);
        System.out.println("Mesure de Gestion : 10");
    }
    // réduction progressive de la taille de la flotte appliquée à tous les engins (réduction de 10% en 2020, 17.5% en 2021, 25% en 2022, 32.5% en 2023, et 40% en 2024 de l'effort nominal)
    // gradual reduction in the size of the fleet applied to all gears(reduction of 10% in 2020, 17.5% in 2021, 25% in 2022, 32.5% in 2023, and 40% in 2024 for nominal effort)
    protected void setupScenario11(SimulationStorage nextSimulation, List<Rule> paramRules) {
        EffortReduction_10_17point5_to40percent_AllGears_t rule = new EffortReduction_10_17point5_to40percent_AllGears_t();
        rule.param_beginStep = new TimeStep(60);
        rule.param_endStep = new TimeStep(120);
        paramRules.add(rule);
        System.out.println("Mesure de Gestion : 11");
    }
    // réduction progressive de la taille de la flotte appliquée à tous les engins (réduction de 10% en 2020, 20% en 2021, 30% en 2022, 40% en 2023, et 50% en 2024 de l'effort nominal)
    // gradual reduction in the size of the fleet applied to all gears (reduction of 10% in 2020, 20% in 2021, 30% in 2022, 40% in 2023, and 50% in 2024 for nominal effort)
    protected void setupScenario12(SimulationStorage nextSimulation, List<Rule> paramRules) {
        EffortReduction_10_20_30_40_50_AllGears_t rule = new EffortReduction_10_20_30_40_50_AllGears_t();
        rule.param_beginStep = new TimeStep(60);
        rule.param_endStep = new TimeStep(120);
        paramRules.add(rule);
        System.out.println("Mesure de Gestion : 12");
    }

// ------------------------------------------------------
// Spatial Temporal Closure SCENARIOS
// ------------------------------------------------------

    // FRA fermé toute l'année
    // FRA closed all year
    protected void setupScenario13(SimulationStorage nextSimulation, List<Rule> paramRules, TopiaContext db) {
        Zone FRA = IsisFishDAOHelper.getZoneDAO(db).findByName("Original_FRA"); // you need the call name of the object zone and not the file name
        Gear OTT = IsisFishDAOHelper.getGearDAO(db).findByName("OTT");
        Gear OTB = IsisFishDAOHelper.getGearDAO(db).findByName("OTB");
        // OTT
        Cantonnement rule1 = new Cantonnement();
        rule1.param_zone = FRA;
        rule1.param_gear = OTT;
        rule1.param_beginStep = new TimeStep(60);
        rule1.param_endStep = new TimeStep(120);
        rule1.param_beginMonth = Month.JANUARY;
        rule1.param_endMonth = Month.DECEMBER;
        paramRules.add(rule1);
        System.out.println("Mesure de Gestion : 13");
        // OTB
        Cantonnement rule2 = new Cantonnement();
        rule2.param_zone = FRA;
        rule2.param_gear = OTB;
        rule2.param_beginStep = new TimeStep(60);
        rule2.param_endStep = new TimeStep(120);
        rule2.param_beginMonth = Month.JANUARY;
        rule2.param_endMonth = Month.DECEMBER;
        paramRules.add(rule2);
        System.out.println("Mesure de Gestion : 13");
    }
    // Nouveau règlement FRA (fermè novembre à avril)
    // New FRA Regulation (November to April Closure)
    protected void setupScenario14(SimulationStorage nextSimulation, List<Rule> paramRules, TopiaContext db) {
        Zone NEWFRAREGULATIONS = IsisFishDAOHelper.getZoneDAO(db).findByName("Northward_Expansion_of_FRA"); // you need the call name of the object zone and not the file name
        Gear OTT = IsisFishDAOHelper.getGearDAO(db).findByName("OTT");
        Gear OTB = IsisFishDAOHelper.getGearDAO(db).findByName("OTB");
        // OTT
        Cantonnement rule1 = new Cantonnement();
        rule1.param_zone = NEWFRAREGULATIONS;
        rule1.param_gear = OTT;
        rule1.param_beginStep = new TimeStep(60);
        rule1.param_endStep = new TimeStep(120);
        rule1.param_beginMonth = Month.NOVEMBER;
        rule1.param_endMonth = Month.DECEMBER;
        paramRules.add(rule1);
        System.out.println("Mesure de Gestion : 14");
        // OTT
        Cantonnement rule2 = new Cantonnement();
        rule2.param_zone = NEWFRAREGULATIONS;
        rule2.param_gear = OTT;
        rule2.param_beginStep = new TimeStep(60);
        rule2.param_endStep = new TimeStep(120);
        rule2.param_beginMonth = Month.JANUARY;
        rule2.param_endMonth = Month.APRIL;
        paramRules.add(rule2);
        System.out.println("Mesure de Gestion : 14");
        // OTB
        Cantonnement rule3 = new Cantonnement();
        rule3.param_zone = NEWFRAREGULATIONS;
        rule3.param_gear = OTB;
        rule3.param_beginStep = new TimeStep(60);
        rule3.param_endStep = new TimeStep(120);
        rule3.param_beginMonth = Month.NOVEMBER;
        rule3.param_endMonth = Month.DECEMBER;
        paramRules.add(rule3);
        System.out.println("Mesure de Gestion : 14");
        // OTB
        Cantonnement rule4 = new Cantonnement();
        rule4.param_zone = NEWFRAREGULATIONS;
        rule4.param_gear = OTB;
        rule4.param_beginStep = new TimeStep(60);
        rule4.param_endStep = new TimeStep(120);
        rule4.param_beginMonth = Month.JANUARY;
        rule4.param_endMonth = Month.APRIL;
        paramRules.add(rule4);
        System.out.println("Mesure de Gestion : 14");
    }
    // Nouveau règlement FRA (fermé toute l'année)
  	// New FRA regulations (closed all year)
    protected void setupScenario15(SimulationStorage nextSimulation, List<Rule> paramRules, TopiaContext db) {
        Zone NEWFRAREGULATIONS = IsisFishDAOHelper.getZoneDAO(db).findByName("Northward_Expansion_of_FRA"); // you need the call name of the object zone and not the file name
        Gear OTT = IsisFishDAOHelper.getGearDAO(db).findByName("OTT");
        Gear OTB = IsisFishDAOHelper.getGearDAO(db).findByName("OTB");
        // OTT
        Cantonnement rule1 = new Cantonnement();
        rule1.param_zone = NEWFRAREGULATIONS;
        rule1.param_gear = OTT;
        rule1.param_beginStep = new TimeStep(60);
        rule1.param_endStep = new TimeStep(120);
        rule1.param_beginMonth = Month.JANUARY;
        rule1.param_endMonth = Month.DECEMBER;
        paramRules.add(rule1);
        System.out.println("Mesure de Gestion : 15");
        // OTB
        Cantonnement rule2 = new Cantonnement();
        rule2.param_zone = NEWFRAREGULATIONS;
        rule2.param_gear = OTB;
        rule2.param_beginStep = new TimeStep(60);
        rule2.param_endStep = new TimeStep(120);
        rule2.param_beginMonth = Month.JANUARY;
        rule2.param_endMonth = Month.DECEMBER;
        paramRules.add(rule2);
        System.out.println("Mesure de Gestion : 15");
    }
    // Nouveau règlement 90-100 m isobathe (fermé septembre à avril)
    // New 90-100 m isobath closure regulations (September to April closure)
    protected void setupScenario16(SimulationStorage nextSimulation, List<Rule> paramRules, TopiaContext db) {
        Zone QUATREVINGTDIXCENTISOBATHE = IsisFishDAOHelper.getZoneDAO(db).findByName("90_100m_isobath_closure"); //you need the call name of the object zone and not the file name
        Gear OTT = IsisFishDAOHelper.getGearDAO(db).findByName("OTT");
        Gear OTB = IsisFishDAOHelper.getGearDAO(db).findByName("OTB");
        // OTT
        Cantonnement rule1 = new Cantonnement();
        rule1.param_zone = QUATREVINGTDIXCENTISOBATHE;
        rule1.param_gear = OTT;
        rule1.param_beginStep = new TimeStep(60);
        rule1.param_endStep = new TimeStep(120);
        rule1.param_beginMonth = Month.SEPTEMBER;
        rule1.param_endMonth = Month.DECEMBER;
        paramRules.add(rule1);
        System.out.println("Mesure de Gestion : 16");
        // OTT
        Cantonnement rule2 = new Cantonnement();
        rule2.param_zone = QUATREVINGTDIXCENTISOBATHE;
        rule2.param_gear = OTT;
        rule2.param_beginStep = new TimeStep(60);
        rule2.param_endStep = new TimeStep(120);
        rule2.param_beginMonth = Month.JANUARY;
        rule2.param_endMonth = Month.APRIL;
        paramRules.add(rule2);
        System.out.println("Mesure de Gestion : 16");
        // OTB
        Cantonnement rule3 = new Cantonnement();
        rule3.param_zone = QUATREVINGTDIXCENTISOBATHE;
        rule3.param_gear = OTB;
        rule3.param_beginStep = new TimeStep(60);
        rule3.param_endStep = new TimeStep(120);
        rule3.param_beginMonth = Month.SEPTEMBER;
        rule3.param_endMonth = Month.DECEMBER;
        paramRules.add(rule3);
        System.out.println("Mesure de Gestion : 16");
        // OTB
        Cantonnement rule4 = new Cantonnement();
        rule4.param_zone = QUATREVINGTDIXCENTISOBATHE;
        rule4.param_gear = OTB;
        rule4.param_beginStep = new TimeStep(60);
        rule4.param_endStep = new TimeStep(120);
        rule4.param_beginMonth = Month.JANUARY;
        rule4.param_endMonth = Month.APRIL;
        paramRules.add(rule4);
        System.out.println("Mesure de Gestion : 16");
    }
    // Nouveau règlement 90-100 m isobathe (fermé toute l'année)
  	// New 90-100 m isobath closure regulations (closed all year)
    protected void setupScenario17(SimulationStorage nextSimulation, List<Rule> paramRules, TopiaContext db) {
        Zone QUATREVINGTDIXCENTISOBATHE = IsisFishDAOHelper.getZoneDAO(db).findByName("90_100m_isobath_closure"); // you need the call name of the object zone and not the file name
        Gear OTT = IsisFishDAOHelper.getGearDAO(db).findByName("OTT");
        Gear OTB = IsisFishDAOHelper.getGearDAO(db).findByName("OTB");
        // OTT
        Cantonnement rule1 = new Cantonnement();
        rule1.param_zone = QUATREVINGTDIXCENTISOBATHE;
        rule1.param_gear = OTT;
        rule1.param_beginStep = new TimeStep(60);
        rule1.param_endStep = new TimeStep(120);
        rule1.param_beginMonth = Month.JANUARY;
        rule1.param_endMonth = Month.DECEMBER;
        paramRules.add(rule1);
        System.out.println("Mesure de Gestion : 17");
        // OTB
        Cantonnement rule2 = new Cantonnement();
        rule2.param_zone = QUATREVINGTDIXCENTISOBATHE;
        rule2.param_gear = OTB;
        rule2.param_beginStep = new TimeStep(60);
        rule2.param_endStep = new TimeStep(120);
        rule2.param_beginMonth = Month.JANUARY;
        rule2.param_endMonth = Month.DECEMBER;
        paramRules.add(rule2);
        System.out.println("Mesure de Gestion : 17");
    }
    // Zones de fermeture en mer appliquées à partir du 28 avril 2018 par les pêcheries pour tous les engins de fond
    // Les fermetures offshore comprennent 3 zones fermées définitivement et une zone de fermeture saisonnière appliquée autour de leur connectivité du 15 octobre au 15 décembre.
    // Pour tenir compte de la résolution du modèle, la date de début de la fermeture saisonnière a été déplacée au 1er octobre et la date de fin au 30 novembre.
    // Offshore closure areas enforced as of April 28th, 2018 by the fisheries for all bottom gears.
    // The offshore closures comprise of 3 zones closed permanently and a seasonal closure zone applied around their connectivity from October 15th to December 15th.
    // To adjust for the model resolution, the starting date of the seasonal closure was moved to October 1st and ending date to November 30th.
    protected void setupScenario18(SimulationStorage nextSimulation, List<Rule> paramRules, TopiaContext db) {
        Zone OFFSHORESEASONALCLOSURE = IsisFishDAOHelper.getZoneDAO(db).findByName("Offshore_seasonal_expansion");
        Zone OFFSHOREPROHIBITEDBOTTOMGEARS = IsisFishDAOHelper.getZoneDAO(db).findByName("Offshore_fixed");
        Gear OTT = IsisFishDAOHelper.getGearDAO(db).findByName("OTT");
        Gear OTB = IsisFishDAOHelper.getGearDAO(db).findByName("OTB");
        Gear LLS = IsisFishDAOHelper.getGearDAO(db).findByName("LLS");
        Gear GNS = IsisFishDAOHelper.getGearDAO(db).findByName("GNS");
        // OTT
        Cantonnement rule1 = new Cantonnement();
        rule1.param_zone = OFFSHOREPROHIBITEDBOTTOMGEARS;
        rule1.param_gear = OTT;
        rule1.param_beginStep = new TimeStep(60);
        rule1.param_endStep = new TimeStep(120);
        rule1.param_beginMonth = Month.JANUARY;
        rule1.param_endMonth = Month.DECEMBER;
        paramRules.add(rule1);
        System.out.println("Mesure de Gestion : 18");
        // OTB
        Cantonnement rule2 = new Cantonnement();
        rule2.param_zone = OFFSHOREPROHIBITEDBOTTOMGEARS;
        rule2.param_gear = OTB;
        rule2.param_beginStep = new TimeStep(60);
        rule2.param_endStep = new TimeStep(120);
        rule2.param_beginMonth = Month.JANUARY;
        rule2.param_endMonth = Month.DECEMBER;
        paramRules.add(rule2);
        System.out.println("Mesure de Gestion : 18");
        // LLS
        Cantonnement rule3 = new Cantonnement();
        rule3.param_zone = OFFSHOREPROHIBITEDBOTTOMGEARS;
        rule3.param_gear = LLS;
        rule3.param_beginStep = new TimeStep(60);
        rule3.param_endStep = new TimeStep(120);
        rule3.param_beginMonth = Month.JANUARY;
        rule3.param_endMonth = Month.DECEMBER;
        paramRules.add(rule3);
        System.out.println("Mesure de Gestion : 18");
        // GNS
        Cantonnement rule4 = new Cantonnement();
        rule4.param_zone = OFFSHOREPROHIBITEDBOTTOMGEARS;
        rule4.param_gear = GNS;
        rule4.param_beginStep = new TimeStep(60);
        rule4.param_endStep = new TimeStep(120);
        rule4.param_beginMonth = Month.JANUARY;
        rule4.param_endMonth = Month.DECEMBER;
        paramRules.add(rule4);
        System.out.println("Mesure de Gestion : 18");
        // Offshore Seasonal Closure (matched to approximate equal area and pattern of the seasonal closure regulation)
        // OTT
        Cantonnement rule5 = new Cantonnement();
        rule5.param_zone = OFFSHORESEASONALCLOSURE;
        rule5.param_gear = OTT;
        rule5.param_beginStep = new TimeStep(60);
        rule5.param_endStep = new TimeStep(120);
        rule5.param_beginMonth = Month.OCTOBER; // actual is October 15th
        rule5.param_endMonth = Month.NOVEMBER; // actual is December 15th
        paramRules.add(rule5);
        System.out.println("Mesure de Gestion : 18");
        // OTB
        Cantonnement rule6 = new Cantonnement();
        rule6.param_zone = OFFSHORESEASONALCLOSURE;
        rule6.param_gear = OTB;
        rule6.param_beginStep = new TimeStep(60);
        rule6.param_endStep = new TimeStep(120);
        rule6.param_beginMonth = Month.OCTOBER; // actual is October 15th
        rule6.param_endMonth = Month.NOVEMBER; // actual is December 15th
        paramRules.add(rule6);
        System.out.println("Mesure de Gestion : 18");
        // LLS
        Cantonnement rule7 = new Cantonnement();
        rule7.param_zone = OFFSHORESEASONALCLOSURE;
        rule7.param_gear = LLS;
        rule7.param_beginStep = new TimeStep(60);
        rule7.param_endStep = new TimeStep(120);
        rule7.param_beginMonth = Month.OCTOBER; // actual is October 15th
        rule7.param_endMonth = Month.NOVEMBER; // actual is December 15th
        paramRules.add(rule7);
        System.out.println("Mesure de Gestion : 18");
        // GNS
        Cantonnement rule8 = new Cantonnement();
        rule8.param_zone = OFFSHORESEASONALCLOSURE;
        rule8.param_gear = GNS;
        rule8.param_beginStep = new TimeStep(60);
        rule8.param_endStep = new TimeStep(120);
        rule8.param_beginMonth = Month.OCTOBER; // actual is October 15th
        rule8.param_endMonth = Month.NOVEMBER; // actual is December 15th
        paramRules.add(rule8);
        System.out.println("Mesure de Gestion : 18");
    }
    // Les fermetures offshore comprennent 3 zones fermées définitivement et étendent la fermeture de la zone de connectivité à toute l'année.
    // The offshore closures comprise of 3 zones closed permanently and extends closure of the connectivity zone to all year.
    protected void setupScenario19(SimulationStorage nextSimulation, List<Rule> paramRules, TopiaContext db) {
        Zone OFFSHORESEASONALCLOSURE = IsisFishDAOHelper.getZoneDAO(db).findByName("Offshore_seasonal_expansion");
        Zone OFFSHOREPROHIBITEDBOTTOMGEARS = IsisFishDAOHelper.getZoneDAO(db).findByName("Offshore_fixed");
        Gear OTT = IsisFishDAOHelper.getGearDAO(db).findByName("OTT");
        Gear OTB = IsisFishDAOHelper.getGearDAO(db).findByName("OTB");
        Gear LLS = IsisFishDAOHelper.getGearDAO(db).findByName("LLS");
        Gear GNS = IsisFishDAOHelper.getGearDAO(db).findByName("GNS");
        // OTT
        Cantonnement rule1 = new Cantonnement();
        rule1.param_zone = OFFSHOREPROHIBITEDBOTTOMGEARS;
        rule1.param_gear = OTT;
        rule1.param_beginStep = new TimeStep(60);
        rule1.param_endStep = new TimeStep(120);
        rule1.param_beginMonth = Month.JANUARY;
        rule1.param_endMonth = Month.DECEMBER;
        paramRules.add(rule1);
        System.out.println("Mesure de Gestion : 19");
        // OTB
        Cantonnement rule2 = new Cantonnement();
        rule2.param_zone = OFFSHOREPROHIBITEDBOTTOMGEARS;
        rule2.param_gear = OTB;
        rule2.param_beginStep = new TimeStep(60);
        rule2.param_endStep = new TimeStep(120);
        rule2.param_beginMonth = Month.JANUARY;
        rule2.param_endMonth = Month.DECEMBER;
        paramRules.add(rule2);
        System.out.println("Mesure de Gestion : 19");
        // LLS
        Cantonnement rule3 = new Cantonnement();
        rule3.param_zone = OFFSHOREPROHIBITEDBOTTOMGEARS;
        rule3.param_gear = LLS;
        rule3.param_beginStep = new TimeStep(60);
        rule3.param_endStep = new TimeStep(120);
        rule3.param_beginMonth = Month.JANUARY;
        rule3.param_endMonth = Month.DECEMBER;
        paramRules.add(rule3);
        System.out.println("Mesure de Gestion : 19");
        // GNS
        Cantonnement rule4 = new Cantonnement();
        rule4.param_zone = OFFSHOREPROHIBITEDBOTTOMGEARS;
        rule4.param_gear = GNS;
        rule4.param_beginStep = new TimeStep(60);
        rule4.param_endStep = new TimeStep(120);
        rule4.param_beginMonth = Month.JANUARY;
        rule4.param_endMonth = Month.DECEMBER;
        paramRules.add(rule4);
        System.out.println("Mesure de Gestion : 19");
        // Offshore Seasonal Closure (matched to approximate equal area and pattern of the seasonal closure regulation)
        // OTT
        Cantonnement rule5 = new Cantonnement();
        rule5.param_zone = OFFSHORESEASONALCLOSURE;
        rule5.param_gear = OTT;
        rule5.param_beginStep = new TimeStep(60);
        rule5.param_endStep = new TimeStep(120);
        rule5.param_beginMonth = Month.JANUARY;
        rule5.param_endMonth = Month.DECEMBER;
        paramRules.add(rule5);
        System.out.println("Mesure de Gestion : 19");
        // OTB
        Cantonnement rule6 = new Cantonnement();
        rule6.param_zone = OFFSHORESEASONALCLOSURE;
        rule6.param_gear = OTB;
        rule6.param_beginStep = new TimeStep(60);
        rule6.param_endStep = new TimeStep(120);
        rule6.param_beginMonth = Month.JANUARY;
        rule6.param_endMonth = Month.DECEMBER;
        paramRules.add(rule6);
        System.out.println("Mesure de Gestion : 19");
        // LLS
        Cantonnement rule7 = new Cantonnement();
        rule7.param_zone = OFFSHORESEASONALCLOSURE;
        rule7.param_gear = LLS;
        rule7.param_beginStep = new TimeStep(60);
        rule7.param_endStep = new TimeStep(120);
        rule7.param_beginMonth = Month.JANUARY;
        rule7.param_endMonth = Month.DECEMBER;
        paramRules.add(rule7);
        System.out.println("Mesure de Gestion : 19");
        // GNS
        Cantonnement rule8 = new Cantonnement();
        rule8.param_zone = OFFSHORESEASONALCLOSURE;
        rule8.param_gear = GNS;
        rule8.param_beginStep = new TimeStep(60);
        rule8.param_endStep = new TimeStep(120);
        rule8.param_beginMonth = Month.JANUARY;
        rule8.param_endMonth = Month.DECEMBER;
        paramRules.add(rule8);
        System.out.println("Mesure de Gestion : 19");
    }

// ------------------------------------------------------
// Combined Spatial Closure  SCENARIOS
// ------------------------------------------------------

    // Nouveau règlement FRA combiné, règlement de fermeture pour 90-100 isobathes (fermé septembre à avril), et fermetures offshore
    // Combined New FRA regulation, closure regulation for 90-100 isobaths (September to April closure), and offshore closures
    protected void setupScenario20(SimulationStorage nextSimulation, List<Rule> paramRules, TopiaContext db) {
       Zone NEWFRAREGULATIONS = IsisFishDAOHelper.getZoneDAO(db).findByName("Northward_Expansion_of_FRA"); // you need the call name of the object zone and not the file name
       Zone QUATREVINGTDIXCENTISOBATHE = IsisFishDAOHelper.getZoneDAO(db).findByName("90_100m_isobath_closure"); // you need the call name of the object zone and not the file name
       Zone OFFSHORESEASONALCLOSURE = IsisFishDAOHelper.getZoneDAO(db).findByName("Offshore_seasonal_expansion");
       Zone OFFSHOREPROHIBITEDBOTTOMGEARS = IsisFishDAOHelper.getZoneDAO(db).findByName("Offshore_fixed");
       Gear OTT = IsisFishDAOHelper.getGearDAO(db).findByName("OTT");
       Gear OTB = IsisFishDAOHelper.getGearDAO(db).findByName("OTB");
       Gear LLS = IsisFishDAOHelper.getGearDAO(db).findByName("LLS");
       Gear GNS = IsisFishDAOHelper.getGearDAO(db).findByName("GNS");
       // NEWFRAREGULATIONS (fermé novembre à avril)
       // NEWFRAREGULATIONS (November to April closure)
       // OTT
       Cantonnement rule1 = new Cantonnement();
       rule1.param_zone = NEWFRAREGULATIONS;
       rule1.param_gear = OTT;
       rule1.param_beginStep = new TimeStep(60);
       rule1.param_endStep = new TimeStep(120);
       rule1.param_beginMonth = Month.NOVEMBER;
       rule1.param_endMonth = Month.DECEMBER;
       paramRules.add(rule1);
       System.out.println("Mesure de Gestion : 20");
       // OTT
       Cantonnement rule2 = new Cantonnement();
       rule2.param_zone = NEWFRAREGULATIONS;
       rule2.param_gear = OTT;
       rule2.param_beginStep = new TimeStep(60);
       rule2.param_endStep = new TimeStep(120);
       rule2.param_beginMonth = Month.JANUARY;
       rule2.param_endMonth = Month.APRIL;
       paramRules.add(rule2);
       System.out.println("Mesure de Gestion : 20");
       // OTB
       Cantonnement rule3 = new Cantonnement();
       rule3.param_zone = NEWFRAREGULATIONS;
       rule3.param_gear = OTB;
       rule3.param_beginStep = new TimeStep(60);
       rule3.param_endStep = new TimeStep(120);
       rule3.param_beginMonth = Month.NOVEMBER;
       rule3.param_endMonth = Month.DECEMBER;
       paramRules.add(rule3);
       System.out.println("Mesure de Gestion : 20");
       // OTB
       Cantonnement rule4 = new Cantonnement();
       rule4.param_zone = NEWFRAREGULATIONS;
       rule4.param_gear = OTB;
       rule4.param_beginStep = new TimeStep(60);
       rule4.param_endStep = new TimeStep(120);
       rule4.param_beginMonth = Month.JANUARY;
       rule4.param_endMonth = Month.APRIL;
       paramRules.add(rule4);
       System.out.println("Mesure de Gestion : 20");
       // Nouveau règlement 90-100 m isobathe (fermé septembre à avril)
       // New 90-100 m isobath closure regulations (September to April closure)
       // OTT
       Cantonnement rule5 = new Cantonnement();
       rule5.param_zone = QUATREVINGTDIXCENTISOBATHE;
       rule5.param_gear = OTT;
       rule5.param_beginStep = new TimeStep(60);
       rule5.param_endStep = new TimeStep(120);
       rule5.param_beginMonth = Month.SEPTEMBER;
       rule5.param_endMonth = Month.DECEMBER;
       paramRules.add(rule5);
       System.out.println("Mesure de Gestion : 20");
       // OTT
       Cantonnement rule6 = new Cantonnement();
       rule6.param_zone = QUATREVINGTDIXCENTISOBATHE;
       rule6.param_gear = OTT;
       rule6.param_beginStep = new TimeStep(60);
       rule6.param_endStep = new TimeStep(120);
       rule6.param_beginMonth = Month.JANUARY;
       rule6.param_endMonth = Month.APRIL;
       paramRules.add(rule6);
       System.out.println("Mesure de Gestion : 20");
       // OTB
       Cantonnement rule7 = new Cantonnement();
       rule7.param_zone = QUATREVINGTDIXCENTISOBATHE;
       rule7.param_gear = OTB;
       rule7.param_beginStep = new TimeStep(60);
       rule7.param_endStep = new TimeStep(120);
       rule7.param_beginMonth = Month.SEPTEMBER;
       rule7.param_endMonth = Month.DECEMBER;
       paramRules.add(rule7);
       System.out.println("Mesure de Gestion : 20");
       // OTB
       Cantonnement rule8 = new Cantonnement();
       rule8.param_zone = QUATREVINGTDIXCENTISOBATHE;
       rule8.param_gear = OTB;
       rule8.param_beginStep = new TimeStep(60);
       rule8.param_endStep = new TimeStep(120);
       rule8.param_beginMonth = Month.JANUARY;
       rule8.param_endMonth = Month.APRIL;
       paramRules.add(rule8);
       System.out.println("Mesure de Gestion : 20");
       // Zones de fermeture en mer appliquées à partir du 28 avril 2018 par les pêcheries pour tous les engins de fond
       // The offshore closures comprise of 3 zones closed permanently and a seasonal closure zone applied around their connectivity from October 15th to December 15th.
       // OTT
       Cantonnement rule9 = new Cantonnement();
       rule9.param_zone = OFFSHOREPROHIBITEDBOTTOMGEARS;
       rule9.param_gear = OTT;
       rule9.param_beginStep = new TimeStep(60);
       rule9.param_endStep = new TimeStep(120);
       rule9.param_beginMonth = Month.JANUARY;
       rule9.param_endMonth = Month.DECEMBER;
       paramRules.add(rule9);
       System.out.println("Mesure de Gestion : 20");
       // OTB
       Cantonnement rule10 = new Cantonnement();
       rule10.param_zone = OFFSHOREPROHIBITEDBOTTOMGEARS;
       rule10.param_gear = OTB;
       rule10.param_beginStep = new TimeStep(60);
       rule10.param_endStep = new TimeStep(120);
       rule10.param_beginMonth = Month.JANUARY;
       rule10.param_endMonth = Month.DECEMBER;
       paramRules.add(rule10);
       System.out.println("Mesure de Gestion : 20");
       // LLS
       Cantonnement rule11 = new Cantonnement();
       rule11.param_zone = OFFSHOREPROHIBITEDBOTTOMGEARS;
       rule11.param_gear = LLS;
       rule11.param_beginStep = new TimeStep(60);
       rule11.param_endStep = new TimeStep(120);
       rule11.param_beginMonth = Month.JANUARY;
       rule11.param_endMonth = Month.DECEMBER;
       paramRules.add(rule11);
       System.out.println("Mesure de Gestion : 20");
       // GNS
       Cantonnement rule12 = new Cantonnement();
       rule12.param_zone = OFFSHOREPROHIBITEDBOTTOMGEARS;
       rule12.param_gear = GNS;
       rule12.param_beginStep = new TimeStep(60);
       rule12.param_endStep = new TimeStep(120);
       rule12.param_beginMonth = Month.JANUARY;
       rule12.param_endMonth = Month.DECEMBER;
       paramRules.add(rule12);
       System.out.println("Mesure de Gestion : 20");
       // Offshore Seasonal Closure (matched to approximate equal area and pattern of the seasonal closure regulation)
       // OTT
       Cantonnement rule13 = new Cantonnement();
       rule13.param_zone = OFFSHORESEASONALCLOSURE;
       rule13.param_gear = OTT;
       rule13.param_beginStep = new TimeStep(60);
       rule13.param_endStep = new TimeStep(120);
       rule13.param_beginMonth = Month.OCTOBER; // actual is October 15th
       rule13.param_endMonth = Month.NOVEMBER; // actual is December 15th
       paramRules.add(rule13);
       System.out.println("Mesure de Gestion : 20");
       // OTB
       Cantonnement rule14 = new Cantonnement();
       rule14.param_zone = OFFSHORESEASONALCLOSURE;
       rule14.param_gear = OTB;
       rule14.param_beginStep = new TimeStep(60);
       rule14.param_endStep = new TimeStep(120);
       rule14.param_beginMonth = Month.OCTOBER; // actual is October 15th
       rule14.param_endMonth = Month.NOVEMBER; // actual is December 15th
       paramRules.add(rule14);
       System.out.println("Mesure de Gestion : 20");
       // LLS
       Cantonnement rule15 = new Cantonnement();
       rule15.param_zone = OFFSHORESEASONALCLOSURE;
       rule15.param_gear = LLS;
       rule15.param_beginStep = new TimeStep(60);
       rule15.param_endStep = new TimeStep(120);
       rule15.param_beginMonth = Month.OCTOBER; // actual is October 15th
       rule15.param_endMonth = Month.NOVEMBER; // actual is December 15th
       paramRules.add(rule15);
       System.out.println("Mesure de Gestion : 20");
       // GNS
       Cantonnement rule16 = new Cantonnement();
       rule16.param_zone = OFFSHORESEASONALCLOSURE;
       rule16.param_gear = GNS;
       rule16.param_beginStep = new TimeStep(60);
       rule16.param_endStep = new TimeStep(120);
       rule16.param_beginMonth = Month.OCTOBER; // actual is October 15th
       rule16.param_endMonth = Month.NOVEMBER; // actual is December 15th
       paramRules.add(rule16);
       System.out.println("Mesure de Gestion : 20");
    }
    // Nouveau règlement FRA combiné, règlement de fermeture pour 90-100 isobathes et fermetures offshore, (toute l'année)
  	// Combined New FRA regulation, closure regulation for 90-100 isobaths, and offshore closures, (all year)
    protected void setupScenario21(SimulationStorage nextSimulation, List<Rule> paramRules, TopiaContext db) {
       Zone NEWFRAREGULATIONS = IsisFishDAOHelper.getZoneDAO(db).findByName("Northward_Expansion_of_FRA"); //you need the call name of the object zone and not the file name
       Zone QUATREVINGTDIXCENTISOBATHE = IsisFishDAOHelper.getZoneDAO(db).findByName("90_100m_isobath_closure"); //you need the call name of the object zone and not the file name
       Zone OFFSHORESEASONALCLOSURE = IsisFishDAOHelper.getZoneDAO(db).findByName("Offshore_seasonal_expansion");
       Zone OFFSHOREPROHIBITEDBOTTOMGEARS = IsisFishDAOHelper.getZoneDAO(db).findByName("Offshore_fixed");
       Gear OTT = IsisFishDAOHelper.getGearDAO(db).findByName("OTT");
       Gear OTB = IsisFishDAOHelper.getGearDAO(db).findByName("OTB");
       Gear LLS = IsisFishDAOHelper.getGearDAO(db).findByName("LLS");
       Gear GNS = IsisFishDAOHelper.getGearDAO(db).findByName("GNS");
       // New FRA Regulations
       // OTT
       Cantonnement rule1 = new Cantonnement();
       rule1.param_zone = NEWFRAREGULATIONS;
       rule1.param_gear = OTT;
       rule1.param_beginStep = new TimeStep(60);
       rule1.param_endStep = new TimeStep(120);
       rule1.param_beginMonth = Month.JANUARY;
       rule1.param_endMonth = Month.DECEMBER;
       paramRules.add(rule1);
       System.out.println("Mesure de Gestion : 21");
       // OTB
       Cantonnement rule2 = new Cantonnement();
       rule2.param_zone = NEWFRAREGULATIONS;
       rule2.param_gear = OTB;
       rule2.param_beginStep = new TimeStep(60);
       rule2.param_endStep = new TimeStep(120);
       rule2.param_beginMonth = Month.JANUARY;
       rule2.param_endMonth = Month.DECEMBER;
       paramRules.add(rule2);
       System.out.println("Mesure de Gestion : 21");
       // Nouveau règlement 90-100 m isobathe
       // New 90-100 m isobath closure regulations
       // OTT
       Cantonnement rule3 = new Cantonnement();
       rule3.param_zone = QUATREVINGTDIXCENTISOBATHE;
       rule3.param_gear = OTT;
       rule3.param_beginStep = new TimeStep(60);
       rule3.param_endStep = new TimeStep(120);
       rule3.param_beginMonth = Month.JANUARY;
       rule3.param_endMonth = Month.DECEMBER;
       paramRules.add(rule3);
       System.out.println("Mesure de Gestion : 21");
       // OTB
       Cantonnement rule4 = new Cantonnement();
       rule4.param_zone = QUATREVINGTDIXCENTISOBATHE;
       rule4.param_gear = OTB;
       rule4.param_beginStep = new TimeStep(60);
       rule4.param_endStep = new TimeStep(120);
       rule4.param_beginMonth = Month.JANUARY;
       rule4.param_endMonth = Month.DECEMBER;
       paramRules.add(rule4);
       System.out.println("Mesure de Gestion : 21");
       // Zones de fermeture en mer
       // The offshore closures
       // OTT
       Cantonnement rule5 = new Cantonnement();
       rule5.param_zone = OFFSHOREPROHIBITEDBOTTOMGEARS;
       rule5.param_gear = OTT;
       rule5.param_beginStep = new TimeStep(60);
       rule5.param_endStep = new TimeStep(120);
       rule5.param_beginMonth = Month.JANUARY;
       rule5.param_endMonth = Month.DECEMBER;
       paramRules.add(rule5);
       System.out.println("Mesure de Gestion : 21");
       // OTB
       Cantonnement rule6 = new Cantonnement();
       rule6.param_zone = OFFSHOREPROHIBITEDBOTTOMGEARS;
       rule6.param_gear = OTB;
       rule6.param_beginStep = new TimeStep(60);
       rule6.param_endStep = new TimeStep(120);
       rule6.param_beginMonth = Month.JANUARY;
       rule6.param_endMonth = Month.DECEMBER;
       paramRules.add(rule6);
       System.out.println("Mesure de Gestion : 21");
       // LLS
       Cantonnement rule7 = new Cantonnement();
       rule7.param_zone = OFFSHOREPROHIBITEDBOTTOMGEARS;
       rule7.param_gear = LLS;
       rule7.param_beginStep = new TimeStep(60);
       rule7.param_endStep = new TimeStep(120);
       rule7.param_beginMonth = Month.JANUARY;
       rule7.param_endMonth = Month.DECEMBER;
       paramRules.add(rule7);
       System.out.println("Mesure de Gestion : 21");
       // GNS
       Cantonnement rule8 = new Cantonnement();
       rule8.param_zone = OFFSHOREPROHIBITEDBOTTOMGEARS;
       rule8.param_gear = GNS;
       rule8.param_beginStep = new TimeStep(60);
       rule8.param_endStep = new TimeStep(120);
       rule8.param_beginMonth = Month.JANUARY;
       rule8.param_endMonth = Month.DECEMBER;
       paramRules.add(rule8);
       System.out.println("Mesure de Gestion : 21");
       // Offshore Seasonal Closure (matched to approximate equal area and pattern) applied annually
       // OTT
       Cantonnement rule9 = new Cantonnement();
       rule9.param_zone = OFFSHORESEASONALCLOSURE;
       rule9.param_gear = OTT;
       rule9.param_beginStep = new TimeStep(60);
       rule9.param_endStep = new TimeStep(120);
       rule9.param_beginMonth = Month.JANUARY;
       rule9.param_endMonth = Month.DECEMBER;
       paramRules.add(rule9);
       System.out.println("Mesure de Gestion : 21");
       // OTB
       Cantonnement rule10 = new Cantonnement();
       rule10.param_zone = OFFSHORESEASONALCLOSURE;
       rule10.param_gear = OTB;
       rule10.param_beginStep = new TimeStep(60);
       rule10.param_endStep = new TimeStep(120);
       rule10.param_beginMonth = Month.JANUARY;
       rule10.param_endMonth = Month.DECEMBER;
       paramRules.add(rule10);
       System.out.println("Mesure de Gestion : 21");
       // LLS
       Cantonnement rule11 = new Cantonnement();
       rule11.param_zone = OFFSHORESEASONALCLOSURE;
       rule11.param_gear = LLS;
       rule11.param_beginStep = new TimeStep(60);
       rule11.param_endStep = new TimeStep(120);
       rule11.param_beginMonth = Month.JANUARY;
       rule11.param_endMonth = Month.DECEMBER;
       paramRules.add(rule11);
       System.out.println("Mesure de Gestion : 21");
       // GNS
       Cantonnement rule12 = new Cantonnement();
       rule12.param_zone = OFFSHORESEASONALCLOSURE;
       rule12.param_gear = GNS;
       rule12.param_beginStep = new TimeStep(60);
       rule12.param_endStep = new TimeStep(120);
       rule12.param_beginMonth = Month.JANUARY;
       rule12.param_endMonth = Month.DECEMBER;
       paramRules.add(rule12);
       System.out.println("Mesure de Gestion : 21");
    }

// --------------------------------------------------------
// Combined Effort Reduction and Spatial Closure SCENARIOS
// --------------------------------------------------------

    // Nouveau règlement FRA combiné, règlement de fermeture pour 90-100 isobathes (fermé septembre à avril), et fermetures offshore
    // Avec un réduction progressive de la taille de la flotte de chalutiers actuelle (réduction de 10% en 2020, 17.5% en 2021, 25% en 2022, 32.5% en 2023, et 40% en 2024 de l'effort nominal)
    // Combined New FRA regulation, closure regulation for 90-100 isobaths (September to April closure), and offshore closures
    // With a gradual reduction in the size of the current trawling fleet (reduction of 10% in 2020, 17.5% in 2021, 25% in 2022, 32.5% in 2023, and 40% in 2024 for nominal effort)
    protected void setupScenario22(SimulationStorage nextSimulation, List<Rule> paramRules, TopiaContext db) {
       Zone NEWFRAREGULATIONS = IsisFishDAOHelper.getZoneDAO(db).findByName("Northward_Expansion_of_FRA"); //you need the call name of the object zone and not the file name
       Zone QUATREVINGTDIXCENTISOBATHE = IsisFishDAOHelper.getZoneDAO(db).findByName("90_100m_isobath_closure"); //you need the call name of the object zone and not the file name
       Zone OFFSHORESEASONALCLOSURE = IsisFishDAOHelper.getZoneDAO(db).findByName("Offshore_seasonal_expansion");
       Zone OFFSHOREPROHIBITEDBOTTOMGEARS = IsisFishDAOHelper.getZoneDAO(db).findByName("Offshore_fixed");
       Gear OTT = IsisFishDAOHelper.getGearDAO(db).findByName("OTT");
       Gear OTB = IsisFishDAOHelper.getGearDAO(db).findByName("OTB");
       Gear LLS = IsisFishDAOHelper.getGearDAO(db).findByName("LLS");
       Gear GNS = IsisFishDAOHelper.getGearDAO(db).findByName("GNS");
       // NEWFRAREGULATIONS (fermé novembre à avril)
       // NEWFRAREGULATIONS (November to April closure)
       // OTT
       Cantonnement rule1 = new Cantonnement();
       rule1.param_zone = NEWFRAREGULATIONS;
       rule1.param_gear = OTT;
       rule1.param_beginStep = new TimeStep(60);
       rule1.param_endStep = new TimeStep(120);
       rule1.param_beginMonth = Month.NOVEMBER;
       rule1.param_endMonth = Month.DECEMBER;
       paramRules.add(rule1);
       System.out.println("Mesure de Gestion : 22");
       // OTT
       Cantonnement rule2 = new Cantonnement();
       rule2.param_zone = NEWFRAREGULATIONS;
       rule2.param_gear = OTT;
       rule2.param_beginStep = new TimeStep(60);
       rule2.param_endStep = new TimeStep(120);
       rule2.param_beginMonth = Month.JANUARY;
       rule2.param_endMonth = Month.APRIL;
       paramRules.add(rule2);
       System.out.println("Mesure de Gestion : 22");
       // OTB
       Cantonnement rule3 = new Cantonnement();
       rule3.param_zone = NEWFRAREGULATIONS;
       rule3.param_gear = OTB;
       rule3.param_beginStep = new TimeStep(60);
       rule3.param_endStep = new TimeStep(120);
       rule3.param_beginMonth = Month.NOVEMBER;
       rule3.param_endMonth = Month.DECEMBER;
       paramRules.add(rule3);
       System.out.println("Mesure de Gestion : 22");
       // OTB
       Cantonnement rule4 = new Cantonnement();
       rule4.param_zone = NEWFRAREGULATIONS;
       rule4.param_gear = OTB;
       rule4.param_beginStep = new TimeStep(60);
       rule4.param_endStep = new TimeStep(120);
       rule4.param_beginMonth = Month.JANUARY;
       rule4.param_endMonth = Month.APRIL;
       paramRules.add(rule4);
       System.out.println("Mesure de Gestion : 22");
       // Nouveau règlement 90-100 m isobathe (fermé septembre à avril)
       // New 90-100 m isobath closure regulations (September to April closure)
       // OTT
       Cantonnement rule5 = new Cantonnement();
       rule5.param_zone = QUATREVINGTDIXCENTISOBATHE;
       rule5.param_gear = OTT;
       rule5.param_beginStep = new TimeStep(60);
       rule5.param_endStep = new TimeStep(120);
       rule5.param_beginMonth = Month.SEPTEMBER;
       rule5.param_endMonth = Month.DECEMBER;
       paramRules.add(rule5);
       System.out.println("Mesure de Gestion : 22");
       // OTT
       Cantonnement rule6 = new Cantonnement();
       rule6.param_zone = QUATREVINGTDIXCENTISOBATHE;
       rule6.param_gear = OTT;
       rule6.param_beginStep = new TimeStep(60);
       rule6.param_endStep = new TimeStep(120);
       rule6.param_beginMonth = Month.JANUARY;
       rule6.param_endMonth = Month.APRIL;
       paramRules.add(rule6);
       System.out.println("Mesure de Gestion : 22");
       // OTB
       Cantonnement rule7 = new Cantonnement();
       rule7.param_zone = QUATREVINGTDIXCENTISOBATHE;
       rule7.param_gear = OTB;
       rule7.param_beginStep = new TimeStep(60);
       rule7.param_endStep = new TimeStep(120);
       rule7.param_beginMonth = Month.SEPTEMBER;
       rule7.param_endMonth = Month.DECEMBER;
       paramRules.add(rule7);
       System.out.println("Mesure de Gestion : 22");
       // OTB
       Cantonnement rule8 = new Cantonnement();
       rule8.param_zone = QUATREVINGTDIXCENTISOBATHE;
       rule8.param_gear = OTB;
       rule8.param_beginStep = new TimeStep(60);
       rule8.param_endStep = new TimeStep(120);
       rule8.param_beginMonth = Month.JANUARY;
       rule8.param_endMonth = Month.APRIL;
       paramRules.add(rule8);
       System.out.println("Mesure de Gestion : 22");
       // Zones de fermeture en mer appliquées à partir du 28 avril 2018 par les pêcheries pour tous les engins de fond
       // The offshore closures comprise of 3 zones closed permanently and a seasonal closure zone applied around their connectivity from October 15th to December 15th.
       // OTT
       Cantonnement rule9 = new Cantonnement();
       rule9.param_zone = OFFSHOREPROHIBITEDBOTTOMGEARS;
       rule9.param_gear = OTT;
       rule9.param_beginStep = new TimeStep(60);
       rule9.param_endStep = new TimeStep(120);
       rule9.param_beginMonth = Month.JANUARY;
       rule9.param_endMonth = Month.DECEMBER;
       paramRules.add(rule9);
       System.out.println("Mesure de Gestion : 22");
       // OTB
       Cantonnement rule10 = new Cantonnement();
       rule10.param_zone = OFFSHOREPROHIBITEDBOTTOMGEARS;
       rule10.param_gear = OTB;
       rule10.param_beginStep = new TimeStep(60);
       rule10.param_endStep = new TimeStep(120);
       rule10.param_beginMonth = Month.JANUARY;
       rule10.param_endMonth = Month.DECEMBER;
       paramRules.add(rule10);
       System.out.println("Mesure de Gestion : 22");
       // LLS
       Cantonnement rule11 = new Cantonnement();
       rule11.param_zone = OFFSHOREPROHIBITEDBOTTOMGEARS;
       rule11.param_gear = LLS;
       rule11.param_beginStep = new TimeStep(60);
       rule11.param_endStep = new TimeStep(120);
       rule11.param_beginMonth = Month.JANUARY;
       rule11.param_endMonth = Month.DECEMBER;
       paramRules.add(rule11);
       System.out.println("Mesure de Gestion : 22");
       // GNS
       Cantonnement rule12 = new Cantonnement();
       rule12.param_zone = OFFSHOREPROHIBITEDBOTTOMGEARS;
       rule12.param_gear = GNS;
       rule12.param_beginStep = new TimeStep(60);
       rule12.param_endStep = new TimeStep(120);
       rule12.param_beginMonth = Month.JANUARY;
       rule12.param_endMonth = Month.DECEMBER;
       paramRules.add(rule12);
       System.out.println("Mesure de Gestion : 22");
       // Offshore Seasonal Closure (matched to approximate equal area and pattern of the seasonal closure regulation)
       // OTT
       Cantonnement rule13 = new Cantonnement();
       rule13.param_zone = OFFSHORESEASONALCLOSURE;
       rule13.param_gear = OTT;
       rule13.param_beginStep = new TimeStep(60);
       rule13.param_endStep = new TimeStep(120);
       rule13.param_beginMonth = Month.OCTOBER; // actual is October 15th
       rule13.param_endMonth = Month.NOVEMBER; // actual is December 15th
       paramRules.add(rule13);
       System.out.println("Mesure de Gestion : 22");
       // OTB
       Cantonnement rule14 = new Cantonnement();
       rule14.param_zone = OFFSHORESEASONALCLOSURE;
       rule14.param_gear = OTB;
       rule14.param_beginStep = new TimeStep(60);
       rule14.param_endStep = new TimeStep(120);
       rule14.param_beginMonth = Month.OCTOBER; // actual is October 15th
       rule14.param_endMonth = Month.NOVEMBER; // actual is December 15th
       paramRules.add(rule14);
       System.out.println("Mesure de Gestion : 22");
       // LLS
       Cantonnement rule15 = new Cantonnement();
       rule15.param_zone = OFFSHORESEASONALCLOSURE;
       rule15.param_gear = LLS;
       rule15.param_beginStep = new TimeStep(60);
       rule15.param_endStep = new TimeStep(120);
       rule15.param_beginMonth = Month.OCTOBER; // actual is October 15th
       rule15.param_endMonth = Month.NOVEMBER; // actual is December 15th
       paramRules.add(rule15);
       System.out.println("Mesure de Gestion : 22");
       // GNS
       Cantonnement rule16 = new Cantonnement();
       rule16.param_zone = OFFSHORESEASONALCLOSURE;
       rule16.param_gear = GNS;
       rule16.param_beginStep = new TimeStep(60);
       rule16.param_endStep = new TimeStep(120);
       rule16.param_beginMonth = Month.OCTOBER; // actual is October 15th
       rule16.param_endMonth = Month.NOVEMBER; // actual is December 15th
       paramRules.add(rule16);
       System.out.println("Mesure de Gestion : 22");
       // EffortReduction_10_17point5_to40percent_sh
       EffortReduction_10_17point5_to40percent_t rule17 = new EffortReduction_10_17point5_to40percent_t();
       rule17.param_beginStep = new TimeStep(60);
       rule17.param_endStep = new TimeStep(120);
       paramRules.add(rule17);
       System.out.println("Mesure de Gestion : 22");
    }
   // Nouveau règlement FRA combiné, règlement de fermeture pour 90-100 isobathes et fermetures offshore
   // Avec un réduction progressive de la taille de la flotte appliquée à tous les engins (réduction de 10% en 2020, 17.5% en 2021, 25% en 2022, 32.5% en 2023, et 40% en 2024 de l'effort nominal)
   // Combined New FRA regulation, closure regulation for 90-1200 isobaths, and offshore closures
   // With a gradual reduction in the size of the fleet applied to all gears(reduction of 10% in 2020, 17.5% in 2021, 25% in 2022, 32.5% in 2023, and 40% in 2024 for nominal effort)
   protected void setupScenario23(SimulationStorage nextSimulation, List<Rule> paramRules, TopiaContext db) {
       Zone NEWFRAREGULATIONS = IsisFishDAOHelper.getZoneDAO(db).findByName("Northward_Expansion_of_FRA"); // you need the call name of the object zone and not the file name
       Zone QUATREVINGTDIXCENTISOBATHE = IsisFishDAOHelper.getZoneDAO(db).findByName("90_100m_isobath_closure"); // you need the call name of the object zone and not the file name
       Zone OFFSHORESEASONALCLOSURE = IsisFishDAOHelper.getZoneDAO(db).findByName("Offshore_seasonal_expansion");
       Zone OFFSHOREPROHIBITEDBOTTOMGEARS = IsisFishDAOHelper.getZoneDAO(db).findByName("Offshore_fixed");
       Gear OTT = IsisFishDAOHelper.getGearDAO(db).findByName("OTT");
       Gear OTB = IsisFishDAOHelper.getGearDAO(db).findByName("OTB");
       Gear LLS = IsisFishDAOHelper.getGearDAO(db).findByName("LLS");
       Gear GNS = IsisFishDAOHelper.getGearDAO(db).findByName("GNS");
       // NEWFRAREGULATIONS (fermé novembre à avril)
       // NEWFRAREGULATIONS (November to April closure)
       // OTT
       Cantonnement rule1 = new Cantonnement();
       rule1.param_zone = NEWFRAREGULATIONS;
       rule1.param_gear = OTT;
       rule1.param_beginStep = new TimeStep(60);
       rule1.param_endStep = new TimeStep(120);
       rule1.param_beginMonth = Month.NOVEMBER;
       rule1.param_endMonth = Month.DECEMBER;
       paramRules.add(rule1);
       System.out.println("Mesure de Gestion : 23");
       // OTT
       Cantonnement rule2 = new Cantonnement();
       rule2.param_zone = NEWFRAREGULATIONS;
       rule2.param_gear = OTT;
       rule2.param_beginStep = new TimeStep(60);
       rule2.param_endStep = new TimeStep(120);
       rule2.param_beginMonth = Month.JANUARY;
       rule2.param_endMonth = Month.APRIL;
       paramRules.add(rule2);
       System.out.println("Mesure de Gestion : 23");
       // OTB
       Cantonnement rule3 = new Cantonnement();
       rule3.param_zone = NEWFRAREGULATIONS;
       rule3.param_gear = OTB;
       rule3.param_beginStep = new TimeStep(60);
       rule3.param_endStep = new TimeStep(120);
       rule3.param_beginMonth = Month.NOVEMBER;
       rule3.param_endMonth = Month.DECEMBER;
       paramRules.add(rule3);
       System.out.println("Mesure de Gestion : 23");
       // OTB
       Cantonnement rule4 = new Cantonnement();
       rule4.param_zone = NEWFRAREGULATIONS;
       rule4.param_gear = OTB;
       rule4.param_beginStep = new TimeStep(60);
       rule4.param_endStep = new TimeStep(120);
       rule4.param_beginMonth = Month.JANUARY;
       rule4.param_endMonth = Month.APRIL;
       paramRules.add(rule4);
       System.out.println("Mesure de Gestion : 23");
       // Nouveau règlement 90-100 m isobathe (fermé septembre à avril)
       // New 90-100 m isobath closure regulations (September to April closure)
       // OTT
       Cantonnement rule5 = new Cantonnement();
       rule5.param_zone = QUATREVINGTDIXCENTISOBATHE;
       rule5.param_gear = OTT;
       rule5.param_beginStep = new TimeStep(60);
       rule5.param_endStep = new TimeStep(120);
       rule5.param_beginMonth = Month.SEPTEMBER;
       rule5.param_endMonth = Month.DECEMBER;
       paramRules.add(rule5);
       System.out.println("Mesure de Gestion : 23");
       // OTT
       Cantonnement rule6 = new Cantonnement();
       rule6.param_zone = QUATREVINGTDIXCENTISOBATHE;
       rule6.param_gear = OTT;
       rule6.param_beginStep = new TimeStep(60);
       rule6.param_endStep = new TimeStep(120);
       rule6.param_beginMonth = Month.JANUARY;
       rule6.param_endMonth = Month.APRIL;
       paramRules.add(rule6);
       System.out.println("Mesure de Gestion : 23");
       // OTB
       Cantonnement rule7 = new Cantonnement();
       rule7.param_zone = QUATREVINGTDIXCENTISOBATHE;
       rule7.param_gear = OTB;
       rule7.param_beginStep = new TimeStep(60);
       rule7.param_endStep = new TimeStep(120);
       rule7.param_beginMonth = Month.SEPTEMBER;
       rule7.param_endMonth = Month.DECEMBER;
       paramRules.add(rule7);
       System.out.println("Mesure de Gestion : 23");
       // OTB
       Cantonnement rule8 = new Cantonnement();
       rule8.param_zone = QUATREVINGTDIXCENTISOBATHE;
       rule8.param_gear = OTB;
       rule8.param_beginStep = new TimeStep(60);
       rule8.param_endStep = new TimeStep(120);
       rule8.param_beginMonth = Month.JANUARY;
       rule8.param_endMonth = Month.APRIL;
       paramRules.add(rule8);
       System.out.println("Mesure de Gestion : 23");
       // Zones de fermeture en mer appliquées à partir du 28 avril 2018 par les pêcheries pour tous les engins de fond
       // The offshore closures comprise of 3 zones closed permanently and a seasonal closure zone applied around their connectivity from October 15th to December 15th.
       // OTT
       Cantonnement rule9 = new Cantonnement();
       rule9.param_zone = OFFSHOREPROHIBITEDBOTTOMGEARS;
       rule9.param_gear = OTT;
       rule9.param_beginStep = new TimeStep(60);
       rule9.param_endStep = new TimeStep(120);
       rule9.param_beginMonth = Month.JANUARY;
       rule9.param_endMonth = Month.DECEMBER;
       paramRules.add(rule9);
       System.out.println("Mesure de Gestion : 23");
       // OTB
       Cantonnement rule10 = new Cantonnement();
       rule10.param_zone = OFFSHOREPROHIBITEDBOTTOMGEARS;
       rule10.param_gear = OTB;
       rule10.param_beginStep = new TimeStep(60);
       rule10.param_endStep = new TimeStep(120);
       rule10.param_beginMonth = Month.JANUARY;
       rule10.param_endMonth = Month.DECEMBER;
       paramRules.add(rule10);
       System.out.println("Mesure de Gestion : 23");
       // LLS
       Cantonnement rule11 = new Cantonnement();
       rule11.param_zone = OFFSHOREPROHIBITEDBOTTOMGEARS;
       rule11.param_gear = LLS;
       rule11.param_beginStep = new TimeStep(60);
       rule11.param_endStep = new TimeStep(120);
       rule11.param_beginMonth = Month.JANUARY;
       rule11.param_endMonth = Month.DECEMBER;
       paramRules.add(rule11);
       System.out.println("Mesure de Gestion : 23");
       // GNS
       Cantonnement rule12 = new Cantonnement();
       rule12.param_zone = OFFSHOREPROHIBITEDBOTTOMGEARS;
       rule12.param_gear = GNS;
       rule12.param_beginStep = new TimeStep(60);
       rule12.param_endStep = new TimeStep(120);
       rule12.param_beginMonth = Month.JANUARY;
       rule12.param_endMonth = Month.DECEMBER;
       paramRules.add(rule12);
       System.out.println("Mesure de Gestion : 23");
       // Offshore Seasonal Closure (matched to approximate equal area and pattern of the seasonal closure regulation)
       // OTT
       Cantonnement rule13 = new Cantonnement();
       rule13.param_zone = OFFSHORESEASONALCLOSURE;
       rule13.param_gear = OTT;
       rule13.param_beginStep = new TimeStep(60);
       rule13.param_endStep = new TimeStep(120);
       rule13.param_beginMonth = Month.OCTOBER; // actual is October 15th
       rule13.param_endMonth = Month.NOVEMBER; // actual is December 15th
       paramRules.add(rule13);
       System.out.println("Mesure de Gestion : 23");
       // OTB
       Cantonnement rule14 = new Cantonnement();
       rule14.param_zone = OFFSHORESEASONALCLOSURE;
       rule14.param_gear = OTB;
       rule14.param_beginStep = new TimeStep(60);
       rule14.param_endStep = new TimeStep(120);
       rule14.param_beginMonth = Month.OCTOBER; // actual is October 15th
       rule14.param_endMonth = Month.NOVEMBER; // actual is December 15th
       paramRules.add(rule14);
       System.out.println("Mesure de Gestion : 23");
       // LLS
       Cantonnement rule15 = new Cantonnement();
       rule15.param_zone = OFFSHORESEASONALCLOSURE;
       rule15.param_gear = LLS;
       rule15.param_beginStep = new TimeStep(60);
       rule15.param_endStep = new TimeStep(120);
       rule15.param_beginMonth = Month.OCTOBER; // actual is October 15th
       rule15.param_endMonth = Month.NOVEMBER; // actual is December 15th
       paramRules.add(rule15);
       System.out.println("Mesure de Gestion : 23");
       // GNS
       Cantonnement rule16 = new Cantonnement();
       rule16.param_zone = OFFSHORESEASONALCLOSURE;
       rule16.param_gear = GNS;
       rule16.param_beginStep = new TimeStep(60);
       rule16.param_endStep = new TimeStep(120);
       rule16.param_beginMonth = Month.OCTOBER; // actual is October 15th
       rule16.param_endMonth = Month.NOVEMBER; // actual is December 15th
       paramRules.add(rule16);
       System.out.println("Mesure de Gestion : 23");
       // EffortReduction_10_17point5_to40percent
       EffortReduction_10_17point5_to40percent_AllGears_t rule17 = new EffortReduction_10_17point5_to40percent_AllGears_t();
       rule17.param_beginStep = new TimeStep(60);
       rule17.param_endStep = new TimeStep(120);
       paramRules.add(rule17);
       System.out.println("Mesure de Gestion : 23");
    }

// ----------------------------------------------------------
// Post-hoc Simulation Senarios Due to progressive Deleterious Combined Effects
// ----------------------------------------------------------

    // Nouveau règlement FRA combiné et fermetures offshore
    // Combined New FRA regulation and offshore closures
    protected void setupScenario24(SimulationStorage nextSimulation, List<Rule> paramRules, TopiaContext db) {
       Zone NEWFRAREGULATIONS = IsisFishDAOHelper.getZoneDAO(db).findByName("Northward_Expansion_of_FRA"); // you need the call name of the object zone and not the file name
       Zone OFFSHORESEASONALCLOSURE = IsisFishDAOHelper.getZoneDAO(db).findByName("Offshore_seasonal_expansion");
       Zone OFFSHOREPROHIBITEDBOTTOMGEARS = IsisFishDAOHelper.getZoneDAO(db).findByName("Offshore_fixed");
       Gear OTT = IsisFishDAOHelper.getGearDAO(db).findByName("OTT");
       Gear OTB = IsisFishDAOHelper.getGearDAO(db).findByName("OTB");
       Gear LLS = IsisFishDAOHelper.getGearDAO(db).findByName("LLS");
       Gear GNS = IsisFishDAOHelper.getGearDAO(db).findByName("GNS");
       // NEWFRAREGULATIONS (fermé novembre à avril)
       // NEWFRAREGULATIONS (November to April closure)
       // OTT
       Cantonnement rule1 = new Cantonnement();
       rule1.param_zone = NEWFRAREGULATIONS;
       rule1.param_gear = OTT;
       rule1.param_beginStep = new TimeStep(60);
       rule1.param_endStep = new TimeStep(120);
       rule1.param_beginMonth = Month.NOVEMBER;
       rule1.param_endMonth = Month.DECEMBER;
       paramRules.add(rule1);
       System.out.println("Mesure de Gestion : 24");
       // OTT
       Cantonnement rule2 = new Cantonnement();
       rule2.param_zone = NEWFRAREGULATIONS;
       rule2.param_gear = OTT;
       rule2.param_beginStep = new TimeStep(60);
       rule2.param_endStep = new TimeStep(120);
       rule2.param_beginMonth = Month.JANUARY;
       rule2.param_endMonth = Month.APRIL;
       paramRules.add(rule2);
       System.out.println("Mesure de Gestion : 24");
       // OTB
       Cantonnement rule3 = new Cantonnement();
       rule3.param_zone = NEWFRAREGULATIONS;
       rule3.param_gear = OTB;
       rule3.param_beginStep = new TimeStep(60);
       rule3.param_endStep = new TimeStep(120);
       rule3.param_beginMonth = Month.NOVEMBER;
       rule3.param_endMonth = Month.DECEMBER;
       paramRules.add(rule3);
       System.out.println("Mesure de Gestion : 24");
       // OTB
       Cantonnement rule4 = new Cantonnement();
       rule4.param_zone = NEWFRAREGULATIONS;
       rule4.param_gear = OTB;
       rule4.param_beginStep = new TimeStep(60);
       rule4.param_endStep = new TimeStep(120);
       rule4.param_beginMonth = Month.JANUARY;
       rule4.param_endMonth = Month.APRIL;
       paramRules.add(rule4);
       System.out.println("Mesure de Gestion : 24");
       // Zones de fermeture en mer appliquées à partir du 28 avril 2018 par les pêcheries pour tous les engins de fond
       // The offshore closures comprise of 3 zones closed permanently and a seasonal closure zone applied around their connectivity from October 15th to December 15th.
       // OTT
       Cantonnement rule5 = new Cantonnement();
       rule5.param_zone = OFFSHOREPROHIBITEDBOTTOMGEARS;
       rule5.param_gear = OTT;
       rule5.param_beginStep = new TimeStep(60);
       rule5.param_endStep = new TimeStep(120);
       rule5.param_beginMonth = Month.JANUARY;
       rule5.param_endMonth = Month.DECEMBER;
       paramRules.add(rule5);
       System.out.println("Mesure de Gestion : 24");
       // OTB
       Cantonnement rule6 = new Cantonnement();
       rule6.param_zone = OFFSHOREPROHIBITEDBOTTOMGEARS;
       rule6.param_gear = OTB;
       rule6.param_beginStep = new TimeStep(60);
       rule6.param_endStep = new TimeStep(120);
       rule6.param_beginMonth = Month.JANUARY;
       rule6.param_endMonth = Month.DECEMBER;
       paramRules.add(rule6);
       System.out.println("Mesure de Gestion : 24");
       // LLS
       Cantonnement rule7 = new Cantonnement();
       rule7.param_zone = OFFSHOREPROHIBITEDBOTTOMGEARS;
       rule7.param_gear = LLS;
       rule7.param_beginStep = new TimeStep(60);
       rule7.param_endStep = new TimeStep(120);
       rule7.param_beginMonth = Month.JANUARY;
       rule7.param_endMonth = Month.DECEMBER;
       paramRules.add(rule7);
       System.out.println("Mesure de Gestion : 24");
       // GNS
       Cantonnement rule8 = new Cantonnement();
       rule8.param_zone = OFFSHOREPROHIBITEDBOTTOMGEARS;
       rule8.param_gear = GNS;
       rule8.param_beginStep = new TimeStep(60);
       rule8.param_endStep = new TimeStep(120);
       rule8.param_beginMonth = Month.JANUARY;
       rule8.param_endMonth = Month.DECEMBER;
       paramRules.add(rule8);
       System.out.println("Mesure de Gestion : 24");
       // Offshore Seasonal Closure (matched to approximate equal area and pattern of the seasonal closure regulation)
       // OTT
       Cantonnement rule9 = new Cantonnement();
       rule9.param_zone = OFFSHORESEASONALCLOSURE;
       rule9.param_gear = OTT;
       rule9.param_beginStep = new TimeStep(60);
       rule9.param_endStep = new TimeStep(120);
       rule9.param_beginMonth = Month.OCTOBER; // actual is October 15th
       rule9.param_endMonth = Month.NOVEMBER; // actual is December 15th
       paramRules.add(rule9);
       System.out.println("Mesure de Gestion : 24");
       // OTB
       Cantonnement rule10 = new Cantonnement();
       rule10.param_zone = OFFSHORESEASONALCLOSURE;
       rule10.param_gear = OTB;
       rule10.param_beginStep = new TimeStep(60);
       rule10.param_endStep = new TimeStep(120);
       rule10.param_beginMonth = Month.OCTOBER; // actual is October 15th
       rule10.param_endMonth = Month.NOVEMBER; // actual is December 15th
       paramRules.add(rule10);
       System.out.println("Mesure de Gestion : 24");
       // LLS
       Cantonnement rule11 = new Cantonnement();
       rule11.param_zone = OFFSHORESEASONALCLOSURE;
       rule11.param_gear = LLS;
       rule11.param_beginStep = new TimeStep(60);
       rule11.param_endStep = new TimeStep(120);
       rule11.param_beginMonth = Month.OCTOBER; // actual is October 15th
       rule11.param_endMonth = Month.NOVEMBER; // actual is December 15th
       paramRules.add(rule11);
       System.out.println("Mesure de Gestion : 24");
       // GNS
       Cantonnement rule12 = new Cantonnement();
       rule12.param_zone = OFFSHORESEASONALCLOSURE;
       rule12.param_gear = GNS;
       rule12.param_beginStep = new TimeStep(60);
       rule12.param_endStep = new TimeStep(120);
       rule12.param_beginMonth = Month.OCTOBER; // actual is October 15th
       rule12.param_endMonth = Month.NOVEMBER; // actual is December 15th
       paramRules.add(rule12);
       System.out.println("Mesure de Gestion : 24");
    }
    // Nouveau règlement FRA combiné et fermetures offshore (toute l'année)
    // Combined New FRA regulation and offshore closures (all year)
    protected void setupScenario25(SimulationStorage nextSimulation, List<Rule> paramRules, TopiaContext db) {
       Zone NEWFRAREGULATIONS = IsisFishDAOHelper.getZoneDAO(db).findByName("Northward_Expansion_of_FRA"); // you need the call name of the object zone and not the file name
       Zone OFFSHORESEASONALCLOSURE = IsisFishDAOHelper.getZoneDAO(db).findByName("Offshore_seasonal_expansion");
       Zone OFFSHOREPROHIBITEDBOTTOMGEARS = IsisFishDAOHelper.getZoneDAO(db).findByName("Offshore_fixed");
       Gear OTT = IsisFishDAOHelper.getGearDAO(db).findByName("OTT");
       Gear OTB = IsisFishDAOHelper.getGearDAO(db).findByName("OTB");
       Gear LLS = IsisFishDAOHelper.getGearDAO(db).findByName("LLS");
       Gear GNS = IsisFishDAOHelper.getGearDAO(db).findByName("GNS");
       // NEWFRAREGULATIONS
       // NEWFRAREGULATIONS
       // OTT
       Cantonnement rule1 = new Cantonnement();
       rule1.param_zone = NEWFRAREGULATIONS;
       rule1.param_gear = OTT;
       rule1.param_beginStep = new TimeStep(60);
       rule1.param_endStep = new TimeStep(120);
       rule1.param_beginMonth = Month.JANUARY;
       rule1.param_endMonth = Month.DECEMBER;
       paramRules.add(rule1);
       System.out.println("Mesure de Gestion : 25");
       // OTB
       Cantonnement rule2 = new Cantonnement();
       rule2.param_zone = NEWFRAREGULATIONS;
       rule2.param_gear = OTB;
       rule2.param_beginStep = new TimeStep(60);
       rule2.param_endStep = new TimeStep(120);
       rule2.param_beginMonth = Month.JANUARY;
       rule2.param_endMonth = Month.DECEMBER;
       paramRules.add(rule2);
       System.out.println("Mesure de Gestion : 25");
       //Zones de fermeture en mer appliquées à partir du 28 avril 2018 par les pêcheries pour tous les engins de fond
       //The offshore closures comprise of 3 zones closed permanently and a seasonal closure zone applied around their connectivity
       // OTT
       Cantonnement rule3 = new Cantonnement();
       rule3.param_zone = OFFSHOREPROHIBITEDBOTTOMGEARS;
       rule3.param_gear = OTT;
       rule3.param_beginStep = new TimeStep(60);
       rule3.param_endStep = new TimeStep(120);
       rule3.param_beginMonth = Month.JANUARY;
       rule3.param_endMonth = Month.DECEMBER;
       paramRules.add(rule3);
       System.out.println("Mesure de Gestion : 25");
       // OTB
       Cantonnement rule4 = new Cantonnement();
       rule4.param_zone = OFFSHOREPROHIBITEDBOTTOMGEARS;
       rule4.param_gear = OTB;
       rule4.param_beginStep = new TimeStep(60);
       rule4.param_endStep = new TimeStep(120);
       rule4.param_beginMonth = Month.JANUARY;
       rule4.param_endMonth = Month.DECEMBER;
       paramRules.add(rule4);
       System.out.println("Mesure de Gestion : 25");
       // LLS
       Cantonnement rule5 = new Cantonnement();
       rule5.param_zone = OFFSHOREPROHIBITEDBOTTOMGEARS;
       rule5.param_gear = LLS;
       rule5.param_beginStep = new TimeStep(60);
       rule5.param_endStep = new TimeStep(120);
       rule5.param_beginMonth = Month.JANUARY;
       rule5.param_endMonth = Month.DECEMBER;
       paramRules.add(rule5);
       System.out.println("Mesure de Gestion : 25");
       // GNS
       Cantonnement rule6 = new Cantonnement();
       rule6.param_zone = OFFSHOREPROHIBITEDBOTTOMGEARS;
       rule6.param_gear = GNS;
       rule6.param_beginStep = new TimeStep(60);
       rule6.param_endStep = new TimeStep(120);
       rule6.param_beginMonth = Month.JANUARY;
       rule6.param_endMonth = Month.DECEMBER;
       paramRules.add(rule6);
       System.out.println("Mesure de Gestion : 25");
       //Offshore Seasonal Closure (matched to approximate equal area and pattern of the seasonal closure regulation)
       // OTT
       Cantonnement rule7 = new Cantonnement();
       rule7.param_zone = OFFSHORESEASONALCLOSURE;
       rule7.param_gear = OTT;
       rule7.param_beginStep = new TimeStep(60);
       rule7.param_endStep = new TimeStep(120);
       rule7.param_beginMonth = Month.JANUARY;
       rule7.param_endMonth = Month.DECEMBER;
       paramRules.add(rule7);
       System.out.println("Mesure de Gestion : 25");
       // OTB
       Cantonnement rule8 = new Cantonnement();
       rule8.param_zone = OFFSHORESEASONALCLOSURE;
       rule8.param_gear = OTB;
       rule8.param_beginStep = new TimeStep(60);
       rule8.param_endStep = new TimeStep(120);
       rule8.param_beginMonth = Month.JANUARY;
       rule8.param_endMonth = Month.DECEMBER;
       paramRules.add(rule8);
       System.out.println("Mesure de Gestion : 25");
       // LLS
       Cantonnement rule9 = new Cantonnement();
       rule9.param_zone = OFFSHORESEASONALCLOSURE;
       rule9.param_gear = LLS;
       rule9.param_beginStep = new TimeStep(60);
       rule9.param_endStep = new TimeStep(120);
       rule9.param_beginMonth = Month.JANUARY;
       rule9.param_endMonth = Month.DECEMBER;
       paramRules.add(rule9);
       System.out.println("Mesure de Gestion : 25");
       // GNS
       Cantonnement rule10 = new Cantonnement();
       rule10.param_zone = OFFSHORESEASONALCLOSURE;
       rule10.param_gear = GNS;
       rule10.param_beginStep = new TimeStep(60);
       rule10.param_endStep = new TimeStep(120);
       rule10.param_beginMonth = Month.JANUARY;
       rule10.param_endMonth = Month.DECEMBER;
       paramRules.add(rule10);
       System.out.println("Mesure de Gestion : 25");
    }
    // Nouveau règlement FRA combiné, règlement de fermeture pour 90-100 isobathes (fermé septembre à avril), et fermetures offshore
    // Avec un réduction progressive de la taille de la flotte de chalutiers actuelle (réduction de 10% en 2020, 17.5% en 2021, 25% en 2022, 32.5% en 2023, et 40% en 2024 de l'effort nominal)
    // Combined New FRA regulation, closure regulation for 90-100 isobaths (September to April closure), and offshore closures
    // With a gradual reduction in the size of the current trawling fleet (reduction of 10% in 2020, 17.5% in 2021, 25% in 2022, 32.5% in 2023, and 40% in 2024 for nominal effort)
    protected void setupScenario26(SimulationStorage nextSimulation, List<Rule> paramRules, TopiaContext db) {
       Zone NEWFRAREGULATIONS = IsisFishDAOHelper.getZoneDAO(db).findByName("Northward_Expansion_of_FRA"); //you need the call name of the object zone and not the file name
       Zone OFFSHORESEASONALCLOSURE = IsisFishDAOHelper.getZoneDAO(db).findByName("Offshore_seasonal_expansion");
       Zone OFFSHOREPROHIBITEDBOTTOMGEARS = IsisFishDAOHelper.getZoneDAO(db).findByName("Offshore_fixed");
       Gear OTT = IsisFishDAOHelper.getGearDAO(db).findByName("OTT");
       Gear OTB = IsisFishDAOHelper.getGearDAO(db).findByName("OTB");
       Gear LLS = IsisFishDAOHelper.getGearDAO(db).findByName("LLS");
       Gear GNS = IsisFishDAOHelper.getGearDAO(db).findByName("GNS");
       // NEWFRAREGULATIONS (fermé novembre à avril)
       // NEWFRAREGULATIONS (November to April closure)
       // OTT
       Cantonnement rule1 = new Cantonnement();
       rule1.param_zone = NEWFRAREGULATIONS;
       rule1.param_gear = OTT;
       rule1.param_beginStep = new TimeStep(60);
       rule1.param_endStep = new TimeStep(120);
       rule1.param_beginMonth = Month.NOVEMBER;
       rule1.param_endMonth = Month.DECEMBER;
       paramRules.add(rule1);
       System.out.println("Mesure de Gestion : 26");
       // OTT
       Cantonnement rule2 = new Cantonnement();
       rule2.param_zone = NEWFRAREGULATIONS;
       rule2.param_gear = OTT;
       rule2.param_beginStep = new TimeStep(60);
       rule2.param_endStep = new TimeStep(120);
       rule2.param_beginMonth = Month.JANUARY;
       rule2.param_endMonth = Month.APRIL;
       paramRules.add(rule2);
       System.out.println("Mesure de Gestion : 26");
       // OTB
       Cantonnement rule3 = new Cantonnement();
       rule3.param_zone = NEWFRAREGULATIONS;
       rule3.param_gear = OTB;
       rule3.param_beginStep = new TimeStep(60);
       rule3.param_endStep = new TimeStep(120);
       rule3.param_beginMonth = Month.NOVEMBER;
       rule3.param_endMonth = Month.DECEMBER;
       paramRules.add(rule3);
       System.out.println("Mesure de Gestion : 26");
       // OTB
       Cantonnement rule4 = new Cantonnement();
       rule4.param_zone = NEWFRAREGULATIONS;
       rule4.param_gear = OTB;
       rule4.param_beginStep = new TimeStep(60);
       rule4.param_endStep = new TimeStep(120);
       rule4.param_beginMonth = Month.JANUARY;
       rule4.param_endMonth = Month.APRIL;
       paramRules.add(rule4);
       System.out.println("Mesure de Gestion : 26");
       // Zones de fermeture en mer appliquées à partir du 28 avril 2018 par les pêcheries pour tous les engins de fond
       // The offshore closures comprise of 3 zones closed permanently and a seasonal closure zone applied around their connectivity from October 15th to December 15th.
       // OTT
       Cantonnement rule5 = new Cantonnement();
       rule5.param_zone = OFFSHOREPROHIBITEDBOTTOMGEARS;
       rule5.param_gear = OTT;
       rule5.param_beginStep = new TimeStep(60);
       rule5.param_endStep = new TimeStep(120);
       rule5.param_beginMonth = Month.JANUARY;
       rule5.param_endMonth = Month.DECEMBER;
       paramRules.add(rule5);
       System.out.println("Mesure de Gestion : 26");
       // OTB
       Cantonnement rule6 = new Cantonnement();
       rule6.param_zone = OFFSHOREPROHIBITEDBOTTOMGEARS;
       rule6.param_gear = OTB;
       rule6.param_beginStep = new TimeStep(60);
       rule6.param_endStep = new TimeStep(120);
       rule6.param_beginMonth = Month.JANUARY;
       rule6.param_endMonth = Month.DECEMBER;
       paramRules.add(rule6);
       System.out.println("Mesure de Gestion : 26");
       // LLS
       Cantonnement rule7 = new Cantonnement();
       rule7.param_zone = OFFSHOREPROHIBITEDBOTTOMGEARS;
       rule7.param_gear = LLS;
       rule7.param_beginStep = new TimeStep(60);
       rule7.param_endStep = new TimeStep(120);
       rule7.param_beginMonth = Month.JANUARY;
       rule7.param_endMonth = Month.DECEMBER;
       paramRules.add(rule7);
       System.out.println("Mesure de Gestion : 26");
       // GNS
       Cantonnement rule8 = new Cantonnement();
       
       rule8.param_zone = OFFSHOREPROHIBITEDBOTTOMGEARS;
       rule8.param_gear = GNS;
       rule8.param_beginStep = new TimeStep(60);
       rule8.param_endStep = new TimeStep(120);
       rule8.param_beginMonth = Month.JANUARY;
       rule8.param_endMonth = Month.DECEMBER;
       paramRules.add(rule8);
       System.out.println("Mesure de Gestion : 26");
       // Offshore Seasonal Closure (matched to approximate equal area and pattern of the seasonal closure regulation)
       // OTT
       Cantonnement rule9 = new Cantonnement();
       rule9.param_zone = OFFSHORESEASONALCLOSURE;
       rule9.param_gear = OTT;
       rule9.param_beginStep = new TimeStep(60);
       rule9.param_endStep = new TimeStep(120);
       rule9.param_beginMonth = Month.OCTOBER; // actual is October 15th
       rule9.param_endMonth = Month.NOVEMBER; // actual is December 15th
       paramRules.add(rule9);
       System.out.println("Mesure de Gestion : 26");
       // OTB
       Cantonnement rule10 = new Cantonnement();
       rule10.param_zone = OFFSHORESEASONALCLOSURE;
       rule10.param_gear = OTB;
       rule10.param_beginStep = new TimeStep(60);
       rule10.param_endStep = new TimeStep(120);
       rule10.param_beginMonth = Month.OCTOBER; // actual is October 15th
       rule10.param_endMonth = Month.NOVEMBER; // actual is December 15th
       paramRules.add(rule10);
       System.out.println("Mesure de Gestion : 26");
       // LLS
       Cantonnement rule11 = new Cantonnement();
       rule11.param_zone = OFFSHORESEASONALCLOSURE;
       rule11.param_gear = LLS;
       rule11.param_beginStep = new TimeStep(60);
       rule11.param_endStep = new TimeStep(120);
       rule11.param_beginMonth = Month.OCTOBER; // actual is October 15th
       rule11.param_endMonth = Month.NOVEMBER; // actual is December 15th
       paramRules.add(rule11);
       System.out.println("Mesure de Gestion : 26");
       // GNS
       Cantonnement rule12 = new Cantonnement();
       rule12.param_zone = OFFSHORESEASONALCLOSURE;
       rule12.param_gear = GNS;
       rule12.param_beginStep = new TimeStep(60);
       rule12.param_endStep = new TimeStep(120);
       rule12.param_beginMonth = Month.OCTOBER; // actual is October 15th
       rule12.param_endMonth = Month.NOVEMBER; // actual is December 15th
       paramRules.add(rule12);
       System.out.println("Mesure de Gestion : 26");
       // EffortReduction_10_17point5_to40percent
       EffortReduction_10_17point5_to40percent_t rule13 = new EffortReduction_10_17point5_to40percent_t();
       rule13.param_beginStep = new TimeStep(60);
       rule13.param_endStep = new TimeStep(120);
       paramRules.add(rule13);
       System.out.println("Mesure de Gestion : 26");
    }
   // Nouveau règlement FRA combiné et fermetures offshore avec un réduction progressive de la taille de la flotte appliquée à tous les engins (réduction de 10% en 2020, 17.5% en 2021, 25% en 2022, 32.5% en 2023, et 40% en 2024 de l'effort nominal)
   // Combined New FRA regulation and offshore closures with a gradual reduction in the size of the fleet applied to all gears(reduction of 10% in 2020, 17.5% in 2021, 25% in 2022, 32.5% in 2023, and 40% in 2024 for nominal effort)
   protected void setupScenario27(SimulationStorage nextSimulation, List<Rule> paramRules, TopiaContext db) {
       Zone NEWFRAREGULATIONS = IsisFishDAOHelper.getZoneDAO(db).findByName("Northward_Expansion_of_FRA"); // you need the call name of the object zone and not the file name
       Zone OFFSHORESEASONALCLOSURE = IsisFishDAOHelper.getZoneDAO(db).findByName("Offshore_seasonal_expansion");
       Zone OFFSHOREPROHIBITEDBOTTOMGEARS = IsisFishDAOHelper.getZoneDAO(db).findByName("Offshore_fixed");
       Gear OTT = IsisFishDAOHelper.getGearDAO(db).findByName("OTT");
       Gear OTB = IsisFishDAOHelper.getGearDAO(db).findByName("OTB");
       Gear LLS = IsisFishDAOHelper.getGearDAO(db).findByName("LLS");
       Gear GNS = IsisFishDAOHelper.getGearDAO(db).findByName("GNS");
       // NEWFRAREGULATIONS (fermé novembre à avril)
       // NEWFRAREGULATIONS (November to April closure)
       // OTT
       Cantonnement rule1 = new Cantonnement();
       rule1.param_zone = NEWFRAREGULATIONS;
       rule1.param_gear = OTT;
       rule1.param_beginStep = new TimeStep(60);
       rule1.param_endStep = new TimeStep(120);
       rule1.param_beginMonth = Month.NOVEMBER;
       rule1.param_endMonth = Month.DECEMBER;
       paramRules.add(rule1);
       System.out.println("Mesure de Gestion : 27");
       // OTT
       Cantonnement rule2 = new Cantonnement();
       rule2.param_zone = NEWFRAREGULATIONS;
       rule2.param_gear = OTT;
       rule2.param_beginStep = new TimeStep(60);
       rule2.param_endStep = new TimeStep(120);
       rule2.param_beginMonth = Month.JANUARY;
       rule2.param_endMonth = Month.APRIL;
       paramRules.add(rule2);
       System.out.println("Mesure de Gestion : 27");
       // OTB
       Cantonnement rule3 = new Cantonnement();
       rule3.param_zone = NEWFRAREGULATIONS;
       rule3.param_gear = OTB;
       rule3.param_beginStep = new TimeStep(60);
       rule3.param_endStep = new TimeStep(120);
       rule3.param_beginMonth = Month.NOVEMBER;
       rule3.param_endMonth = Month.DECEMBER;
       paramRules.add(rule3);
       System.out.println("Mesure de Gestion : 27");
       // OTB
       Cantonnement rule4 = new Cantonnement();
       rule4.param_zone = NEWFRAREGULATIONS;
       rule4.param_gear = OTB;
       rule4.param_beginStep = new TimeStep(60);
       rule4.param_endStep = new TimeStep(120);
       rule4.param_beginMonth = Month.JANUARY;
       rule4.param_endMonth = Month.APRIL;
       paramRules.add(rule4);
       System.out.println("Mesure de Gestion : 27");
       // Zones de fermeture en mer appliquées à partir du 28 avril 2018 par les pêcheries pour tous les engins de fond
       // The offshore closures comprise of 3 zones closed permanently and a seasonal closure zone applied around their connectivity from October 15th to December 15th.
       // OTT
       Cantonnement rule5 = new Cantonnement();
       rule5.param_zone = OFFSHOREPROHIBITEDBOTTOMGEARS;
       rule5.param_gear = OTT;
       rule5.param_beginStep = new TimeStep(60);
       rule5.param_endStep = new TimeStep(120);
       rule5.param_beginMonth = Month.JANUARY;
       rule5.param_endMonth = Month.DECEMBER;
       paramRules.add(rule5);
       System.out.println("Mesure de Gestion : 27");
       // OTB
       Cantonnement rule6 = new Cantonnement();
       rule6.param_zone = OFFSHOREPROHIBITEDBOTTOMGEARS;
       rule6.param_gear = OTB;
       rule6.param_beginStep = new TimeStep(60);
       rule6.param_endStep = new TimeStep(120);
       rule6.param_beginMonth = Month.JANUARY;
       rule6.param_endMonth = Month.DECEMBER;
       paramRules.add(rule6);
       System.out.println("Mesure de Gestion : 27");
       // LLS
       Cantonnement rule7 = new Cantonnement();
       rule7.param_zone = OFFSHOREPROHIBITEDBOTTOMGEARS;
       rule7.param_gear = LLS;
       rule7.param_beginStep = new TimeStep(60);
       rule7.param_endStep = new TimeStep(120);
       rule7.param_beginMonth = Month.JANUARY;
       rule7.param_endMonth = Month.DECEMBER;
       paramRules.add(rule7);
       System.out.println("Mesure de Gestion : 27");
       // GNS
       Cantonnement rule8 = new Cantonnement();
       rule8.param_zone = OFFSHOREPROHIBITEDBOTTOMGEARS;
       rule8.param_gear = GNS;
       rule8.param_beginStep = new TimeStep(60);
       rule8.param_endStep = new TimeStep(120);
       rule8.param_beginMonth = Month.JANUARY;
       rule8.param_endMonth = Month.DECEMBER;
       paramRules.add(rule8);
       System.out.println("Mesure de Gestion : 27");
       // Offshore Seasonal Closure (matched to approximate equal area and pattern of the seasonal closure regulation)
       // OTT
       Cantonnement rule9 = new Cantonnement();
       rule9.param_zone = OFFSHORESEASONALCLOSURE;
       rule9.param_gear = OTT;
       rule9.param_beginStep = new TimeStep(60);
       rule9.param_endStep = new TimeStep(120);
       rule9.param_beginMonth = Month.OCTOBER; // actual is October 15th
       rule9.param_endMonth = Month.NOVEMBER; // actual is December 15th
       paramRules.add(rule9);
       System.out.println("Mesure de Gestion : 27");
       // OTB
       Cantonnement rule10 = new Cantonnement();
       rule10.param_zone = OFFSHORESEASONALCLOSURE;
       rule10.param_gear = OTB;
       rule10.param_beginStep = new TimeStep(60);
       rule10.param_endStep = new TimeStep(120);
       rule10.param_beginMonth = Month.OCTOBER; // actual is October 15th
       rule10.param_endMonth = Month.NOVEMBER; // actual is December 15th
       paramRules.add(rule10);
       System.out.println("Mesure de Gestion : 27");
       // LLS
       Cantonnement rule11 = new Cantonnement();
       rule11.param_zone = OFFSHORESEASONALCLOSURE;
       rule11.param_gear = LLS;
       rule11.param_beginStep = new TimeStep(60);
       rule11.param_endStep = new TimeStep(120);
       rule11.param_beginMonth = Month.OCTOBER; // actual is October 15th
       rule11.param_endMonth = Month.NOVEMBER; // actual is December 15th
       paramRules.add(rule11);
       System.out.println("Mesure de Gestion : 27");
       // GNS
       Cantonnement rule12 = new Cantonnement();
       rule12.param_zone = OFFSHORESEASONALCLOSURE;
       rule12.param_gear = GNS;
       rule12.param_beginStep = new TimeStep(60);
       rule12.param_endStep = new TimeStep(120);
       rule12.param_beginMonth = Month.OCTOBER; // actual is October 15th
       rule12.param_endMonth = Month.NOVEMBER; // actual is December 15th
       paramRules.add(rule12);
       System.out.println("Mesure de Gestion : 27");
       // EffortReduction_10_17point5_to40percent_AllGears
       EffortReduction_10_17point5_to40percent_AllGears_t rule13 = new EffortReduction_10_17point5_to40percent_AllGears_t();
       rule13.param_beginStep = new TimeStep(60);
       rule13.param_endStep = new TimeStep(120);
       paramRules.add(rule13);
       System.out.println("Mesure de Gestion : 27");
    }

    /**
     * Call after each simulation.
     *
     * @param context        plan context
     * @param lastSimulation storage used for simulation
     * @return true if we must do next simulation, false to stop plan
     * @throws Exception
     */

    @Override
    public boolean afterSimulation(SimulationPlanContext context, SimulationStorage lastSimulation) throws Exception {
        return true;  /** here it does not apply, which is why only a logical vector is applied in this case.
         But this is where you would designate rules following the full simulation before running another simulation that is
         dependent on the results of the previous one */
    }
}
