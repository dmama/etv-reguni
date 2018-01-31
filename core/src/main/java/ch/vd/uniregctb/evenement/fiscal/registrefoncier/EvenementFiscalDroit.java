package ch.vd.uniregctb.evenement.fiscal.registrefoncier;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.LengthConstants;

@Entity
public abstract class EvenementFiscalDroit extends EvenementFiscalRF {

	/**
	 * Différents types d'événements fiscaux autour des droits
	 */
	public enum TypeEvenementFiscalDroitPropriete {
		OUVERTURE,
		FERMETURE,
		MODIFICATION
	}

	private TypeEvenementFiscalDroitPropriete type;

	public EvenementFiscalDroit() {
	}

	public EvenementFiscalDroit(@Nullable RegDate dateValeur, TypeEvenementFiscalDroitPropriete type) {
		super(dateValeur);
		this.type = type;
	}

	@Column(name = "TYPE_EVT_DROIT", length = LengthConstants.EVTFISCAL_TYPE_EVT_DROIT)
	@Enumerated(EnumType.STRING)
	public TypeEvenementFiscalDroitPropriete getType() {
		return type;
	}

	public void setType(TypeEvenementFiscalDroitPropriete type) {
		this.type = type;
	}
}
