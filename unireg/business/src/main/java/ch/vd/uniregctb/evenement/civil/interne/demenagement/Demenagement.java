package ch.vd.uniregctb.evenement.civil.interne.demenagement;

import java.util.List;

import org.apache.log4j.Logger;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.adresse.AdressesCiviles;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.DonneesCivilesException;
import ch.vd.uniregctb.common.FiscalDateHelper;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneAvecAdresses;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.TypeEtatCivil;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementErreur;

/**
 * Modélise un événement de déménagement.
 *
 * @author Ludovic Bertin </a>
 */
public class Demenagement extends EvenementCivilInterneAvecAdresses {

	/** LOGGER log4J */
	protected static Logger LOGGER = Logger.getLogger(Demenagement.class);

	/**
	 * L'adresse de départ.
	 */
	private Adresse ancienneAdressePrincipale;

	/**
	 * La commune de la nouvelle adresse principale.
	 */
	private Commune nouvelleCommunePrincipale;

	protected Demenagement(EvenementCivilExterne evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);

		// il faut récupérer les adresses actuelles, ce seront les nouvelles
		// adresses

		// Distinction adresse principale et adresse courrier
		final AdressesCiviles adresses;
		try {
			adresses = new AdressesCiviles(context.getServiceCivil().getAdresses(super.getNoIndividu(), evenement.getDateEvenement().getOneDayBefore(), false));
		}
		catch (DonneesCivilesException e) {
			throw new EvenementCivilException(e);
		}
		this.ancienneAdressePrincipale = adresses.principale;

		// on recupere la commune de la nouvelle adresse
		try {
			this.nouvelleCommunePrincipale = context.getServiceInfra().getCommuneByAdresse(getNouvelleAdressePrincipale(), evenement.getDateEvenement());
		}
		catch (ServiceInfrastructureException e) {
			throw new EvenementCivilException(e);
		}
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected Demenagement(Individu individu, Long principalPPId, Individu conjoint, Long conjointPPId, RegDate date, Integer numeroOfsCommuneAnnonce,
	                       Commune communePrincipale, Adresse ancienneAdressePrincipale, Adresse nouvelleAdressePrincipale, EvenementCivilContext context) {
		super(individu, principalPPId, conjoint, conjointPPId, TypeEvenementCivil.DEMENAGEMENT_DANS_COMMUNE, date, numeroOfsCommuneAnnonce, nouvelleAdressePrincipale, null, null, context);
		this.ancienneAdressePrincipale = ancienneAdressePrincipale;
		this.nouvelleCommunePrincipale = communePrincipale;
	}

	public Commune getNouvelleCommunePrincipale() {
		return nouvelleCommunePrincipale;
	}

	public Adresse getNouvelleAdresseCourrier() {
		return getAdresseCourrier();
	}

	public Adresse getNouvelleAdressePrincipale() {
		return getAdressePrincipale();
	}

	public Adresse getAncienneAdressePrincipale() {
		return ancienneAdressePrincipale;
	}

	@Override
	public void validateSpecific(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) throws EvenementCivilException {

		/*
		 * La date de début de la nouvelle adresse principale de l’individu est
		 * antérieure ou identique à la date de l'ancienne.
		 */
		Adresse ancienneAdresse = getAncienneAdressePrincipale();
		if (ancienneAdresse == null || ancienneAdresse.getDateFin() == null) {
			erreurs.add(new EvenementCivilExterneErreur("L'individu n°" + getNoIndividu() + " n'a jamais déménagé"));
		}
		else if (!getDate().isAfter(ancienneAdresse.getDateFin())) {
			erreurs.add(new EvenementCivilExterneErreur("La date du déménagement est antérieure à la date de fin de l'ancienne adresse"));
		}

		/*
		 * La nouvelle adresse principale n’est pas dans le canton (il n’est pas
		 * obligatoire que l’adresse courrier soit dans le canton).
		 */
		final Commune nouvelleCommune = getNouvelleCommunePrincipale();
		if (nouvelleCommune == null || !nouvelleCommune.isVaudoise()) {
			erreurs.add(new EvenementCivilExterneErreur("La nouvelle adresse est en dehors du canton"));
		}

		/*
		 * Pour les communes du Sentier, il n'est pas possible de déterminer automatiquement le for principal. Un traitement
		 * manuel est nécessaire.
		 */
		if (nouvelleCommune != null && nouvelleCommune.getNoOFSEtendu() == EvenementCivilInterne.NO_OFS_FRACTION_SENTIER) {
			warnings.add(new EvenementCivilExterneErreur("déménagement dans la fraction de commune du Sentier: " +
					"veuillez vérifier la fraction de commune du for principal", TypeEvenementErreur.WARNING));
		}
	}

