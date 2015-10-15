package ch.vd.uniregctb.evenement.organisation.interne;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.Siege;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.engine.translator.EvenementOrganisationTranslationStrategy;
import ch.vd.uniregctb.evenement.organisation.interne.creation.CreateEntreprise;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Classe regroupant des méthodes communes. Certaines sont clairement des paliatifs en attendant une meilleure
 * solution.
 * TODO: Trouver de vraies solutions aux problèmes pointés ci-dessous.
 *
 * @author Raphaël Marmier, 2015-10-02
 */
public abstract class AbstractOrganisationStrategy implements EvenementOrganisationTranslationStrategy {

	/**
	 * Détecte les mutations pour lesquelles la création d'un événement interne {@link CreateEntreprise} est
	 * pertinente.
	 *
	 * Spécifications:
	 *  - Ti01SE03-Identifier et traiter les mutations entreprise.doc - Version 1.1 - 23.09.2015
	 *
	 * @param event   un événement organisation reçu de RCEnt
	 * @param organisation
	 * @param context le context d'exécution de l'événement
	 * @param options des options de traitement
	 * @return
	 * @throws EvenementOrganisationException
	 */
	@Override
	public abstract EvenementOrganisationInterne matchAndCreate(EvenementOrganisation event,
	                                                   final Organisation organisation,
	                                                   Entreprise entreprise,
	                                                   EvenementOrganisationContext context,
	                                                   EvenementOrganisationOptions options) throws EvenementOrganisationException;

	/*
	TODO: Implémenter au niveau de l'adapteur?
	 */
	protected boolean hasSitePrincipalVD(Organisation organisation, RegDate date) {
		Siege siegePrincipal = organisation.getSiegePrincipal(date);
		return siegePrincipal != null && siegePrincipal.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;
	}

	/*
	Todo: Implémenter au niveau de l'adapteur?
	 */
	protected boolean hasSiteVD(Organisation organisation, RegDate date) {
		for (SiteOrganisation site : organisation.getDonneesSites()) {
			final Siege siege = site.getSiege(date);
			if (siege != null && siege.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
				return true;
			}
		}
		return false;
	}
}
