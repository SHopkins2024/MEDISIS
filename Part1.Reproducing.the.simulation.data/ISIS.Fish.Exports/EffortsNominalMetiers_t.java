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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Writer;

import org.nuiton.math.matrix.*;

import fr.ifremer.isisfish.entities.*;
import fr.ifremer.isisfish.export.ExportStep;
import fr.ifremer.isisfish.types.TimeStep;
import resultinfos.MatrixEffortNominalPerStrategyMet;
import fr.ifremer.isisfish.datastore.SimulationStorage;

/**
 * EffortsMetier_t.java
 */
public class EffortsNominalMetiers_t implements ExportStep {

    /** to use log facility, just put in your code: log.info("..."); */
    static private Log log = LogFactory.getLog(EffortsNominalMetiers_t.class);

    protected String [] necessaryResult = {
        MatrixEffortNominalPerStrategyMet.NAME
    };

    @Override
    public String[] getNecessaryResult() {
        return this.necessaryResult;
    }

    @Override
    public String getExportFilename() {
        return "EffortsNominalMetiers";
    }

    @Override
    public String getExtensionFilename() {
        return ".csv";
    }

    @Override
    public String getDescription() {
        return "retourne un tableau step;strategie;metier;effort";
    }
    //return a table in the format: step,strategy,metier,effort

    /*@Override
    public void export(SimulationStorage simulation, Writer out) throws Exception {
        MatrixND mat = simulation.getResultStorage().getMatrix(MatrixEffortPerStrategyMet.NAME);
        for (MatrixIterator i = mat.iterator(); i.hasNext();) {
            i.next();
            Object [] sems = i.getSemanticsCoordinates();
            TimeStep step = (TimeStep)sems[0];
            Strategy str = (Strategy)sems[1];
            Metier metier = (Metier)sems[2];
            
            double val = i.getValue();
            out.write(str.getName() +";"+ metier.getName() +";"+ step.getStep() +";"+ val +"\n");
        }
    }*/

    @Override
    public void exportBegin(SimulationStorage simulation, Writer out) throws Exception {
        out.write("step;strategy;metier;value\n");
    }

    @Override
    public void export(SimulationStorage simulation, TimeStep step, Writer out) throws Exception {
        MatrixND mat = simulation.getResultStorage().getMatrix(step, MatrixEffortNominalPerStrategyMet.NAME);
        for (MatrixIterator i = mat.iterator(); i.hasNext();) {
            i.next();
            Object [] sems = i.getSemanticsCoordinates();
            Strategy str = (Strategy)sems[0];
            Metier metier = (Metier)sems[1];
            
            double val = i.getValue();
            out.write(step.getStep() +";" + str.getName() + ";" + metier.getName() + ";" + val + "\n");
        }
    }

    @Override
    public void exportEnd(SimulationStorage simulation, Writer out) throws Exception {

    }
}
