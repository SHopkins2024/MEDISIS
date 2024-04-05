package rules;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import scripts.SiMatrix;

import fr.ifremer.isisfish.datastore.RegionStorage;
import fr.ifremer.isisfish.entities.EffortDescription;
import fr.ifremer.isisfish.entities.EffortDescriptionDAO;
import fr.ifremer.isisfish.datastore.SimulationStorage;
import fr.ifremer.isisfish.entities.FisheryRegion;
import fr.ifremer.isisfish.entities.Gear;
import fr.ifremer.isisfish.entities.Metier;
import fr.ifremer.isisfish.entities.MetierDAO;
import fr.ifremer.isisfish.entities.Population;
import fr.ifremer.isisfish.entities.PopulationGroup;
import fr.ifremer.isisfish.entities.Species;
import fr.ifremer.isisfish.entities.MetierSeasonInfo;
import fr.ifremer.isisfish.entities.MetierSeasonInfoDAO;
import fr.ifremer.isisfish.entities.SetOfVessels;
import fr.ifremer.isisfish.entities.Strategy;
import fr.ifremer.isisfish.entities.StrategyMonthInfo;
import fr.ifremer.isisfish.entities.Zone;
import fr.ifremer.isisfish.rule.AbstractRule;
import fr.ifremer.isisfish.simulator.SimulationContext;
import fr.ifremer.isisfish.types.TimeStep;
import fr.ifremer.isisfish.types.Month;
import fr.ifremer.isisfish.util.Doc;
import org.nuiton.math.matrix.*;
import org.nuiton.math.matrix.MatrixND;
import org.nuiton.math.matrix.*;
import org.nuiton.util.*;
import org.nuiton.topia.*;
import fr.ifremer.isisfish.*;
import fr.ifremer.isisfish.types.*;
import fr.ifremer.isisfish.rule.Rule;
import fr.ifremer.isisfish.rule.RuleHelper;
import fr.ifremer.isisfish.entities.*;
import fr.ifremer.isisfish.simulator.MetierMonitor;

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
public class Distri_Rec_variable_t extends AbstractRule {
    /** to use log facility, just put in your code: log.info("..."); */
    static private Log log = LogFactory.getLog(Distri_Rec_variable_t.class);
        
    @Doc(value= "simulation starting year compared to estimation starting year (0:2015;1:2016;2:2017)")
    public int param_startDate = 0;
    
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
        return ("Modif distribution trimestrielle du recrutement chaque annee"); //Modify recruitment distribution quarterly over the year
         
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
        
        // reccuperation des metiers et strategies // recover the métiers and strategies
      SiMatrix siMatrix = SiMatrix.getSiMatrix(context);
      TimeStep date = new TimeStep(0);
 
    }// fin de init
    // end of the initial step

    /**
     * La condition qui doit etre vrai pour faire les actions
     * @param simulation La simulation pour lequel on utilise cette regle
     * @return vrai si on souhaite que les actions soit faites
     * ----------------------------------------------------------------------------
     * The condition that must be true to perform the actions
     * @param simulation The simulation for which we use this rule
     * @return true if we want the actions to be done
     */ 
    public boolean condition(SimulationContext context, TimeStep step, Metier metier) throws Exception {
       return (step.getYear()<(3-param_startDate)); // le script ne s'applique que si on est en 2015/2016/2017
        }
 
    /**
     * Si la condition est vrai alors cette action est execute avant le pas
     * de temps de la simulation.
     * @param simulation La simulation pour lequel on utilise cette regle
     * -----------------------------------------------------------------------------
     * If the condition is true then this action is executed before the step
     * simulation time.
     * @param simulation The simulation for which we use this rule
     */
    // Booleen permettant que ne boucler que sur un seul metier dans la preaction :
    // Boolean allowing only looping on a single metier in the preaction:
    //boolean first = true;
    public void preAction(SimulationContext context, TimeStep step, Metier metier) throws Exception {        

         //  if (first){ // on passe dans preaction pour la premiere fois
        //  if (first){ // we go into preaction for the first time
//System.out.println("Oui, preaction : ");
                
int y = context.getSimulationControl().getStep().getYear();

//moyenne des trois années
//average of the three years
double RecT1=0.17;
double RecT2=0.41;
double RecT3=0.32;
double RecT4=0.09;

        if(y == 0){
         RecT1 = 0.22 ;
         RecT2 = 0.44 ;
         RecT3 = 0.04 ;
         RecT4 = 0.3 ;
        } else {
                if(y == 1){
         RecT1 = 0.17 ;
         RecT2 = 0.61 ;
         RecT3 = 0.21 ;
         RecT4 = 0.01 ;
                } else {
                if(y == 2) {
         RecT1 = 0.43 ;
         RecT2 = 0.014 ;
         RecT3 = 0.095 ;
         RecT4 = 0.46 ;
                        }
                        }           
                }

System.out.println("Distribution recrutement : " + RecT1 + RecT2 + RecT3 + RecT4 + " - year " + y);

List<Population> pop = context.getSimulationStorage().getParameter().getPopulations() ;

    for(Population p : pop){
    if(p.getName().equals("Hake")){
        p = context.getPopulationDAO().findByTopiaId(p.getTopiaId());
        double[] values = { RecT1/3 , RecT1/3 , RecT1/3 , RecT2/3 , RecT2/3 , RecT2/3 , RecT3/3 , RecT3/3 , RecT3/3 , RecT4/3 , RecT4/3 , RecT4/3} ;
        MatrixND RecDistri = MatrixFactory.getInstance().create(values, new int[] {12});
        p.setRecruitmentDistribution(RecDistri);
        System.out.println("Distribution recrutement :"+ RecDistri);
        System.out.println("Distribution recrutement :"+ p.getRecruitmentDistribution());
     }
}


               //first = false;   
       // }// fin de first= true
    }// fin de pre action
    // end of pre action
 
    public void postAction(SimulationContext context, TimeStep step, Metier metier) throws Exception {
    //first = true ;   
    }

}
