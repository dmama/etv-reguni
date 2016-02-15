package ch.vd.uniregctb.evenement.organisation.interne.radiation;

import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationErreurCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationSuiviCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationWarningCollector;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterneDeTraitement;
import ch.vd.uniregctb.evenement.organisation.interne.HandleStatus;
import ch.vd.uniregctb.tiers.CategorieEntrepriseHelper;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.type.CategorieEntreprise;

/**
 * @author Raphaël Marmier, 2015-11-10
 */
public class Radiation extends EvenementOrganisationInterneDeTraitement {

	private final RegDate dateApres;
	private final StatusInscriptionRC statusInscriptionRCApres;
	private final StatusRegistreIDE statusRegistreIDEApres;

	protected Radiation(EvenementOrganisation evenement, Organisation organisation,
	                    Entreprise entreprise, EvenementOrganisationContext context,
	                    EvenementOrganisationOptions options) throws EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options);

		dateApres = evenement.getDateEvenement();

		final SiteOrganisation sitePrincipalApres = organisation.getSitePrincipal(dateApres).getPayload();

		statusInscriptionRCApres = sitePrincipalApres.getDonneesRC().getStatusInscription(dateApres);
		statusRegistreIDEApres = sitePrincipalApres.getDonneesRegistreIDE().getStatus(dateApres);
	}

	@Override
	public String describe() {
		return "Radiation";
	}


	@Override
	public void doHandle(EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		if (isRadieIDE(statusRegistreIDEApres)) {
			warnings.addWarning("Une vérification manuelle est requise pour cause de Radiation de l'entreprise.");
		} else {
			if (CategorieEntrepriseHelper.getCategorieEntreprise(getOrganisation(), dateApres) != CategorieEntreprise.APM) {
				throw new EvenementOrganisationException("Entreprise radiée du RC, toujours présente de l'IDE, mais pas une APM.");
			}
			suivis.addSuivi("Aucune action requise pour une APM désinscrite du RC toujours en activité.");
		}
		raiseStatusTo(HandleStatus.TRAITE);
	}

	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException {
		Assert.isTrue(isRadieRC(statusInscriptionRCApres));
	}

	protected boolean isRadieIDE(StatusRegistreIDE statusRegistreIDE) {
		return statusRegistreIDE != null && (statusRegistreIDE == StatusRegistreIDE.RADIE || statusRegistreIDE == StatusRegistreIDE.DEFINITIVEMENT_RADIE);
	}

	private boolean isRadieRC(StatusInscriptionRC statusInscriptionRC) {
		return statusInscriptionRC != null && statusInscriptionRC == StatusInscriptionRC.RADIE;
	}
}
