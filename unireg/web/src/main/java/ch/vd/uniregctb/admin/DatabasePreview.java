package ch.vd.uniregctb.admin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class DatabasePreview {

	Map<Class,List<InfoTiers>> infoTiers;
	final List<Class> tiersTypes = new ArrayList<>();

	@SuppressWarnings({"UnusedDeclaration"})
	public List<Class> getTiersTypes() {
		return tiersTypes;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public Map<Class,List<InfoTiers>> getInfoTiers() {
		return infoTiers;
	}

	public void setInfoTiers(Map<Class, List<InfoTiers>> infoTiers) {
		this.infoTiers = infoTiers;
		tiersTypes.clear();
		tiersTypes.addAll(infoTiers.keySet());
		tiersTypes.sort(Comparator.comparing(Class::getSimpleName));
	}
}
