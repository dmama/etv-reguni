package ch.vd.uniregctb.evenement.civil.ech;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

/**
 * Informations de base sur un événement civil.
 */
public final class EvenementCivilEchBasicInfo {
	public final long idEvenement;
	public final long noIndividu;
	public final EtatEvenementCivil etat;
	public final TypeEvenementCivilEch type;
	public final ActionEvenementCivilEch action;
	public final Long idEvenementReference;
	public final RegDate date;

	public EvenementCivilEchBasicInfo(long idEvenement, long noIndividu, EtatEvenementCivil etat, TypeEvenementCivilEch type, ActionEvenementCivilEch action, @Nullable Long idEvenementReference,
	                                  RegDate date) {
		this.idEvenement = idEvenement;
		this.noIndividu = noIndividu;
		this.etat = etat;
		this.type = type;
		this.action = action;
		this.idEvenementReference = idEvenementReference;
		this.date = date;

		if (this.date == null) {
			throw new IllegalArgumentException("La date de l'événement ne doit pas être nulle");
		}
		if (this.type == null) {
			throw new NullPointerException("Le type de l'événement ne doit pas être nul");
		}
	}
}
