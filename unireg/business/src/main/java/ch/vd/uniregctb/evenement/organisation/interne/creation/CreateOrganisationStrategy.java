package ch.vd.uniregctb.evenement.organisation.interne.creation;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.engine.translator.EvenementOrganisationTranslationStrategy;
import ch.vd.uniregctb.evenement.organisation.interne.CategorieEntreprise;
import ch.vd.uniregctb.evenement.organisation.interne.CategorieEntrepriseHelper;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.uniregctb.evenement.organisation.interne.TraitementManuel;
import ch.vd.uniregctb.tiers.Entreprise;

/**
 * @author Raphaël Marmier, 2015-09-02
 */
public class CreateOrganisationStrategy implements EvenementOrganisationTranslationStrategy {

	/**
	 * Détecte les mutations pour lesquelles la création d'un événement interne {@link CreateEntreprisePMAPM} est
	 * pertinente.
	 *
	 * Spécifications:
	 *  - Ti01SE03-Identifier et traiter les mutations entreprise.doc - Version 0.6 - 08.09.2015
	 *
	 * @param event   un événement organisation reçu de RCEnt
	 * @param organisation
	 * @param context le context d'exécution de l'événement
	 * @param options des options de traitement
	 * @return
	 * @throws EvenementOrganisationException
	 */
	@Override
	public EvenementOrganisationInterne matchAndCreate(EvenementOrganisation event, final Organisation organisation, EvenementOrganisationContext context, EvenementOrganisationOptions options) throws
			EvenementOrganisationException {

		/*
		 * Si l'entreprise existe déjà, on ignore
		 */
		Entreprise entreprise = context.getTiersDAO().getEntrepriseByNumeroOrganisation(organisation.getNo());
		if (entreprise != null) {
			return null;
		}

		CategorieEntreprise category = getCurrentCategorieEntreprise(event, organisation);

		if (category != null) {
			/*
				Soit on ignore l'événement, soit on crée un événement interne
			 */
			if (hasSiteVD(organisation, event, context)) {
				switch (category) {
				case PP:
					return null;
				case SP:
					return handleSP(event, organisation, context, options);
				case PM:
				case APM:
					return handlePMAPM(event, organisation, context, options);
				case FDS_PLAC:
					return new CreateEntrepriseFDSPLAC(event, organisation, null, context, options);
				case DP_PM:
					return new CreateEntrepriseDPPM(event, organisation, null, context, options);
				}
			} else {
				return null;
			}
		}

		/*
			On part en traitement manuel
		 */
		return new TraitementManuel(event, organisation, null, context, options, createTraitementManuelMessage(event, organisation));
	}

	@NotNull
	private String createTraitementManuelMessage(EvenementOrganisation event, Organisation organisation) {
		return String.format("L'organisation %s %s, commune OFS %s et juridique %s ne peut être créée sans intervention utilisateur.",
		                     organisation.getNo(), organisation.getNom(), organisation.getSiegePrincipal(event.getDateEvenement()), organisation.getFormeLegale(event.getDateEvenement()));
	}

	private boolean hasSiteVD(Organisation organisation, EvenementOrganisation event, EvenementOrganisationContext context) {
		for (Map.Entry<Long, SiteOrganisation> entry : organisation.getDonneesSites().entrySet()) {
			List<DateRanged<Integer>> siegesRange = entry.getValue().getSiege();
			if (siegesRange != null) {
				DateRanged<Integer> siegeRange = DateRangeHelper.rangeAt(siegesRange, event.getDateEvenement());
				if (siegeRange != null) {
					Integer siege = siegeRange.getPayload();
					Commune commune = context.getServiceInfra().getCommuneByNumeroOfs(siege, event.getDateEvenement());
					if (commune.isVaudoise()) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@NotNull
	private CreateEntreprisePMAPM handlePMAPM(EvenementOrganisation event, Organisation organisation, EvenementOrganisationContext context, EvenementOrganisationOptions options) throws
			EvenementOrganisationException {
		return new CreateEntreprisePMAPM(event, organisation, null, context, options);
	}

	@NotNull
	private CreateEntrepriseSP handleSP(EvenementOrganisation event, Organisation organisation, EvenementOrganisationContext context, EvenementOrganisationOptions options) throws
			EvenementOrganisationException {
		return new CreateEntrepriseSP(event, organisation, null, context, options);
	}

	@Nullable
	private CategorieEntreprise getCurrentCategorieEntreprise(EvenementOrganisation event, Organisation organisation) {
		return CategorieEntrepriseHelper.map(organisation.getFormeLegale(event.getDateEvenement()));
	}
}
