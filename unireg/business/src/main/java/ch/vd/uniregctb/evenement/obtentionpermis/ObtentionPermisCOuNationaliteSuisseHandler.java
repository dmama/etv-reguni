package ch.vd.uniregctb.evenement.obtentionpermis;

import java.util.List;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.EtatCivilHelper;
import ch.vd.uniregctb.common.FiscalDateHelper;
import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerBase;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.interfaces.model.CommuneSimple;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.tiers.*;
import ch.vd.uniregctb.type.*;

/**
 * Règles métiers permettant de traiter les événements suivants :
 * - obtention de permis C
 * - obtention de la nationalité suisse
 *
 * @author Ludovic BERTIN <mailto:ludovic.bertin@oosphere.com>
 *
 */
public abstract class ObtentionPermisCOuNationaliteSuisseHandler extends EvenementCivilHandlerBase {

	private AdresseService adresseService;
	
	public AdresseService getAdresseService() {
		return adresseService;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	/**
	 * @see ch.vd.uniregctb.evenement.EvenementCivilHandler#validate(java.lang.Object,
	 *      java.util.List)
	 */
	@Override
	public void validateSpecific(EvenementCivil evenementCivil, List<EvenementCivilErreur> errors, List<EvenementCivilErreur> warnings) {

		/*
		 * L'evenenement est mis en erreur dans les cas suivants
		 */

		/*
		 * Il n'existe pas de tiers contribuable correspondant à l'individu,
		 * assujetti ou non (mineur, conjoint) correspondant à l'individu.
		 */
		Individu individu = evenementCivil.getIndividu();
		PersonnePhysique habitant = getPersonnePhysiqueOrFillErrors(individu.getNoTechnique(), errors);
		if (habitant == null) {
			return;
		}

	}

	/**
	 * Méthode utilitaire pour continuer/remplacer un for fiscal principal existant
	 * en changeant juste le mode d'imposition, la date et le motif d'ouverture
	 */
	private void openForFiscalPrincipalChangementModeImposition(Contribuable contribuable,
	                                                            ForFiscalPrincipal reference,
	                                                            RegDate dateOuverture,
	                                                            MotifFor motifOuverture,
	                                                            ModeImposition nouveauModeImposition,
	                                                            boolean changeHabitantFlag) {
		openForFiscalPrincipal(contribuable, dateOuverture, reference.getTypeAutoriteFiscale(), reference.getNumeroOfsAutoriteFiscale(), reference.getMotifRattachement(), motifOuverture, nouveauModeImposition, changeHabitantFlag);
	}

	/**
	 * Traite l'événement passé en paramètre.
	 *
	 */
	@Override
	public Pair<PersonnePhysique,PersonnePhysique> handle(EvenementCivil evenement, List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException {
		// Recupere le tiers correspondant a l'individu
		final Individu individu = evenement.getIndividu();
		final PersonnePhysique habitant = getPersonnePhysiqueOrThrowException(individu.getNoTechnique());

		final RegDate dateEvenement = evenement.getDate();
		final ForFiscalPrincipal forPrincipalHabitant = habitant.getForFiscalPrincipalAt(null);
		final EnsembleTiersCouple ensembleTiersCouple = getService().getEnsembleTiersCouple(habitant, evenement.getDate());
		MenageCommun menage = null;
		if (ensembleTiersCouple != null) {
			menage = ensembleTiersCouple.getMenage();
		}

		if (forPrincipalHabitant != null) { //individu seul assujetti
			final EtatCivil etatCivilIndividu = individu.getEtatCivilCourant();
			if (etatCivilIndividu == null) {
				throw new EvenementCivilHandlerException("Impossible de récupérer l'état civil courant de l'individu");
			}

			if (EtatCivilHelper.estMarieOuPacse(etatCivilIndividu)){
				//le for devrait être sur le menage commun
				throw new EvenementCivilHandlerException("Un individu avec conjoint non séparé possède un for principal individuel actif");
			}

			final ModeImposition modeImposition = forPrincipalHabitant.getModeImposition();
			if (modeImposition == ModeImposition.SOURCE || modeImposition == ModeImposition.MIXTE_137_2 || modeImposition.equals(ModeImposition.MIXTE_137_1)) {
				//obtention permis ou nationalité le jour de l'arrivée
				if (forPrincipalHabitant.getDateDebut().isAfterOrEqual(dateEvenement) && 
						(MotifFor.ARRIVEE_HC == forPrincipalHabitant.getMotifOuverture() || MotifFor.ARRIVEE_HS == forPrincipalHabitant.getMotifOuverture())) {
					getService().annuleForFiscal(forPrincipalHabitant, true);
					openForFiscalPrincipalChangementModeImposition(habitant, forPrincipalHabitant, forPrincipalHabitant.getDateDebut(), forPrincipalHabitant.getMotifOuverture(), ModeImposition.ORDINAIRE, true);
					Audit.info(evenement.getNumeroEvenement(), "Mise au role ordinaire de l'individu");
				} else {
					closeForFiscalPrincipal(habitant, dateEvenement.getOneDayBefore(), MotifFor.PERMIS_C_SUISSE);
					openForFiscalPrincipalChangementModeImposition(habitant, forPrincipalHabitant, dateEvenement, MotifFor.PERMIS_C_SUISSE, ModeImposition.ORDINAIRE, true);
					Audit.info(evenement.getNumeroEvenement(), "Mise à jour du for principal de l'individu au rôle ordinaire");
				}
			}

			// [UNIREG-725] si déjà ordinaire, indigent ou à la dépense : rien à faire!
		}
		else if (menage != null && menage.getForFiscalPrincipalAt(null) != null) { //couple assujetti
			final ForFiscalPrincipal forPrincipalMenage = menage.getForFiscalPrincipalAt(null);
			final ModeImposition modeImposition = forPrincipalMenage.getModeImposition();
			if(modeImposition == ModeImposition.SOURCE || modeImposition == ModeImposition.MIXTE_137_2 || modeImposition == ModeImposition.MIXTE_137_1) {
				//obtention permis ou nationalité le jour de l'arrivée
				if (forPrincipalMenage.getDateDebut().isAfterOrEqual(dateEvenement) && 
						(MotifFor.ARRIVEE_HC == forPrincipalMenage.getMotifOuverture() || MotifFor.ARRIVEE_HS == forPrincipalMenage.getMotifOuverture())) {
					getService().annuleForFiscal(forPrincipalMenage, true);
					openForFiscalPrincipalChangementModeImposition(menage, forPrincipalMenage, forPrincipalMenage.getDateDebut(), forPrincipalMenage.getMotifOuverture(), ModeImposition.ORDINAIRE, true);
					Audit.info(evenement.getNumeroEvenement(), "Mise au role ordinaire du ménage");
				} else {
					closeForFiscalPrincipal(menage, dateEvenement.getOneDayBefore(), MotifFor.PERMIS_C_SUISSE);
					openForFiscalPrincipalChangementModeImposition(menage, forPrincipalMenage, dateEvenement, MotifFor.PERMIS_C_SUISSE, ModeImposition.ORDINAIRE, true);
					Audit.info(evenement.getNumeroEvenement(), "Mise à jour du for principal du ménage au rôle ordinaire");
				}
			}
			//else pas de changement du mode d'imposition
		}
		else if (!FiscalDateHelper.isMajeurAt(individu, dateEvenement)) { //individu mineur non assujetti
			Audit.info(evenement.getNumeroEvenement(), "Individu mineur non assujetti, il reste non assujetti");
		}
		else { //individu majeur non assujetti
			final EtatCivil etatCivilIndividu = individu.getEtatCivilCourant();
			if (etatCivilIndividu == null) {
				throw new EvenementCivilHandlerException("Impossible de récupérer l'état civil courant de l'individu");
			}

			int noOfsEtendu = 0;
			if(evenement instanceof ObtentionNationalite){
				noOfsEtendu = ((ObtentionNationalite) evenement).getNumeroOfsEtenduCommunePrincipale();
			}
			else if(evenement instanceof ObtentionPermis){
				noOfsEtendu = ((ObtentionPermis) evenement).getNumeroOfsEtenduCommunePrincipale();
			}

			if (noOfsEtendu == 0) {
				// récupération du numero OFS de la commune à partir de l'adresse du tiers
				try {
					final AdresseGenerique adresse = adresseService.getAdresseFiscale(habitant, TypeAdresseFiscale.DOMICILE, dateEvenement, false);
					if (adresse != null) {
						final CommuneSimple commune = getService().getServiceInfra().getCommuneByAdresse(adresse);
						// uniquement si la commune de domicile est vaudoise
						if (commune != null && commune.isVaudoise()) {
							noOfsEtendu = commune.getNoOFSEtendu();
						}
					}
				}
				catch (AdresseException e) {
					throw new EvenementCivilHandlerException("Impossible de récupérer l'adresse", e);
				} catch (InfrastructureException e) {
					throw new EvenementCivilHandlerException("Impossible de récupérer la commune", e);
				}
			}
			
			// ouverture d'un for si la commune est vaudoise
			if (noOfsEtendu != 0) {
				if (noOfsEtendu == NO_OFS_FRACTION_SENTIER) {
					warnings.add(new EvenementCivilErreur("Ouverture d'un for dans la fraction de commune du Sentier: " +
						"veuillez vérifier la fraction de commune du for principal", TypeEvenementErreur.WARNING));
				}
				//TODO chercher dans les adresses si arrivée après obtention permis pour ouvrir le for à la date d'arrivée (pas de for ouvert car bridage IS)
				if (EtatCivilHelper.estMarieOuPacse(etatCivilIndividu)) { // le for est ouvert sur le ménage commun
					if (menage != null) {
						openForFiscalPrincipalDomicileVaudoisOrdinaire(menage, dateEvenement, noOfsEtendu, MotifFor.PERMIS_C_SUISSE, true);
						Audit.info(evenement.getNumeroEvenement(), "Ouverture du for principal du ménage au rôle ordinaire");
					}
					else {
						throw new EvenementCivilHandlerException("L'individu est marié ou en partenariat enregistré mais ne possède pas de ménage commun");
					}
				}
				else { // le for est ouvert sur l'individu
					openForFiscalPrincipalDomicileVaudoisOrdinaire(habitant, dateEvenement, noOfsEtendu, MotifFor.PERMIS_C_SUISSE, true);
					Audit.info(evenement.getNumeroEvenement(), "Ouverture du for principal de l'individu au rôle ordinaire");
				}
			}
		}
		return null;
	}
}
