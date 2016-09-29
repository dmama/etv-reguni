package ch.vd.unireg.interfaces.organisation.mock.data.builder;

import java.math.BigDecimal;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.InscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeOrganisationRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.builder.CapitalBuilder;
import ch.vd.unireg.interfaces.organisation.mock.data.MockDonneesRC;
import ch.vd.unireg.interfaces.organisation.mock.data.MockDonneesREE;
import ch.vd.unireg.interfaces.organisation.mock.data.MockDonneesRegistreIDE;
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
	                                           FormeLegale formeLegale,
	                                           @Nullable Boolean principal,
	                                           @Nullable TypeAutoriteFiscale typeAutoriteFiscaleDomicile,
	                                           @Nullable Integer noOfsDomicile,
	                                           @Nullable StatusInscriptionRC statusInscriptionRC,
	                                           @Nullable RegDate dateInscriptionRC,
	                                           @Nullable StatusRegistreIDE statusIde,
	                                           @Nullable TypeOrganisationRegistreIDE typeIde,
	                                           @Nullable String numeroIDE) {
		return addSite(cantonalId, organisation, dateDebut, dateFin, nom, formeLegale, principal, typeAutoriteFiscaleDomicile, noOfsDomicile, statusInscriptionRC, dateInscriptionRC, statusIde, typeIde, numeroIDE, null, null);
	}

	public static MockSiteOrganisation addSite(long cantonalId,
	                                           MockOrganisation organisation,
	                                           RegDate dateDebut,
	                                           RegDate dateFin,
	                                           String nom,
	                                           FormeLegale formeLegale,
	                                           @Nullable Boolean principal,
	                                           @Nullable TypeAutoriteFiscale typeAutoriteFiscaleDomicile,
	                                           @Nullable Integer noOfsDomicile,
	                                           @Nullable StatusInscriptionRC statusInscriptionRC,
	                                           @Nullable RegDate dateInscriptionRC,
	                                           @Nullable StatusRegistreIDE statusIde,
	                                           @Nullable TypeOrganisationRegistreIDE typeIde,
	                                           @Nullable String numeroIDE,
	                                           @Nullable BigDecimal capitalAmount,
	                                           @Nullable String capitalCurrency) {
		MockSiteOrganisation mockSite =  mockSite(cantonalId, dateDebut, dateFin, nom, formeLegale, principal, typeAutoriteFiscaleDomicile, noOfsDomicile, statusInscriptionRC, dateInscriptionRC, statusIde, typeIde, numeroIDE, capitalAmount, capitalCurrency);
		organisation.addDonneesSite(mockSite);
		return mockSite;
	}

	public static MockSiteOrganisation mockSite(long cantonalId,
	                                            RegDate dateDebut,
	                                            RegDate dateFin,
	                                            String nom,
	                                            FormeLegale formeLegale,
	                                            @Nullable Boolean principal,
	                                            @Nullable TypeAutoriteFiscale typeAutoriteFiscaleDomicile,
	                                            @Nullable Integer noOfsDomicile,
	                                            @Nullable StatusInscriptionRC statusInscriptionRC,
	                                            @Nullable RegDate dateInscriptionRC,
	                                            @Nullable StatusRegistreIDE statusIde,
	                                            @Nullable TypeOrganisationRegistreIDE typeIde,
	                                            @Nullable String numeroIDE,
	                                            @Nullable BigDecimal capitalAmount,
	                                            @Nullable String capitalCurrency) {

		final MockDonneesRC donneesRC = new MockDonneesRC();
		if (statusInscriptionRC != null) {
			donneesRC.addInscription(dateDebut, dateFin, new InscriptionRC(statusInscriptionRC, null, dateInscriptionRC, null, dateInscriptionRC, null));
			if (capitalAmount != null) {
				donneesRC.addCapital(dateDebut,
				                     dateFin,
				                     new CapitalBuilder()
						                     .withDateDebut(dateDebut)
						                     .withCapitalAmount(capitalAmount)
						                     .withCurrency(capitalCurrency)
						                     .build()
				);
			}
		}

		MockDonneesRegistreIDE donneesRegistreIDE = new MockDonneesRegistreIDE();
		if (statusIde != null) {
			donneesRegistreIDE.changeStatus(dateDebut, statusIde);
			if (typeIde != null) {
				donneesRegistreIDE.changeTypeOrganisation(dateDebut, typeIde);
			}
		}

		MockDonneesREE donneesREE = new MockDonneesREE();

		final MockSiteOrganisation mock = new MockSiteOrganisation(cantonalId, donneesRegistreIDE, donneesRC, donneesREE);
		mock.changeNom(dateDebut, nom);
		if (dateFin != null) {
			mock.changeNom(dateFin.getOneDayAfter(), null);
		}

		if (numeroIDE != null) {
			mock.changeNumeroIDE(dateDebut, numeroIDE);
		}

		if (principal != null) {
			mock.changeTypeDeSite(dateDebut, principal ? ETABLISSEMENT_PRINCIPAL : ETABLISSEMENT_SECONDAIRE);
			if (dateFin != null) {
				mock.changeTypeDeSite(dateFin.getOneDayAfter(), null);
			}
		}

		mock.changeFormeLegale(dateDebut, formeLegale);
		if (dateFin != null) {
			mock.changeFormeLegale(dateFin.getOneDayAfter(), null);
		}

		if (typeAutoriteFiscaleDomicile != null && noOfsDomicile != null) {
			mock.changeDomicile(dateDebut, typeAutoriteFiscaleDomicile, noOfsDomicile);
			if (dateFin != null) {
				mock.changeDomicile(dateFin.getOneDayAfter(), null, null);
			}
		}
		return mock;
	}
}
