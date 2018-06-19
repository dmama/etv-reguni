package ch.vd.unireg.evenement.organisation.interne.creation;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.evenement.organisation.EvenementEntreprise;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseAbortException;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseContext;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseException;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseOptions;
import ch.vd.unireg.evenement.organisation.audit.EvenementEntrepriseErreurCollector;
import ch.vd.unireg.evenement.organisation.audit.EvenementEntrepriseSuiviCollector;
import ch.vd.unireg.evenement.organisation.audit.EvenementEntrepriseWarningCollector;
import ch.vd.unireg.evenement.organisation.interne.EvenementEntrepriseInterneDeTraitement;
import ch.vd.unireg.interfaces.organisation.data.Domicile;
import ch.vd.unireg.interfaces.organisation.data.EntreeJournalRC;
import ch.vd.unireg.interfaces.organisation.data.EntrepriseCivile;
import ch.vd.unireg.interfaces.organisation.data.EntrepriseHelper;
import ch.vd.unireg.interfaces.organisation.data.EtablissementCivil;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;

/**
 * Classe de base implémentant la création d'une entreprise et de son établissement principal dans Unireg.
 *
 * @author Raphaël Marmier, 2015-09-02
 */
public abstract class CreateEntreprise extends EvenementEntrepriseInterneDeTraitement {

	final private RegDate dateDeCreation;
	final private RegDate dateOuvertureFiscale;
	final private boolean isCreation;
	final private EtablissementCivil etablissementPrincipal;
	final private Domicile autoriteFiscalePrincipale;

	protected CreateEntreprise(EvenementEntreprise evenement, EntrepriseCivile entrepriseCivile, Entreprise entreprise,
	                           EvenementEntrepriseContext context,
	                           EvenementEntrepriseOptions options,
	                           RegDate dateDeCreation,
	                           RegDate dateOuvertureFiscale,
	                           boolean isCreation) throws EvenementEntrepriseException {
		super(evenement, entrepriseCivile, entreprise, context, options);

		etablissementPrincipal = entrepriseCivile.getEtablissementPrincipal(getDateEvt()).getPayload();
		if (dateDeCreation == null) {
			throw new EvenementEntrepriseException("Date nulle pour la création d'une entreprise. Probablement une erreur de programmation à ce stade..");
		}
		this.dateDeCreation = dateDeCreation;
		this.dateOuvertureFiscale = dateOuvertureFiscale;
		this.isCreation = isCreation;


		autoriteFiscalePrincipale = etablissementPrincipal.getDomicile(getDateEvt());

		// TODO: Générer événements fiscaux

		// TODO: Générer documents éditique
	}

	@NotNull
	public RegDate getDateDeCreation() {
		return dateDeCreation;
	}

	public RegDate getDateOuvertureFiscale() {
		return dateOuvertureFiscale;
	}

	public boolean isCreation() {
		return isCreation;
	}

	@NotNull
	public EtablissementCivil getEtablissementPrincipal() {
		return etablissementPrincipal;
	}

	@NotNull
	public Domicile getAutoriteFiscalePrincipale() {
		return autoriteFiscalePrincipale;
	}

