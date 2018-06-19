package ch.vd.unireg.interfaces.entreprise.mock.data.builder;

import java.math.BigDecimal;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.interfaces.entreprise.data.InscriptionRC;
import ch.vd.unireg.interfaces.entreprise.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.entreprise.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.data.TypeEntrepriseRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.data.builder.CapitalBuilder;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockDonneesRC;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockDonneesREE;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockDonneesRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockEntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockEtablissementCivil;
import ch.vd.unireg.type.TypeAutoriteFiscale;

import static ch.vd.unireg.interfaces.entreprise.data.TypeEtablissementCivil.ETABLISSEMENT_PRINCIPAL;
import static ch.vd.unireg.interfaces.entreprise.data.TypeEtablissementCivil.ETABLISSEMENT_SECONDAIRE;

/**
 * @author Raphaël Marmier, 2015-07-30
 */
public abstract class MockEtablissementCivilFactory {

	public static MockEtablissementCivil addEtablissement(long cantonalId,
	                                                      MockEntrepriseCivile entreprise,
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
	                                                      @Nullable TypeEntrepriseRegistreIDE typeIde,
	                                                      @Nullable String numeroIDE) {
		return addEtablissement(cantonalId, entreprise, dateDebut, dateFin, nom, formeLegale, principal, typeAutoriteFiscaleDomicile, noOfsDomicile, statusInscriptionRC, dateInscriptionRC, statusIde, typeIde, numeroIDE, null, null);
	}

	public static MockEtablissementCivil addEtablissement(long cantonalId,
	                                                      MockEntrepriseCivile entreprise,
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
	                                                      @Nullable TypeEntrepriseRegistreIDE typeIde,
	                                                      @Nullable String numeroIDE,
	                                                      @Nullable BigDecimal capitalAmount,
	                                                      @Nullable String capitalCurrency) {
		MockEtablissementCivil
				mockSite =  mockSite(cantonalId, dateDebut, dateFin, nom, formeLegale, principal, typeAutoriteFiscaleDomicile, noOfsDomicile, statusInscriptionRC, dateInscriptionRC, statusIde, typeIde, numeroIDE, capitalAmount, capitalCurrency);
		entreprise.addDonneesEtablissement(mockSite);
		return mockSite;
	}

	public static MockEtablissementCivil mockSite(long cantonalId,
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
	                                              @Nullable TypeEntrepriseRegistreIDE typeIde,
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
				donneesRegistreIDE.changeTypeEntreprise(dateDebut, typeIde);
			}
		}

		MockDonneesREE donneesREE = new MockDonneesREE();

		final MockEtablissementCivil mock = new MockEtablissementCivil(cantonalId, donneesRegistreIDE, donneesRC, donneesREE);
		mock.changeNom(dateDebut, nom);
		if (dateFin != null) {
			mock.changeNom(dateFin.getOneDayAfter(), null);
		}

		if (numeroIDE != null) {
			mock.changeNumeroIDE(dateDebut, numeroIDE);
		}

		if (principal != null) {
			mock.changeTypeEtablissement(dateDebut, principal ? ETABLISSEMENT_PRINCIPAL : ETABLISSEMENT_SECONDAIRE);
			if (dateFin != null) {
				mock.changeTypeEtablissement(dateFin.getOneDayAfter(), null);
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
