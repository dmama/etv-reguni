package ch.vd.uniregctb.situationfamille;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.SituationFamille;
import ch.vd.uniregctb.type.EtatCivil;

public interface SituationFamilleService {

	/**
	 * Ajoute la nouvelle situation de famille au contribuable, fermant la précédente (si elle existe) à la veille. Cette méthode doit aussi
	 * publier l'événement fiscal correspondant.
	 *
	 * @return la nouvelle situation de famille ajoutée (celle incluse dans la session Hibernate).
	 */
	SituationFamille addSituationFamille(SituationFamille situationFamille, Contribuable contribuable);

	/**
	 * Assemble les données du registre civil et celles du registre fiscal pour construire une vue cohérente de la situation de famille d'un
	 * contribuable à une date donnée.
	 *
	 * @param contribuable
	 *            le contribuable dont on veut obtenir la situation de famille
	 * @param date
	 *            la date de validité de la situation de famille, ou <b>null</b> pour obtenir la situation de famille la plus à jour.
	 * @return une vue de la situation de famille
	 */
	VueSituationFamille getVue(Contribuable contribuable, RegDate date);

	/**
	 * Assemble les données du registre civil et celles du registre fiscal pour construire un historique cohérente de la situation de
	 * famille d'un contribuable.
	 *
	 * @param contribuable
	 *            le contribuable dont on veut obtenir la situation de famille
	 * @param date
	 *            la date de validité de la situation de famille, ou <b>null</b> pour obtenir la situation de famille la plus à jour.
	 * @return une vue de la situation de famille
	 */
	List<VueSituationFamille> getVueHisto(Contribuable contribuable);

	/**
	 * @return l'état civil à la date spécifié d'une personne physique.
	 * @param pp
	 * @param dateMariage
	 * @return la date de naissance
	 */
	public EtatCivil getEtatCivil(PersonnePhysique pp, RegDate date);

	/**
	 * Annule une situation de famille en réouvrant la précédente si elle existe
	 */
	public void annulerSituationFamille(long idSituationFamille);

	/**
	 * Annule une situation de famille
	 *
	 * @param idSituationFamille
	 */
	public void annulerSituationFamilleSansRouvrirPrecedente(long idSituationFamille);

	/**
	 * Ferme la situation de famille d'un contribuable
	 */
	public void closeSituationFamille(Contribuable contribuable, RegDate date);

	/**
	 * Cette méthode réinitialise à la valeur NORMAL les barêmes double-gains sur les situations de famille actives des ménages-communs
	 * sourciers.
	 *
	 * @param dateTraitement
	 *            la date d'exécution du traitement
	 * @return le résultat détaillé de la réinitialisation
	 */
	ReinitialiserBaremeDoubleGainResults reinitialiserBaremeDoubleGain(RegDate dateTraitement, StatusManager statusManager);
}
