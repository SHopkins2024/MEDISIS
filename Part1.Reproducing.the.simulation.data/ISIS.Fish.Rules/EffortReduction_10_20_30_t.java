/*
 * #%L
 * IsisFish data
 * %%
 * Copyright (C) 2006 - 2014 Ifremer, CodeLutin
 * %%
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
 * #L%
 */
package rules;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
import fr.ifremer.isisfish.types.Month;
import fr.ifremer.isisfish.datastore.ResultStorage;

import scripts.SiMatrix;
import fr.ifremer.isisfish.entities.*;
import fr.ifremer.isisfish.rule.AbstractRule;
import fr.ifremer.isisfish.simulator.SimulationContext;
import fr.ifremer.isisfish.types.TimeStep;
import fr.ifremer.isisfish.util.Doc;

/**
 * NewEffortReduction_10_20_30.java
 *
 * Updated: 30 March 2021
 *
 *
 * Gradual reduction in nominal effort begining in January 2020 and accumilated until 30% is reached where it is fixed for the remaining simulation years.
 * ATTENTION: rule applies to French OTB and OTT trawlers (decrease in the number of boats) and to Spanish trawlers are decreased based on their proportional
 representation in the the Spanish_Stategy.
 *
 */

public class EffortReduction_10_20_30_t extends AbstractRule {

    /** to use log facility, just put in your code: log.info("..."); */
    static private Log log = LogFactory.getLog(EffortReduction_10_20_30_t.class);

    @Doc("Begin step")
    public TimeStep param_beginStep = new TimeStep(60);
    @Doc("End step")
    public TimeStep param_endStep = new TimeStep(120);

    @Doc("Pourcentage de reduction d effort applique.")
    //Percentage of effort reduction applied
    public double param_PercentReduction = 0;

    @Doc("Annee ou la reduction d effort commence a s appliquer")
    //First year when effort reduction applies
    public double start = 5;

    protected boolean first = true;

    protected String[] necessaryResult = {
        // put here all necessary result for this rule
        // example:
        // MatrixBiomass.NAME,
        // MatrixNetValueOfLandingsPerStrategyMet.NAME
    };

    /**
     * @return the necessaryResult
     */
    @Override
    public String[] getNecessaryResult() {
        return this.necessaryResult;
    }

    /**
     * Used to display help on the rule to the user.
     * @return Help or description of the rule
     * -------------------------------------------------------------
     * Permet d'afficher a l'utilisateur une aide sur la regle.
     * @return L'aide ou la description de la regle
     */
    @Override
    public String getDescription() {
        return "Reduce monthly effort of each strategy of the percent indicated";
    }

    /**
     * Called when starting the simulation, this method is used to initialize
     * values.
     * @param context The simulation for which we use this rule
     * -------------------------------------------------------------
     * Appele au demarrage de la simulation, cette methode permet d'initialiser
     * des valeurs.
     * @param context La simulation pour lequel on utilise cette regle
     */
    @Override
    public void init(SimulationContext context) throws Exception {
    }

    /**
     * The condition that must be true to do the actions.
     *
     * @param context the simulation for which we use this rule
     * @param step the current time step
     * @param metier the metier concerned
     * @return true if we want the actions to be done
     * -------------------------------------------------------------
     * La condition qui doit etre vrai pour faire les actions.
     *
     * @param context la simulation pour lequel on utilise cette regle
     * @param step le pas de temps courant
     * @param metier le metier concerné
     * @return vrai si on souhaite que les actions soit faites
     */
    @Override
    public boolean condition(SimulationContext context, TimeStep step, Metier metier)
            throws Exception {

        boolean result = true;
        if (step.before(param_beginStep)) {
            result = false;
        } else if (step.after(param_endStep)) {
            result = false;
        }
        if (result) {
            log.info("condition vraie");
            //condition true
        }
        return result;
    }

    /**
     * If the condition is true then this action is executed before the step
     * simulation time.
     *
     * @param context the simulation for which we use this rule
     * @param step the current time step
     * @param metier the metier concerned
     *-----------------------------------------------------------------------------------------
     * Si la condition est vrai alors cette action est executee avant le pas
     * de temps de la simulation.
     *
     * @param context la simulation pour lequel on utilise cette regle
     * @param step le pas de temps courant
     * @param metier le metier concerné
     */
    @Override
    public void preAction(SimulationContext context, TimeStep step, Metier metier)
            throws Exception {
        if (first) {
            first = false;
        // percent reduction depending on the simulation year (y)
        // percent reduction dependant de l'annee de simulation (y)
        int y = context.getSimulationControl().getStep().getYear();

        if(y == 0 + start){
         param_PercentReduction = 0.1 ;
        } else {
                if(y == 1 + start){
                 param_PercentReduction = 0.2 ;
                } else {
                        if(y >= 2 + start) {
                        param_PercentReduction = 0.3 ;
                         }
                       }
                }

           System.out.println("PERCENT REDUCTION : " + param_PercentReduction + " - annee " + y);

            SiMatrix siMatrix = SiMatrix.getSiMatrix(context);
            List<Strategy> strs = siMatrix.getStrategies(step);
            for (Strategy str : strs) {
                if(!(str.getName().equals("Spanish_Strategy") || str.getName().equals("FrenchGillnetters"))){
                    System.out.println("strategy evaluee : " + str.getName());

                    double propOld = str.getProportionSetOfVessels();
                    System.out.println("ancien nb de bateaux : " + propOld);
                    double newProp = propOld * (1 - param_PercentReduction);
                    str.setProportionSetOfVessels(newProp);
                    System.out.println("nouveau nb de bateaux : "+  newProp);
                }

                  /** Unlike the first method, which adjusted the target factor and thus changed catchability to achieve effort reduction,
                here we are isolation the proportion of the spanish strategy that corresponds to trawlers, and based on that proportion, we
                are applying the percent effort reduction. As you recall ESP_LLS and ESP_OTB (a grouped OTT and OTB value) are share the
                same fishing zone and grouped proportion
                */

                else if (str.getName().equals("Spanish_Strategy")) {
                  List<StrategyMonthInfo> smis = str.getStrategyMonthInfo(); // stores the effort proportion per metier of the strategy
                  for (StrategyMonthInfo smi : smis){
                    Collection<EffortDescription> efforts = str.getSetOfVessels().getPossibleMetiers();
                    for(EffortDescription effort : efforts) {
                      Metier effortMetier = effort.getPossibleMetiers();

                      if(effortMetier.getName().equals("OTB_ESP")){
                        //get the old value of effort proportion
                        double oldValue = smi.getProportionMetier(effortMetier);
                        // set the new value
                        smi.setProportionMetier(effortMetier, oldValue*(1-param_PercentReduction));
                      }
                    }
                  }
                }
              }
            }

        if (log.isDebugEnabled()) {
            log.debug("fin Effort Action avant");
            // End of effort forward action
        }
            }

    /**
     * If the condition is true then this action is executed after the step
     * simulation time.
     *
     * @param context The simulation for which we use this rule
     * @param step the current time step
     * @param metier the metier concerned
     *--------------------------------------------------------------------------------------------------
     * Si la condition est vrai alors cette action est executée apres le pas
     * de temps de la simulation.
     *
     * @param context La simulation pour lequel on utilise cette regle
     * @param step le pas de temps courant
     * @param metier le metier concerné
     */
    @Override
    public void postAction(SimulationContext context, TimeStep step, Metier metier)
            throws Exception {
        first = true;
    }

}
