package ch.vd.uniregctb.evenement.organisation.interne.creation;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.Siege;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationErreurCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationSuiviCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationWarningCollector;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.type.CategorieEntreprise;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Evénement interne de création d'entreprise de catégorie "Personnes morales de droit public" (DP/PM)
 *
 * @author Raphaël Marmier, 2015-09-02
 */
public class CreateEntreprisePM extends CreateEntreprise {

	protected CreateEntreprisePM(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise,
	                             EvenementOrganisationContext context,
	                             EvenementOrganisationOptions options) throws EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options);
	}

	@Override
	public void doHandle(EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		super.doHandle(warnings, suivis);

		Siege autoriteFiscalePrincipale = getAutoriteFiscalePrincipale();

		MotifFor motifOuverture = determineMotifOuvertureFor();

		openForFiscalPrincipal(getDateDeDebut(),
		                       autoriteFiscalePrincipale,
		                       MotifRattachement.DOMICILE,
		                       motifOuverture, warnings, suivis);

		// Création du bouclement
		createAddBouclement(getDateDeDebut(), suivis);

		// Gestion des sites secondaires non supportée pour l'instant, en attente du métier.
		if (false) {
			for (SiteOrganisation site : getOrganisation().getSitesSecondaires(getDateEvt())) {
				handleEtablissementsSecondaires(autoriteFiscalePrincipale, site, warnings, suivis);
			}
		}
	}

	private void handleEtablissementsSecondaires(Siege siegePrincipal, SiteOrganisation site, EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		long numeroSite = site.getNumeroSite();
		Etablissement etablissement = getEtablissementByNumeroSite(numeroSite);
		if (etablissement != null) {
			throw new EvenementOrganisationException(
					String.format("Trouvé un établissement existant %s pour l'organisation en création %s %s. Impossible de continuer.",
					              numeroSite, getNoOrganisation(), getOrganisation().getNom(getDateDeDebut())));
		}

		final Siege autoriteFiscale = site.getSiege(getDateEvt());
		if (autoriteFiscale == null) {
			throw new EvenementOrganisationException(
					String.format(
							"Autorité fiscale (siège) introuvable pour le site secondaire %s de l'organisation %s %s. Site probablement à l'étranger. Impossible pour le moment de créer le domicile de l'établissement secondaire.",
							site.getNumeroSite(), getNoOrganisation(), getOrganisation().getNom(getDateEvt())));
		}

		final List<Integer> autoritesAvecForSecondaire = new ArrayList<>();

		if (autoriteFiscale.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
			createAddEtablissement(site.getNumeroSite(), autoriteFiscale, false, getDateDeDebut(), suivis);

			if (siegePrincipal.getTypeAutoriteFiscale() != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD &&
					(!autoritesAvecForSecondaire.contains(autoriteFiscale.getNoOfs()))) {
				openForFiscalSecondaire(getDateDeDebut(), autoriteFiscale, MotifRattachement.ETABLISSEMENT_STABLE, MotifFor.DEBUT_EXPLOITATION, warnings, suivis);
				autoritesAvecForSecondaire.add(autoriteFiscale.getNoOfs());
			}
		}
	}


	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException {
		super.validateSpecific(erreurs, warnings);

		Assert.state(getCategory() == CategorieEntreprise.PM, String.format("Catégorie d'entreprise non supportée! %s", getCategory()));

		if (getCategory() == null) {
			FormeLegale formeLegale = getOrganisation().getFormeLegale(getDateDeDebut());
			erreurs.addErreur(String.format("Catégorie introuvable pour l'organisation no %s de forme juridique %s, en date du %s.", getOrganisation().getNumeroOrganisation(),
			                                formeLegale != null ? formeLegale : "inconnue", RegDateHelper.dateToDisplayString(getDateDeDebut())));
		}

		if (!inscritAuRC()) {
			erreurs.addErreur("Inscription au RC manquante.");
		}
	}
}
