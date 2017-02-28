package ch.vd.uniregctb.foncier.migration;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.uniregctb.scheduler.JobCategory;
import ch.vd.uniregctb.scheduler.JobDefinition;

public abstract class MigrationDonneesFoncieresJob extends JobDefinition {

	private static final Logger LOGGER = LoggerFactory.getLogger(MigrationDonneesFoncieresJob.class);

	public MigrationDonneesFoncieresJob(String name, JobCategory categorie, int sortOrder, String description) {
		super(name, categorie, sortOrder, description);
	}

	private static InputStream getFusionCommunesContentInputStream(byte[] uploadedContent) {
		if (uploadedContent == null) {
			return MigrationDonneesFoncieresJob.class.getResourceAsStream("default-fusions-communes.csv");
		}
		else {
			return new ByteArrayInputStream(uploadedContent);
		}
	}

	public static DonneesFusionsCommunes getDonneesFusionsCommunes(byte[] uploadedContent) throws IOException {
		final List<DonneeBruteFusionCommunes> dataList = new LinkedList<>();
		try (InputStream is = getFusionCommunesContentInputStream(uploadedContent);
		     Reader r = new InputStreamReader(is);
		     BufferedReader br = new BufferedReader(r)) {

			String line;
			while ((line = br.readLine()) != null) {
				try {
					final DonneeBruteFusionCommunes data = DonneeBruteFusionCommunes.valueOf(line);

					// on ne prend en compte que les lignes qui apportent quelque chose...
					if (data.offsetParcelle != 0 || data.ofsAncienneCommune != data.ofsNouvelleCommune) {
						dataList.add(data);
					}
				}
				catch (ParseException e) {
					LOGGER.warn("Ligne ignorée du fichier de fusion des communes : " + line);
				}
			}
		}

		return new DonneesFusionsCommunes(dataList);
	}
}
