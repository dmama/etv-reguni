package ch.vd.unireg.interfaces.organisation.data;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;

/**
 * Les différentes formes légale en provenance du registre des entreprises
 */
public enum FormeLegale {

	N_0101_ENTREPRISE_INDIVIDUELLE("0101", FormeJuridiqueEntreprise.EI.getLibelle()),
	N_0103_SOCIETE_NOM_COLLECTIF("0103", FormeJuridiqueEntreprise.SNC.getLibelle()),
	N_0104_SOCIETE_EN_COMMANDITE("0104", FormeJuridiqueEntreprise.SC.getLibelle()),
	N_0105_SOCIETE_EN_COMMANDITE_PAR_ACTIONS("0105", FormeJuridiqueEntreprise.SCA.getLibelle()),
	N_0106_SOCIETE_ANONYME("0106", FormeJuridiqueEntreprise.SA.getLibelle()),
	N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE("0107", FormeJuridiqueEntreprise.SARL.getLibelle()),
	N_0108_SOCIETE_COOPERATIVE("0108", FormeJuridiqueEntreprise.SCOOP.getLibelle()),
	N_0109_ASSOCIATION("0109", FormeJuridiqueEntreprise.ASSOCIATION.getLibelle()),
	N_0110_FONDATION("0110", FormeJuridiqueEntreprise.FONDATION.getLibelle()),
	N_0111_FILIALE_ETRANGERE_AU_RC("0111", FormeJuridiqueEntreprise.FILIALE_HS_RC.getLibelle()),
	N_0113_FORME_JURIDIQUE_PARTICULIERE("0113", FormeJuridiqueEntreprise.PARTICULIER.getLibelle()),
	N_0114_SOCIETE_EN_COMMANDITE_POUR_PLACEMENTS_CAPITAUX("0114", FormeJuridiqueEntreprise.SCPC.getLibelle()),
	N_0115_SOCIETE_INVESTISSEMENT_CAPITAL_VARIABLE("0115", FormeJuridiqueEntreprise.SICAV.getLibelle()),
	N_0116_SOCIETE_INVESTISSEMENT_CAPITAL_FIXE("0116", FormeJuridiqueEntreprise.SICAF.getLibelle()),
	N_0117_INSTITUT_DE_DROIT_PUBLIC("0117", FormeJuridiqueEntreprise.IDP.getLibelle()),
	N_0118_PROCURATIONS_NON_COMMERCIALES("0118", FormeJuridiqueEntreprise.PNC.getLibelle()),
	N_0119_CHEF_INDIVISION("0119", FormeJuridiqueEntreprise.INDIVISION.getLibelle()),
	N_0151_SUCCURSALE_SUISSE_AU_RC("0151", FormeJuridiqueEntreprise.FILIALE_CH_RC.getLibelle()),
	N_0220_ADMINISTRATION_CONFEDERATION("0220", FormeJuridiqueEntreprise.ADM_CH.getLibelle()),
	N_0221_ADMINISTRATION_CANTON("0221", FormeJuridiqueEntreprise.ADM_CT.getLibelle()),
	N_0222_ADMINISTRATION_DISTRICT("0222", FormeJuridiqueEntreprise.ADM_DI.getLibelle()),
	N_0223_ADMINISTRATION_COMMUNE("0223", FormeJuridiqueEntreprise.ADM_CO.getLibelle()),
	N_0224_CORPORATION_DE_DROIT_PUBLIC_ADMINISTRATION("0224", FormeJuridiqueEntreprise.CORP_DP_ADM.getLibelle()),
	N_0230_ENTREPRISE_CONFEDERATION("0230", FormeJuridiqueEntreprise.ENT_CH.getLibelle()),
	N_0231_ENTREPRISE_CANTON("0231", FormeJuridiqueEntreprise.ENT_CT.getLibelle()),
	N_0232_ENTREPRISE_DISTRICT("0232", FormeJuridiqueEntreprise.ENT_DI.getLibelle()),
	N_0233_ENTREPRISE_COMMUNE("0233", FormeJuridiqueEntreprise.ENT_CO.getLibelle()),
	N_0234_CORPORATION_DE_DROIT_PUBLIC_ENTREPRISE("0234", FormeJuridiqueEntreprise.CORP_DP_ENT.getLibelle()),
	N_0302_SOCIETE_SIMPLE("0302", FormeJuridiqueEntreprise.SS.getLibelle()),
	N_0312_FILIALE_ETRANGERE_NON_AU_RC("0312", FormeJuridiqueEntreprise.FILIALE_HS_NIRC.getLibelle()),
	N_0327_ENTREPRISE_PUBLIQUE_ETRANGERE("0327", FormeJuridiqueEntreprise.ENT_PUBLIQUE_HS.getLibelle()),
	N_0328_ADMINISTRATION_PUBLIQUE_ETRANGERE("0328", FormeJuridiqueEntreprise.ADM_PUBLIQUE_HS.getLibelle()),
	N_0329_ORGANISATION_INTERNATIONALE("0329", FormeJuridiqueEntreprise.ORG_INTERNAT.getLibelle()),
	N_0441_ENTREPRISE_ETRANGERE("0441", FormeJuridiqueEntreprise.ENT_HS.getLibelle());

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

	public String getLibelle() {
		return libelle;
	}

	@Nullable
	public static FormeLegale fromCode(String code) {
		final FormeLegale formeLegale = byCode.get(code);
		if (formeLegale == null) {
			throw new IllegalArgumentException(String.format("Code de forme légale inconnu: %s", code));
		}
		return formeLegale;
	}
}
