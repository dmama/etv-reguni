package ch.vd.unireg.interfaces.civil.data;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

/**
 * Classe container qui regroupe l'état d'un individu juste après un événement
 */
public class IndividuApresEvenement {

	/**
	 * Etat de l'individu lui-même
	 */
	private final Individu individu;

	/**
	 * Date de l'événement juste après lequel cet état est fourni
	 */
	private final RegDate dateEvenement;

	/**
	 * Type de l'événement juste après lequel cet état est fourni
	 */
	private final TypeEvenementCivilEch typeEvenement;

	/**
	 * Action de l'événement juste après lequel cet état est fourni
	 */
	private final ActionEvenementCivilEch actionEvenement;

	/**
	 * Eventuelle référence vers un autre événement (cas des corrections/annulations d'annonce, voir {@link #actionEvenement})
	 */
	@Nullable
	private final Long idEvenementRef;

	public IndividuApresEvenement(Individu individu, RegDate dateEvenement, TypeEvenementCivilEch typeEvenement, ActionEvenementCivilEch actionEvenement, Long idEvenementRef) {
		this.individu = individu;
		this.dateEvenement = dateEvenement;
		this.typeEvenement = typeEvenement;
		this.actionEvenement = actionEvenement;
		this.idEvenementRef = idEvenementRef;
	}

	public Individu getIndividu() {
		return individu;
	}

	public RegDate getDateEvenement() {
		return dateEvenement;
	}

	public TypeEvenementCivilEch getTypeEvenement() {
		return typeEvenement;
	}

	public ActionEvenementCivilEch getActionEvenement() {
		return actionEvenement;
	}

	@Nullable
	public Long getIdEvenementRef() {
		return idEvenementRef;
	}
}
