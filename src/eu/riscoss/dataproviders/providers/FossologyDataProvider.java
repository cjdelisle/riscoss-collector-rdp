package eu.riscoss.dataproviders.providers;

/**
 * @author Mirko Morandini, Fabio Mancinelli
 */

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import eu.riscoss.dataproviders.common.IndicatorsMap;

public class FossologyDataProvider implements AbstractDataProvider {

	/**
	 * Parses a LicensesCfg file
	 * @param target
	 * @return HashMap: License Types, each with a Collection of Licenses
	 * @throws IOException
	 */
	protected static HashMap<String, Collection<String>> parseLicensesFile (String target) throws IOException {
		HashMap<String, Collection<String>> result = new HashMap<String, Collection<String>>();
		Document document;
		if (target.startsWith("http")) {
			document = Jsoup.connect(target).get();
		} else {
			File file = new File(target);
			System.out.println("Fossology config file used: "+file.getCanonicalPath());
			document = Jsoup.parse(file, "UTF-8", "http://localhost");
		}

		//    	 System.out.println(document.outerHtml());

		Elements licensesLinks = document.getElementsByAttribute("id");

		for (Element element : licensesLinks) {
			String licenseName = element.child(0).text();
			if (element.children().size() >1) {
				String s = element.child(1).text();
				Collection<String> licensesList = Arrays.asList(s.split("\\s*\\|\\s*")); //("\\s*\\|\\s*"));

//xDebug				System.out.println("Analysed license type: "+licenseName+": "+licensesList);
				result.put(licenseName, licensesList);
			}
		}
		//    	StringBuilder xmlContent = new StringBuilder();
		//      xmlContent.append(document.body().html());
		//      FileWriter fileWriter = new FileWriter("C:/licenses.html");
		//      BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
		//      bufferedWriter.write(xmlContent.toString());
		//      bufferedWriter.close();
		return result;
	}

	/**
	 * Analyses a fossology html file
	 * @param target
	 * @param licenseFile
	 * @return
	 * @throws IOException
	 */
	public HashMap<String, Integer> analyseReport(String target, String licenseFile) throws IOException {
		//private static HashMap<String, Integer> analyseFossologyReport(String target, String licenseFile) throws IOException {
		//        List<String> result = new ArrayList<String>();
		Document document;

		if (target.startsWith("http")) {
			document = Jsoup.connect(target).get();
		} else {
			File file = new File(target);
			document = Jsoup.parse(file, "UTF-8", "http://localhost");
		}

		Element table = document.select("table[id=lichistogram]").first();
		Elements rows = table.select("tr");

		List<LicenseEntry> llist= new ArrayList<LicenseEntry>(); //list of licenses in the fossology file

		//for each license, parses the name (0) and the number of occurrences (2) and saves it as a LicenseEntry
		for (Element element : rows) {
			Elements col= element.select("td");

			if (col.size()!=0) {
				int c=Integer.parseInt(col.get(0).ownText());//num of occurrences
				String lic=col.get(2).text();
				llist.add(new LicenseEntry(c,lic));
				//mlist.put(lic, c);
			}
			//        	System.out.println(col.get(1).ownText());
			//        	Element count=col.get(0);
		}

		//get license type buckets

		HashMap<String, Integer> licenseBuckets = new HashMap<String, Integer>();
		int total=0;

		HashMap<String, Collection<String>> licensesMap = parseLicensesFile(licenseFile);
		Set<String> licenseTypes = licensesMap.keySet();
		//initialize with 0 to avoid missing types
		for (String licensetype : licenseTypes) {
			licenseBuckets.put(licensetype, 0);
		}

		boolean matched = false;
		int numUnknown = 0;
		for (LicenseEntry le : llist) {
			for (String licenseType : licenseTypes) {//cycles on license types from config file
				if (le.matchesOneOf(licensesMap.get(licenseType), licenseType)) {
					Integer currentcount=licenseBuckets.get(le.licensetype);
					if (currentcount==null) //for safety, but should be initialised
						currentcount=0;
					licenseBuckets.put(le.licensetype, currentcount+le.count);
					matched = true;
				}
			}
			total+=le.count;
			if (matched==false) { //unknown
				numUnknown+=le.count;
				System.out.println("Unknown license: " +le.getName());
			}
		}

		licenseBuckets.put("_unknown_", numUnknown);
		licenseBuckets.put("_sum_", total);
		licenseBuckets.put("_count_", llist.size());
		
		System.out.println("\nLicense Buckets Fossology:");
		System.out.println(licenseBuckets);

		//        for (String license : result) {
		//            System.out.format("%s\n", license);
		//        }
		return licenseBuckets;
	}

	/**
	 * 
	 * @param targetFosslolgy
	 * @param licenseFile
	 * @throws IOException
	 */

