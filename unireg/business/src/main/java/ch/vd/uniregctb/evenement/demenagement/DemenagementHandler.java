package ch.vd.uniregctb.evenement.demenagement;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.FiscalDateHelper;
import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerBase;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.CommuneSimple;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.TypeEtatCivil;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementErreur;

/**
 * Règles métiers permettant de traiter les événements de déménagement intra
 * communal.
 *
 * @author Ludovic Bertin
 *
 */
public class DemenagementHandler extends EvenementCivilHandlerBase {

	@Override
	public void checkCompleteness(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
	}

	/**
	 * @see ch.vd.uniregctb.evenement.common.EvenementCivilHandler#validate(java.lang.Object,
	 *      java.util.List)
	 */
	@Override
	protected void validateSpecific(EvenementCivil target, List<EvenementCivilErreur> errors, List<EvenementCivilErreur> warnings) {
		/* L’événement est mis en erreur dans les cas suivants */
		Demenagement demenagement = (Demenagement) target;

		/*
		 * La date de début de la nouvelle adresse principale de l’individu est
		 * antérieure ou identique à la date de l'ancienne.
		 */
		Adresse ancienneAdresse = demenagement.getAncienneAdressePrincipale();
		if (ancienneAdresse == null || ancienneAdresse.getDateFin() == null) {
			errors.add(new EvenementCivilErreur("L'individu n°" + demenagement.getNoIndividu() + " n'a jamais déménagé"));
		}
		else if (!demenagement.getDate().isAfter(ancienneAdresse.getDateFin())) {
			errors.add(new EvenementCivilErreur("La date du déménagement est antérieure à la date de fin de l'ancienne adresse"));
		}

		/*
		 * La nouvelle adresse principale n’est pas dans le canton (il n’est pas
		 * obligatoire que l’adresse courrier soit dans le canton).
		 */
		final CommuneSimple nouvelleCommune = demenagement.getNouvelleCommunePrincipale();
		if (nouvelleCommune == null || !nouvelleCommune.isVaudoise()) {
			errors.add(new EvenementCivilErreur("La nouvelle adresse est en dehors du canton"));
		}

		/*
		 * Pour les communes du Sentier, il n'est pas possible de déterminer automatiquement le for principal. Un traitement
		 * manuel est nécessaire.
		 */
		if (nouvelleCommune != null && nouvelleCommune.getNoOFSEtendu() == NO_OFS_FRACTION_SENTIER) {
			warnings.add(new EvenementCivilErreur("déménagement dans la fraction de commune du Sentier: " +
				"veuillez vérifier la fraction de commune du for principal", TypeEvenementErreur.WARNING));
		}
	}

