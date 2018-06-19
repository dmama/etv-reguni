package ch.vd.unireg.interfaces.entreprise.mock.data.builder;

import java.math.BigDecimal;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.common.CasePostale;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.interfaces.entreprise.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.entreprise.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.data.TypeEntrepriseRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockEntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockEtablissementCivil;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.type.TexteCasePostale;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeAutoriteFiscale;

/**
 * Helper de création pour des cas spécifiques de MockEntrepriseCivile
 */
public abstract class MockEntrepriseFactory {

	public static final MockEntrepriseCivile NESTLE = createNestle();
	public static final MockEntrepriseCivile BCV = createBCV();
	public static final MockEntrepriseCivile KPMG = createKPMG();
	public static final MockEntrepriseCivile CURIA_TREUHAND = createCuriaTreuhand();
	public static final MockEntrepriseCivile JAL_HOLDING = createJalHolding();
	public static final MockEntrepriseCivile BANQUE_COOP = createBanqueCoop();
	public static final MockEntrepriseCivile TENNIS_CLUB_FOREL_SAVIGNY = createTennisClub();

	public static MockEntrepriseCivile createDummySA(long cantonalId, String nom, RegDate dateDebut) {
		FormeLegale formeLegale = FormeLegale.N_0106_SOCIETE_ANONYME;
		return createSimpleEntrepriseRC(cantonalId, cantonalId + 999373737, nom, dateDebut, null, formeLegale, MockCommune.Lausanne);
	}

	/**
	 * Crée une entreprise au RC et à l'IDE. La date d'inscription au RC est trois jours avant la date de l'événement.
	 */
	public static MockEntrepriseCivile createSimpleEntrepriseRC(long cantonalId, long cantonalIdSitePrincipal, String nom, RegDate dateDebut, RegDate dateFin, FormeLegale formeLegale, MockCommune commune) {
		return createEntreprise(cantonalId,
		                        cantonalIdSitePrincipal,
		                        nom,
		                        dateDebut,
		                        dateFin,
		                        formeLegale,
		                        commune != null ? (commune.isVaudoise() ? TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD : TypeAutoriteFiscale.COMMUNE_HC) : null,
		                        commune != null ? commune.getNoOFS() : null,
		                        StatusInscriptionRC.ACTIF,
		                        dateDebut.addDays(-3),
		                        StatusRegistreIDE.DEFINITIF,
		                        TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE,
		                        "CHE999999996"
		);
	}

	/**
	 * Crée une entreprise au RC et à l'IDE. La date d'inscription au RC est trois jours avant la date de l'événement.
	 */
	public static MockEntrepriseCivile createSimpleEntrepriseRC(long cantonalId, long cantonalIdSitePrincipal, String nom, RegDate dateDebut, RegDate dateFin, FormeLegale formeLegale, MockCommune commune, String noIde) {
		return createEntreprise(cantonalId,
		                        cantonalIdSitePrincipal,
		                        nom,
		                        dateDebut,
		                        dateFin,
		                        formeLegale,
		                        commune != null ? (commune.isVaudoise() ? TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD : TypeAutoriteFiscale.COMMUNE_HC) : null,
		                        commune != null ? commune.getNoOFS() : null,
		                        StatusInscriptionRC.ACTIF,
		                        dateDebut.addDays(-3),
		                        StatusRegistreIDE.DEFINITIF,
		                        TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE,
		                        noIde
		);
	}

	/**
	 * Crée une entreprise au RC et à l'IDE. La date d'inscription au RC est trois jours avant la date de l'événement.
	 */
	public static MockEntrepriseCivile createSimpleEntrepriseRC(long cantonalId, long cantonalIdSitePrincipal, String nom, RegDate dateDebut, RegDate dateFin, FormeLegale formeLegale, MockPays pays) {
		return createEntreprise(cantonalId,
		                        cantonalIdSitePrincipal,
		                        nom,
		                        dateDebut,
		                        dateFin,
		                        formeLegale,
		                        pays != null ? TypeAutoriteFiscale.PAYS_HS : null,
		                        pays != null ? pays.getNoOFS() : null,
		                        StatusInscriptionRC.ACTIF,
		                        dateDebut.addDays(-3),
		                        StatusRegistreIDE.DEFINITIF,
		                        TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE,
		                        "CHE999999996"
		);
	}

