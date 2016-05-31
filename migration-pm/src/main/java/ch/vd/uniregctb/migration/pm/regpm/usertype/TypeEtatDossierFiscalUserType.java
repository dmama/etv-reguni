package ch.vd.uniregctb.migration.pm.regpm.usertype;

import java.util.HashMap;
import java.util.Map;

import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeEtatDossierFiscal;

public class TypeEtatDossierFiscalUserType extends EnumCharMappingUserType<RegpmTypeEtatDossierFiscal> {

	private static final Map<String, RegpmTypeEtatDossierFiscal> MAPPING = buildMapping();

	private static Map<String, RegpmTypeEtatDossierFiscal> buildMapping() {
		final Map<String, RegpmTypeEtatDossierFiscal> map = new HashMap<>();
		map.put("01", RegpmTypeEtatDossierFiscal.ENVOYE);
		map.put("02", RegpmTypeEtatDossierFiscal.RECU);
		map.put("03", RegpmTypeEtatDossierFiscal.SOMME);
		map.put("04", RegpmTypeEtatDossierFiscal.ANNULE);
		map.put("AMENDE DD", RegpmTypeEtatDossierFiscal.AMENDE_DD);
		map.put("EN SAISIE", RegpmTypeEtatDossierFiscal.EN_SAISIE);
		map.put("TRAITE", RegpmTypeEtatDossierFiscal.TRAITE);
		return map;
	}

	public TypeEtatDossierFiscalUserType() {
		super(RegpmTypeEtatDossierFiscal.class, MAPPING);
	}
}
