package ch.vd.uniregctb.registrefoncier;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.springframework.util.ResourceUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.BusinessItTestApplication;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.document.RapprocherCtbRapport;
import ch.vd.uniregctb.rapport.RapportService;


public class RapprochementCtbTestApp extends BusinessItTestApplication {
	private static final Logger LOGGER = Logger.getLogger(RapprochementCtbTestApp.class);

	private final static String RAPPROCHEMENT_FILE = "rf_dev1000.csv";

	private RegistreFoncierService registreFoncierService;
	private RapportService rapportService;
	private StatusManager status;


	public static void main(String[] args) throws Exception {

		RapprochementCtbTestApp app = new RapprochementCtbTestApp();
		app.run();

		System.exit(0);
	}

	@Override
	protected void run() throws Exception {
		super.run();
		AuthenticationHelper.setPrincipal("[RapprochementCtbTestApp]");
		LOGGER.info("***** START RapprochementCtbTestApp Main *****");
		registreFoncierService = (RegistreFoncierService) context.getBean("registreFoncierService");
		rapportService = (RapportService) context.getBean("rapportService");
		status =  new JobStatusManager();


		LOGGER.info("==> chargement du fichier des proprio");

		byte[] byteFile = loadFile(RAPPROCHEMENT_FILE);
		List<ProprietaireFoncier> listeProprietaireFoncier = extractProprioFromCSV(byteFile, status);

		LOGGER.info("==> Rapprochement des ctb et proprio");

		final RapprocherCtbResults results = registreFoncierService.rapprocherCtbRegistreFoncier(listeProprietaireFoncier, status, RegDate.get());
		final RapprocherCtbRapport rapport = rapportService.generateRapport(results, status);

		AuthenticationHelper.resetAuthentication();


		LOGGER.info("***** END RapprochementCtbTestApp Main *****");
	}

	/**
	 * Extrait les ids d'un fichier CSV contenant des ids séparés par des virgules, des points-virgules ou des retours de ligne.
	 *
	 * @param csv
	 *            le contenu d'un fichier CSV
	 * @return une liste d'ids
	 */
	protected static List<ProprietaireFoncier> extractProprioFromCSV(byte[] csv, StatusManager status) {


		final List<ProprietaireFoncier> listeProprio = new ArrayList<ProprietaireFoncier>();
        final Pattern p = Pattern.compile("^([0-9]+);(.*?);(.*?);(.*?);(.*)");

		// on parse le fichier
        final String csvString = new String(csv);
		Scanner s = new Scanner(csvString);

        final String[] lines = csvString.split("[\n]");
        final int nombreProprio = lines.length;
        int proprioLu = 0;
        LOGGER.info("nombre de propriétaire "+nombreProprio);
		try {
			while (s.hasNextLine()) {

				final String line = s.nextLine();
				  //Audit.info("nombre de propriétaire tratés "+proprioLu);
                int percent = (proprioLu * 100) / nombreProprio;
                status.setMessage("Chargement des propriétaires fonciers",percent);
                if (status.interrupted()) {
                	break;
                }

				Matcher m = p.matcher(line);

				// on a un numero du registre foncier
				if (m.matches()) {

					Long numeroRegistreFoncier = null;
					String nom = null;
					String prenom = null;
					RegDate dateNaissance = null;
					Long numeroContribuable = null;

					// String[] tokens = line.split(";");

					numeroRegistreFoncier = Long.valueOf(m.group(1));

					nom = String.valueOf(m.group(2));
					prenom = String.valueOf(m.group(3));
					String stringDateNaissance = String.valueOf(m.group(4));
					//FIXME (BNM) Traiter les date de naissance null
					if (!"".equals(stringDateNaissance)) {
                      if(stringDateNaissance.contains("/")){
                       stringDateNaissance = stringDateNaissance.replace("/",".");
                       }

						try {
							dateNaissance = RegDateHelper.displayStringToRegDate(stringDateNaissance, true);
						}
						catch (Exception e) {
							LOGGER.error("Exception dan sla lecture de la date de naissance pour le proprio "+numeroRegistreFoncier+": " + e.getMessage());
						}
					}
					numeroContribuable = Long.valueOf(m.group(5));
					listeProprio.add(new ProprietaireFoncier(numeroRegistreFoncier, nom, prenom, dateNaissance, numeroContribuable));
                    proprioLu++;
				}

			}
		}
		finally {
			s.close();
		}

		return listeProprio;
	}

	protected byte[] loadFile(final String filename) throws Exception {

		File file = null;

		// Essaie d'abord tel-quel
		try {
			file = ResourceUtils.getFile(filename);
		}
		catch (Exception ignored) {
			// La variable file est nulle, ca nous suffit
		}

		// Ensuite avec classpath: devant
		if (file == null || !file.exists()) {
			try {
				String name = "classpath:" + filename;
				file = ResourceUtils.getFile(name);
			}
			catch (Exception ignored) {
				// La variable file est nulle, ca nous suffit
			}
		}

		// Ensuite avec classpath: et le chemin du package devant
		if (file == null || !file.exists()) {
			try {
				String packageName = getClass().getPackage().getName();
				packageName = packageName.replace('.', '/');

				String name = "classpath:" + packageName + "/" + filename;
				file = ResourceUtils.getFile(name);
			}
			catch (Exception ignored) {
				// La variable file est nulle, ca nous suffit
			}
		}

		InputStream is = new FileInputStream(file);
        System.out.println("\nDEBUG: FileInputStream is " + file);

        // Get the size of the file
        long length = file.length();



        // Create the byte array to hold the data
        byte[] bytes = new byte[(int)length];

        // Read in the bytes
        int offset = 0;
        int numRead = 0;
        while ( (offset < bytes.length)
                &&
                ( (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) ) {

            offset += numRead;

        }

        // Ensure all the bytes have been read in
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file " + file.getName());
        }

        is.close();
        return bytes;

	}

	public class JobStatusManager implements StatusManager {

		public JobStatusManager() {
		}

		@Override
		public synchronized boolean interrupted() {
			return false;
		}

		@Override
		public synchronized void setMessage(String msg) {

		}

		@Override
		public void setMessage(String msg, int percentProgression) {

		}
	}

}
