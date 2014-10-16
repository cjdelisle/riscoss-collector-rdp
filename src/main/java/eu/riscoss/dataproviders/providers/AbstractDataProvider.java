package eu.riscoss.dataproviders.providers;

/**
 * @author Mirko Morandini
 */

import java.io.IOException;
import java.util.Properties;
import eu.riscoss.dataproviders.common.IndicatorsMap;

public interface AbstractDataProvider {
	
	
//	abstract HashMap<String, Integer> analyseReport(String target, String configFile) throws IOException;
	
	abstract void createIndicators(IndicatorsMap im, Properties properties) throws IOException;

}