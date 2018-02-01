package ch.vd.unireg.evenement.organisation.interne.formejuridique;

import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.evenement.organisation.EvenementOrganisation;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationContext;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationException;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.unireg.evenement.organisation.audit.EvenementOrganisationErreurCollector;
import ch.vd.unireg.evenement.organisation.audit.EvenementOrganisationSuiviCollector;
import ch.vd.unireg.evenement.organisation.audit.EvenementOrganisationWarningCollector;
import ch.vd.unireg.evenement.organisation.interne.EvenementOrganisationInterneInformationPure;
import ch.vd.unireg.tiers.Entreprise;

import static ch.vd.unireg.evenement.fiscal.EvenementFiscalInformationComplementaire.TypeInformationComplementaire;

/**
 * @author Raphaël Marmier, 2015-10-15
 */
public class ChangementNeutreFormeJuridique extends EvenementOrganisationInterneInformationPure {

	private final TypeInformationComplementaire typeInfo;

	protected ChangementNeutreFormeJuridique(EvenementOrganisation evenement, Organisation organisation,
	                                         Entreprise entreprise, EvenementOrganisationContext context,
	                                         EvenementOrganisationOptions options) {
		super(evenement, organisation, entreprise, context, options);
		typeInfo = TypeInformationComplementaire.CHANGEMENT_FORME_JURIDIQUE_MEME_CATEGORIE;
	}

	@Override
	public String describe() {
		return "Changement de forme juridique sans changement de régime fiscal.";
	}

	@Override
	public void doHandle(EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		String message = String.format("Envoi d'un événement d'information: %s. Changement neutre de forme juridique. Entreprise n°%s (civil: %d.",
		                               typeInfo.toString(), FormatNumeroHelper.numeroCTBToDisplay(getEntreprise().getNumero()), getNoOrganisation());
		emetEvtFiscalInformation(getDateEvt(), getEntreprise(), typeInfo, message, suivis);
	}

	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {

	}
}
