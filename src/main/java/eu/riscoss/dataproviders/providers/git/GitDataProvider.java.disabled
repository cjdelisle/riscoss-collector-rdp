package eu.riscoss.dataproviders.providers.git;

/**
 * @author Mirko Morandini, Fabio Mancinelli
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;


//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class GitDataProvider implements RiskDataProvider {

	//    protected static final Logger LOGGER = LoggerFactory.getLogger(SonarDataProvider.class);

	protected final String gitPath;
	protected final String bashPath;
	protected final String awkPath;

	public static class GitLogStatistics
	{
		public int commits;

		public int filesChanged;

		public int linesAdded;

		public int linesRemoved;
	}


	public GitDataProvider(String gitPath, String bashPath, String awkPath)
	{
		if (gitPath == null) {
			throw new IllegalArgumentException("No git path provided");
		}

		if (bashPath == null) {
			throw new IllegalArgumentException("No bash path provided");
		}

		if (awkPath == null) {
			throw new IllegalArgumentException("No awk path provided");
		}

		this.gitPath = gitPath;
		this.bashPath = bashPath;
		this.awkPath = awkPath;

		//	        LOGGER.info(String.format("git measurements tool initialized with git:%s, bash:%s, awk:%s", gitPath, bashPath,
		//	                awkPath));
	}

	/**
	 * To be adapted to GIT: 
	 * extracts and stores Markmail indicators.
	 * @param configFile not used.
	 */
	public void createIndicators(IndicatorsMap im, Properties properties) throws IOException {
		
		Files.createTempDirectory
		File tempDir = new File(properties.getProperty("tempDir"));
		
		//		String repositoryURI = parameters.get(GitMeasurementsToolFactory.REPOSITORY_URI_PARAMETER);
		
		String repositoryURI = properties.getProperty("GitRepositoryURI")
		if (repositoryURI == null) {
			System.out.println("Repository URI is null.");
		}


		String repositoryName = new File(repositoryURI).getName();
		File destination = new File(tempDir, repositoryName);

		try {
			if (!destination.exists()) {
				cloneRepository(repositoryURI, destination);
			} else {
				updateRepository(destination);
			}

			GitMeasurementsTool.GitLogStatistics statistics = getStatistics(destination);

			if (statistics != null) {
				Measurement measurement = new Measurement();
				measurement.setScope(scope);
				measurement.setType("files-changed-per-commit");
				measurement.setValue(
					String.format("%.2f", (double) statistics.filesChanged / (double) statistics.commits));
				riscossPlatform.storeMeasurement(measurement);

				measurement = new Measurement();
				measurement.setScope(scope);
				measurement.setType("lines-added-per-commit");
				measurement
				.setValue(String.format("%.2f", (double) statistics.linesAdded / (double) statistics.commits));
				riscossPlatform.storeMeasurement(measurement);

				measurement = new Measurement();
				measurement.setScope(scope);
				measurement.setType("lines-removed-per-commit");
				measurement.setValue(
					String.format("%.2f", (double) statistics.linesRemoved / (double) statistics.commits));
				riscossPlatform.storeMeasurement(measurement);

				/////////////////////////////

				try {
					GitDataProvider.MarkmailStatistics statistics = getStatistics(markmailURI);

					if (statistics != null) {
						//				im.add("MarkMail posts-per-day", String.format("%.2f", (double) statistics.postsPerDay)); //Number of component licenses
						im.add("MarkMail posts-per-day", statistics.postsPerDay); //Number of component licenses
						im.add("MarkMail total messages", statistics.totalMessages); //Number of component licenses
						im.add("MarkMail messages starting date", statistics.startingDate); //Number of component licenses
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
				System.out.println("PPD: "+totalMessages);

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
						System.out.println("PPD: "+totalMessages);
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
						System.out.println("TOTAL: "+totalMessages);
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
				System.out.println("DATE: "+startingDate);
				stats.totalMessages = Integer.parseInt(totalMessages);

				//		DateFormat df = DateFormat.getDateInstance(DateFormat.LONG, Locale.US);
				stats.startingDate = startingDate; //df.parse(startingDate);

				return stats;


			}
			
		    protected boolean cloneRepository(String repositoryURI, File destination) throws Exception
		    {
		        String[] cmd = {
		                gitPath,
		                "clone",
		                repositoryURI.toString(),
		                destination.toString()
		        };

		        Process p = Runtime.getRuntime().exec(cmd);
		        int result = p.waitFor();

		        return result == 0;
		    }

		    protected boolean updateRepository(File repository) throws Exception
		    {
		        String[] cmd = {
		                gitPath,
		                "pull",
		                repository.toString()
		        };

		        Process p = Runtime.getRuntime().exec(cmd);
		        int result = p.waitFor();

		        return result == 0;
		    }

		}
