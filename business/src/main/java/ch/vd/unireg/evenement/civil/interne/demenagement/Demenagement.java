package ch.vd.unireg.evenement.civil.interne.demenagement;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.DonneesCivilesException;
import ch.vd.unireg.common.EtatCivilHelper;
import ch.vd.unireg.common.FiscalDateHelper;
import ch.vd.unireg.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.unireg.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.common.EvenementCivilOptions;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.unireg.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.unireg.evenement.civil.interne.EvenementCivilInterneAvecAdresses;
import ch.vd.unireg.evenement.civil.interne.HandleStatus;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.interfaces.civil.data.EtatCivil;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.infra.InfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.model.AdressesCiviles;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeAutoriteFiscale;

/**
 * Modélise un événement de déménagement.
 *
 * @author Ludovic Bertin </a>
 */
public class Demenagement extends EvenementCivilInterneAvecAdresses {

	/** LOGGER log4J */
	protected static Logger LOGGER = LoggerFactory.getLogger(Demenagement.class);

	/**
	 * L'adresse de départ.
	 */
	private Adresse ancienneAdressePrincipale;

	/**
	 * La commune de la nouvelle adresse principale.
	 */
	private Commune nouvelleCommunePrincipale;

	protected Demenagement(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);

		final RegDate dateEvenement = evenement.getDateEvenement();

