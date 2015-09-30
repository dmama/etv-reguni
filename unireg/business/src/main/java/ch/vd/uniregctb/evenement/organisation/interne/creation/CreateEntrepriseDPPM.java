package ch.vd.uniregctb.evenement.organisation.interne.creation;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.unireg.interfaces.organisation.data.Capital;
import ch.vd.unireg.interfaces.organisation.data.DonneesRC;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
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

/**
 * Evénement interne de création d'entreprise de catégorie "Personnes morales de droit public" (DP/PM)
 *
 *  Spécification:
 *  - Ti01SE03-Identifier et traiter les mutations entreprise.doc - Version 0.6 - 08.09.2015
 *
 * @author Raphaël Marmier, 2015-09-02
 */
public class CreateEntrepriseDPPM extends CreateEntrepriseBase {

	protected CreateEntrepriseDPPM(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise,
	                               EvenementOrganisationContext context,
	                               EvenementOrganisationOptions options) throws EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options);
	}

	@Override
	public void doHandle(EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException {
		super.doHandle(warnings);
		openForFiscalPrincipal(getDateDeDebut(),
		                       getAutoriteFiscalePrincipale().getTypeAutoriteFiscale(),
		                       getAutoriteFiscalePrincipale().getNoOfs(),
		                       MotifRattachement.DOMICILE,
		                       MotifFor.DEBUT_EXPLOITATION);

		// Création du bouclement
		createAddBouclement(getDateDeDebut());

		raiseStatusTo(HandleStatus.A_VERIFIER);
	}

	private Capital getCapital(DonneesRC donneesRC) {
		return DateRangeHelper.rangeAt(donneesRC.getCapital(), getDateEvt());
	}


	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings) throws EvenementOrganisationException {
		super.validateSpecific(erreurs, warnings);

		if (!inscritAuRC(getSitePrincipal()) || getCapital(getSitePrincipal().getDonneesRC()) == null) {
			throw new EvenementOrganisationException("Capital introuvable"); // TODO: Mettre un message correct dans les erreurs.
		}
	}
}
