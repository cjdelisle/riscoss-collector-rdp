package eu.riscoss.dataproviders;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Iterator;
import org.json.JSONObject;
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
import org.apache.commons.io.IOUtils;

public class Provider
{
    private static Properties getDefaults()
    {
        Properties out = new Properties();
        out.put("JIRA_InitialDate", "2014/09/15");
        out.put("Sonar_singleMetrics", "ncloc, duplicated_lines_density, line_coverage, tests");
        out.put("Sonar_historyMetrics", "ncloc, comment_lines");
        out.put("Sonar_by_file_Metrics", "ncloc, complexity");
        out.put("licenseFile", "./input/LicensesCfg.html");
        out.put("Indicators_XML", "./input/Indicators.xml");
        return out;
    }

    public static void main(String[] args) throws Exception
    {
        String stdin = IOUtils.toString(System.in, "UTF-8");
        JSONObject input = new JSONObject(stdin);

        String targetEntity = input.getString("riscoss_targetName");

        //read the default config file
        Properties properties = getDefaults();
        for (Iterator it = input.keys(); it.hasNext();) {
            String k = (String) it.next();
            properties.put(k, ""+input.get(k));
        }

        System.err.println();
        System.err.println("************************************************");
        System.err.printf("Starting the analysis for component %s.\n\n",targetEntity);
            
        IndicatorsMap im = new IndicatorsMap(targetEntity);

        /* Risk Data collector main logic called here */
        DataProviderManager.init(im, properties);

        DataProviderManager.register(1, new FossologyDataProvider());
        DataProviderManager.register(2, new MavenLicensesProvider());
        DataProviderManager.register(3, new MarkmailDataProvider());
        DataProviderManager.register(4, new JiraDataProvider());
        DataProviderManager.register(5, new SonarDataProvider());
        DataProviderManager.register(0, new ManualDataProvider());

//        DataProviderManager.execAll();
        DataProviderManager.exec(5);
//        DataProviderManager.exec(4);

        System.err.println("\n**** Resulting indicators ****" + im);
        System.err.flush();

        String out = RDR.getRiskDataJson(new ArrayList<RiskData>(im.values())).toString();
        System.out.println("-----BEGIN RISK DATA-----");
        System.out.println(out);
        System.out.println("-----END RISK DATA-----");
    }
}
