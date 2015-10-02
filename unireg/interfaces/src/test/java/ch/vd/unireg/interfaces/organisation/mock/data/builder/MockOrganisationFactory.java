package ch.vd.unireg.interfaces.organisation.mock.data.builder;

import java.math.BigDecimal;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.common.CasePostale;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeOrganisationRegistreIDE;
import ch.vd.unireg.interfaces.organisation.mock.data.MockOrganisation;
import ch.vd.uniregctb.type.TexteCasePostale;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Helper de création pour des cas spécifiques de MockOrganisation
 */
public abstract class MockOrganisationFactory {

	public static final MockOrganisation NESTLE = createNestle();
	public static final MockOrganisation BCV = createBCV();
	public static final MockOrganisation KPMG = createKPMG();
	public static final MockOrganisation CURIA_TREUHAND = createCuriaTreuhand();
	public static final MockOrganisation JAL_HOLDING = createJalHolding();
	public static final MockOrganisation BANQUE_COOP = createBanqueCoop();

	public static MockOrganisation createDummySA(long cantonalId, String nom, RegDate dateDebut) {
		FormeLegale formeLegale = FormeLegale.N_0106_SOCIETE_ANONYME;
		return createSimpleEntrepriseRC(cantonalId, cantonalId + 999373737, nom, dateDebut, formeLegale, MockCommune.Lausanne);
	}

	public static MockOrganisation createSimpleEntrepriseRC(long cantonalId, long cantonalIdSitePrincipal, String nom, RegDate dateDebut, FormeLegale formeLegale, MockCommune commune) {
		return createOrganisation(cantonalId,
		                          cantonalIdSitePrincipal,
		                          nom,
		                          dateDebut,
		                          formeLegale,
		                          commune != null ? (commune.isVaudoise() ? TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD : TypeAutoriteFiscale.COMMUNE_HC) : null,
		                          commune != null ? commune.getNoOFS() : null,
		                          StatusRC.INSCRIT,
		                          StatusInscriptionRC.ACTIF,
		                          StatusRegistreIDE.DEFINITIF,
		                          TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE
		);
	}

	public static MockOrganisation createSimpleEntrepriseRC(long cantonalId, long cantonalIdSitePrincipal, String nom, RegDate dateDebut, FormeLegale formeLegale, MockPays pays) {
		return createOrganisation(cantonalId,
		                          cantonalIdSitePrincipal,
		                          nom,
		                          dateDebut,
		                          formeLegale,
		                          pays != null ? TypeAutoriteFiscale.PAYS_HS : null,
		                          pays != null ? pays.getNoOFS() : null,
		                          StatusRC.INSCRIT,
		                          StatusInscriptionRC.ACTIF,
		                          StatusRegistreIDE.DEFINITIF,
		                          TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE
		);
	}

	public static MockOrganisation createOrganisation(long cantonalId,
	                                                  long cantonalIdSitePrincipal,
	                                                  String nom,
	                                                  RegDate dateDebut,
	                                                  @Nullable FormeLegale formeLegale,
	                                                  @Nullable TypeAutoriteFiscale typeAutoriteFiscaleSiegePrincipal,
	                                                  @Nullable Integer noOfsSiegePrincipal,
	                                                  @Nullable StatusRC statusRC,
	                                                  @Nullable StatusInscriptionRC statusInscriptionRC,
	                                                  @Nullable StatusRegistreIDE statusIde,
	                                                  @Nullable TypeOrganisationRegistreIDE typeIde) {

		final MockOrganisation mockOrg = new MockOrganisation(cantonalId, dateDebut, nom, formeLegale);

		MockSiteOrganisationFactory.addSite(cantonalIdSitePrincipal,
		                                    mockOrg,
		                                    dateDebut,
		                                    nom,
		                                    true,
		                                    typeAutoriteFiscaleSiegePrincipal,
		                                    noOfsSiegePrincipal,
		                                    statusRC,
		                                    statusInscriptionRC,
		                                    statusIde,
		                                    typeIde);

		return mockOrg;
	}

