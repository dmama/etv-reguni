package ch.vd.uniregctb.type;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

public enum FormeJuridiqueEntreprise {

	EI("Entreprise individuelle", "0101", TypeFormeJuridique.DROIT_PRIVE, false),
	SNC("Société en nom collectif", "0103", TypeFormeJuridique.DROIT_PRIVE, false),
	SC("Société en commandite", "0104", TypeFormeJuridique.DROIT_PRIVE, false),
	SCA("Société en commandite par actions", "0105", TypeFormeJuridique.DROIT_PRIVE, false),
	SA("Société anonyme", "0106", TypeFormeJuridique.DROIT_PRIVE, false),
	SARL("Société à responsabilité limitée", "0107", TypeFormeJuridique.DROIT_PRIVE, false),
	SCOOP("Société coopérative", "0108", TypeFormeJuridique.DROIT_PRIVE, false),
	ASSOCIATION("Association", "0109", TypeFormeJuridique.DROIT_PRIVE, true),
	FONDATION("Fondation", "0110", TypeFormeJuridique.DROIT_PRIVE, true),
	FILIALE_HS_RC("Filiale étrangère inscrite au registre du commerce", "0111", TypeFormeJuridique.DROIT_PRIVE, true),
	PARTICULIER("Forme juridique particulière", "0113", TypeFormeJuridique.DROIT_PRIVE, false),
	SCPC("Société en commandite de placements collectifs", "0114", TypeFormeJuridique.DROIT_PRIVE, false),          // fonds de placement ?
	SICAV("Société d'investissement à capital variable (SICAV)", "0115", TypeFormeJuridique.DROIT_PRIVE, false),
	SICAF("Socitété d'investissement à capital fixe (SICAF)", "0116", TypeFormeJuridique.DROIT_PRIVE, false),
	IDP("Institut de droit public", "0117", TypeFormeJuridique.DROIT_PRIVE, false),
	PNC("Procuration non-commerciale", "0118", TypeFormeJuridique.DROIT_PRIVE, false),
	INDIVISION("Chef d'indivision", "0119", TypeFormeJuridique.DROIT_PRIVE, false),
	FILIALE_CH_RC("Succursale suisse inscrite au registre du commerce", "0151", TypeFormeJuridique.DROIT_PRIVE, false),

	ADM_CH("Administration fédérale", "0220", TypeFormeJuridique.DROIT_PUBLIC, false),
	ADM_CT("Administration cantonale", "0221", TypeFormeJuridique.DROIT_PUBLIC, false),
	ADM_DI("Administration de district", "0222", TypeFormeJuridique.DROIT_PUBLIC, false),
	ADM_CO("Administration communale", "0223", TypeFormeJuridique.DROIT_PUBLIC, false),
	CORP_DP_ADM("Corporation de droit public (administration)", "0224", TypeFormeJuridique.DROIT_PUBLIC, false),
	ENT_CH("Entreprise fédérale", "0230", TypeFormeJuridique.DROIT_PUBLIC, false),
	ENT_CT("Entreprise cantonale", "0231", TypeFormeJuridique.DROIT_PUBLIC, false),
	ENT_DI("Entreprise de district", "0232", TypeFormeJuridique.DROIT_PUBLIC, false),
	ENT_CO("Entreprise communale", "0233", TypeFormeJuridique.DROIT_PUBLIC, false),
	CORP_DP_ENT("Corporation de droit public (entreprise)", "0234", TypeFormeJuridique.DROIT_PUBLIC, false),

	SS("Société simple", "0302", TypeFormeJuridique.NON_RC, false),
	FILIALE_HS_NIRC("Filiale étrangère non-inscrite au registre du commerce", "0312", TypeFormeJuridique.NON_RC, false),
	ENT_PUBLIQUE_HS("Entreprise publique étrangère", "0327", TypeFormeJuridique.NON_RC, false),
	ADM_PUBLIQUE_HS("Administration publique étrangère", "0328", TypeFormeJuridique.NON_RC, false),
	ORG_INTERNAT("Organisation internationale", "0329", TypeFormeJuridique.NON_RC, false),

	ENT_HS("Entreprise étrangère", "0441", TypeFormeJuridique.ETRANGER, false);

	private final String codeECH;
	private final String libellé;
	private final TypeFormeJuridique type;
	private final boolean creable;

	FormeJuridiqueEntreprise(String libelle, String codeECH, TypeFormeJuridique type, boolean creable) {
		if (StringUtils.isBlank(codeECH)) {
			throw new IllegalArgumentException("Toute forme juridique utilisée ici doit avoir une codification selon eCH-0097.");
		}
		if (StringUtils.isBlank(libelle)) {
			throw new IllegalArgumentException("Toute forme juridique utilisée ici doit avoir un libellé.");
		}
		this.codeECH = codeECH;
		this.libellé = libelle;
		this.type = type;
		this.creable = creable;
	}

	@NotNull
	public String getCodeECH() {
		return codeECH;
	}

	@NotNull
	public String getLibelle() {
		return libellé;
	}

	public TypeFormeJuridique getType() {
		return type;
	}

	public boolean isCreable() {
		return creable;
	}
}
