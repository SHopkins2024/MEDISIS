package exports;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Writer;

import org.nuiton.math.matrix.*;

import fr.ifremer.isisfish.entities.*;
import fr.ifremer.isisfish.export.ExportStep;
import fr.ifremer.isisfish.types.TimeStep;
import resultinfos.MatrixBiomass;
import resultinfos.MatrixBiomassBeginMonth;
import fr.ifremer.isisfish.datastore.SimulationStorage;

/**
 * BiomasseBeginMonth_t.java
 */
public class BiomasseBeginMonth_t implements ExportStep {

    /** to use log facility, just put in your code: log.info("..."); */
    static private Log log = LogFactory.getLog(BiomasseBeginMonth_t.class);

    protected String[] necessaryResult = {
        MatrixBiomassBeginMonth.NAME
    };

    @Override
    public String[] getNecessaryResult() {
        return this.necessaryResult;
    }

    @Override
    public String getExportFilename() {
        return "BiomasseBeginMonth";
    }

    @Override
    public String getExtensionFilename() {
        return ".csv";
    }

    @Override
    public String getDescription() {
        return "Exporte les biomasses de début de mois en nombre tableau avec des lignes step;pop;id;zone;nombre.";
        //Export the biomass at the start of the month in table format: step, pop,id,zone,number
    }


    @Override
    public void exportBegin(SimulationStorage simulation, Writer out) throws Exception {
        out.write("step;population;group;zone;value\n");
    }

    @Override
    public void export(SimulationStorage simulation, TimeStep step, Writer out) throws Exception {
        for (Population pop : simulation.getParameter().getPopulations()) {
            MatrixND mat = simulation.getResultStorage().getMatrix(step, pop, MatrixBiomassBeginMonth.NAME);
            for (MatrixIterator i = mat.iterator(); i.hasNext();) {
                i.next();
                Object[] sems = i.getSemanticsCoordinates();
                PopulationGroup group = (PopulationGroup) sems[0];
                Zone zone = (Zone) sems[1];

                double val = i.getValue();
                out.write(step.getStep() + ";" + pop.getName() + ";" + group.getId() + ";" + zone.getName() + ";" + val + "\n");
            }
        }
    }

    @Override
    public void exportEnd(SimulationStorage simulation, Writer out) throws Exception {

    }
}