	public static MockOrganisation createOrganisation(long cantonalId,
	                                                  long cantonalIdSitePrincipal,
	                                                  String nom,
	                                                  RegDate dateDebut,
	                                                  @Nullable FormeLegale formeLegale,
	                                                  @Nullable TypeAutoriteFiscale typeAutoriteFiscaleSiegePrincipal,
	                                                  @Nullable Integer noOfsSiegePrincipal,
	                                                  @Nullable StatusRC statusRC,
	                                                  @Nullable StatusInscriptionRC statusInscriptionRC,
	                                                  @Nullable StatusRegistreIDE statusIde,
	                                                  @Nullable TypeOrganisationRegistreIDE typeIde,
	                                                  @Nullable BigDecimal capitalAmount,
	                                                  @Nullable String capitalCurrency) {

		final MockOrganisation mockOrg = new MockOrganisation(cantonalId, dateDebut, nom, formeLegale);

		MockSiteOrganisationFactory.addSite(cantonalIdSitePrincipal,
		                                    mockOrg,
		                                    dateDebut,
		                                    nom,
		                                    true,
		                                    typeAutoriteFiscaleSiegePrincipal,
		                                    noOfsSiegePrincipal,
		                                    statusRC,
		                                    statusInscriptionRC,
		                                    statusIde,
		                                    typeIde,
		                                    capitalAmount,
		                                    capitalCurrency);

		return mockOrg;
	}

	public static MockOrganisation createOrganisationAvecSiteSecondaire(long cantonalId,
	                                                                    long cantonalIdSitePrincipal,
	                                                                    long cantonalIdSiteSecondaire,
	                                                                    String nom,
	                                                                    RegDate dateDebut,
	                                                                    @Nullable FormeLegale formeLegale,
	                                                                    @Nullable TypeAutoriteFiscale tafSiegePrincipal,
	                                                                    @Nullable Integer noOfsSiegePrincipal,
	                                                                    @Nullable TypeAutoriteFiscale tafSiegeSecondaire,
	                                                                    @Nullable Integer noOfsSiegeSecondaire,
	                                                                    @Nullable StatusRC statusRCPrincipal,
	                                                                    @Nullable StatusRC statusRCSecondaire,
	                                                                    @Nullable StatusInscriptionRC statusInscriptionRCPrincipal,
	                                                                    @Nullable StatusInscriptionRC statusInscriptionRCSecondaire,
	                                                                    @Nullable StatusRegistreIDE statusIdePrincipal,
	                                                                    @Nullable StatusRegistreIDE statusIdeSecondaire,
	                                                                    @Nullable TypeOrganisationRegistreIDE typeIdePrincipal,
	                                                                    @Nullable TypeOrganisationRegistreIDE typeIdeSecondaire) {

		final MockOrganisation mockOrg = new MockOrganisation(cantonalId, dateDebut, nom, formeLegale);

		MockSiteOrganisationFactory.addSite(cantonalIdSitePrincipal,
		                                    mockOrg,
		                                    dateDebut,
		                                    nom,
		                                    true,
		                                    tafSiegePrincipal,
		                                    noOfsSiegePrincipal,
		                                    statusRCPrincipal,
		                                    statusInscriptionRCPrincipal,
		                                    statusIdePrincipal,
		                                    typeIdePrincipal);

		MockSiteOrganisationFactory.addSite(cantonalIdSiteSecondaire,
		                                    mockOrg,
		                                    dateDebut,
		                                    nom,
		                                    false,
		                                    tafSiegeSecondaire,
		                                    noOfsSiegeSecondaire,
		                                    statusRCSecondaire,
		                                    statusInscriptionRCSecondaire,
		                                    statusIdeSecondaire,
		                                    typeIdeSecondaire);

		return mockOrg;
	}

	/**
	 * @return une organisation qui ressemble à Nestlé
	 */
	private static MockOrganisation createNestle() {
		final RegDate dateDebut = RegDate.get(1996, 12, 18);
		final MockOrganisation org = createOrganisation(45121L,
		                                                48751L,
		                                                "Nestlé Suisse S.A.",
		                                                dateDebut,
		                                                FormeLegale.N_0106_SOCIETE_ANONYME,
		                                                TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
		                                                MockCommune.Vevey.getNoOFS(),
		                                                StatusRC.INSCRIT, StatusInscriptionRC.ACTIF,
		                                                StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE);
		org.addNumeroIDE(dateDebut, null, "CHE10123723");
		org.addAdresse(new MockAdresse(TypeAdresseCivil.PRINCIPALE, null, "Entre-Deux-Villes", MockLocalite.Vevey, dateDebut, null));
		org.addAdresse(new MockAdresse(TypeAdresseCivil.COURRIER, new CasePostale(TexteCasePostale.CASE_POSTALE, 352), "pa Myriam Steiner", MockLocalite.Vevey, dateDebut, null));
		return org;
	}

