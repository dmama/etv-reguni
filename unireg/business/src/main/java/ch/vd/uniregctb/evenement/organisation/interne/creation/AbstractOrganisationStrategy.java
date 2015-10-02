package ch.vd.uniregctb.evenement.organisation.interne.creation;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.Siege;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.unireg.interfaces.organisation.data.TypeDeSite;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.engine.translator.EvenementOrganisationTranslationStrategy;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterne;
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
	 * Détecte les mutations pour lesquelles la création d'un événement interne {@link CreateEntrepriseBase} est
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
	protected boolean hasSitePrincipalVD(Organisation organisation, EvenementOrganisation event) {
		for (SiteOrganisation site : organisation.getDonneesSites()) {
			final Siege siege = rangeAt(site.getSieges(), event.getDateEvenement());
			final DateRanged<TypeDeSite> type = rangeAt(site.getTypeDeSite(), event.getDateEvenement());
			if (siege != null && siege.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD &&  type != null && type.getPayload() == TypeDeSite.ETABLISSEMENT_PRINCIPAL) {
				return true;
			}
		}
		return false;
	}

	/*
	Todo: Implémenter au niveau de l'adapteur?
	 */
	protected boolean hasSiteVD(Organisation organisation, EvenementOrganisation event) {
		for (SiteOrganisation site : organisation.getDonneesSites()) {
			final Siege siege = rangeAt(site.getSieges(), event.getDateEvenement());
			if (siege != null && siege.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Appelle la méthode analogue de DateRangeHelper, mais seulement après avoir controler que la liste des
	 * ranges n'est pas nulle.
	 * TODO: Trouver une autre solution
	 * @param ranges La liste des ranges
	 * @param date La date dont on cherche le range correspondant
	 * @param <T> Le type encapsulé par le range
	 * @return Le range, ou null s'il n'y en a pas ou si la liste est null.
	 */
	@Nullable
	protected static <T extends DateRange> T rangeAt(@Nullable List<? extends T> ranges, RegDate date) {
		if (ranges == null) {
			return null;
		}
		return DateRangeHelper.rangeAt(ranges, date);
	}
}
