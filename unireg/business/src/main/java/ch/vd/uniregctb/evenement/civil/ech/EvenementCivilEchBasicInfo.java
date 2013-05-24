package ch.vd.uniregctb.evenement.civil.ech;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

/**
 * Informations de base sur un événement civil.
 */
public final class EvenementCivilEchBasicInfo implements Serializable {

	private static final long serialVersionUID = -8328617338785565663L;

	private final long id;
	private final long noIndividu;
	private final EtatEvenementCivil etat;
	private final TypeEvenementCivilEch type;
	private final ActionEvenementCivilEch action;
	private final Long idReference;
	private final RegDate date;
	private final List<EvenementCivilEchBasicInfo> referers = new LinkedList<>();

	public EvenementCivilEchBasicInfo(long id, long noIndividu, EtatEvenementCivil etat, TypeEvenementCivilEch type, ActionEvenementCivilEch action, @Nullable Long idReference,
	                                  RegDate date) {
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
		return date;
	}

	public void addReferer(EvenementCivilEchBasicInfo referer) {
		referers.add(referer);
	}

	public List<EvenementCivilEchBasicInfo> getReferers() {
		return referers;
	}

	public List<EvenementCivilEchBasicInfo> getSortedReferers() {
		final List<EvenementCivilEchBasicInfo> liste = new ArrayList<>(referers);
		if (liste.size() > 1) {
			Collections.sort(liste, new Comparator<EvenementCivilEchBasicInfo>() {
				@Override
				public int compare(EvenementCivilEchBasicInfo o1, EvenementCivilEchBasicInfo o2) {
					return Long.compare(o1.getId(), o2.getId());
				}
			});
		}
		return liste;
	}
}
