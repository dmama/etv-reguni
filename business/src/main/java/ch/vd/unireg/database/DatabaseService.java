package ch.vd.unireg.database;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import ch.vd.unireg.common.StatusManager;

/**
 * Ce service expose des méthodes utilitaire pour obtenir des informations et pour agir sur la base de données Unireg.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface DatabaseService {

	/**
	 * Vide entièrement la base de toutes ses données.
	 */
	void truncateDatabase() throws Exception;

	/**
	 * @return la liste des noms de tables selon la base de données.
	 */
	String[] getTableNamesFromDatabase();

	/**
	 * @return la liste des noms de tables selon la factory hibernate.
	 */
	String[] getTableNamesFromHibernate(boolean reverse);

	/**
	 * Dump la base de donnée complète dans un fichier DbUnit.
	 */
	void dumpToDbunitFile(OutputStream outputStream) throws Exception;

	class DumpParts {
		public final boolean sitFamille;
		public final boolean declarations;
		public final boolean rapportsEntreTiers;

		public DumpParts(boolean sitFamille, boolean declarations, boolean rapportsEntreTiers) {
			this.sitFamille = sitFamille;
			this.declarations = declarations;
			this.rapportsEntreTiers = rapportsEntreTiers;
		}
	}

	/**
	 * Dump les données relatives aux tiers (fors, adresses, rapports).
	 *
	 * @param tiersList liste d'id des tiers
	 * @param parts     les parties à dumper
	 * @param status    un status manager
	 */
	int dumpTiersListToDbunitFile(List<Long> tiersList, DumpParts parts, OutputStream outputStream, StatusManager status) throws Exception;

	/**
	 * Efface et recharge la base de données avec un fichier DBUnit.
	 *
	 * @param inputStream    le contenu du fichier DBUnit
	 * @param status         un status manager
	 * @param truncateBefore <b>vrai</b> si la base doit être truncatée avant le load; <b>faux</b> autrement.
	 * @throws Exception en cas d'erreur
	 */
	void loadFromDbunitFile(InputStream inputStream, StatusManager status, boolean truncateBefore) throws Exception;

	/**
	 * Cette méthode s'assure que la séquence hibernate est initialisée avec une valeur assez grande pour ne pas générer d'ids qui entre en
	 * collision avec des valeurs existantes dans la base. Il est utile d'appeler cette méthode après le chargement d'un fichier DBUnit, par
	 * exemple.
	 */
	void ensureSequencesUpToDate(boolean updateHibernateSequence, boolean updateCAACSequence, boolean updateDPISequence, boolean updatePMSequence, boolean updateETBSequence);
}