	/**
	 * @return une organisation qui ressemble à la BCV
	 */
	private static MockOrganisation createBCV() {
		final RegDate dateDebut = RegDate.get(1883, 1, 6);
		final MockOrganisation org = createOrganisation(45518L, 481554L,
		                                                "Banque Cantonale Vaudoise",
		                                                dateDebut,
		                                                FormeLegale.N_0234_CORPORATION_DE_DROIT_PUBLIC_ENTREPRISE,
		                                                TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
		                                                MockCommune.Lausanne.getNoOFS(),
		                                                StatusRC.INSCRIT, StatusInscriptionRC.ACTIF,
		                                                StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE);
		org.addNumeroIDE(dateDebut, null, "CHE105934376");
		org.addAdresse(new MockAdresse(TypeAdresseCivil.PRINCIPALE, null, "Place Saint-François 14", MockLocalite.Lausanne, dateDebut, null));
		return org;
	}

	private static MockOrganisation createKPMG() {
		final RegDate dateDebut = RegDate.get(1901, 1, 1);
		final MockOrganisation org = createOrganisation(81574L, 8157L,
		                                                "KPMG SA",
		                                                dateDebut,
		                                                FormeLegale.N_0106_SOCIETE_ANONYME,
		                                                TypeAutoriteFiscale.COMMUNE_HC,
		                                                MockCommune.Zurich.getNoOFS(),
		                                                StatusRC.INSCRIT, StatusInscriptionRC.ACTIF,
		                                                StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE);
		org.addNumeroIDE(dateDebut, null, "CHE269292664");
		org.addAdresse(new MockAdresse(TypeAdresseCivil.PRINCIPALE, null, "Badenerstrasse 172", MockLocalite.Zurich8004, dateDebut, null));
		return org;
	}

	private static MockOrganisation createCuriaTreuhand() {
		final RegDate dateDebut = RegDate.get(1901, 1, 1);
		final MockOrganisation org = createOrganisation(784515L, 418451L,
		                                                "Curia Treuhand AG",
		                                                dateDebut,
		                                                FormeLegale.N_0106_SOCIETE_ANONYME,
		                                                TypeAutoriteFiscale.COMMUNE_HC,
		                                                MockCommune.Chur.getNoOFS(),
		                                                StatusRC.INSCRIT, StatusInscriptionRC.ACTIF,
		                                                StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE);
		org.addNumeroIDE(dateDebut, null, "CHE107060819");
		org.addAdresse(new MockAdresse(TypeAdresseCivil.PRINCIPALE, null, "Grabenstrasse 15", MockLocalite.Chur, dateDebut, null));
		return org;
	}

	private static MockOrganisation createJalHolding() {
		final RegDate dateDebut = RegDate.get(1975, 12, 24);
		final MockOrganisation org = createOrganisation(454585L, 4656484L,
		                                                "JAL Holding, en liquidation",
		                                                dateDebut,
		                                                FormeLegale.N_0106_SOCIETE_ANONYME,
		                                                TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
		                                                MockCommune.Lausanne.getNoOFS(),
		                                                StatusRC.INSCRIT, StatusInscriptionRC.ACTIF,
		                                                StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE);
		org.addNumeroIDE(dateDebut, null, "CHE102392906");

		final MockAdresse adresse = new MockAdresse(TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.CheminMessidor, null, dateDebut, null);
		adresse.setNumero("5");
		adresse.setTitre("Fidu. Commerce & Industrie S.A.");
		org.addAdresse(adresse);
		return org;
	}

	private static MockOrganisation createBanqueCoop() {
		final RegDate dateDebut = RegDate.get(1971, 3, 23);
		final MockOrganisation org = createOrganisation(1874515L, 8791056469L,
		                                                "Bank Coop AG",
		                                                dateDebut,
		                                                FormeLegale.N_0106_SOCIETE_ANONYME,
		                                                TypeAutoriteFiscale.COMMUNE_HC,
		                                                MockCommune.Bale.getNoOFS(),
		                                                StatusRC.INSCRIT, StatusInscriptionRC.ACTIF,
		                                                StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE);
		org.addNumeroIDE(dateDebut, null, "CHE101390939");
		org.addAdresse(new MockAdresse(TypeAdresseCivil.PRINCIPALE, null, "Paradestrasse 2", MockLocalite.Bale, dateDebut, null));
		org.addNomsAdditionnels(dateDebut, null, "Banque Coop SA");
		return org;
	}
}
