package ch.vd.unireg.interfaces.organisation.data;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

/**
 * Les différentes formes légale en provenance du registre des entreprises
 */
public enum FormeLegale {

	N_00_AUTRE("00", "Autre"),
	N_01_FORMES_JUR_DE_DROIT_PRIVE_UTILISEES_DANS_RC("01", "Formes juridiques de droit privé"),
	N_0101_ENTREPRISE_INDIVIDUELLE("0101", "Entreprise individuelle"),
	N_0103_SOCIETE_NOM_COLLECTIF("0103", "Société en nom collectif"),
	N_0104_SOCIETE_EN_COMMANDITE("0104", "Société en commandite"),
	N_0105_SOCIETE_EN_COMMANDITE_PAR_ACTIONS("0105", "Société en commandite par actions"),
	N_0106_SOCIETE_ANONYME("0106", "Société anonyme"),
	N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE("0107", "Société à responsabilité limitée"),
	N_0108_SOCIETE_COOPERATIVE("0108", "Société coopérative"),
	N_0109_ASSOCIATION("0109", "Association"),
	N_0110_FONDATION("0110", "Fondation"),
	N_0111_FILIALE_ETRANGERE_AU_RC("0111", "Filiale étrangère inscrite au registre du commerce"),
	N_0113_FORME_JURIDIQUE_PARTICULIERE("0113", "Forme juridique particulière"),
	N_0114_SOCIETE_EN_COMMANDITE_POUR_PLACEMENTS_CAPITAUX("0114", "Société en commandite pour les placements collectifs de capitaux"),
	N_0115_SOCIETE_INVESTISSEMENT_CAPITAL_VARIABLE("0115", "Société d’investissement à capital variable (SICAV)"),
	N_0116_SOCIETE_INVESTISSEMENT_CAPITAL_FIXE("0116", "Société d’investissement à capital fixe (SICAF)"),
	N_0117_INSTITUT_DE_DROIT_PUBLIC("0117", "Institut de droit public"),
	N_0118_PROCURATIONS_NON_COMMERCIALES("0118", "Procurations non-commerciales"),
	N_0119_CHEF_INDIVISION("0119", "Chef d’indivision"),
	N_0151_SUCCURSALE_SUISSE_AU_RC("0151", "Succursale suisse inscrite au registre du commerce"),
	N_02_FORMES_JUR_DE_DROIT_PUBLIC_NON_UTILISEES_DANS_RC("02", "Formes juridiques de droit public"),
	N_0220_ADMINISTRATION_CONFEDERATION("0220", "Administration de la Confédération"),
	N_0221_ADMINISTRATION_CANTON("0221", "Administration du canton"),
	N_0222_ADMINISTRATION_DISTRICT("0222", "Administration du district"),
	N_0223_ADMINISTRATION_COMMUNE("0223", "Administration de la commune"),
	N_0224_CORPORATION_DE_DROIT_PUBLIC_ADMINISTRATION("0224", "Corporation de droit public (administration)"),
	N_0230_ENTREPRISE_CONFEDERATION("0230", "Entreprise de la Confédération"),
	N_0231_ENTREPRISE_CANTON("0231", "Entreprise du canton"),
	N_0232_ENTREPRISE_DISTRICT("0232", "Entreprise du district"),
	N_0233_ENTREPRISE_COMMUNE("0233", "Entreprise de la commune"),
	N_0234_CORPORATION_DE_DROIT_PUBLIC_ENTREPRISE("0234", "Entreprise de la commune"),
	N_03_AUTRES_FORMES_JUR_NON_UTILISEES_DANS_RC("03", "Autres formes juridiques non utilisées dans le registre du commerce"),
	N_0302_SOCIETE_SIMPLE("0302", "Société simple"),
	N_0312_FILIALE_ETRANGERE_NON_AU_RC("0312", "Filiale étrangère non inscrite au registre du commerce"),
	N_0327_ENTREPRISE_PUBLIQUE_ETRANGERE("0327", "Entreprise publique étrangère"),
	N_0328_ADMINISTRATION_PUBLIQUE_ETRANGERE("0328", "Administration publique étrangère"),
	N_0329_ORGANISATION_INTERNATIONALE("0329", "Organisation internationale"),
	N_04_ENTREPRISE_ETRANGERE("04", "Entreprise étrangère"),
	N_0441_ENTREPRISE_ETRANGERE("0441", "Entreprise étrangère");

	private final String code;

	private static final Map<String, FormeLegale> byCode;

	static {
		byCode = new HashMap<>(FormeLegale.values().length);
		for (FormeLegale fl : FormeLegale.values()) {
			final FormeLegale old = byCode.put(fl.code, fl);
			if (old != null) {
				throw new IllegalArgumentException(String.format("Code %s utilisé plusieurs fois !", old.code));
			}
		}
	}

	private final String libelle;

	FormeLegale(String code, String libelle) {
		this.code = code;
		this.libelle = libelle;
	}

	@Override
	public String toString() {
		return String.format("(%s) %s", code, libelle);
	}

	public String getCode() {
		return this.code;
	}

	@Nullable
	public static FormeLegale fromCode(String code) {
		return byCode.get(code);
	}
}
