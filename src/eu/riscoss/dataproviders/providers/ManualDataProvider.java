package eu.riscoss.dataproviders.providers;

/**
 * @author Mirko Morandini
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import eu.riscoss.dataproviders.common.IndicatorsMap;
import eu.riscoss.rdr.model.Distribution;
import eu.riscoss.rdr.model.RiskDataType;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class ManualDataProvider implements AbstractDataProvider {

	/**
	 * Reads an XML file and creates indicators for the target entity.
	 * Name: indicator name
	 * EntityName: name of the target component
	 * Value: indicator value (Double)
	 * Values: indicator values as a "density", i.e. slots that sum to 1. If they do not sum to 1 they are normalised here.
	 * 
	 * @param im
	 * @param properties
	 *            property Indicators_XML: the file name needs Sonar_host and Sonar_resourceKey
	 */
	public void createIndicators(IndicatorsMap im, Properties properties) {

		String xmlFile = properties.getProperty("Indicators_XML");
		String targetEntity = im.getTargetEntity();

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();

			File f = new File(xmlFile);
			if (f.exists()) {

				Document document = db.parse(new FileInputStream(xmlFile));

				System.out.println("Parsing file " + xmlFile + " for entity " + targetEntity);

				NodeList descNodes = document.getElementsByTagName("entity");
				
				int entries = 0;

				for (int i = 0; i < descNodes.getLength(); i++) {

					NamedNodeMap attrList;
					// TODO error handling
					
					if ((attrList = descNodes.item(i).getAttributes()) != null) {
						// if name of entity the searched one

						if (!attrList.getNamedItem("name").getNodeValue().equalsIgnoreCase(targetEntity))
							continue;

						NodeList indicators = descNodes.item(i).getChildNodes();
						
						for (int j = 0; j < indicators.getLength(); j++) {

							if (!indicators.item(j).getNodeName().equals("indicator"))
								continue;

							boolean isDistribution = false;
							
							NamedNodeMap indAttr = indicators.item(j).getAttributes();

							String indicatorName = "";
							Double indicatorValue = 0.0;
							Double[] distributionValues=null;
							
							for (int k = 0; k < indAttr.getLength(); k++) {
								String nn = indAttr.item(k).getNodeName();
								if (nn.equalsIgnoreCase("name"))
									indicatorName = indAttr.item(k).getNodeValue();
								
								if (nn.equalsIgnoreCase("value")) {
									String indicatorValueStr = indAttr.item(k).getNodeValue();
									indicatorValue = Double.parseDouble(indicatorValueStr);
								
								} else if (nn.equalsIgnoreCase("values")) { //distributon, needs to sum to 1!
									isDistribution = true;
									String indicatorValueStr = indAttr.item(k).getNodeValue();

									String[] singleValues = indicatorValueStr.split("\\s*\\,\\s*");
									distributionValues = new Double[singleValues.length];

									double total=0;
									for (int y=0; y<singleValues.length; y++) {
										String s = singleValues[y];
										distributionValues[y] = Double.parseDouble(singleValues[y]);
										total += distributionValues[y];
									}
									//normalize to 0..1 as requested in a distribution
									for  (int y=0; y<singleValues.length; y++){
										distributionValues[y]/=total;
									}
								}
							}
							
							if (isDistribution)
								im.add(indicatorName, RiskDataType.DISTRIBUTION, new Distribution(distributionValues));
							else
								im.add(indicatorName, indicatorValue);
							
							entries  ++;

						}

					}
				}
				System.out.println(entries +" entries found for " + targetEntity);
				
			} else System.err.println("Warning[ManualDataProvider]: " + xmlFile + " does not exist.");
		} catch (DOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