	/**
	 * Traite l'événement passé en paramètre.
	 *
	 */
	@Override
	public Pair<PersonnePhysique,PersonnePhysique> handle(EvenementCivil evenement, List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException {

		Demenagement demenagement = (Demenagement) evenement;

		/*
		 * Dans le cas d'une commune normale, rien a faire. Traitement
		 * particulier de la fraction de commune
		 */
		if (demenagement.getNouvelleCommunePrincipale().isFraction()) {
			int numeroOfsCommuneEtendu = demenagement.getNouvelleCommunePrincipale().getNoOFSEtendu();

			// Date poussée au 1er janvier si la
			// mutation est envoyé après le 20 décembre.
			RegDate dateEvenement = FiscalDateHelper.getDateEvenementFiscal(evenement.getDate());

			/*
			 * le contribuable est seul, on ferme ses for principaux et on
			 * en ouvre des nouveaux
			 */
			Individu individu = demenagement.getIndividu();
			PersonnePhysique habitant = getPersonnePhysiqueOrThrowException(individu.getNoTechnique());
			
			if (demenagement.getNoIndividuConjoint() == null) {
				// Mise à jour des fors fiscaux du contribuable (que
				// les for principaux)
				ForFiscalPrincipal forPrincipalHabitant = habitant.getForFiscalPrincipalAt(null);
				if (forPrincipalHabitant != null) {
					if ((forPrincipalHabitant.getNumeroOfsAutoriteFiscale() == null) || (!forPrincipalHabitant.getNumeroOfsAutoriteFiscale().equals(numeroOfsCommuneEtendu))) {
						// mise à jour du for fiscal habitant avec le même mode d'imposition
						updateForFiscalPrincipal(habitant, dateEvenement, numeroOfsCommuneEtendu, MotifFor.DEMENAGEMENT_VD,
								TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null, true);
						Audit.info(evenement.getNumeroEvenement(), "Mise à jour du for fiscal principal du tiers habitant");
					}
					/*
					 * Fermetures des adresses temporaires du fiscal 
					 */
					fermeAdresseTiersTemporaire(habitant, evenement.getDate().getOneDayBefore());
				}
				else {//cas du marié seul
					EnsembleTiersCouple ensembleTiersCouple = getService().getEnsembleTiersCouple(habitant, dateEvenement);
					MenageCommun menage = null;
					if (ensembleTiersCouple != null) {
						menage = ensembleTiersCouple.getMenage();
					}
					if(menage != null){
						ForFiscalPrincipal forPrincipalMenage = menage.getForFiscalPrincipalAt(null);
						if(forPrincipalMenage != null){
							if ((forPrincipalMenage.getNumeroOfsAutoriteFiscale() == null) || (!forPrincipalMenage.getNumeroOfsAutoriteFiscale().equals(numeroOfsCommuneEtendu))) {
								// mise à jour du for fiscal menage commun avec le même mode d'imposition
								updateForFiscalPrincipal(menage, dateEvenement, numeroOfsCommuneEtendu, MotifFor.DEMENAGEMENT_VD,
										TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null, true);
								Audit.info(evenement.getNumeroEvenement(), "Mise à jour du for fiscal principal du tiers ménage commun");
							}
						}
						/*
						 * Fermetures des adresses temporaires du fiscal 
						 */
						fermeAdresseTiersTemporaire(menage, evenement.getDate().getOneDayBefore());

					} else {
						/*
						 * Fermetures des adresses temporaires du fiscal 
						 */
						fermeAdresseTiersTemporaire(habitant, evenement.getDate().getOneDayBefore());
					}

				}
			}

			/*
			 * Cas d'un individu avec conjoint, c'est le contibuable ménage qui est impacté
			 */
			else {
				PersonnePhysique conjoint = getPersonnePhysiqueOrThrowException(demenagement.getNoIndividuConjoint());

				EnsembleTiersCouple ensembleTiersCouple = getService().getEnsembleTiersCouple(habitant, dateEvenement);
				if (!ensembleTiersCouple.estComposeDe(habitant, conjoint)) {
					throw new EvenementCivilHandlerException(
							"Les tiers composant le tiers ménage trouvé ne correspondent pas avec les individus unis dans le civil");
				}

				EtatCivil etatCivilIndividu = individu.getEtatCivilCourant();
				if (etatCivilIndividu == null) {
					throw new EvenementCivilHandlerException("Impossible de récupérer l'état civil courant de l'individu");
				}

				TypeEtatCivil typeEtatCivilIndividu = etatCivilIndividu.getTypeEtatCivil();
				ForFiscalPrincipal forPrincipalHabitant = habitant.getForFiscalPrincipalAt(null);
				if (forPrincipalHabitant != null && typeEtatCivilIndividu != TypeEtatCivil.SEPARE) {
					throw new EvenementCivilHandlerException(
							"l'habitant possède un for principal actif alors qu'il a un conjoint et qu'il n'est pas séparé");
				}

				ForFiscalPrincipal forPrincipalConjoint = conjoint.getForFiscalPrincipalAt(null);
				if (forPrincipalConjoint != null && typeEtatCivilIndividu != TypeEtatCivil.SEPARE) {
					throw new EvenementCivilHandlerException(
							"le conjoint possède un for principal actif");
				}

				/*
				 * on ferme un éventuel for fiscal principal du tiers menage
				 * commun
				 */
				MenageCommun menage = ensembleTiersCouple.getMenage();
				ForFiscalPrincipal forPrincipalMenage = menage.getForFiscalPrincipalAt(null);
				if(forPrincipalMenage != null){
					if ((forPrincipalMenage.getNumeroOfsAutoriteFiscale() == null) || (!forPrincipalMenage.getNumeroOfsAutoriteFiscale().equals(numeroOfsCommuneEtendu))) {
						// mise à jour du for fiscal menage commun avec le même mode d'imposition
						updateForFiscalPrincipal(menage, dateEvenement, numeroOfsCommuneEtendu, MotifFor.DEMENAGEMENT_VD, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null,
								true);
						Audit.info(evenement.getNumeroEvenement(), "Mise à jour du for fiscal principal du tiers ménage commun");
					}
				}
				/*
				 * Fermetures des adresses temporaires du fiscal 
				 */
				fermeAdresseTiersTemporaire(menage, evenement.getDate().getOneDayBefore());
			}
		}
		return null;
	}

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.DEMENAGEMENT_DANS_COMMUNE);
		return types;
	}

	@Override
	public GenericEvenementAdapter createAdapter() {
		return new DemenagementAdapter();
	}

}
