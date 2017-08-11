package ch.vd.uniregctb.rf;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ResourceUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.BusinessItTestApplication;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.document.RapprocherCtbRapport;
import ch.vd.uniregctb.rapport.RapportService;


public class RapprochementCtbTestApp extends BusinessItTestApplication {
	private static final Logger LOGGER = LoggerFactory.getLogger(RapprochementCtbTestApp.class);

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

		// Get the size of the file
		final long length = file.length();
		if (length > Integer.MAX_VALUE || length < Integer.MIN_VALUE) {
			throw new IllegalArgumentException("File length out of bounds");
		}

		// Create the byte array to hold the data
		final byte[] bytes;

		System.out.println("\nDEBUG: FileInputStream is " + file);
		try (InputStream is = new FileInputStream(file); ByteArrayOutputStream out = new ByteArrayOutputStream((int) length)) {
			IOUtils.copy(is, out);
			bytes = out.toByteArray();
		}
        return bytes;
	}

	public static class JobStatusManager implements StatusManager {

		public JobStatusManager() {
		}

		@Override
		public synchronized boolean isInterrupted() {
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
