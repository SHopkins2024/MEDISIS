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
package exports;

import fr.ifremer.isisfish.datastore.SimulationStorage;
import fr.ifremer.isisfish.entities.Metier;
import fr.ifremer.isisfish.entities.Population;
import fr.ifremer.isisfish.entities.Strategy;
import fr.ifremer.isisfish.export.Export;
import fr.ifremer.isisfish.types.TimeStep;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuiton.math.matrix.MatrixIterator;
import org.nuiton.math.matrix.MatrixND;
//import scripts.ResultName;
import resultinfos.MatrixCatchWeightPerStrategyMetPerZonePop;
import java.io.Writer;

/**
 * CatchWeightSpStrYear_t.java
 * 
 * Export des captures en poids de la forme :
 * Population ; StratÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©gie ; MÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©tier ; Groupe ; Zone ; Pas de temps ; Valeur
 *
 * @author anonymous <anonymous@labs.libre-entreprise.org>
 * @version $Revision: 1.4 $
 * --------------------------------------------------------------------------------------------------------------------------------------------------------------------
 * Export catch weight per Population, Strategy,  métier, Age group, zone, time step, value
 * 
 * Last update: $Date: 2007-05-24 09:30:07 $ by : $Author: bpoussin $
 */
public class CatchWeightSpStrYear_t implements Export {

    /** to use log facility, just put in your code: log.info("..."); */
    static private Log log = LogFactory.getLog(CatchWeightSpStrYear_t.class);

    protected String[] necessaryResult = {
        MatrixCatchWeightPerStrategyMetPerZonePop.NAME
    };

    @Override
    public String[] getNecessaryResult() {
        return this.necessaryResult;
    }

    @Override
    public String getExportFilename() {
        return "CatchWeightSpStrYear";
    }

    @Override
    public String getExtensionFilename() {
        return ".csv";
    }

    @Override
    public String getDescription() {
        return "Export les valeurs debarquees de la simulation. tableau pop;strategie;metier;nombre";
        //Export the catch weight removed from population during the simulation. Table format: pop,strategy,metier,number

    }

    @Override
    public void export(SimulationStorage simulation, Writer out)
            throws Exception {
        out.write("population;strategy;year;value\n");
        for (Population pop : simulation.getParameter().getPopulations()) {
				MatrixND mat = simulation.getResultStorage().getMatrix(pop,MatrixCatchWeightPerStrategyMetPerZonePop.NAME); // step, str, met, pop
            if (mat != null) { // can be null if simulation is stopped before last year simulation
                    mat = mat.sumOverDim(0,12); //sum per year
					mat = mat.sumOverDim(2); //sum on metier
					mat = mat.sumOverDim(3); //sum on group
					mat = mat.sumOverDim(4); //sum on zones
                    mat = mat.reduce();					
                    for (MatrixIterator i = mat.iterator(); i.hasNext();) {
                        i.next();
                        Object[] sems = i.getSemanticsCoordinates();
                        Strategy strategy = (Strategy) sems[1];
						int year = (int) sems[0];
                        double val = i.getValue();
                        out.write(pop.getName() + ";" + strategy.getName() + ";"
                               + year + ";"
                                + val + "\n");
                    }
                
            }
		}
    }
}
