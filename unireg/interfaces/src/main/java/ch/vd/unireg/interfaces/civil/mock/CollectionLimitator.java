package ch.vd.unireg.interfaces.civil.mock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.AdoptionReconnaissance;
import ch.vd.unireg.interfaces.civil.data.EtatCivil;
import ch.vd.unireg.interfaces.civil.data.Nationalite;
import ch.vd.unireg.interfaces.civil.data.Permis;
import ch.vd.unireg.interfaces.civil.data.RelationVersIndividu;
import ch.vd.unireg.interfaces.common.Adresse;

public class CollectionLimitator {

	public static final Limitator<AdoptionReconnaissance> ADOPTION_LIMITATOR = new Limitator<AdoptionReconnaissance>() {
		@Override
		public boolean keep(AdoptionReconnaissance element, RegDate date) {
			return (element.getDateAdoption() != null && element.getDateAdoption().isBeforeOrEqual(date))
					|| (element.getDateReconnaissance() != null && element.getDateReconnaissance().isBeforeOrEqual(date));
		}
	};

	public static final Limitator<RelationVersIndividu> RELATION_LIMITATOR = new Limitator<RelationVersIndividu>() {
		@Override
		public boolean keep(RelationVersIndividu element, RegDate date) {
			return element.getDateDebut() == null || element.getDateDebut().isBeforeOrEqual(date);
		}
	};

	public static final Limitator<Permis> PERMIS_LIMITATOR = new Limitator<Permis>() {
		@Override
		public boolean keep(Permis element, RegDate date) {
			return element.getDateValeur() == null || element.getDateValeur().isBeforeOrEqual(date);
		}
	};

	public static final Limitator<Adresse> ADRESSE_LIMITATOR = new Limitator<Adresse>() {
		@Override
		public boolean keep(Adresse element, RegDate date) {
			return element.getDateDebut() == null || element.getDateDebut().isBeforeOrEqual(date);
		}
	};

	public static final Limitator<EtatCivil> ETAT_CIVIL_LIMITATOR = new Limitator<EtatCivil>() {
		@Override
		public boolean keep(EtatCivil element, RegDate date) {
			return element.getDateDebut() == null || element.getDateDebut().isBeforeOrEqual(date);
		}
	};

	public static final Limitator<Nationalite> NATIONALITE_LIMITATOR = new Limitator<Nationalite>() {
		@Override
		public boolean keep(Nationalite element, RegDate date) {
			return element.getDateDebut() == null || element.getDateDebut().isBeforeOrEqual(date);
		}
	};

	public static <T> List<T> limit(Collection<T> original, RegDate date, Limitator<T> limitator) {
		if (original == null) {
			return null;
		}
		else if (original.isEmpty()) {
			return Collections.emptyList();
		}
		else {
			final List<T> limited = new ArrayList<>(original);
			if (date != null) {
				limited.removeIf(element -> !limitator.keep(element, date));
			}
			return Collections.unmodifiableList(limited.size() == original.size() ? new ArrayList<>(original) : limited);
		}
	}

	public interface Limitator<T> {
		boolean keep(T element, RegDate date);
	}
}
