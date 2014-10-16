package eu.riscoss.dataproviders.common;

/**
 * @author Mirko Morandini
 */

import java.io.IOException;
import java.util.Properties;
import java.util.TreeMap;
import eu.riscoss.dataproviders.providers.AbstractDataProvider;


public class DataProviderManager {
	
	private static IndicatorsMap im;
	private static Properties properties;
	private static TreeMap<Integer,AbstractDataProvider> dataProviders = new TreeMap<Integer,AbstractDataProvider>();
	
	public static void init(IndicatorsMap im, Properties properties) {
		DataProviderManager.im=im;
		DataProviderManager.properties = properties;
	}

	public static void register(Integer i, AbstractDataProvider dataProvider) {
		dataProviders.put(i, dataProvider);
	}

	public static void execAll() {
		for (Integer i : dataProviders.keySet()) {
			exec(i);
		}
	}

	public static boolean exec(Integer i) {
		if (dataProviders.containsKey(i)) {
			try {
				System.out.println("**** "+ dataProviders.get(i).getClass().getName() +" ****");
				dataProviders.get(i).createIndicators(im, properties);
			} catch (IOException e) {
				System.err.println("Error with data provider "+ dataProviders.get(i).getClass().getName());
				e.printStackTrace();
			}
			return true;
		}
		return false;
	}

//	public static void main(String[] args) throws Exception {
//		Options options = new Options();
//		CommandLineParser parser = new GnuParser();
//		CommandLine cmd = parser.parse(options, args);
//
//		List<String> argList = cmd.getArgList();
//
//		String targetFosslolgy = "./input/Bonita_Fossology.html";
//		String targetMaven = "./input/dependencies.html";
//		String licenseFile = "./input/LicensesCfg.html";
//
//		if (argList.size() > 0) {
//			targetFosslolgy = argList.get(0);
//			if (argList.size() > 1)
//				licenseFile = argList.get(1);
//		} else {
//			System.out.println("I take the default files");
//			// targetFosslolgy="http://fossology.ow2.org/?mod=nomoslicense&upload=38&item=292002";
//
//			// System.out.format("Please specify a report file");
//		}
//
//		// initialize the indicators map
////		IndicatorsMap im = new IndicatorsMap("xwiki");
////
////		AbstractDataProvider fossologyProvider = new FossologyDataProvider();
////		fossologyProvider.createIndicators(targetFosslolgy, licenseFile);
////
////		System.out.println("*************************************************");
////
////		AbstractDataProvider mavenLicensesProvider = new MavenLicensesProvider();
////		mavenLicensesProvider.createIndicators(targetMaven, licenseFile);
////		System.out.println("*************************************************");
////		System.out.println("\nResulting indicators:");
////		System.out.println(IndicatorsMap.get().toString());
//	}

}
