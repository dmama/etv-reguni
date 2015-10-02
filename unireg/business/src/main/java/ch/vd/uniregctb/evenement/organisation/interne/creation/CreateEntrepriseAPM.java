package ch.vd.uniregctb.evenement.organisation.interne.creation;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.Siege;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationErreurCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationWarningCollector;
import ch.vd.uniregctb.evenement.organisation.interne.HandleStatus;
import ch.vd.uniregctb.evenement.organisation.interne.helper.CategorieEntreprise;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Evénement interne de création d'entreprise de catégorie "Personnes morales de droit public" (DP/PM)
 *
 *  Spécification:
 *  - Ti01SE03-Identifier et traiter les mutations entreprise.doc - Version 0.6 - 08.09.2015
 *
 * @author Raphaël Marmier, 2015-09-02
 */
public class CreateEntrepriseAPM extends CreateEntrepriseBase {

	protected CreateEntrepriseAPM(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise,
	                              EvenementOrganisationContext context,
	                              EvenementOrganisationOptions options) throws EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options);
	}

	@Override
	public void doHandle(EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException {
		super.doHandle(warnings);

		// Ouverture du For principal seulement si inscrit au RC (certaines APM ne sont pas au RC)
		if (inscritAuRC()) { // TODO: Tester!
			openForFiscalPrincipal(getDateDeDebut(),
			                       getAutoriteFiscalePrincipale().getTypeAutoriteFiscale(),
			                       getAutoriteFiscalePrincipale().getNoOfs(),
			                       MotifRattachement.DOMICILE,
			                       MotifFor.DEBUT_EXPLOITATION);

			// Création du bouclement
			createAddBouclement(getDateDeDebut());
		} else {
			raiseStatusTo(HandleStatus.A_VERIFIER);
		}

		// Gestion des sites secondaires non supportée pour l'instant, en attente du métier.
		if (false) {
			for (SiteOrganisation site : getOrganisation().getSitesSecondaires(getDateEvt())) {
				handleEtablissementsSecondaires(getAutoriteFiscalePrincipale(), site);
			}
		}

		Audit.info(String.format("Entreprise créée avec le numéro %s", getEntreprise().getNumero()));

		raiseStatusTo(HandleStatus.TRAITE);
	}

	private void handleEtablissementsSecondaires(Siege siegePrincipal, SiteOrganisation site) throws EvenementOrganisationException {
		long numeroSite = site.getNumeroSite();
		ensureNotExistsEtablissement(numeroSite, getDateDeDebut());

		final Siege autoriteFiscale = determineAutoriteFiscaleSiteSecondaire(site, getDateEvt());

		final List<Integer> autoritesAvecForSecondaire = new ArrayList<>();

		if (autoriteFiscale.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
			createAddEtablissement(site.getNumeroSite(), autoriteFiscale, false, getDateDeDebut());

			if (siegePrincipal.getTypeAutoriteFiscale() != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD &&
					(!autoritesAvecForSecondaire.contains(autoriteFiscale.getNoOfs()))) {
				openForFiscalSecondaire(getDateDeDebut(), autoriteFiscale.getTypeAutoriteFiscale(), autoriteFiscale.getNoOfs(), MotifRattachement.ETABLISSEMENT_STABLE, MotifFor.DEBUT_EXPLOITATION);
				autoritesAvecForSecondaire.add(autoriteFiscale.getNoOfs());
			}
		}
	}


	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException {
		super.validateSpecific(erreurs, warnings);

		// Vérifier qu'on est bien en présence d'un type qu'on supporte.
		Assert.state(getCategory() == CategorieEntreprise.APM, String.format("Catégorie d'entreprise non supportée! %s", getCategory()));
	}
}
