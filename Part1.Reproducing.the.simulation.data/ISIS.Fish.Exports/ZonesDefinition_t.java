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
import java.util.List;

import org.nuiton.topia.TopiaContext;

import fr.ifremer.isisfish.entities.*;
import fr.ifremer.isisfish.export.ExportStep;
import fr.ifremer.isisfish.types.TimeStep;
import fr.ifremer.isisfish.datastore.SimulationStorage;

/**
 * ZonesDefinition_t.java
 */
public class ZonesDefinition_t implements ExportStep {

    /** to use log facility, just put in your code: log.info("..."); */
    static private Log log = LogFactory.getLog(ZonesDefinition_t.class);

    protected String [] necessaryResult = {
        // put here all necessary result for this rule
        // example: 
        // MatrixBiomass.NAME,
        // MatrixNetValueOfLandingsPerStrategyMet.NAME
    };

    @Override
    public String[] getNecessaryResult() {
        return this.necessaryResult;
    }

    @Override
    public String getExportFilename() {
        return "ZonesDefinition";
    }

    @Override
    public String getExtensionFilename() {
        return ".csv";
    }

    @Override
    public String getDescription() {
        return "Export cell's zone constitution";
    }

    /*@Override
    public void export(SimulationStorage simulation, Writer out) throws Exception {
        TopiaContext tx = simulation.getStorage().beginTransaction();
        List<Zone> zones = SimulationStorage.getFisheryRegion(tx).getZone();
        
        TimeStep lastStep = simulation.getResultStorage().getLastStep();       
        
        for (TimeStep step = new TimeStep(0); !step.after(lastStep); step = step.next() ) {
            for (Zone zone : zones) {
                for (Cell cell : zone.getCell()) {
                    out.write(zone + ";" + cell + ";" + step.getStep() + "\n");
                }
            }
        }
        
        tx.closeContext();
    }*/

    @Override
    public void exportBegin(SimulationStorage simulation, Writer out) throws Exception {
        out.write("step;zone;cell\n");
    }

    @Override
    public void export(SimulationStorage simulation, TimeStep step, Writer out) throws Exception {
        TopiaContext tx = simulation.getStorage().beginTransaction();
        List<Zone> zones = SimulationStorage.getFisheryRegion(tx).getZone();

        for (Zone zone : zones) {
            for (Cell cell : zone.getCell()) {
                out.write(step.getStep() + ";" + zone + ";" + cell + "\n");
            }
        }

        tx.closeContext();
    }

    @Override
    public void exportEnd(SimulationStorage simulation, Writer out) throws Exception {

    }
}