		// il faut récupérer les adresses actuelles, ce seront les nouvelles
		// adresses
		setAncienneAdresseEtNouvelleCommune(context, dateEvenement);


	}

	private void setAncienneAdresseEtNouvelleCommune(EvenementCivilContext context, RegDate dateEvenement) throws EvenementCivilException {
		final RegDate oneDayBefore = dateEvenement.getOneDayBefore();

		// Distinction adresse principale et adresse courrier
		final AdressesCiviles adresses;
		try {

			adresses = context.getServiceCivil().getAdresses(super.getNoIndividu(), oneDayBefore, false);
		}
		catch (DonneesCivilesException e) {
			throw new EvenementCivilException(e);
		}
		this.ancienneAdressePrincipale = adresses.principale;

		// on recupere la commune de la nouvelle adresse
		try {
			this.nouvelleCommunePrincipale = context.getServiceInfra().getCommuneByAdresse(getNouvelleAdressePrincipale(), dateEvenement);
		}
		catch (InfrastructureException e) {
			throw new EvenementCivilException(e);
		}
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected Demenagement(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, Commune communePrincipale, Adresse ancienneAdressePrincipale,
	                       Adresse nouvelleAdressePrincipale, EvenementCivilContext context) {
		super(individu, conjoint, date, numeroOfsCommuneAnnonce, nouvelleAdressePrincipale, null, null, context);
		this.ancienneAdressePrincipale = ancienneAdressePrincipale;
		this.nouvelleCommunePrincipale = communePrincipale;
	}

	public Demenagement(EvenementCivilEchFacade event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(event, context, options);
		final RegDate dateEvenement = event.getDateEvenement();
		// il faut récupérer les adresses actuelles, ce seront les nouvelles
		// adresses
		setAncienneAdresseEtNouvelleCommune(context, dateEvenement);

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
	public void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {

		/*
		 * La date de début de la nouvelle adresse principale de l’individu est
		 * antérieure ou identique à la date de l'ancienne.
		 */
		Adresse ancienneAdresse = getAncienneAdressePrincipale();
		if (ancienneAdresse == null || ancienneAdresse.getDateFin() == null) {
			erreurs.addErreur("L'individu n°" + getNoIndividu() + " n'a jamais déménagé");
		}
		else if (!getDate().isAfter(ancienneAdresse.getDateFin())) {
			erreurs.addErreur("La date du déménagement est antérieure à la date de fin de l'ancienne adresse");
		}

		/*
		 * La nouvelle adresse principale n’est pas dans le canton (il n’est pas
		 * obligatoire que l’adresse courrier soit dans le canton).
		 */
		final Commune nouvelleCommune = getNouvelleCommunePrincipale();
		if (nouvelleCommune == null || !nouvelleCommune.isVaudoise()) {
			erreurs.addErreur("La nouvelle adresse est en dehors du canton");
		}

		/*
		 * Pour les communes du Sentier, il n'est pas possible de déterminer automatiquement le for principal. Un traitement
		 * manuel est nécessaire.
		 */
		if (nouvelleCommune != null && nouvelleCommune.getNoOFS() == EvenementCivilInterne.NO_OFS_FRACTION_SENTIER) {
			warnings.addWarning("déménagement dans la fraction de commune du Sentier: veuillez vérifier la fraction de commune du for principal");
		}

		final PersonnePhysique ppPrincipale = getPrincipalPP();
		EnsembleTiersCouple etc = context.getTiersService().getEnsembleTiersCouple(ppPrincipale, getDate().getOneDayBefore());
		PersonnePhysique conjoint =null;
		MenageCommun couple = null;
		if (etc != null) {
			conjoint = etc.getConjoint(ppPrincipale);
		}

		verifierPresenceDecisionEnCours(ppPrincipale,getDate());

		verifierPresenceDecisionsEnCoursSurCouple(ppPrincipale);

		if (conjoint != null) {
			verifierPresenceDecisionEnCours(conjoint,ppPrincipale,getDate());

		}


	}

	@NotNull
	@Override
	public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {

		/*
		 * Dans le cas d'une commune normale, rien a faire. Traitement
		 * particulier de la fraction de commune
		 */
		if (getNouvelleCommunePrincipale().isFraction()) {
			int numeroOfsCommune = getNouvelleCommunePrincipale().getNoOFS();

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
					if ((forPrincipalHabitant.getNumeroOfsAutoriteFiscale() == null) || (!forPrincipalHabitant.getNumeroOfsAutoriteFiscale().equals(numeroOfsCommune))) {
						// mise à jour du for fiscal habitant avec le même mode d'imposition
						updateForFiscalPrincipal(habitant, dateEvenement, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, numeroOfsCommune, null, MotifFor.DEMENAGEMENT_VD, null);
						context.audit.info(getNumeroEvenement(), "Mise à jour du for fiscal principal du tiers habitant");
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
							if ((forPrincipalMenage.getNumeroOfsAutoriteFiscale() == null) || (!forPrincipalMenage.getNumeroOfsAutoriteFiscale().equals(numeroOfsCommune))) {
								// mise à jour du for fiscal menage commun avec le même mode d'imposition
								updateForFiscalPrincipal(menage, dateEvenement, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, numeroOfsCommune, null, MotifFor.DEMENAGEMENT_VD, null);
								context.audit.info(getNumeroEvenement(), "Mise à jour du for fiscal principal du tiers ménage commun");
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

				ForFiscalPrincipal forPrincipalHabitant = habitant.getForFiscalPrincipalAt(null);
				if (forPrincipalHabitant != null && !EtatCivilHelper.estSepare(etatCivilIndividu)) {
					throw new EvenementCivilException(
							"l'habitant possède un for principal actif alors qu'il a un conjoint et qu'il n'est pas séparé");
				}

				ForFiscalPrincipal forPrincipalConjoint = conjoint.getForFiscalPrincipalAt(null);
				if (forPrincipalConjoint != null && !EtatCivilHelper.estSepare(etatCivilIndividu)) {
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
					if ((forPrincipalMenage.getNumeroOfsAutoriteFiscale() == null) || (!forPrincipalMenage.getNumeroOfsAutoriteFiscale().equals(numeroOfsCommune))) {
						// mise à jour du for fiscal menage commun avec le même mode d'imposition
						updateForFiscalPrincipal(menage, dateEvenement, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, numeroOfsCommune, null, MotifFor.DEMENAGEMENT_VD, null);
						context.audit.info(getNumeroEvenement(), "Mise à jour du for fiscal principal du tiers ménage commun");
					}
				}
				/*
				 * Fermetures des adresses temporaires du fiscal 
				 */
				fermeAdresseTiersTemporaire(menage, getDate().getOneDayBefore());
			}
		}
		return HandleStatus.TRAITE;
	}
}
