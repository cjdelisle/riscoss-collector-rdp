package eu.riscoss.dataproviders.providers;

/**
 * @author Mirko Morandini
 */

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import eu.riscoss.dataproviders.common.IndicatorsMap;

public class MavenLicensesProvider implements AbstractDataProvider {

	public HashMap<String, Integer> analyseReport(String target, String licenseFile) throws IOException {

		Document document;

		if (target.startsWith("http")) {
			document = Jsoup.connect(target).get();
		} else {
			File file = new File(target);
			document = Jsoup.parse(file, "UTF-8", "http://localhost");
		}

		List<LicenseEntry> llist = new ArrayList<LicenseEntry>(); // list of licenses in the fossology file

		for (Element table : document.select("table[class=bodyTable]")) { // gets all license tables
			if (table.select(":contains(GroupId)").size() > 0) {
				for (Element row : table.select("tr:gt(0)")) { // tr except first
					Elements tds = row.select("td:eq(4)"); // 5th column

					if (tds.select("a").size() > 0) {

						String licenseString = tds.text();

						Element link = tds.select("a").get(0);
						String licenseLink = link.attr("href");
						licenseString = correctLicenseString(licenseString);

						// xDEBUG System.out.println(licenseLink+"    "+licenseString); //INFO: link not used
						// for now!

						llist.add(new LicenseEntry(1, licenseString)); // double entries permitted here
					}
					// System.out.println(tds.select("a[href]").toString());
					// System.out.println(tds.get(0).text());// + "->" + tds.get(1).text());
				}
				// xDebug System.out.println("Fin Table\n");
			}
			// break;

		}
		// analysis////////////////////
		HashMap<String, Integer> licenseBuckets = populateLicenseBuckets(licenseFile, llist);

		// for (String license : result) {
		// System.out.format("%s\n", license);
		// }
		return licenseBuckets;
	}

	/**
	 * @param licenseFile
	 * @param llist
	 * @param licenseBuckets
	 * @throws IOException
	 */
	private HashMap<String, Integer> populateLicenseBuckets(String licenseFile, List<LicenseEntry> llist)
		throws IOException {
		int total = 0;

		HashMap<String, Integer> licenseBuckets = new HashMap<String, Integer>();

		// uses the same fossology license data, parsed from the LicensesCfg config file:
		HashMap<String, Collection<String>> licensesMap = FossologyDataProvider.parseLicensesFile(licenseFile);

		for (String licensetype : licensesMap.keySet()) {
			// initiates the bucket list with every type
			licenseBuckets.put(licensetype, 0);
		}

		boolean matched = false;
		int numUnknown = 0;
		for (LicenseEntry le : llist) {
			for (String licenseType : licensesMap.keySet()) {// cycles on license types from the config file
				if (le.matchesOneOf_Maven(licensesMap.get(licenseType), licenseType)) {
					Integer currentcount = licenseBuckets.get(le.licensetype);
					if (currentcount == null) // for safety, but should be initialised
						currentcount = 0;
					licenseBuckets.put(le.licensetype, currentcount + le.count);
					// xDebug System.out.println(le.getName()+": "+le.licensetype);
					matched = true;
				}
			}
			total += le.count;
			if (matched == false) { // unknown
				numUnknown += le.count;
				System.out.println("Unknown license: " + le.getName());
			}
		}

		licenseBuckets.put("_unknown_", numUnknown);
		licenseBuckets.put("_sum_", total);
		licenseBuckets.put("_count_", llist.size());

		System.out.println("\nLicense Buckets Maven:");
		System.out.println(licenseBuckets);

		return licenseBuckets;
	}

	/**
	 * Correct the various, difficultly parseable Maven license strings, replacing them with the short form
	 * 
	 * @param label
	 * @return corrected label
	 */
	private String correctLicenseString(String label) {
		// Collection<String> parts = Arrays.asList(href.split("/"));
		String result;
		result = label.replaceFirst("(?i)" + "lesser general public license", "LGPL");
		result = result.replaceFirst("(?i)" + "general public license", "GPL"); // seq. important
		return result;
	}

	/**
	 * TODO: adapt to Maven particularities
	 */
	public void createIndicators(IndicatorsMap im, Properties properties) throws IOException {
		
		String licenseFile = properties.getProperty("licenseFile");
		String target = properties.getProperty("targetMaven");
		
		HashMap<String, Integer> licenseBuckets = analyseReport(target, licenseFile);

		// add all measures to the IndicatorsMap (= Risk Data)
		for (String licenseBucket : licenseBuckets.keySet()) {
			im.add("Measure_Maven." + licenseBucket, licenseBuckets.get(licenseBucket));
		}

		float total = licenseBuckets.get("_sum_"); // to make sure that the division result is a float
		Integer licenseCount = licenseBuckets.get("_count_");
		Integer numPermissive = licenseBuckets.get("Permissive License");
		Integer numCopyleft = licenseBuckets.get("FSF Copyleft");
		Integer numNoLicense = licenseBuckets.get("No License");
		Integer numUnknown = licenseBuckets.get("_unknown_");
		Integer numLinkingPermitted = licenseBuckets.get("FSF linking permitted");

		im.add("i103a_M", licenseCount); // Number of component licenses
		im.add("i103b_M", numNoLicense / total); // % of files without license (Fossology)
		im.add("i103c_M", numUnknown / total); // % of files with unclear/unknown license
															// (Fossology)
		im.add("i91_M", numCopyleft / total); // % of licenses: viral (Fossology)
		im.add("i93f_M", numLinkingPermitted / total); // % of licenses: library viral (Fossology)
		im.add("i93g_M", numPermissive / total); // % of licenses: without constraints (Fossology)

		// i93b" label="Amount of OSS code integrated"
		// i93c" label="Technique used for integrating code (static/dynamic linking, copy)"
		// i93d" label="Type of licenses in core components"
		// i93h" label="Amount of component code imported/linked from other OSS projects"
		// i120" label="Percentage of US code"
	}

	
}
