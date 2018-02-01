package ch.vd.unireg.fors;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.TypeAutoriteFiscale;

public interface EditForView {

	long getId();

	long getTiersId();

	RegDate getDateDebut();

	RegDate getDateFin();

	TypeAutoriteFiscale getTypeAutoriteFiscale();

	Integer getNoAutoriteFiscale();

	/**
	 * Cette méthode peut paraître un peu surprenante, car la date de début d'un for n'est jamais nulle... mais dans les cas où cette date n'est pas éditable,
	 * il n'est pas exclu du tout que celle-ci ne soit pas fournie du tout dans le formulaire d'édition (auquel cas le champ ne peut être que null).
	 * @return si le champ de la date de début peutêtre accepté à <code>null</code>
	 */
	boolean isDateDebutNulleAutorisee();

	boolean isDateFinFutureAutorisee();

	GenreImpot getGenreImpot();
}