	@Override
	public void doHandle(EvenementEntrepriseWarningCollector warnings, EvenementEntrepriseSuiviCollector suivis) throws EvenementEntrepriseException {
		// SIFISC-19700: Contrôle que la date d'inscription au RC rapportée par RCEnt correspond à celle de l'entrée de journal au RC. (il y a eu des erreurs de transcription au RC!)
		if (isCreation() && getEntrepriseCivile().isInscriteAuRC(getDateEvt())) {
			final List<EntreeJournalRC> entreesJournalPourDatePublication = etablissementPrincipal.getDonneesRC().getEntreesJournalPourDatePublication(getDateEvt());
			/*
			 On part du principe que lors d'une inscription d'une nouvelle entreprise au RC, on a une et une seule publication FOSC portant sur une entrée de journal.
			 S'il y a plusieurs entrées, c'est qu'on n'est pas vraiment dans un cas de création d'entreprise, en tout cas pas un cas standard. On ignore.
			  */
			if (entreesJournalPourDatePublication.size() == 1) {
				final EntreeJournalRC entreeJournalRC = entreesJournalPourDatePublication.get(0);
				if (entreeJournalRC.getDate() != dateDeCreation) {
					throw new EvenementEntrepriseAbortException(
							String.format("La date d'inscription au RC (%s) de l'entreprise %s (civil: %s) diffère de la date de l'entrée de journal au RC (%s)! (Possible problème de transcription au RC) Impossible de continuer.",
							              RegDateHelper.dateToDisplayString(dateDeCreation),
							              getEntrepriseCivile().getNom(getDateEvt()),
							              getNoEntrepriseCivile(),
							              RegDateHelper.dateToDisplayString(entreeJournalRC.getDate()))
					);
				}
			}
		}

		// Création & vérification de la surcharge corrective s'il y a lieu
		SurchargeCorrectiveRange surchargeCorrectiveRange = null;
		if (dateDeCreation.isBefore(getDateEvt())) {
			surchargeCorrectiveRange = new SurchargeCorrectiveRange(dateDeCreation, getDateEvt().getOneDayBefore());
			if (!surchargeCorrectiveRange.isAcceptable()) {
				throw new EvenementEntrepriseException(
						String.format("Refus de créer dans Unireg une entreprise dont la fondation remonte à %s, %d jours avant la date de l'événement. La tolérance étant de %d jours. " +
								              "Il y a probablement une erreur d'identification ou un problème de date.",
						              RegDateHelper.dateToDisplayString(dateDeCreation),
						              surchargeCorrectiveRange.getEtendue(),
						              EntrepriseHelper.NB_JOURS_TOLERANCE_DE_DECALAGE_RC)
				);
			}
		}

		// Création de l'entreprise
		createEntreprise(dateDeCreation, suivis);

		// Création de l'établissement principal
		createAddEtablissement(etablissementPrincipal.getNumeroEtablissement(), autoriteFiscalePrincipale, true, dateDeCreation, suivis);

		if (dateDeCreation.isBefore(getDateEvt())) {
			appliqueDonneesCivilesSurPeriode(getEntreprise(), surchargeCorrectiveRange, getDateEvt(), warnings, suivis);
		}

		// Création des établissement secondaires
		for (EtablissementCivil etablissement : getEntrepriseCivile().getEtablissementsSecondaires(getDateEvt())) {
			Etablissement etablissementSecondaire = addEtablissementSecondaire(etablissement, dateDeCreation, warnings, suivis);
			if (dateDeCreation.isBefore(getDateEvt())) {
				appliqueDonneesCivilesSurPeriode(etablissementSecondaire, surchargeCorrectiveRange, getDateEvt(), warnings, suivis);
			}
		}
		if (!isCreation()) {
			regleDateDebutPremierExerciceCommercial(getEntreprise(), dateDeCreation, suivis);
		}
	}

	@Override
	protected void validateSpecific(EvenementEntrepriseErreurCollector erreurs, EvenementEntrepriseWarningCollector warnings, EvenementEntrepriseSuiviCollector suivis) throws EvenementEntrepriseException {

		/*
		 Erreurs techniques fatale
		  */
		if (dateDeCreation == null) {
			throw new IllegalArgumentException();
		}

		// Vérifier qu'il n'y a pas d'entreprise préexistante en base ? (Ca ne devrait pas se produire ici)
		if (getEntreprise() != null) {
			throw new IllegalArgumentException();
		}

		/*
		 Problèmes métiers empêchant la progression
		  */

		if (etablissementPrincipal == null) {
			erreurs.addErreur(String.format("Aucun établissement principal trouvé pour la date du %s. [no entreprise: %d]",
			                                RegDateHelper.dateToDisplayString(getDateDeCreation()), getEntrepriseCivile().getNumeroEntreprise()));
		}

		if (autoriteFiscalePrincipale == null) {
			erreurs.addErreur(String.format("Autorité fiscale introuvable pour la date du %s. [no entreprise: %d]",
			                                RegDateHelper.dateToDisplayString(getDateDeCreation()), getEntrepriseCivile().getNumeroEntreprise()));
		}

		// TODO: Vérifier que le siège n'est pas sur une commune faîtière et passer en manuel si c'est le cas. (fractions de communes)


		// Vérifier la présence des autres données nécessaires ?
	}

	protected boolean inscriteAuRC() {
		return getEntrepriseCivile().isInscriteAuRC(getDateEvt());
	}
}
