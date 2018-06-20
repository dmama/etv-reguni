package ch.vd.unireg.evenement.entreprise.interne.information;

import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseContext;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseException;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseOptions;
import ch.vd.unireg.evenement.entreprise.audit.EvenementEntrepriseSuiviCollector;
import ch.vd.unireg.evenement.entreprise.audit.EvenementEntrepriseWarningCollector;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.tiers.Entreprise;

import static ch.vd.unireg.evenement.fiscal.EvenementFiscalInformationComplementaire.TypeInformationComplementaire;

/**
 * @author Raphaël Marmier, 2015-10-15
 */
public class InformationComplementaireAVerifier extends InformationComplementaire {

	protected InformationComplementaireAVerifier(EvenementEntreprise evenement, EntrepriseCivile entrepriseCivile,
	                                             Entreprise entreprise, EvenementEntrepriseContext context,
	                                             EvenementEntrepriseOptions options, TypeInformationComplementaire typeInfo) throws EvenementEntrepriseException {
		super(evenement, entrepriseCivile, entreprise, context, options, typeInfo);
	}

	@Override
	public void doHandle(EvenementEntrepriseWarningCollector warnings, EvenementEntrepriseSuiviCollector suivis) throws EvenementEntrepriseException {
		super.doHandle(warnings, suivis);
		warnings.addWarning("Une vérification manuelle est requise pour contrôler la situation de faillite ou le transfert à l’étranger.");
	}

}
