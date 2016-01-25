package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0022.v1.LegalForm;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;


public class LegalFormConverter extends BaseEnumConverter<LegalForm, FormeLegale> {

	@Override
	@NotNull
	protected FormeLegale convert(@NotNull LegalForm value) {
		switch (value) {
		case N_00_AUTRE:
			return FormeLegale.N_00_AUTRE;
		case N_01_FORMES_JUR_DE_DROIT_PRIVE_UTILISEES_DANS_RC:
			return FormeLegale.N_01_FORMES_JUR_DE_DROIT_PRIVE_UTILISEES_DANS_RC;
		case N_0101_ENTREPRISE_INDIVIDUELLE:
			return FormeLegale.N_0101_ENTREPRISE_INDIVIDUELLE;
		case N_0103_SOCIETE_NOM_COLLECTIF:
			return FormeLegale.N_0103_SOCIETE_NOM_COLLECTIF;
		case N_0104_SOCIETE_EN_COMMANDITE:
			return FormeLegale.N_0104_SOCIETE_EN_COMMANDITE;
		case N_0105_SOCIETE_EN_COMMANDITE_PAR_ACTIONS:
			return FormeLegale.N_0105_SOCIETE_EN_COMMANDITE_PAR_ACTIONS;
		case N_0106_SOCIETE_ANONYME:
			return FormeLegale.N_0106_SOCIETE_ANONYME;
		case N_0107_SOCIETE_A_RESPONSABILITE_LIMITE:
			return FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE;
		case N_0108_SOCIETE_COOPERATIVE:
			return FormeLegale.N_0108_SOCIETE_COOPERATIVE;
		case N_0109_ASSOCIATION:
			return FormeLegale.N_0109_ASSOCIATION;
		case N_0110_FONDATION:
			return FormeLegale.N_0110_FONDATION;
		case N_0111_FILIALE_ETRANGERE_AU_RC:
			return FormeLegale.N_0111_FILIALE_ETRANGERE_AU_RC;
		case N_0113_FORME_JURIDIQUE_PARTICULIERE:
			return FormeLegale.N_0113_FORME_JURIDIQUE_PARTICULIERE;
		case N_0114_SOCIETE_EN_COMMANDITE_POUR_PLACEMENTS_CAPITAUX:
			return FormeLegale.N_0114_SOCIETE_EN_COMMANDITE_POUR_PLACEMENTS_CAPITAUX;
		case N_0115_SOCIETE_INVESTISSEMENT_CAPITAL_VARIABLE:
			return FormeLegale.N_0115_SOCIETE_INVESTISSEMENT_CAPITAL_VARIABLE;
		case N_0116_SOCIETE_INVESTISSEMENT_CAPITAL_FIXE:
			return FormeLegale.N_0116_SOCIETE_INVESTISSEMENT_CAPITAL_FIXE;
		case N_0117_INSTITUT_DE_DROIT_PUBLIC:
			return FormeLegale.N_0117_INSTITUT_DE_DROIT_PUBLIC;
		case N_0118_PROCURATIONS_NON_COMMERCIALES:
			return FormeLegale.N_0118_PROCURATIONS_NON_COMMERCIALES;
		case N_0119_CHEF_INDIVISION:
			return FormeLegale.N_0119_CHEF_INDIVISION;
		case N_0151_SUCCURSALE_SUISSE_AU_RC:
			return FormeLegale.N_0151_SUCCURSALE_SUISSE_AU_RC;
		case N_02_FORMES_JUR_DE_DROIT_PUBLIC_NON_UTILISEES_DANS_RC:
			return FormeLegale.N_02_FORMES_JUR_DE_DROIT_PUBLIC_NON_UTILISEES_DANS_RC;
		case N_0220_ADMINISTRATION_CONFEDERATION:
			return FormeLegale.N_0220_ADMINISTRATION_CONFEDERATION;
		case N_0221_ADMINISTRATION_CANTON:
			return FormeLegale.N_0221_ADMINISTRATION_CANTON;
		case N_0222_ADMINISTRATION_DISTRICT:
			return FormeLegale.N_0222_ADMINISTRATION_DISTRICT;
		case N_0223_ADMINISTRATION_COMMUNE:
			return FormeLegale.N_0223_ADMINISTRATION_COMMUNE;
		case N_0224_CORPORATION_DE_DROIT_PUBLIC_ADMINISTRATION:
			return FormeLegale.N_0224_CORPORATION_DE_DROIT_PUBLIC_ADMINISTRATION;
		case N_0230_ENTREPRISE_CONFEDERATION:
			return FormeLegale.N_0230_ENTREPRISE_CONFEDERATION;
		case N_0231_ENTREPRISE_CANTON:
			return FormeLegale.N_0231_ENTREPRISE_CANTON;
		case N_0232_ENTREPRISE_DISTRICT:
			return FormeLegale.N_0232_ENTREPRISE_DISTRICT;
		case N_0233_ENTREPRISE_COMMUNE:
			return FormeLegale.N_0233_ENTREPRISE_COMMUNE;
		case N_0234_CORPORATION_DE_DROIT_PUBLIC_ENTREPRISE:
			return FormeLegale.N_0234_CORPORATION_DE_DROIT_PUBLIC_ENTREPRISE;
		case N_03_AUTRES_FORMES_JUR_NON_UTILISEES_DANS_RC:
			return FormeLegale.N_03_AUTRES_FORMES_JUR_NON_UTILISEES_DANS_RC;
		case N_0302_SOCIETE_SIMPLE:
			return FormeLegale.N_0302_SOCIETE_SIMPLE;
		case N_0312_FILIALE_ETRANGERE_NON_AU_RC:
			return FormeLegale.N_0312_FILIALE_ETRANGERE_NON_AU_RC;
		case N_0327_ENTREPRISE_PUBLIQUE_ETRANGERE:
			return FormeLegale.N_0327_ENTREPRISE_PUBLIQUE_ETRANGERE;
		case N_0328_ADMINISTRATION_PUBLIQUE_ETRANGERE:
			return FormeLegale.N_0328_ADMINISTRATION_PUBLIQUE_ETRANGERE;
		case N_0329_ORGANISATION_INTERNATIONALE:
			return FormeLegale.N_0329_ORGANISATION_INTERNATIONALE;
		case N_04_ENTREPRISE_ETRANGERE:
			return FormeLegale.N_04_ENTREPRISE_ETRANGERE;
		case N_0441_ENTREPRISE_ETRANGERE:
			return FormeLegale.N_0441_ENTREPRISE_ETRANGERE;
		default:
			throw new IllegalArgumentException(genericUnsupportedValueMessage(value));
		}
	}
}
