package ch.vd.uniregctb.entreprise;

import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.uniregctb.type.FormeJuridique;

/**
 * @author Raphaël Marmier, 2015-08-27
 */
public class HorribleFormeJuridiqueMapper {

	public static FormeLegale map(FormeJuridique hostFormeJuridique) {
		switch (hostFormeJuridique) {
		case SNC: return FormeLegale.N_0103_SOCIETE_NOM_COLLECIF;
		case SC: return FormeLegale.N_0104_SOCIETE_EN_COMMANDITE;
		case SCA: return FormeLegale.N_0105_SOCIETE_EN_COMMANDITE_PAR_ACTIONS;
		case SA: return FormeLegale.N_0106_SOCIETE_ANONYME;
		case SARL: return FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITE;
		case ASS: return FormeLegale.N_0109_ASSOCIATION;
		case COOP: return FormeLegale.N_0108_SOCIETE_COOPERATIVE;
		case FOND: return FormeLegale.N_0110_FONDATION;
		case EDP: return FormeLegale.N_0234_CORPORATION_DE_DROIT_PUBLIC_ENTREPRISE; // FIXME: Allo c'est juste?
		default: throw new IllegalArgumentException("Forme juridique non reconnue!: " + hostFormeJuridique.name());
		}
	}









}
