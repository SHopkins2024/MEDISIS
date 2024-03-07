/*
 * #%L
 * IsisFish data
 * %%
 * Copyright (C) 2006 - 2016 Ifremer, CodeLutin
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
package exports;

import fr.ifremer.isisfish.datastore.SimulationStorage;
import fr.ifremer.isisfish.entities.Metier;
import fr.ifremer.isisfish.entities.Population;
import fr.ifremer.isisfish.entities.PopulationGroup;
import fr.ifremer.isisfish.entities.Strategy;
import fr.ifremer.isisfish.entities.Zone;
import fr.ifremer.isisfish.export.ExportStep;
import fr.ifremer.isisfish.types.TimeStep;
import resultinfos.MatrixCatchWeightPerStrategyMetPerZonePop;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuiton.math.matrix.MatrixIterator;
import org.nuiton.math.matrix.MatrixND;

import java.io.Writer;

/**
 * CapturesPoidsStrategies_t.java
 * 
 * Export des captures en poids de la forme :
 * Pas de temps ; Population ; Stratégie ; Métier ; Groupe ; Zone ; Valeur
 * ------------------------------------------------------------------------------
 * Export the catch weight in Table format: step,pop,strategy,metier,id,zone,number
 * */
public class CapturesPoidsStrategies_t implements ExportStep {

    /** to use log facility, just put in your code: log.info("..."); */
    static private Log log = LogFactory.getLog(CapturesPoidsStrategies_t.class);

    protected String[] necessaryResult = {
        MatrixCatchWeightPerStrategyMetPerZonePop.NAME
    };

    @Override
    public String[] getNecessaryResult() {
        return this.necessaryResult;
    }

    @Override
    public String getExportFilename() {
        return "CapturesPoidsStrategies";
    }//CatchWeightStrategies

    @Override
    public String getExtensionFilename() {
        return ".csv";
    }

    @Override
    public String getDescription() {
        return "Export les captures en poids de la simulation. tableau step;pop;strategie;metier;id;zone;nombre";
        //Export the catch weight of the simulation. Table format: step,pop,strategy,metier,id,zone,number
    }

    /*@Override
    public void export(SimulationStorage simulation, Writer out)
            throws Exception {
        TimeStep lastStep = simulation.getResultStorage().getLastStep();

        for (Population pop : simulation.getParameter().getPopulations()) {
            for (TimeStep step = new TimeStep(0); !step.after(lastStep); step = step
                    .next()) {
                MatrixND mat = simulation.getResultStorage().getMatrix(step,
                        pop, MatrixCatchWeightPerStrategyMetPerZonePop.NAME);
                if (mat != null) { // can be null if simulation is stopped before last year simulation
                    //mat = mat.sumOverDim(0); //sum on strategy
                    for (MatrixIterator i = mat.iterator(); i.hasNext();) {
                        i.next();
                        Object[] sems = i.getSemanticsCoordinates();
                        Metier metier = (Metier) sems[1];
                        PopulationGroup group = (PopulationGroup) sems[2];
                        Zone zone = (Zone) sems[3];
                        Strategy strategy = (Strategy) sems[0];

                        double val = i.getValue();
                        out.write(pop.getName() + ";" + strategy.getName() + ";"
                                + metier.getName() + ";" + group.getId() + ";"
                                + zone.getName() + ";" + step.getStep() + ";"
                                + val + "\n");
                    }
                }
            }
        }
    }*/

    @Override
    public void exportBegin(SimulationStorage simulation, Writer out) throws Exception {
        out.write("step;population;strategy;metier;group;zone;value\n");
    }

    @Override
    public void export(SimulationStorage simulation, TimeStep step, Writer out) throws Exception {
        for (Population pop : simulation.getParameter().getPopulations()) {
            MatrixND mat = simulation.getResultStorage().getMatrix(step, pop, MatrixCatchWeightPerStrategyMetPerZonePop.NAME);
            for (MatrixIterator i = mat.iterator(); i.hasNext();) {
                i.next();
                Object[] sems = i.getSemanticsCoordinates();
                Metier metier = (Metier) sems[1];
                PopulationGroup group = (PopulationGroup) sems[2];
                Zone zone = (Zone) sems[3];
                Strategy strategy = (Strategy) sems[0];

                double val = i.getValue();
                out.write(step.getStep() + ";" + pop.getName() + ";" + strategy.getName() + ";"
                        + metier.getName() + ";" + group.getId() + ";"
                        + zone.getName() + ";" + val + "\n");
            }
        }
    }

    @Override
    public void exportEnd(SimulationStorage simulation, Writer out) throws Exception {

    }
}
