package ch.vd.unireg.situationfamille;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.SituationFamille;
import ch.vd.unireg.type.EtatCivil;

public interface SituationFamilleService {

	/**
	 * Ajoute la nouvelle situation de famille au contribuable, fermant la précédente (si elle existe) à la veille. Cette méthode doit aussi
	 * publier l'événement fiscal correspondant.
	 *
	 * @return la nouvelle situation de famille ajoutée (celle incluse dans la session Hibernate).
	 */
	SituationFamille addSituationFamille(SituationFamille situationFamille, ContribuableImpositionPersonnesPhysiques contribuable);

	/**
	 * Assemble les données du registre civil et celles du registre fiscal pour construire une vue cohérente de la situation de famille d'un
	 * contribuable à une date donnée.
	 *
	 * @param contribuable le contribuable dont on veut obtenir la situation de famille
	 * @param date la date de validité de la situation de famille, ou <b>null</b> pour obtenir la situation de famille la plus à jour.
	 * @param yComprisCivil <code>true</code> si les données civiles sont considérées en plus des données fiscales, <code>false</code> si on ne s'intéresse qu'aux données purement fiscales
	 * @return une vue de la situation de famille
	 */
	VueSituationFamille getVue(Contribuable contribuable, RegDate date, boolean yComprisCivil);

	/**
	 * Assemble les données du registre civil et celles du registre fiscal pour construire un historique cohérente de la situation de
	 * famille d'un contribuable.
	 *
	 * @param contribuable
	 *            le contribuable dont on veut obtenir la situation de famille
	 * @return une vue de la situation de famille
	 */
	List<VueSituationFamille> getVueHisto(Contribuable contribuable);

	/**
	 * @param pp la personne physique
	 * @param date date de référence
	 * @param takeCivilAsDefault <code>true</code> si on doit prendre en compte le registre civil, <code>false</code> si on ne s'intéresse qu'aux données purement fiscales
	 * @return l'état civil à la date spécifié d'une personne physique.
	 */
	EtatCivil getEtatCivil(PersonnePhysique pp, RegDate date, boolean takeCivilAsDefault);

	/**
	 * Annule une situation de famille en réouvrant la précédente si elle existe
	 */
	void annulerSituationFamille(long idSituationFamille);

	/**
	 * Annule une situation de famille
	 *
	 * @param idSituationFamille
	 */
	void annulerSituationFamilleSansRouvrirPrecedente(long idSituationFamille);

	/**
	 * Ferme la situation de famille d'un contribuable
	 */
	void closeSituationFamille(ContribuableImpositionPersonnesPhysiques contribuable, RegDate date);

	/**
	 * Cette méthode réinitialise à la valeur NORMAL les barèmes double-gains sur les situations de famille actives des ménages-communs
	 * sourciers.
	 *
	 * @param dateTraitement
	 *            la date d'exécution du traitement
	 * @return le résultat détaillé de la réinitialisation
	 */
	ReinitialiserBaremeDoubleGainResults reinitialiserBaremeDoubleGain(RegDate dateTraitement, StatusManager statusManager);

	/**
	 * permete de comparer les situation de famille crées dans unireg et les information civiles
	 * afin de lever les incohérences
	 * @param dateTraitement
	 * @param nbThreads
	 * @param status
	 * @return
	 */
	ComparerSituationFamilleResults comparerSituationFamille(RegDate dateTraitement, int nbThreads, StatusManager status);
}