	public static MockEntrepriseCivile createEntreprise(long cantonalId,
	                                                    long cantonalIdSitePrincipal,
	                                                    String nom,
	                                                    RegDate dateDebut,
	                                                    RegDate dateFin,
	                                                    @Nullable FormeLegale formeLegale,
	                                                    @Nullable TypeAutoriteFiscale typeAutoriteFiscaleSiegePrincipal,
	                                                    @Nullable Integer noOfsSiegePrincipal,
	                                                    @Nullable StatusInscriptionRC statusInscriptionRC,
	                                                    @Nullable RegDate dateInscriptionRC,
	                                                    @Nullable StatusRegistreIDE statusIde,
	                                                    @Nullable TypeEntrepriseRegistreIDE typeIde,
	                                                    @Nullable String numeroIDE) {

		final MockEntrepriseCivile mockEnt = new MockEntrepriseCivile(cantonalId);

		MockEtablissementCivilFactory.addEtablissement(cantonalIdSitePrincipal,
		                                               mockEnt,
		                                               dateDebut,
		                                               dateFin,
		                                               nom,
		                                               formeLegale,
		                                               true,
		                                               typeAutoriteFiscaleSiegePrincipal,
		                                               noOfsSiegePrincipal,
		                                               statusInscriptionRC,
		                                               dateInscriptionRC,
		                                               statusIde,
		                                               typeIde,
		                                               numeroIDE);

		return mockEnt;
	}

	public static MockEntrepriseCivile createEntreprise(long cantonalId,
	                                                    long cantonalIdSitePrincipal,
	                                                    String nom,
	                                                    RegDate dateDebut,
	                                                    RegDate dateFin,
	                                                    @Nullable FormeLegale formeLegale,
	                                                    @Nullable TypeAutoriteFiscale typeAutoriteFiscaleSiegePrincipal,
	                                                    @Nullable Integer noOfsSiegePrincipal,
	                                                    @Nullable StatusInscriptionRC statusInscriptionRC,
	                                                    @Nullable RegDate dateInscriptionRC,
	                                                    @Nullable StatusRegistreIDE statusIde,
	                                                    @Nullable TypeEntrepriseRegistreIDE typeIde,
	                                                    @Nullable String numeroIDE,
	                                                    @Nullable BigDecimal capitalAmount,
	                                                    @Nullable String capitalCurrency) {

		final MockEntrepriseCivile mockEnt = new MockEntrepriseCivile(cantonalId);

		MockEtablissementCivilFactory.addEtablissement(cantonalIdSitePrincipal,
		                                               mockEnt,
		                                               dateDebut,
		                                               dateFin,
		                                               nom,
		                                               formeLegale,
		                                               true,
		                                               typeAutoriteFiscaleSiegePrincipal,
		                                               noOfsSiegePrincipal,
		                                               statusInscriptionRC,
		                                               dateInscriptionRC,
		                                               statusIde,
		                                               typeIde,
		                                               numeroIDE,
		                                               capitalAmount,
		                                               capitalCurrency);

		return mockEnt;
	}

	public static MockEntrepriseCivile createEntrepriseAvecEtablissementSecondaire(long cantonalId,
	                                                                               long cantonalIdSitePrincipal,
	                                                                               long cantonalIdSiteSecondaire,
	                                                                               String nom,
	                                                                               RegDate dateDebut,
	                                                                               RegDate dateFin,
	                                                                               @Nullable FormeLegale formeLegale,
	                                                                               @Nullable TypeAutoriteFiscale tafSiegePrincipal,
	                                                                               @Nullable Integer noOfsSiegePrincipal,
	                                                                               @Nullable TypeAutoriteFiscale tafDomicileSecondaire,
	                                                                               @Nullable Integer noOfsDomicileSecondaire,
	                                                                               @Nullable StatusInscriptionRC statusInscriptionRCPrincipal,
	                                                                               @Nullable RegDate dateInscriptionRCPrincipal,
	                                                                               @Nullable StatusInscriptionRC statusInscriptionRCSecondaire,
	                                                                               @Nullable RegDate dateInscriptionRCSecondaire,
	                                                                               @Nullable StatusRegistreIDE statusIdePrincipal,
	                                                                               @Nullable StatusRegistreIDE statusIdeSecondaire,
	                                                                               @Nullable TypeEntrepriseRegistreIDE typeIdePrincipal,
	                                                                               @Nullable TypeEntrepriseRegistreIDE typeIdeSecondaire,
	                                                                               @Nullable String numeroIDEPrincipal,
	                                                                               @Nullable String numeroIDESecondaire) {

		final MockEntrepriseCivile mockEnt = new MockEntrepriseCivile(cantonalId);

		MockEtablissementCivilFactory.addEtablissement(cantonalIdSitePrincipal,
		                                               mockEnt,
		                                               dateDebut,
		                                               dateFin,
		                                               nom,
		                                               formeLegale,
		                                               true,
		                                               tafSiegePrincipal,
		                                               noOfsSiegePrincipal,
		                                               statusInscriptionRCPrincipal,
		                                               dateInscriptionRCPrincipal,
		                                               statusIdePrincipal,
		                                               typeIdePrincipal,
		                                               numeroIDEPrincipal);

		MockEtablissementCivilFactory.addEtablissement(cantonalIdSiteSecondaire,
		                                               mockEnt,
		                                               dateDebut,
		                                               dateFin,
		                                               nom,
		                                               formeLegale,
		                                               false,
		                                               tafDomicileSecondaire,
		                                               noOfsDomicileSecondaire,
		                                               statusInscriptionRCSecondaire,
		                                               dateInscriptionRCSecondaire,
		                                               statusIdeSecondaire,
		                                               typeIdeSecondaire,
		                                               numeroIDESecondaire);

		return mockEnt;
	}

