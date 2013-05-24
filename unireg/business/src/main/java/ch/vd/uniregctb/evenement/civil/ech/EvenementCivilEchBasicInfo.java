package ch.vd.uniregctb.evenement.civil.ech;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

/**
 * Informations de base sur un événement civil.
 * <p/><b>Note: </b> cette classe n'est pas <i>thread-safe</i>.
 */
public final class EvenementCivilEchBasicInfo implements Serializable {

	private static final long serialVersionUID = 396438041113830368L;

	private final long id;
	private final long noIndividu;
	private final EtatEvenementCivil etat;
	private final TypeEvenementCivilEch type;
	private final ActionEvenementCivilEch action;
	private final Long idReference;
	private final RegDate date;
	private List<EvenementCivilEchBasicInfo> referrers = new LinkedList<>();
	private transient List<EvenementCivilEchBasicInfo> sortedReferrers;

	public EvenementCivilEchBasicInfo(long id, long noIndividu, EtatEvenementCivil etat, TypeEvenementCivilEch type, ActionEvenementCivilEch action, @Nullable Long idReference, RegDate date) {
		this.id = id;
		this.noIndividu = noIndividu;
		this.etat = etat;
		this.type = type;
		this.action = action;
		this.idReference = idReference;
		this.date = date;

		if (this.date == null) {
			throw new IllegalArgumentException("La date de l'événement ne doit pas être nulle");
		}
		if (this.type == null) {
			throw new NullPointerException("Le type de l'événement ne doit pas être nul");
		}
	}

	public EvenementCivilEchBasicInfo(EvenementCivilEch evt) {
		this(evt.getId(), evt.getNumeroIndividu(), evt.getEtat(), evt.getType(), evt.getAction(), evt.getRefMessageId(), evt.getDateEvenement());
	}

	public EvenementCivilEchBasicInfo(EvenementCivilEch evt, long noIndividu) {
		this(evt.getId(), noIndividu, evt.getEtat(), evt.getType(), evt.getAction(), evt.getRefMessageId(), evt.getDateEvenement());
		if (evt.getNumeroIndividu() != null && noIndividu != evt.getNumeroIndividu()) {
			// ce serait un gros bug... mais on n'est jamais trop sûr...
			throw new IllegalArgumentException("Numéros d'individus différents : l'événement avait " + evt.getNumeroIndividu() + " mais on veut le traiter avec " + noIndividu);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final EvenementCivilEchBasicInfo that = (EvenementCivilEchBasicInfo) o;

		if (id != that.id) return false;
		if (noIndividu != that.noIndividu) return false;
		if (action != that.action) return false;
		if (!date.equals(that.date)) return false;
		if (etat != that.etat) return false;
		if (idReference != null ? !idReference.equals(that.idReference) : that.idReference != null) return false;
		if (type != that.type) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = (int) (id ^ (id >>> 32));
		result = 31 * result + (int) (noIndividu ^ (noIndividu >>> 32));
		result = 31 * result + etat.hashCode();
		result = 31 * result + type.hashCode();
		result = 31 * result + action.hashCode();
		result = 31 * result + (idReference != null ? idReference.hashCode() : 0);
		result = 31 * result + date.hashCode();
		return result;
	}

	public long getId() {
		return id;
	}

	public long getNoIndividu() {
		return noIndividu;
	}

	public EtatEvenementCivil getEtat() {
		return etat;
	}

	public TypeEvenementCivilEch getType() {
		return type;
	}

	public ActionEvenementCivilEch getAction() {
		return action;
	}

	public Long getIdReference() {
		return idReference;
	}

	public RegDate getDate() {
		if (referrers.size() == 0) {
			return date;
		}

		final List<EvenementCivilEchBasicInfo> sortedReferrers = getSortedReferrers();
		final EvenementCivilEchBasicInfo lastReferrer = sortedReferrers.get(sortedReferrers.size() - 1);
		return lastReferrer.getDate();
	}

	public void addReferrer(EvenementCivilEchBasicInfo referrer) {
		referrers.add(referrer);
		sortedReferrers = null;
	}

	public void setReferrers(List<EvenementCivilEchBasicInfo> referrers) {
		this.referrers = new LinkedList<>(referrers);
		sortedReferrers = null;
	}

	public List<EvenementCivilEchBasicInfo> getReferrers() {
		return Collections.unmodifiableList(referrers);
	}

	public List<EvenementCivilEchBasicInfo> getSortedReferrers() {
		if (sortedReferrers == null) {
			buildSortedReferrers();
		}
		return sortedReferrers;
	}

	private void buildSortedReferrers() {
		if (sortedReferrers == null) {
			if (referrers.size() < 2) {
				sortedReferrers = getReferrers();
			}
	        else {
				final Set<Long> taken = new HashSet<>(referrers.size() + 1);
				taken.add(id);

				final List<EvenementCivilEchBasicInfo> remaining = new LinkedList<>(referrers);
				Collections.sort(remaining, new Comparator<EvenementCivilEchBasicInfo>() {
					@Override
					public int compare(EvenementCivilEchBasicInfo o1, EvenementCivilEchBasicInfo o2) {
						return Long.compare(o1.getId(), o2.getId());
					}
				});

				final List<EvenementCivilEchBasicInfo> sorted = new ArrayList<>(referrers.size());
				while (remaining.size() > 0) {
					final Iterator<EvenementCivilEchBasicInfo> iter = remaining.iterator();
					final Set<Long> locallyTaken = new HashSet<>(referrers.size());
					while (iter.hasNext()) {
						final EvenementCivilEchBasicInfo referrer = iter.next();
						if (taken.contains(referrer.getIdReference())) {
							locallyTaken.add(referrer.getId());
							sorted.add(referrer);
							iter.remove();
						}
					}
					taken.addAll(locallyTaken);
					if (locallyTaken.size() == 0 && remaining.size() > 0) {
						throw new RuntimeException("Faulty algorithm in event group sorting...");
					}
				}
				sortedReferrers = Collections.unmodifiableList(sorted);
			}
		}
	}
}
