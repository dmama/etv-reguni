package ch.vd.uniregctb.admin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class DatabasePreview {

	Map<Class,List<InfoTiers>> infoTiers;
	final List<Class> tiersTypes = new ArrayList<Class>();

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
		for (Class clazz : infoTiers.keySet()) {
			tiersTypes.add(clazz);
		}
		Collections.sort(tiersTypes, new Comparator<Class>() {
			@Override
			public int compare(Class o1, Class o2) {
				return o1.getSimpleName().compareTo(o2.getSimpleName());
			}
		});
	}
}
