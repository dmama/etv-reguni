package ch.vd.uniregctb.efacture;

import ch.vd.registre.base.dao.GenericDAO;

/**
 * DAO des éléments {@link DocumentEFacture}
 */
public interface DocumentEFactureDAO extends GenericDAO<DocumentEFacture, Long> {

	/**
	 * @param tiersId identifiant d'un tiers
	 * @param cleArchivage clé d'archivage (FOLDERS) d'un document e-facture associé à ce tiers
	 * @return l'instance de {@link DocumentEFacture} correspondante au critères choisis
	 */
	DocumentEFacture findByTiersEtCleArchivage(long tiersId, String cleArchivage);
}
