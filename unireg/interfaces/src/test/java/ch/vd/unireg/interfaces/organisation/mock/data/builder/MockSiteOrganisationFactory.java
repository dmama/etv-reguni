package ch.vd.unireg.interfaces.organisation.mock.data.builder;

import java.math.BigDecimal;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.DonneesRC;
import ch.vd.unireg.interfaces.organisation.data.DonneesRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeOrganisationRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.builder.CapitalBuilder;
import ch.vd.unireg.interfaces.organisation.data.builder.DonneesRCBuilder;
import ch.vd.unireg.interfaces.organisation.data.builder.DonneesRegistreIDEBuilder;
import ch.vd.unireg.interfaces.organisation.mock.data.MockOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.MockSiteOrganisation;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

import static ch.vd.unireg.interfaces.organisation.data.TypeDeSite.ETABLISSEMENT_PRINCIPAL;
import static ch.vd.unireg.interfaces.organisation.data.TypeDeSite.ETABLISSEMENT_SECONDAIRE;

/**
 * @author Raphaël Marmier, 2015-07-30
 */
public abstract class MockSiteOrganisationFactory {

	public static MockSiteOrganisation addSite(long cantonalId,
	                                           MockOrganisation organisation,
	                                           RegDate dateDebut,
	                                           RegDate dateFin,
	                                           String nom,
	                                           @Nullable Boolean principal,
	                                           @Nullable TypeAutoriteFiscale typeAutoriteFiscaleSiege,
	                                           @Nullable Integer noOfsSiege,
	                                           @Nullable StatusRC statusRC,
	                                           @Nullable StatusInscriptionRC statusInscriptionRC,
	                                           @Nullable StatusRegistreIDE statusIde,
	                                           @Nullable TypeOrganisationRegistreIDE typeIde) {
		return addSite(cantonalId, organisation, dateDebut, dateFin, nom, principal, typeAutoriteFiscaleSiege, noOfsSiege, statusRC, statusInscriptionRC, statusIde, typeIde, null, null);
	}

	public static MockSiteOrganisation addSite(long cantonalId,
	                                           MockOrganisation organisation,
	                                           RegDate dateDebut,
	                                           RegDate dateFin,
	                                           String nom,
	                                           @Nullable Boolean principal,
	                                           @Nullable TypeAutoriteFiscale typeAutoriteFiscaleSiege,
	                                           @Nullable Integer noOfsSiege,
	                                           @Nullable StatusRC statusRC,
	                                           @Nullable StatusInscriptionRC statusInscriptionRC,
	                                           @Nullable StatusRegistreIDE statusIde,
	                                           @Nullable TypeOrganisationRegistreIDE typeIde,
	                                           @Nullable BigDecimal capitalAmount,
	                                           @Nullable String capitalCurrency) {

		final DonneesRC donneesRC;
		final DonneesRCBuilder rcBuilder = new DonneesRCBuilder();
		if (statusRC != null) {
			rcBuilder.addNom(dateDebut, dateFin, nom);
			rcBuilder.addStatus(dateDebut, dateFin, statusRC);
			if (statusInscriptionRC != null) {
				rcBuilder.addStatusInscription(dateDebut, dateFin, statusInscriptionRC);
			}
			if (capitalAmount != null) {
				rcBuilder.addCapital(new CapitalBuilder()
						                     .withDateDebut(dateDebut)
						                     .withCapitalAmount(capitalAmount)
						                     .withCurrency(capitalCurrency)
						                     .build()
				);
			}
		}
		donneesRC = rcBuilder.build();

		final DonneesRegistreIDE donneesRegistreIDE;
		if (statusIde != null) {
			final DonneesRegistreIDEBuilder idebuilder = new DonneesRegistreIDEBuilder()
					.addStatus(dateDebut, dateFin, statusIde);
			if (typeIde != null) {
				idebuilder.addTypeOrganisation(dateDebut, dateFin, typeIde);
			}
			donneesRegistreIDE = idebuilder.build();
		}
		else {
			donneesRegistreIDE = null;
		}

		final MockSiteOrganisation mock = new MockSiteOrganisation(cantonalId, donneesRegistreIDE, donneesRC);
		organisation.addDonneesSite(mock);
		mock.changeNom(dateDebut, nom);
		if (dateFin != null) {
			mock.changeNom(dateFin.getOneDayAfter(), null);
		}

		if (principal != null) {
			mock.changeTypeDeSite(dateDebut, principal ? ETABLISSEMENT_PRINCIPAL : ETABLISSEMENT_SECONDAIRE);
			if (dateFin != null) {
				mock.changeTypeDeSite(dateFin.getOneDayAfter(), null);
			}
		}

		if (typeAutoriteFiscaleSiege != null && noOfsSiege != null) {
			mock.changeSiege(dateDebut, typeAutoriteFiscaleSiege, noOfsSiege);
			if (dateFin != null) {
				mock.changeSiege(dateFin.getOneDayAfter(), null, null);
			}
		}
		return mock;
	}
}