	@Override
	public Pair<PersonnePhysique, PersonnePhysique> handle(List<EvenementCivilExterneErreur> warnings) throws EvenementCivilException {

		/*
		 * Dans le cas d'une commune normale, rien a faire. Traitement
		 * particulier de la fraction de commune
		 */
		if (getNouvelleCommunePrincipale().isFraction()) {
			int numeroOfsCommuneEtendu = getNouvelleCommunePrincipale().getNoOFSEtendu();

			// Date poussée au 1er janvier si la
			// mutation est envoyé après le 20 décembre.
			RegDate dateEvenement = FiscalDateHelper.getDateOuvertureForFiscal(getDate());

			/*
			 * le contribuable est seul, on ferme ses for principaux et on
			 * en ouvre des nouveaux
			 */
			Individu individu = getIndividu();
			PersonnePhysique habitant = getPersonnePhysiqueOrThrowException(individu.getNoTechnique());
			
			if (getNoIndividuConjoint() == null) {
				// Mise à jour des fors fiscaux du contribuable (que
				// les for principaux)
				ForFiscalPrincipal forPrincipalHabitant = habitant.getForFiscalPrincipalAt(null);
				if (forPrincipalHabitant != null) {
					if ((forPrincipalHabitant.getNumeroOfsAutoriteFiscale() == null) || (!forPrincipalHabitant.getNumeroOfsAutoriteFiscale().equals(numeroOfsCommuneEtendu))) {
						// mise à jour du for fiscal habitant avec le même mode d'imposition
						updateForFiscalPrincipal(habitant, dateEvenement, numeroOfsCommuneEtendu, MotifFor.DEMENAGEMENT_VD,
								TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null, true);
						Audit.info(getNumeroEvenement(), "Mise à jour du for fiscal principal du tiers habitant");
					}
					/*
					 * Fermetures des adresses temporaires du fiscal 
					 */
					fermeAdresseTiersTemporaire(habitant, getDate().getOneDayBefore());
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
								Audit.info(getNumeroEvenement(), "Mise à jour du for fiscal principal du tiers ménage commun");
							}
						}
						/*
						 * Fermetures des adresses temporaires du fiscal 
						 */
						fermeAdresseTiersTemporaire(menage, getDate().getOneDayBefore());

					} else {
						/*
						 * Fermetures des adresses temporaires du fiscal 
						 */
						fermeAdresseTiersTemporaire(habitant, getDate().getOneDayBefore());
					}

				}
			}

			/*
			 * Cas d'un individu avec conjoint, c'est le contibuable ménage qui est impacté
			 */
			else {
				PersonnePhysique conjoint = getPersonnePhysiqueOrThrowException(getNoIndividuConjoint());

				EnsembleTiersCouple ensembleTiersCouple = getService().getEnsembleTiersCouple(habitant, dateEvenement);
				if (!ensembleTiersCouple.estComposeDe(habitant, conjoint)) {
					throw new EvenementCivilException(
							"Les tiers composant le tiers ménage trouvé ne correspondent pas avec les individus unis dans le civil");
				}

				EtatCivil etatCivilIndividu = individu.getEtatCivilCourant();
				if (etatCivilIndividu == null) {
					throw new EvenementCivilException("Impossible de récupérer l'état civil courant de l'individu");
				}

				TypeEtatCivil typeEtatCivilIndividu = etatCivilIndividu.getTypeEtatCivil();
				ForFiscalPrincipal forPrincipalHabitant = habitant.getForFiscalPrincipalAt(null);
				if (forPrincipalHabitant != null && typeEtatCivilIndividu != TypeEtatCivil.SEPARE) {
					throw new EvenementCivilException(
							"l'habitant possède un for principal actif alors qu'il a un conjoint et qu'il n'est pas séparé");
				}

				ForFiscalPrincipal forPrincipalConjoint = conjoint.getForFiscalPrincipalAt(null);
				if (forPrincipalConjoint != null && typeEtatCivilIndividu != TypeEtatCivil.SEPARE) {
					throw new EvenementCivilException(
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
						Audit.info(getNumeroEvenement(), "Mise à jour du for fiscal principal du tiers ménage commun");
					}
				}
				/*
				 * Fermetures des adresses temporaires du fiscal 
				 */
				fermeAdresseTiersTemporaire(menage, getDate().getOneDayBefore());
			}
		}
		return null;
	}
}