	/**
	 * @return une entreprise qui ressemble à Nestlé
	 */
	private static MockEntrepriseCivile createNestle() {
		final RegDate dateDebut = RegDate.get(1996, 12, 18);
		final MockEntrepriseCivile ent = createEntreprise(45121L,
		                                                  48751L,
		                                                  "Nestlé Suisse S.A.",
		                                                  dateDebut,
		                                                  null,
		                                                  FormeLegale.N_0106_SOCIETE_ANONYME,
		                                                  TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
		                                                  MockCommune.Vevey.getNoOFS(),
		                                                  StatusInscriptionRC.ACTIF,
		                                                  RegDate.get(1996, 12, 15),
		                                                  StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE,
		                                                  "CHE101237723");
		MockEtablissementCivil etablissement = (MockEtablissementCivil) ent.getEtablissements().get(0);
		ent.addAdresse(new MockAdresse(TypeAdresseCivil.PRINCIPALE, null, "Entre-Deux-Villes", MockLocalite.Vevey, dateDebut, null));
		ent.addAdresse(new MockAdresse(TypeAdresseCivil.COURRIER, new CasePostale(TexteCasePostale.CASE_POSTALE, 352), "pa Myriam Steiner", MockLocalite.Vevey, dateDebut, null));
		return ent;
	}

	/**
	 * @return une entreprise qui ressemble à la BCV
	 */
	private static MockEntrepriseCivile createBCV() {
		final RegDate dateDebut = RegDate.get(1883, 1, 6);
		final MockEntrepriseCivile ent = createEntreprise(45518L, 481554L,
		                                                  "Banque Cantonale Vaudoise",
		                                                  dateDebut,
		                                                  null,
		                                                  FormeLegale.N_0234_CORPORATION_DE_DROIT_PUBLIC_ENTREPRISE,
		                                                  TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
		                                                  MockCommune.Lausanne.getNoOFS(),
		                                                  StatusInscriptionRC.ACTIF,
		                                                  RegDate.get(1883, 1, 2),
		                                                  StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE,
		                                                  "CHE105934376");
		MockEtablissementCivil etablissement = (MockEtablissementCivil) ent.getEtablissements().get(0);
		ent.addAdresse(new MockAdresse(TypeAdresseCivil.PRINCIPALE, null, "Place Saint-François 14", MockLocalite.Lausanne, dateDebut, null));
		return ent;
	}

	private static MockEntrepriseCivile createKPMG() {
		final RegDate dateDebut = RegDate.get(1901, 1, 1);
		final MockEntrepriseCivile ent = createEntreprise(81574L, 8157L,
		                                                  "KPMG SA",
		                                                  dateDebut,
		                                                  null,
		                                                  FormeLegale.N_0106_SOCIETE_ANONYME,
		                                                  TypeAutoriteFiscale.COMMUNE_HC,
		                                                  MockCommune.Zurich.getNoOFS(),
		                                                  StatusInscriptionRC.ACTIF,
		                                                  RegDate.get(1900, 12, 28),
		                                                  StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE,
		                                                  "CHE269292664");
		MockEtablissementCivil etablissement = (MockEtablissementCivil) ent.getEtablissements().get(0);
		ent.addAdresse(new MockAdresse(TypeAdresseCivil.PRINCIPALE, null, "Badenerstrasse 172", MockLocalite.Zurich8004, dateDebut, null));
		return ent;
	}

	private static MockEntrepriseCivile createCuriaTreuhand() {
		final RegDate dateDebut = RegDate.get(1901, 1, 1);
		final MockEntrepriseCivile ent = createEntreprise(784515L, 418451L,
		                                                  "Curia Treuhand AG",
		                                                  dateDebut,
		                                                  null,
		                                                  FormeLegale.N_0106_SOCIETE_ANONYME,
		                                                  TypeAutoriteFiscale.COMMUNE_HC,
		                                                  MockCommune.Chur.getNoOFS(),
		                                                  StatusInscriptionRC.ACTIF,
		                                                  RegDate.get(1900, 12, 28),
		                                                  StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE,
		                                                  "CHE107060819");
		MockEtablissementCivil etablissement = (MockEtablissementCivil) ent.getEtablissements().get(0);
		ent.addAdresse(new MockAdresse(TypeAdresseCivil.PRINCIPALE, null, "Grabenstrasse 15", MockLocalite.Chur, dateDebut, null));
		return ent;
	}