	public void createIndicators(IndicatorsMap im, Properties properties) throws IOException {
		//private static void createIndicatorsFromFossologyMeasures(String targetFosslolgy, String licenseFile) throws IOException {
		
		String licenseFile = properties.getProperty("licenseFile");
		String targetFosslolgy = properties.getProperty("targetFossology");
		
		HashMap<String, Integer> licenseBuckets = analyseReport(targetFosslolgy, licenseFile);


		//add all measures to the IndicatorsMap (= Risk Data)
		boolean addAll = false;
		if (addAll)
			for (String licenseBucket : licenseBuckets.keySet()) {
				im.add("Measure_Fossology."+licenseBucket, licenseBuckets.get(licenseBucket));
			}

		float total =  licenseBuckets.get("_sum_"); //to make sure that the division result is a float
		Integer licenseCount =  licenseBuckets.get("_count_");
		Integer numPermissive = licenseBuckets.get("Permissive License");
		Integer numCopyleft =  licenseBuckets.get("FSF Copyleft");
		Integer numNoLicense =  licenseBuckets.get("No License");
		Integer numUnknown =  licenseBuckets.get("_unknown_");
		Integer numLinkingPermitted =  licenseBuckets.get("FSF linking permitted");
		Integer numCommercial =  licenseBuckets.get("Commercial license");

		im.add("number-of-different-licenses", licenseCount); //Number of (different?) component licenses
		im.add("percentage-of-files-without-license", numNoLicense/total); //% of files without license (Fossology)
		im.add("files-with-unknown-license", numUnknown/total); //% of files with unclear/unknown license (Fossology)
		im.add("copyleft-licenses", numCopyleft/total); //% of licenses: viral (Fossology)
		im.add("copyleft-licenses-with-linking", numLinkingPermitted/total); //% of licenses: library viral (Fossology)
		im.add("percentage-of-files-with-permissive-license", numPermissive/total); //% of licenses: without constraints (Fossology)
		im.add("files-with-commercial-license",numCommercial/total); //% of licenses: commercial (Fossology)

		//TODO
		im.add("percentage-of-files-with-public-domain-license",0);
		//TODO
		im.add("files-with-ads-required-liceses",0);
		
		//    	i93b" label="Amount of OSS code integrated"
		//    	i93c" label="Technique used for integrating code (static/dynamic linking, copy)"
		//    	i93d" label="Type of licenses in core components"
		//    	i93h" label="Amount of component code imported/linked from other OSS projects"
		//    	i120" label="Percentage of US code"

		//System.out.println(IndicatorsMap.get().toString());
	}


	//  public static void main(String[] args) throws Exception {
	//      Options options = new Options();
	//      CommandLineParser parser = new GnuParser();
	//      CommandLine cmd = parser.parse(options, args);
	//
	//      List<String> argList = cmd.getArgList();
	//
	//      String targetFosslolgy;
	//      String licenseFile = "./input/LicensesCfg.html";
	//
	//      if (argList.size() > 0) {
	//      	targetFosslolgy = argList.get(0);
	//      	if (argList.size() > 1)
	//      		licenseFile = argList.get(1);
	//      } else {
	//      	System.out.println("I take the default file");
	////      	targetFosslolgy="http://fossology.ow2.org/?mod=nomoslicense&upload=38&item=292002";
	//      	targetFosslolgy="./input/Bonita_Fossology.html";
	////        System.out.format("Please specify a report file");
	//      }
	//
	//      HashMap<String, Object> indicatorsMasp = new HashMap<String, Object>();
	//      IndicatorsMap im = IndicatorsMap.create();
	//
	//      createIndicators(targetFosslolgy, licenseFile);
	//
	//  }

}

// from the legal risk model xml:
//		<indicator id="i103a" label="Number of component licenses (Fossology)" datatype="integer"/>
//		<indicator id="i103b" label="Amount of files without license (Fossology)" datatype="real"/>
//		<indicator id="i103c" label="Amount of files with unclear/unknown license (Fossology)" datatype="real"/>
//		<indicator id="i91" label="Amount of licenses: viral (Fossology)" datatype="real"/>
//		<indicator id="i93f" label="Amount of licenses: library viral (Fossology)" datatype="real"/>
//		<indicator id="i93g" label="Amount of licenses: without constraints (Fossology)" datatype="real"/>
//		<indicator id="i93b" label="Amount of OSS code integrated" datatype="real"/>
//		<indicator id="i93c" label="Technique used for integrating code (static/dynamic linking, copy)" datatype="integer"/>
//		<indicator id="i93d" label="Type of licenses in core components" datatype="String"/>
//		<indicator id="i93h" label="Amount of component code imported/linked from other OSS projects" datatype="real"/>
//		<indicator id="i120" label="Percentage of US code" datatype="integer"/>

//<situation id="s1" label="License virality" threshold="0.5"/> <!-- i91, i93f, i93g, i93c-->
//<situation id="s2" label="License compatibility" threshold="0.5"/> <!-- i93d, i103c -->
//<situation id="s3" label="License uncertainty" threshold="0.5"/> <!-- i103b, i103c, -i93g, -->
//<situation id="s4" label="Code problematicity" threshold="0.5"/> <!-- i103a, i93b, i93h -->
//<situation id="s5" label="Availability and verifiability of information on ownership and quality assurance" threshold="0.5"/> <!-- i93d, i93h -->
//<situation id="s6" label="Percentage of US code" threshold="10"/> <!-- i120 -->

