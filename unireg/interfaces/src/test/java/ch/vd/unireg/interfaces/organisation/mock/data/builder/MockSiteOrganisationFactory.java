package ch.vd.unireg.interfaces.organisation.mock.data.builder;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.DonneesRC;
import ch.vd.unireg.interfaces.organisation.data.DonneesRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeOrganisationRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.builder.DonneesRCBuilder;
import ch.vd.unireg.interfaces.organisation.data.builder.DonneesRegistreIDEBuilder;
import ch.vd.unireg.interfaces.organisation.mock.data.MockOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.MockSiteOrganisation;

import static ch.vd.unireg.interfaces.organisation.data.TypeDeSite.ETABLISSEMENT_PRINCIPAL;
import static ch.vd.unireg.interfaces.organisation.data.TypeDeSite.ETABLISSEMENT_SECONDAIRE;

/**
 * @author Raphaël Marmier, 2015-07-30
 */
public abstract class MockSiteOrganisationFactory {

	public static MockSiteOrganisation addSite(long cantonalId,
	                                           MockOrganisation organisation,
	                                           RegDate dateDebut,
	                                           String nom,
	                                           @Nullable Boolean principal,
	                                           @Nullable Integer noOfsSiege,
	                                           @Nullable StatusRC statusRC,
	                                           @Nullable StatusInscriptionRC statusInscriptionRC,
	                                           @Nullable StatusRegistreIDE statusIde,
	                                           @Nullable TypeOrganisationRegistreIDE typeIde) {

		final DonneesRC donneesRC;
		if (statusRC != null) {
			final DonneesRCBuilder rcBuilder = new DonneesRCBuilder()
					.addNom(dateDebut, null, nom)
					.addStatus(dateDebut, null, statusRC);
			if (statusInscriptionRC != null) {
				rcBuilder.addStatusInscription(dateDebut, null, statusInscriptionRC);
			}
			donneesRC = rcBuilder.build();
		}
		else {
			donneesRC = null;
		}

		final DonneesRegistreIDE donneesRegistreIDE;
		if (statusIde != null) {
			final DonneesRegistreIDEBuilder idebuilder = new DonneesRegistreIDEBuilder()
					.addStatus(dateDebut, null, statusIde);
			if (typeIde != null) {
				idebuilder.addTypeOrganisation(dateDebut, null, typeIde);
			}
			donneesRegistreIDE = idebuilder.build();
		}
		else {
			donneesRegistreIDE = null;
		}

		final MockSiteOrganisation mock = new MockSiteOrganisation(cantonalId, donneesRegistreIDE, donneesRC);
		organisation.addSiteId(dateDebut, null, cantonalId);
		organisation.addDonneesSite(mock);
		mock.changeNom(dateDebut, nom);

		if (principal != null) {
			mock.changeTypeDeSite(dateDebut, principal ? ETABLISSEMENT_PRINCIPAL : ETABLISSEMENT_SECONDAIRE);
		}

		if (noOfsSiege != null) {
			mock.changeSiege(dateDebut, noOfsSiege);
		}

		return mock;
	}
}