	private static MockEntrepriseCivile createJalHolding() {
		final RegDate dateDebut = RegDate.get(1975, 12, 24);
		final MockEntrepriseCivile ent = createEntreprise(454585L, 4656484L,
		                                                  "JAL Holding, en liquidation",
		                                                  dateDebut,
		                                                  null,
		                                                  FormeLegale.N_0106_SOCIETE_ANONYME,
		                                                  TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
		                                                  MockCommune.Lausanne.getNoOFS(),
		                                                  StatusInscriptionRC.ACTIF,
		                                                  RegDate.get(1975, 12, 23),
		                                                  StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE,
		                                                  "CHE102392906");
		MockEtablissementCivil etablissement = (MockEtablissementCivil) ent.getEtablissements().get(0);

		final MockAdresse adresse = new MockAdresse(TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.CheminMessidor, null, dateDebut, null);
		adresse.setNumero("5");
		adresse.setTitre("Fidu. Commerce & Industrie S.A.");
		ent.addAdresse(adresse);
		return ent;
	}

	private static MockEntrepriseCivile createBanqueCoop() {
		final RegDate dateDebut = RegDate.get(1971, 3, 23);
		final MockEntrepriseCivile ent = createEntreprise(1874515L, 8791056469L,
		                                                  "Bank Coop AG",
		                                                  dateDebut,
		                                                  null,
		                                                  FormeLegale.N_0106_SOCIETE_ANONYME,
		                                                  TypeAutoriteFiscale.COMMUNE_HC,
		                                                  MockCommune.Bale.getNoOFS(),
		                                                  StatusInscriptionRC.ACTIF,
		                                                  RegDate.get(1971, 3, 20),
		                                                  StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE,
		                                                  "CHE101390939");
		MockEtablissementCivil etablissement = (MockEtablissementCivil) ent.getEtablissements().get(0);
		ent.addAdresse(new MockAdresse(TypeAdresseCivil.PRINCIPALE, null, "Paradestrasse 2", MockLocalite.Bale, dateDebut, null));
		final MockEtablissementCivil etablissementCivil = (MockEtablissementCivil) ent.getEtablissements().get(0);
		etablissementCivil.addNomAdditionnel(dateDebut, null, "Banque Coop SA");
		return ent;
	}

	private static MockEntrepriseCivile createTennisClub() {

		final RegDate dateDebut = RegDate.get(2016, 9, 21);
		final RegDate dateDemenagement = RegDate.get(2017, 8, 23);
		final MockEntrepriseCivile ent = createEntreprise(101830038L, 101830039L,
		                                                  "Tennis-Club Forel-Savigny",
		                                                  dateDebut,
		                                                  null,
		                                                  FormeLegale.N_0109_ASSOCIATION,
		                                                  TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
		                                                  MockCommune.Savigny.getNoOFS(),
		                                                  StatusInscriptionRC.NON_INSCRIT,
		                                                  null,
		                                                  StatusRegistreIDE.DEFINITIF,
		                                                  TypeEntrepriseRegistreIDE.ASSOCIATION,
		                                                  "CHE310742139");

		ent.addAdresse(new MockAdresse(TypeAdresseCivil.COURRIER, null, null, MockLocalite.Savigny, dateDebut, dateDemenagement.getOneDayBefore()));
		ent.addAdresse(new MockAdresse(TypeAdresseCivil.COURRIER, null, "Route de Vevey", MockLocalite.ForelLavaux, dateDemenagement, null));
		ent.addAdresse(new MockAdresse(TypeAdresseCivil.CASE_POSTALE, new CasePostale(TexteCasePostale.CASE_POSTALE, 38), null, MockLocalite.Savigny, RegDate.get(2016, 12, 15), null));

		final MockEtablissementCivil etablissement = (MockEtablissementCivil) ent.getEtablissements().get(0);
		etablissement.changeDomicile(dateDemenagement, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.ForelLavaux.getNoOFS());

		etablissement.addAdresse(new MockAdresse(TypeAdresseCivil.COURRIER, null, null, MockLocalite.Savigny, dateDebut, dateDemenagement.getOneDayBefore()));
		etablissement.addAdresse(new MockAdresse(TypeAdresseCivil.COURRIER, null, "Route de Vevey", MockLocalite.ForelLavaux, dateDemenagement, null));
		etablissement.addAdresse(new MockAdresse(TypeAdresseCivil.CASE_POSTALE, new CasePostale(TexteCasePostale.CASE_POSTALE, 38), null, MockLocalite.Savigny, RegDate.get(2016, 12, 15), null));

		return ent;
	}
}
