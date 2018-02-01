package ch.vd.unireg.situationfamille;

import java.util.Date;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.Annulable;
import ch.vd.unireg.type.EtatCivil;

/**
 * Vue de la situation de famille d'un contribuable telle que composée par le service corespondant.
 */
public interface VueSituationFamille extends DateRange, Annulable {

	enum Source {
		CIVILE,
		FISCALE_TIERS,
		FISCALE_AUTRE_TIERS
	}

	/**
	 * @return l'id de l'entité situation de famille sous-jacente si la source est fiscale, ou <b>null</b> si la source est civile.
	 */
	Long getId();

	/**
	 * Retourne la source de la situation de famille (civile, fiscal tiers, fiscal autre tiers).
	 */
	Source getSource();

	/**
	 * @return la date de début de validité de la situation de famille.
	 */
	@Override
	RegDate getDateDebut();

	/**
	 * @return la date de fin de validité de la situation de famille, ou <b>null</b> s'il s'agit de la situation de famille courante.
	 */
	@Override
	RegDate getDateFin();

	/**
	 * @return le nombre d'enfant à charge du contribuable, ou <b>null</b> si cette information n'est pas disponible.
	 */
	Integer getNombreEnfants();

	/**
	 * @return l'état civil du contribuable.
	 */
	EtatCivil getEtatCivil();

	/**
	 * @return si la situation de famille est annulée ou non.
	 */
	boolean isAnnule();

	/**
	 * @return la date d'annulation de la situation de famille, ou <b>null</b> si elle n'est pas annulée
	 */
	Date getAnnulationDate();
}
