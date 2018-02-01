package ch.vd.unireg.evenement.organisation.interne.information;

import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.evenement.organisation.EvenementOrganisation;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationContext;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationException;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.unireg.evenement.organisation.audit.EvenementOrganisationSuiviCollector;
import ch.vd.unireg.evenement.organisation.audit.EvenementOrganisationWarningCollector;
import ch.vd.unireg.tiers.Entreprise;

import static ch.vd.unireg.evenement.fiscal.EvenementFiscalInformationComplementaire.TypeInformationComplementaire;

/**
 * @author Raphaël Marmier, 2015-10-15
 */
public class InformationComplementaireAVerifier extends InformationComplementaire {

	protected InformationComplementaireAVerifier(EvenementOrganisation evenement, Organisation organisation,
	                                             Entreprise entreprise, EvenementOrganisationContext context,
	                                             EvenementOrganisationOptions options, TypeInformationComplementaire typeInfo) throws EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options, typeInfo);
	}

	@Override
	public void doHandle(EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		super.doHandle(warnings, suivis);
		warnings.addWarning("Une vérification manuelle est requise pour contrôler la situation de faillite ou le transfert à l’étranger.");
	}

}
