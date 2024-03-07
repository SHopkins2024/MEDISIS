package rules;

//import static org.nuiton.i18n.I18n._;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import fr.ifremer.isisfish.util.Doc;


import scripts.GravityModel;
//import scripts.ResultName;
import scripts.SiMatrix;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.io.File;
import java.io.Writer;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.FileReader;

import org.nuiton.math.matrix.*;

import fr.ifremer.isisfish.IsisFishDAOHelper;
import fr.ifremer.isisfish.simulator.MetierMonitor;
import fr.ifremer.isisfish.simulator.SimulationContext;
import fr.ifremer.isisfish.types.TimeStep;
import fr.ifremer.isisfish.types.Month;
import fr.ifremer.isisfish.entities.*;
import fr.ifremer.isisfish.rule.AbstractRule;
import fr.ifremer.isisfish.datastore.ResultStorage;

import org.nuiton.util.FileUtil;

/**
 * effortObs_MultiSp
 *
 * Created: 26 aout 2008
 *
 * @author anonymous <anonymous@labs.libre-entreprise.org>
 * @version $Revision: 1.2 $
 *
 * Last update: $Date: 2013-06-12 08:59:38 $
 * by : $Author: 2SY- Sigrid $
 */
public class Effort_2015_2017_GDL_local_t extends AbstractRule {
    /** to use log facility, just put in your code: log.info("..."); */
    static private Log log = LogFactory.getLog(Effort_2015_2017_GDL_local_t.class);

    // Change "param_cheminFichierAverage" definition to where you the tables are stored
    public String param_directory_rules = "C:/Users/steph/Documents/MEDISIS/Part1.Reproducing.the.simulation.data/ISIS.Fish.Input.Tables/";
    public String param_cheminFichierAverage = param_directory_rules;
    public String param_InactivityFileName = "/Total.Inactivite.effort.csv";
    public String param_nbVessFileName = "/Total.NbBateau.effort.csv";
    public String param_EffortPropFileName = "/Total.Proportion.strategy.csv";
    
    @Doc(value= "simulation starting year compared to estimation starting year (0:2015;1:2016;2:2017)")
    public int param_startDate = 0;

    protected List<Strategy> allStrategies ;
    protected File Inactivity;
    protected File Effort;
    protected File nbVess;

    protected MatrixND matInactivity;
    protected MatrixND matEffort;
    protected MatrixND matNbVess;

    public String [] necessaryResult = {
    };

    public String[] getNecessaryResult() {
        return this.necessaryResult;
    }

    /**
     * Permet d'afficher a l'utilisateur une aide sur la regle.
     * @return L'aide ou la description de la regle
     * ----------------------------------------------------------------
     * Allows you to display help on the rule to the user.
     * @return Help or description of the rule
     */
    public String getDescription() throws Exception {
        return ("Chaque annee et mois les efforts % et inactivity observ");
        //"Every year and month the % efforts and inactivity observed".

    }

     /**
     * Appel
     * des valeurs
     * @param simulation La simulation pour lequel on utilise cette regle
     * --------------------------------------------------------------------
     * Call
     * values
     * @param simulation The simulation for which we use this rule
     */
    public void init(SimulationContext context) throws Exception {

        // reccuperation des metiers et strategies
        // recover the metiers and strategies
        SiMatrix siMatrix = SiMatrix.getSiMatrix(context);
        TimeStep date = new TimeStep(0);

        // reccuperation des strategies
        // recover the strategies
        allStrategies = siMatrix.getStrategies(date);

        // Creation des objects files à partir des chemins
        // Create object folders from paths
        String InactivityPath = param_cheminFichierAverage + param_InactivityFileName;
        String nbVessPath = param_cheminFichierAverage + param_nbVessFileName;
        String EffortPropPath = param_cheminFichierAverage + param_EffortPropFileName;

        Inactivity = new File(InactivityPath);
        Effort = new File(EffortPropPath);
        nbVess = new File(nbVessPath);

        // Import des matrices
        matInactivity = MatrixFactory.getInstance().create(Inactivity);
        System.out.println("matrice inactivite " + matInactivity);
        matEffort = MatrixFactory.getInstance().create(Effort);
        System.out.println("matrice effort " + matEffort);
        matNbVess = MatrixFactory.getInstance().create(nbVess);
        System.out.println("matrice nb navires " + matNbVess);

    }// fin de init
    // end of initial step

    /**
     * La condition qui doit etre vrai pour faire les actions
     * @param simulation La simulation pour lequel on utilise cette regle
     * @return vrai si on souhaite que les actions soit faites
     * -----------------------------------------------------------------------
     * The condition that must be true to perform the actions
     * @param simulation The simulation for which this rule is used
     * @return true if we want the actions to be done
     */
    public boolean condition(SimulationContext context, TimeStep step, Metier metier) throws Exception {
        return (step.getYear()<(3-param_startDate)); // le script ne s'applique que si on est en 2015/2016/2017
        // the script does not apply to the years 2015, 2016, or 2017

    }

