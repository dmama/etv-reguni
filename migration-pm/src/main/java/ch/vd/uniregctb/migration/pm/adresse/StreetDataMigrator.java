package ch.vd.uniregctb.migration.pm.adresse;

import ch.vd.uniregctb.migration.pm.MigrationResultProduction;
import ch.vd.uniregctb.migration.pm.regpm.AdresseAvecRue;

public interface StreetDataMigrator {

	/**
	 * @param adresse adresse en provenance du Mainframe
	 * @param mr collecteur de messages de suivi
	 * @return données pour l'adresse (référentiel FiDoR/RefInf) (<code>null</code> en cas d'adresse hors-Suisse ou d'adresse non-reconnue)
	 */
	StreetData migrate(AdresseAvecRue adresse, MigrationResultProduction mr);
}
