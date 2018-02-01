package ch.vd.uniregctb.parentes;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Parente;

public final class ParenteUpdateInfo implements Comparable<ParenteUpdateInfo> {

	public enum Action {
		CREATION, ANNULATION
	}

	public final Action action;
	public final long noCtbEnfant;
	public final long noCtbParent;
	public final RegDate dateDebut;
	public final RegDate dateFin;

	private ParenteUpdateInfo(Action action, long noCtbEnfant, long noCtbParent, RegDate dateDebut, @Nullable RegDate dateFin) {
		this.action = action;
		this.noCtbEnfant = noCtbEnfant;
		this.noCtbParent = noCtbParent;
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
	}

	private ParenteUpdateInfo(Action action, Parente parente) {
		this(action, parente.getSujetId(), parente.getObjetId(), parente.getDateDebut(), parente.getDateFin());
	}

	public static ParenteUpdateInfo getCreation(Parente parente) {
		return new ParenteUpdateInfo(Action.CREATION, parente);
	}

	public static ParenteUpdateInfo getAnnulation(Parente parente) {
		return new ParenteUpdateInfo(Action.ANNULATION, parente);
	}

	@Override
	public int compareTo(ParenteUpdateInfo o) {
		int comparison = Long.compare(noCtbParent, o.noCtbParent);
		if (comparison == 0) {
			comparison = Long.compare(noCtbEnfant, o.noCtbEnfant);
		}
		if (comparison == 0) {
			comparison = action.compareTo(o.action);
		}
		if (comparison == 0) {
			comparison = NullDateBehavior.EARLIEST.compare(dateDebut, o.dateDebut);
		}
		if (comparison == 0) {
			comparison = NullDateBehavior.LATEST.compare(dateFin, o.dateFin);
		}
		return comparison;
	}
}
