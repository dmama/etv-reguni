package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0022.v3.LegalForm;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;


public class FormeLegaleConverter  extends BaseEnumConverter<FormeLegale, LegalForm> {

	@NotNull
	@Override
	public LegalForm convert(@NotNull FormeLegale value) {
		switch (value) {
		case N_0101_ENTREPRISE_INDIVIDUELLE:
			return LegalForm.N_0101_ENTREPRISE_INDIVIDUELLE;
		case N_0103_SOCIETE_NOM_COLLECTIF:
			return LegalForm.N_0103_SOCIETE_NOM_COLLECTIF;
		case N_0104_SOCIETE_EN_COMMANDITE:
			return LegalForm.N_0104_SOCIETE_EN_COMMANDITE;
		case N_0105_SOCIETE_EN_COMMANDITE_PAR_ACTIONS:
			return LegalForm.N_0105_SOCIETE_EN_COMMANDITE_PAR_ACTIONS;
		case N_0106_SOCIETE_ANONYME:
			return LegalForm.N_0106_SOCIETE_ANONYME;
		case N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE:
			return LegalForm.N_0107_SOCIETE_A_RESPONSABILITE_LIMITE;
		case N_0108_SOCIETE_COOPERATIVE:
			return LegalForm.N_0108_SOCIETE_COOPERATIVE;
		case N_0109_ASSOCIATION:
			return LegalForm.N_0109_ASSOCIATION;
		case N_0110_FONDATION:
			return LegalForm.N_0110_FONDATION;
		case N_0111_FILIALE_ETRANGERE_AU_RC:
			return LegalForm.N_0111_FILIALE_ETRANGERE_AU_RC;
		case N_0113_FORME_JURIDIQUE_PARTICULIERE:
			return LegalForm.N_0113_FORME_JURIDIQUE_PARTICULIERE;
		case N_0114_SOCIETE_EN_COMMANDITE_POUR_PLACEMENTS_CAPITAUX:
			return LegalForm.N_0114_SOCIETE_EN_COMMANDITE_POUR_PLACEMENTS_CAPITAUX;
		case N_0115_SOCIETE_INVESTISSEMENT_CAPITAL_VARIABLE:
			return LegalForm.N_0115_SOCIETE_INVESTISSEMENT_CAPITAL_VARIABLE;
		case N_0116_SOCIETE_INVESTISSEMENT_CAPITAL_FIXE:
			return LegalForm.N_0116_SOCIETE_INVESTISSEMENT_CAPITAL_FIXE;
		case N_0117_INSTITUT_DE_DROIT_PUBLIC:
			return LegalForm.N_0117_INSTITUT_DE_DROIT_PUBLIC;
		case N_0118_PROCURATIONS_NON_COMMERCIALES:
			return LegalForm.N_0118_PROCURATIONS_NON_COMMERCIALES;
		case N_0119_CHEF_INDIVISION:
			return LegalForm.N_0119_CHEF_INDIVISION;
		case N_0151_SUCCURSALE_SUISSE_AU_RC:
			return LegalForm.N_0151_SUCCURSALE_SUISSE_AU_RC;
		case N_0220_ADMINISTRATION_CONFEDERATION:
			return LegalForm.N_0220_ADMINISTRATION_CONFEDERATION;
		case N_0221_ADMINISTRATION_CANTON:
			return LegalForm.N_0221_ADMINISTRATION_CANTON;
		case N_0222_ADMINISTRATION_DISTRICT:
			return LegalForm.N_0222_ADMINISTRATION_DISTRICT;
		case N_0223_ADMINISTRATION_COMMUNE:
			return LegalForm.N_0223_ADMINISTRATION_COMMUNE;
		case N_0224_CORPORATION_DE_DROIT_PUBLIC_ADMINISTRATION:
			return LegalForm.N_0224_CORPORATION_DE_DROIT_PUBLIC_ADMINISTRATION;
		case N_0230_ENTREPRISE_CONFEDERATION:
			return LegalForm.N_0230_ENTREPRISE_CONFEDERATION;
		case N_0231_ENTREPRISE_CANTON:
			return LegalForm.N_0231_ENTREPRISE_CANTON;
		case N_0232_ENTREPRISE_DISTRICT:
			return LegalForm.N_0232_ENTREPRISE_DISTRICT;
		case N_0233_ENTREPRISE_COMMUNE:
			return LegalForm.N_0233_ENTREPRISE_COMMUNE;
		case N_0234_CORPORATION_DE_DROIT_PUBLIC_ENTREPRISE:
			return LegalForm.N_0234_CORPORATION_DE_DROIT_PUBLIC_ENTREPRISE;
		case N_0302_SOCIETE_SIMPLE:
			return LegalForm.N_0302_SOCIETE_SIMPLE;
		case N_0312_FILIALE_ETRANGERE_NON_AU_RC:
			return LegalForm.N_0312_FILIALE_ETRANGERE_NON_AU_RC;
		case N_0327_ENTREPRISE_PUBLIQUE_ETRANGERE:
			return LegalForm.N_0327_ENTREPRISE_PUBLIQUE_ETRANGERE;
		case N_0328_ADMINISTRATION_PUBLIQUE_ETRANGERE:
			return LegalForm.N_0328_ADMINISTRATION_PUBLIQUE_ETRANGERE;
		case N_0329_ORGANISATION_INTERNATIONALE:
			return LegalForm.N_0329_ORGANISATION_INTERNATIONALE;
		case N_0441_ENTREPRISE_ETRANGERE:
			return LegalForm.N_0441_ENTREPRISE_ETRANGERE;
		default:
			throw new IllegalArgumentException(genericUnsupportedValueMessage(value));
		}
	}
}
