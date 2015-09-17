package ch.vd.unireg.interfaces.organisation.data;

import java.util.List;

import ch.vd.unireg.interfaces.common.Adresse;

public interface Organisation {

	/**
	 * @return l'identifiant de l'organisation dans le système source, utilisé comme clé dans Unireg (id cantonal)
	 */
	long getNumeroOrganisation();

	/**
	 * @return Liste des sites de l'organisation
	 */
	List<SiteOrganisation> getDonneesSites();

	/**
	 * @return historique des formes juridiques de l'organisation
	 */
	List<DateRanged<FormeLegale>> getFormeLegale();

	/**
	 * @return historique des identifiants IDE
	 */
	List<DateRanged<String>> getNumeroIDE();

	/**
	 * @return historique des raisons sociales de l'organisation
	 */
	List<DateRanged<String>> getNom();

	/**
	 * @return historiques des noms additionnels de l'organisation
	 */
	List<DateRanged<String>> getNomsAdditionels();

	List<Capital> getCapitaux();

	List<Siege> getSiegesPrincipaux();

	List<Adresse> getAdresses();

	List<DateRanged<Long>> getEnRemplacementDe();

	List<DateRanged<Long>> getRemplacePar();

	List<DateRanged<Long>> getTransferDe();

	List<DateRanged<Long>> getTransfereA();

}
