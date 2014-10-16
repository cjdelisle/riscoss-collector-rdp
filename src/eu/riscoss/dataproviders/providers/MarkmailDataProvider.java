package eu.riscoss.dataproviders.providers;

/**
 * @author Mirko Morandini, Fabio Mancinelli
 */

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import eu.riscoss.dataproviders.common.IndicatorsMap;
import eu.riscoss.rdr.model.Distribution;
import eu.riscoss.rdr.model.RiskDataType;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class MarkmailDataProvider implements AbstractDataProvider {

	//    protected static final Logger LOGGER = LoggerFactory.getLogger(ManualDataProvider.class);

	public static class MarkmailStatistics
	{
		public int postsPerDay;
		public int totalMessages;
		public String startingDate; //should be "Date" for being compareable
		public Distribution postsPerMonth_monthly = new Distribution();
	}

	/**
	 * extracts and stores Markmail indicators.
	 * @param configFile not used.
	 */
	public void createIndicators(IndicatorsMap im, Properties properties)  {

		String markmailURI = properties.getProperty("targetMarkmail");

		if (markmailURI == null) {
			//			LOGGER.error("Markmail URI is null.");
			System.out.println("Markmail URI is null.");
		}

		try {

			MarkmailStatistics statistics = getStatistics(markmailURI);

			if (statistics != null) {
				//				im.add("MarkMail posts-per-day", String.format("%.2f", (double) statistics.postsPerDay)); //Number of component licenses
				im.add("MarkMail posts-per-day", statistics.postsPerDay); //Number of component licenses
				im.add("MarkMail total messages", statistics.totalMessages); //Number of component licenses
				//TODO add DATE to RiskData
				//im.add("MarkMail messages starting date", statistics.startingDate); //Number of component licenses
				im.add("MarkMail posts-per-month monthly", RiskDataType.DISTRIBUTION, statistics.postsPerMonth_monthly);
				//				LOGGER.info(String.format("Analysis completed [%d]. Results stored", statistics.postsPerDay));
			}



		} catch (Exception e) {
			//			LOGGER.error(String.format("Error while executing analysis on %s", markmailURI), e);
			System.err.println("Error while executing analysis on " + markmailURI);
			e.printStackTrace();
		}
	}

	protected MarkmailStatistics getStatistics(String markmailURI) throws Exception
	{

		URL url;
		// get URL content
		// open the stream and put it into BufferedReader
		BufferedReader br;

		url = new URL(markmailURI);
		URLConnection conn = url.openConnection();

		br = new BufferedReader(new InputStreamReader(conn.getInputStream()));


		String inputLine;
		String postsPerDay = "";
		String totalMessages = "";
		String startingDate = "";

		while ((inputLine = br.readLine()) != null) {

			if (inputLine.contains(" messages per day")) {
				int endIndex = inputLine.indexOf(" messages per day");
				while (!Character.isDigit(inputLine.charAt(endIndex - 1))) {
					endIndex--;
				}
				int beginIndex = endIndex;
				while (Character.isDigit(inputLine.charAt(beginIndex - 1))) {
					beginIndex--;
				}
				postsPerDay = inputLine.substring(beginIndex, endIndex);
			}
			String s = " messages</strong>."; //total number of messages
			if (inputLine.contains(s)) {

				String content = "";
				int endIndex = inputLine.indexOf(s);
				while (!Character.isDigit(inputLine.charAt(endIndex - 1))) {
					endIndex--;
				}
				int beginIndex = endIndex;
				while (Character.isDigit(inputLine.charAt(beginIndex - 1))) { //remove 1000s commas
					content =  inputLine.charAt(beginIndex - 1) + content;
					beginIndex--;
					if ( inputLine.charAt(beginIndex - 1) == (',') || inputLine.charAt(beginIndex - 1) == ('.') )
						beginIndex--;
				}
				totalMessages = content; //inputLine.substring(beginIndex, endIndex);
			}

			s = "First list started in <strong>";
			if (inputLine.contains(s)) {
				int beginIndex = inputLine.indexOf(s) + s.length();
				int endIndex=beginIndex;
				while (Character.isLetter(inputLine.charAt(endIndex + 1))) { //Month
					endIndex++;
				}
				while (Character.isSpaceChar(inputLine.charAt(endIndex + 1))) {
					endIndex++;
				}
				while (Character.isDigit(inputLine.charAt(endIndex + 1))) { //Year
					endIndex++;
				}
				startingDate = inputLine.substring(beginIndex, endIndex+1);
			}

		}

		br.close();

		MarkmailStatistics stats = new MarkmailStatistics();

		stats.postsPerDay = Integer.parseInt(postsPerDay);
		stats.totalMessages = Integer.parseInt(totalMessages);
		//		DateFormat df = DateFormat.getDateInstance(DateFormat.LONG, Locale.US);
		stats.startingDate = startingDate; //df.parse(startingDate);
		stats.postsPerMonth_monthly.setValues(getPostsMonthly(markmailURI));

		return stats;

	}

	/**
	 * Retrieves the /graph.xqy data from Markmail graphs and reads out their monthly message number values.
	 * @param markmailURI
	 * @return
	 * @throws Exception
	 */
	private ArrayList<Double> getPostsMonthly(String markmailURI) throws Exception{

		ArrayList<Double> result = new ArrayList<Double>();
		
		URLConnection conn = new URL(markmailURI+"/graph.xqy").openConnection();
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		
		Document document = db.parse(conn.getInputStream()); //parseXML(conn.getInputStream());
	    
		
		
		NodeList descNodes = document.getElementsByTagName("data");
	
		for (int i=0;i<descNodes.getLength();i++) {
			
			NamedNodeMap attrList;
			
			//TODO error handling empty/notanumber
			if ((attrList = descNodes.item(i).getAttributes()) != null){
				String content = attrList.getNamedItem("value").getTextContent();
				result.add(Double.parseDouble(content));
			}
		}
		return result;
	}
	
	
}
