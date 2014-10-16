package eu.riscoss.dataproviders;

/**
 * @author Mirko Morandini
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import eu.riscoss.dataproviders.common.DataProviderManager;
import eu.riscoss.dataproviders.common.IndicatorsMap;
import eu.riscoss.dataproviders.providers.FossologyDataProvider;
import eu.riscoss.dataproviders.providers.ManualDataProvider;
import eu.riscoss.dataproviders.providers.MarkmailDataProvider;
import eu.riscoss.dataproviders.providers.MavenLicensesProvider;
import eu.riscoss.dataproviders.providers.jira.JiraDataProvider;
import eu.riscoss.dataproviders.providers.sonar.SonarDataProvider;
import eu.riscoss.dataproviders.restAPI.RDR;
import eu.riscoss.rdr.model.RiskData;

public class Main
{
	private static final boolean sendResults = true; //false;
	private static final boolean writeProperties = false; //false;
	private static final String defaultPropertiesFile = "default.properties";

	//	private static final String propertiesFile = "Riscossconfig_XWiki.properties";

	private static Properties properties;

	//just for a faster testing in Eclipse ;)
	public static void main(String[] args)
	{
		List<String> components = new ArrayList<String>();
		components.add("XWiki");
		components.add("WebLab");
		components.add("SAT4J");
		components.add("Easybeans");
		components.add("SpagoBI");
		
//		components.add("ASM");
//		components.add("test");
		
		String[] s = new String[4];
		s[0] = "-rdr";
		s[1] = "http://riscoss-platform.devxwiki.com/rdr";
		s[2] = "-properties";
		
		for (String component : components) {
			s[3] = "Riscossconfig_"+component+".properties";
			exec_single(s);
		}
		
	}
	
	//main
	public static void exec_single(String[] args)
	{

		Options options = new Options();

		/* These two options are mandatory for the Risk Data Collector */
		Option rdr =
			OptionBuilder.withArgName("url").hasArg().withDescription("Risk Data Repository URL").create("rdr");
		//		Option entity = OptionBuilder.withArgName("entityId").hasArg().withDescription("Entity ID (OSS name)").create("entity");
		Option entity = OptionBuilder.withArgName("properties").hasArg().withDescription("Properties File (with component-related config)").create("properties");
		options.addOption(rdr);
		options.addOption(entity);

		/* Print help if no arguments are specified. */
		if (args.length == 0) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("rdc-template", options);
			System.exit(1);
		}

		/* Parse the command line */
		CommandLineParser parser = new GnuParser();
		CommandLine cmd;
		try {
			cmd = parser.parse(options, args);

			String riskDataRepositoryURL = cmd.getOptionValue("rdr");
			if (riskDataRepositoryURL == null) {
				System.err.format("Risk data repository not specified.");
				System.exit(1);
			}

			//	the name of the OSS to analyse
			String propertiesFile = cmd.getOptionValue("properties");
			if (propertiesFile == null) {
				System.err.format("Properties file not specified.");
				System.exit(1);
			}

			/* Initialisation here, will be passed by parameters or config file */
			//      targetEntity = "XWiki"; //(should be parameter)

			if (writeProperties)
				//creates a new, hard-coded config
				properties = RdpConfig.loadWrite(propertiesFile);
			else {
				//read the default config file
				Properties defaultProperties = RdpConfig.loadDefaults(defaultPropertiesFile);
				//read the config from file
				properties = RdpConfig.load(propertiesFile, defaultProperties);
			}
			String targetEntity = properties.getProperty("targetEntity");
			
			System.out.println();
			System.out.println("************************************************");
			System.out.printf("Starting the analysis for component %s.\n\n",targetEntity);
			
			IndicatorsMap im = new IndicatorsMap(targetEntity);

			/* Risk Data collector main logic called here */
			DataProviderManager.init(im, properties);

			DataProviderManager.register(1, new FossologyDataProvider());
			DataProviderManager.register(2, new MavenLicensesProvider());
			DataProviderManager.register(3, new MarkmailDataProvider());
			DataProviderManager.register(4, new JiraDataProvider());
			DataProviderManager.register(5, new SonarDataProvider());
			DataProviderManager.register(0, new ManualDataProvider());

//			DataProviderManager.execAll();
			DataProviderManager.exec(5);
//			DataProviderManager.exec(4);
			
			
			System.out.println("\n**** Resulting indicators ****" + im);
			System.out.flush();
			/******************************************************/

			/*
			 * At the end, send the result to the Risk Data Repository
			 * Example repository: http://riscoss-platform.devxwiki.com/rdr/xwiki?limit=10000
			 */
			
			if (sendResults) {
				//put all the indicators into the riskData List
				List<RiskData> riskData = new ArrayList<RiskData>(); /* This list should be generated by the Risk Data Collector logic :) */
				riskData.addAll(im.values());
				//riskData.add(RiskDataFactory.createRiskData("iop", targetEntity, new Date(), RiskDataType.NUMBER, 1.0));
				try {
					RDR.sendRiskData(riskDataRepositoryURL, riskData);
					System.out.println("\nIndicators sent via REST to "+riskDataRepositoryURL+"/" + targetEntity);
				} catch (Exception e) {
					System.err.print("Warning: Not able to send inicators via REST to "+riskDataRepositoryURL);
					e.printStackTrace();
					//System.err.println(" Exception: "+e.getClass().getName());
				}
			}
		}catch (ParseException e1) {
			System.err.println("Error in parsing command line arguments. Exiting.");
			e1.printStackTrace();
			System.exit(1);
		}
	}
}