    /**
     * Si la condition est vrai alors cette action est execute avant le pas
     * de temps de la simulation.
     * @param simulation La simulation pour lequel on utilise cette regle
     * -------------------------------------------------------------------------
     * If the condition is true, then this action is executed before the simulation's
     * time step of the simulation.
     * @param simulation The simulation for which this rule is used
     */
    // Booleen permettant que ne boucler que sur un seul metier dans la preaction :
    // Boolean makes it possible to loop only on one metier in the preaction:
    boolean first = true;
    public void preAction(SimulationContext context, TimeStep step, Metier metier) throws Exception {
            if (log.isDebugEnabled()) {
                log.debug("first = "+ first + "date:"+ step);
            }
            //System.out.println("first = " + first+ " ,on passe dans la preaction ?");
            if (first){ // on passe dans preaction pour la premiere fois
                        // We go to the preaction for the first time
                System.out.println("Oui, preaction : ");

                // If startDate != 0
                int start = param_startDate*12;
                TimeStep s = step.add(start);

                 System.out.println("Timestep" + s);

                // Boucle sur les strategies
                for(Strategy strIndex : allStrategies){


                        if(!(strIndex.getName().equals("Spanish_Strategy") || strIndex.getName().equals("FrenchGillnetters"))){
                        // interdit de faire des set sur les strategies de la semantique de la matrice, il faut recuperer les strategies de la date courante
                        // forbidden to set the strategies of the matrix semantics, it is necessary to retrieve the strategies of the current date.
                        Strategy str = (Strategy)context.getDB().findByTopiaId(strIndex.getTopiaId());

                        System.out.println("Strategie : " + str);

                        // Change vessel number
                        //---------------------
                        //int nbv = (int) matNbVess.getValue(str,s.getYear());
                        double nbv = (double) matNbVess.getValue(str,s.getYear());
                        //str.getSetOfVessels().setNumberOfVessels(nbv);
                        str.setProportionSetOfVessels(nbv);
                        System.out.println("Nb bateaux : " + nbv);

                        // Change Effort proportion
                        //--------------------------
                        StrategyMonthInfo smi = str.getStrategyMonthInfo(s.getMonth());
                        Collection<EffortDescription> strMet = str.getSetOfVessels().getPossibleMetiers() ;
                        for (EffortDescription ed : strMet){
                            Metier strMetier = ed.getPossibleMetiers();
                            double newProp = matEffort.getValue(str,strMetier,s);
                            smi.setProportionMetier(strMetier,newProp);
                            System.out.println("proportion : " + strMetier + " " + newProp);
                            System.out.println(smi.getProportionMetier(strMetier));
                        }

                        // Change Inactivity equation
                        //---------------------------

                        double newInactivity = (double) matInactivity.getValue(str,s);
                        //double newInactivity = 20;
                        System.out.println("newInactivity :" + newInactivity);
                        String newEq = "return "+newInactivity+" ;";
                        Equation eqInac = str.getInactivityEquation();
                        eqInac.setContent(newEq);
                        str.setInactivityEquationUsed(true) ;
                        //str.getInactivityEquation().setContent(newEq);
                        System.out.println("Inactivity :" + eqInac.getContent());



                        //double newInactivity = (double) matInactivity.getValue(str,s);
                        //smi.setMinInactivityDays(newInactivity);
                        //System.out.println("Inactivity :" + newInactivity);
                        //System.out.println(smi.getMinInactivityDays());


                        } // fin du if espagnols
                        // end of Spanish treatment

                }//fin de boucle sur strategies
                // end of strategy loop

                first = false;
            }// fin de first= true
            if (log.isDebugEnabled()) {
                log.debug("fin Effort Action avant"); //End before Effort Action
            }
    }// fin de pre action
    // end of pre action
    /**
     * Si la condition est vrai alors cette action est strategy=" + str.getName() +", l'anne : "+ date.getYear() + " et le mois = "+date.getMonth()+": newInactivity= "+ newInactivity);

                    // on boucle sur les metiers de la strategie pour initialiser les efforts avec matEffort
                    Collection<EffortDescription> strMet = str.getSetOfVessels().getPossibleMetiers() ;
                    for (EffortDescription ed : strMet){
                        Metier strMetier = ed.getPossibleMetiers();
                        System.out.println("pour le metier=" + strMetier);
execut
     * de temps de la simulation.
     * @param simulation La simulation pour lequel on utilise cette regle
     * ------------------------------------------------------------------------------------------------------------------------
     * If the condition is true, then this action is strategy=" + str.getName() +", l'anne : "+ date.getYear() + " et le mois = "+date.getMonth()+": newInactivity= "+ newInactivity);
     *             // loop on the strategy trades to initialize efforts with matEffort
     *             Collection<EffortDescription> strMet = str.getSetOfVessels().getPossibleMetiers() ;
     *             for (EffortDescription ed : strMet){
     *                  Metier strMetier = ed.getPossibleMetiers();
     *                  System.out.println("pour le metier=" + strMetier);
     * execut
     *      * simulation time.
     *      * @param simulation The simulation for which the rule applies.
     */
    public void postAction(SimulationContext context, TimeStep step, Metier metier) throws Exception {
    first = true ;
    }

}
