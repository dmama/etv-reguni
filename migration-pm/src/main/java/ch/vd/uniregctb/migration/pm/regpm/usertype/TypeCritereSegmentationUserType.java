package ch.vd.uniregctb.migration.pm.regpm.usertype;

import java.util.HashMap;
import java.util.Map;

import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeCritereSegmentation;

public class TypeCritereSegmentationUserType extends EnumCharMappingUserType<RegpmTypeCritereSegmentation> {

	private static final Map<String, RegpmTypeCritereSegmentation> MAPPING = buildMapping();

	private static Map<String, RegpmTypeCritereSegmentation> buildMapping() {
		final Map<String, RegpmTypeCritereSegmentation> map = new HashMap<>();
		map.put("ALLEG", RegpmTypeCritereSegmentation.ALLEG);
		map.put("APMAL", RegpmTypeCritereSegmentation.APMAL);
		map.put("APMEF", RegpmTypeCritereSegmentation.APMEF);
		map.put("APMEX", RegpmTypeCritereSegmentation.APMEX);
		map.put("APMIM", RegpmTypeCritereSegmentation.APMIM);
		map.put("APMOF", RegpmTypeCritereSegmentation.APMOF);
		map.put("APMOR", RegpmTypeCritereSegmentation.APMOR);
		map.put("ASSUC", RegpmTypeCritereSegmentation.ASSUC);
		map.put("ASSUV", RegpmTypeCritereSegmentation.ASSUV);
		map.put("AUDIT", RegpmTypeCritereSegmentation.AUDIT);
		map.put("AUTP2", RegpmTypeCritereSegmentation.AUTP2);
		map.put("AUTP3", RegpmTypeCritereSegmentation.AUTP3);
		map.put("AUTPF", RegpmTypeCritereSegmentation.AUTPF);
		map.put("AUTPM", RegpmTypeCritereSegmentation.AUTPM);
		map.put("AUTRE", RegpmTypeCritereSegmentation.AUTRE);
		map.put("CBACK", RegpmTypeCritereSegmentation.CBACK);
		map.put("CRIT2", RegpmTypeCritereSegmentation.CRIT2);
		map.put("CRIT3", RegpmTypeCritereSegmentation.CRIT3);
		map.put("DDTDA", RegpmTypeCritereSegmentation.DDTDA);
		map.put("DDTDM", RegpmTypeCritereSegmentation.DDTDM);
		map.put("DEBAS", RegpmTypeCritereSegmentation.DEBAS);
		map.put("DOAEX", RegpmTypeCritereSegmentation.DOAEX);
		map.put("DODAC", RegpmTypeCritereSegmentation.DODAC);
		map.put("DODAP", RegpmTypeCritereSegmentation.DODAP);
		map.put("DOLOC", RegpmTypeCritereSegmentation.DOLOC);
		map.put("ELUES", RegpmTypeCritereSegmentation.ELUES);
		map.put("ENFAI", RegpmTypeCritereSegmentation.ENFAI);
		map.put("ETRAN", RegpmTypeCritereSegmentation.ETRAN);
		map.put("EXPER", RegpmTypeCritereSegmentation.EXPER);
		map.put("FINAS", RegpmTypeCritereSegmentation.FINAS);
		map.put("FJSNC", RegpmTypeCritereSegmentation.FJSNC);
		map.put("FJUDP", RegpmTypeCritereSegmentation.FJUDP);
		map.put("FONDS", RegpmTypeCritereSegmentation.FONDS);
		map.put("HOLDI", RegpmTypeCritereSegmentation.HOLDI);
		map.put("INSTR", RegpmTypeCritereSegmentation.INSTR);
		map.put("LIASF", RegpmTypeCritereSegmentation.LIASF);
		map.put("LIQUI", RegpmTypeCritereSegmentation.LIQUI);
		map.put("NOASS", RegpmTypeCritereSegmentation.NOASS);
		map.put("NODIR", RegpmTypeCritereSegmentation.NODIR);
		map.put("NODOS", RegpmTypeCritereSegmentation.NODOS);
		map.put("ORDIG", RegpmTypeCritereSegmentation.ORDIG);
		map.put("ORDIM", RegpmTypeCritereSegmentation.ORDIM);
		map.put("ORDIP", RegpmTypeCritereSegmentation.ORDIP);
		map.put("ORDIZ", RegpmTypeCritereSegmentation.ORDIZ);
		map.put("PDDPA", RegpmTypeCritereSegmentation.PDDPA);
		map.put("PPPTD", RegpmTypeCritereSegmentation.PPPTD);
		map.put("QUOTA", RegpmTypeCritereSegmentation.QUOTA);
		map.put("REDPA", RegpmTypeCritereSegmentation.REDPA);
		map.put("REPRI", RegpmTypeCritereSegmentation.REPRI);
		map.put("RESVD", RegpmTypeCritereSegmentation.RESVD);
		map.put("RULIN", RegpmTypeCritereSegmentation.RULIN);
		map.put("SBAS1", RegpmTypeCritereSegmentation.SBAS1);
		map.put("SBAS2", RegpmTypeCritereSegmentation.SBAS2);
		map.put("SBASE", RegpmTypeCritereSegmentation.SBASE);
		map.put("SERFO", RegpmTypeCritereSegmentation.SERFO);
		map.put("SERVI", RegpmTypeCritereSegmentation.SERVI);
		map.put("SHCFS", RegpmTypeCritereSegmentation.SHCFS);
		map.put("SIALO", RegpmTypeCritereSegmentation.SIALO);
		map.put("SIMMF", RegpmTypeCritereSegmentation.SIMMF);
		map.put("SIMMO", RegpmTypeCritereSegmentation.SIMMO);
		map.put("SIORD", RegpmTypeCritereSegmentation.SIORD);
		map.put("SISOC", RegpmTypeCritereSegmentation.SISOC);
		map.put("SISUB", RegpmTypeCritereSegmentation.SISUB);
		map.put("SOLDE", RegpmTypeCritereSegmentation.SOLDE);
		map.put("STASP", RegpmTypeCritereSegmentation.STASP);
		map.put("TAXAU", RegpmTypeCritereSegmentation.TAXAU);
		map.put("TAXMO", RegpmTypeCritereSegmentation.TAXMO);
		map.put("TRANC", RegpmTypeCritereSegmentation.TRANC);
		map.put("TRIMM", RegpmTypeCritereSegmentation.TRIMM);
		map.put("VENTI", RegpmTypeCritereSegmentation.VENTI);
		return map;
	}

	public TypeCritereSegmentationUserType() {
		super(RegpmTypeCritereSegmentation.class, MAPPING);
	}

}
