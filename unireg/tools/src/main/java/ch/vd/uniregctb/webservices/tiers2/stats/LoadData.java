package ch.vd.uniregctb.webservices.tiers2.stats;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.utils.Assert;

class LoadData {

	private Set<String> users = new HashSet<String>();
	private List<LoadPoint> list = new ArrayList<LoadPoint>();
	private int lastPeriodeIndex = 0;

	public LoadData() {
		for (Periode periode : Periode.DEFAULT_PERIODES) {
			list.add(new LoadPoint(periode));
		}
	}

	public void addCall(Call call) {

		final String user = call.getUser();
		users.add(user);

		final HourMinutes timestamp = call.getTimestamp();

		// optim : on commence à la position de la dernière période trouvée
		boolean found = false;
		for (int i = lastPeriodeIndex, listSize = list.size(); i < listSize; i++) {
			final LoadPoint point = list.get(i);
			if (point.isInPeriode(timestamp)) {
				point.add(user);
				lastPeriodeIndex = i;
				found = true;
				break;
			}
		}
		if (!found) {
			// si on a pas trouvé, on recommence au début (ne devrait pas arriver, si les logs sont ordonnés de manière croissante dans le fichier)
			for (int i = 0; i < lastPeriodeIndex; i++) {
				final LoadPoint point = list.get(i);
				if (point.isInPeriode(timestamp)) {
					point.add(user);
					lastPeriodeIndex = i;
					found = true;
					break;
				}
			}
		}
		Assert.isTrue(found);
	}

	public Set<String> getUsers() {
		return users;
	}

	public List<LoadPoint> getList() {
		return list;
	}
}
