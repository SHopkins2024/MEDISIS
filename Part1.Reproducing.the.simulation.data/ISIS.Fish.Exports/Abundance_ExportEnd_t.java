/*
 * #%L
 * IsisFish data
 * %%
 * Copyright (C) 2006 - 2014 Ifremer, CodeLutin, Chatellier Eric
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
import resultinfos.MatrixAbundanceBeginMonth;
import fr.ifremer.isisfish.entities.*;
import fr.ifremer.isisfish.export.Export;
import fr.ifremer.isisfish.types.TimeStep;
import fr.ifremer.isisfish.datastore.SimulationStorage;

/**
 * Abundances_t.java
 *
 * Created: 1 septembre 2006
 *
 * @author anonymous <anonymous@labs.libre-entreprise.org>
 * @version $Revision: 1.3 $
 *
 * Last update: $Date: 2007-05-24 09:30:07 $
 * by : $Author: bpoussin $
 */
public class Abundance_ExportEnd_t implements Export {

    /** to use log facility, just put in your code: log.info("..."); */
    static private Log log = LogFactory.getLog(Abundance_ExportEnd_t.class);

    protected String[] necessaryResult = {
        MatrixAbundanceBeginMonth.NAME
    };

    @Override
    public String[] getNecessaryResult() {
        return this.necessaryResult;
    }

    @Override
    public String getExportFilename() {
        return "Abundance_ExportEnd";
    }

    @Override
    public String getExtensionFilename() {
        return ".csv";
    }

    @Override
    public String getDescription() {
        return "Exporte les abondances en nombre tableau avec des lignes step;pop;id;zone;nombre.";
        //Export the abundance in table format: step, pop,id,zone,number
    }

    @Override
    public void export(SimulationStorage simulation, Writer out) throws Exception {
		out.write("step;population;group;zone;value\n");
        TimeStep lastStep = simulation.getResultStorage().getLastStep();
        for (Population pop : simulation.getParameter().getPopulations()) {
			for (TimeStep step = new TimeStep(0); !step.after(lastStep); step = step
						.next()) {
				MatrixND mat = simulation.getResultStorage().getMatrix(step, pop, MatrixAbundanceBeginMonth.NAME);
				if(mat!=null){
				//mat = mat.sumOverDim(1).reduceDims(1); // zones
				for (MatrixIterator i = mat.iterator(); i.hasNext();) {
					i.next();
					Object[] sems = i.getSemanticsCoordinates();
					PopulationGroup group = (PopulationGroup) sems[0];
					Zone zone = (Zone) sems[1];
					double val = i.getValue();
					out.write(step.getStep() + ";" +pop.getName() + ";" + group.getId() + ";" +zone.getName() +";"+  val + "\n");
				}
			   }}
			}
    }
}
