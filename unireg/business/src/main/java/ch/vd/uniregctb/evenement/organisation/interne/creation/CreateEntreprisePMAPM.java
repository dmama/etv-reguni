package ch.vd.uniregctb.evenement.organisation.interne.creation;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

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
public class CreateEntreprisePMAPM extends CreateEntrepriseBase {

	protected CreateEntreprisePMAPM(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise,
	                                EvenementOrganisationContext context,
	                                EvenementOrganisationOptions options) throws EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options);
	}

	@NotNull
	@Override
	public HandleStatus handle(EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException {
		super.handle(warnings);

		// Ouverture du For principal seulement si inscrit au RC (certaines APM ne sont pas au RC)
		if (inscritAuRC(getSitePrincipal())) { // TODO: Tester!
			openForFiscalPrincipal(getDateDeDebut(),
			                       getAutoriteFiscalePrincipale().getTypeAutoriteFiscale(),
			                       getAutoriteFiscalePrincipale().getNoOfs(),
			                       MotifRattachement.DOMICILE,
			                       MotifFor.DEBUT_EXPLOITATION);

			// Création du bouclement
			createAddBouclement(getDateDeDebut());
		}

		// Gestion des sites secondaires non supportée pour l'instant, en attente du métier.
		if (false) {
			for (SiteOrganisation site : getOrganisation().getSitesSecondaires(getDateEvt())) {
				handleEtablissementsSecondaires(getAutoriteFiscalePrincipale(), site);
			}
		}

		Audit.info(String.format("Entreprise créée avec le numéro %s", getEntreprise().getNumero()));
		return HandleStatus.TRAITE;

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

	private boolean inscritAuRC(SiteOrganisation sitePrincipal) {
		// Comme nous sommes dans le cadre d'une création,
		if (sitePrincipal.getDonneesRC() != null) {
			return true;
		}
		return false;
	}


	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException {
		super.validateSpecific(erreurs, warnings);
	}
}
