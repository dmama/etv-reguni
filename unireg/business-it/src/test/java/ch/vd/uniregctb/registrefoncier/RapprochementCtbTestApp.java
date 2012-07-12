package ch.vd.uniregctb.registrefoncier;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.util.ResourceUtils;

import ch.vd.registre.base.date.RegDate;
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

		AuthenticationHelper.pushPrincipal("[RapprochementCtbTestApp]");
		try {
			LOGGER.info("***** START RapprochementCtbTestApp Main *****");
			registreFoncierService = (RegistreFoncierService) context.getBean("registreFoncierService");
			rapportService = (RapportService) context.getBean("rapportService");
			status =  new JobStatusManager();


			LOGGER.info("==> chargement du fichier des proprio");

			final byte[] byteFile = loadFile(RAPPROCHEMENT_FILE);
			final List<ProprietaireFoncier> listeProprietaireFoncier = RapprocherCtbRegistreFoncierJob.extractProprioFromCSV(byteFile, status);

			LOGGER.info("==> Rapprochement des ctb et proprio");

			final RapprocherCtbResults results = registreFoncierService.rapprocherCtbRegistreFoncier(listeProprietaireFoncier, status, RegDate.get(), 1);
			final RapprocherCtbRapport rapport = rapportService.generateRapport(results, status);

		}
		finally {
			AuthenticationHelper.popPrincipal();
		}

		LOGGER.info("***** END RapprochementCtbTestApp Main *****");
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

				String name = "classpath:" + packageName + '/' + filename;
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

	public static class JobStatusManager implements StatusManager {

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
