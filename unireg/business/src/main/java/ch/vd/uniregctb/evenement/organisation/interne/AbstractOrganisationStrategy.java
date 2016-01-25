package ch.vd.uniregctb.evenement.organisation.interne;

import java.util.List;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.organisation.data.Domicile;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.engine.translator.EvenementOrganisationTranslationStrategy;
import ch.vd.uniregctb.evenement.organisation.interne.creation.CreateEntreprise;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Classe regroupant des méthodes communes. Certaines sont clairement des paliatifs en attendant une meilleure
 * solution.
 *
 * @author Raphaël Marmier, 2015-10-02
 */
public abstract class AbstractOrganisationStrategy implements EvenementOrganisationTranslationStrategy {

	/**
	 * Détecte les mutations pour lesquelles la création d'un événement interne {@link CreateEntreprise} est
	 * pertinente.
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

	protected boolean hasSitePrincipalVD(Organisation organisation, RegDate date) {
		Domicile siegePrincipal = organisation.getSiegePrincipal(date);
		return siegePrincipal != null && siegePrincipal.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;
	}

	protected boolean hasSiteVD(Organisation organisation, RegDate date) {
		for (SiteOrganisation site : organisation.getDonneesSites()) {
			final Domicile domicile = site.getDomicile(date);
			if (domicile != null && domicile.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Est-ce que cette organisation existant d'aujourd'hui existait déjà hier, selon RCEnt?
	 *
	 * @param organisation Une organisation existant pour la date fournie
	 * @param date La date "aujourd'hui"
	 * @return Vrai si existait hier
	 */
	protected boolean isExisting(Organisation organisation, RegDate date) throws EvenementOrganisationException {
		String nom = organisation.getNom(date);
		if (nom == null) {
			throw new EvenementOrganisationException(
					String.format("Entreprise %s inexistante au %s ne peut être utilisée pour savoir si elle existe déjà à cette date. Ne devrait jamais arriver en production.",
					              organisation.getNumeroOrganisation(), RegDateHelper.dateToDisplayString(date)));
		}
		return organisation.getNom(date.getOneDayBefore()) != null;
	}

	protected boolean isRadieIDE(StatusRegistreIDE statusRegistreIDE) {
		return statusRegistreIDE != null && (statusRegistreIDE == StatusRegistreIDE.RADIE || statusRegistreIDE == StatusRegistreIDE.DEFINITIVEMENT_RADIE);
	}

	protected boolean isRadieRC(StatusInscriptionRC statusInscriptionRC) {
		return statusInscriptionRC != null && statusInscriptionRC == StatusInscriptionRC.RADIE;
	}

	protected boolean isInscritRC(StatusRC statusRC) {
		return statusRC != null && statusRC == StatusRC.INSCRIT;
	}

	protected boolean isAssujetti(Entreprise entreprise, RegDate date, EvenementOrganisationContext context) throws AssujettissementException {
		List<Assujettissement> assujettissements = context.getAssujettissementService().determine(entreprise);
		Assujettissement assujettissement = null;
		if (assujettissements != null && !assujettissements.isEmpty()) {
			assujettissement = DateRangeHelper.rangeAt(assujettissements, date);
		}
		return assujettissement != null;
	}
}
