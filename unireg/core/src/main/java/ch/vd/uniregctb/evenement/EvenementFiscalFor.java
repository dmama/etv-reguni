package ch.vd.uniregctb.evenement;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.hibernate.annotations.Type;

import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;

@Entity
@DiscriminatorValue("EvenementFiscalFor")
public class EvenementFiscalFor extends EvenementFiscal{

	/**
	 *
	 */
	private static final long serialVersionUID = 4102176084574322243L;

	private MotifFor motifFor;
	
	private ModeImposition modeImposition;

	@Column(name = "MOTIF_FOR", length = LengthConstants.FOR_MOTIF)
	@Type(type = "ch.vd.uniregctb.hibernate.MotifForUserType")
	public MotifFor getMotifFor() {
		return motifFor;
	}

	public void setMotifFor(MotifFor motifFor) {
		this.motifFor = motifFor;
	}

	@Column(name = "MODE_IMPOSITION", length = LengthConstants.FOR_IMPOSITION)
	@Type(type = "ch.vd.uniregctb.hibernate.ModeImpositionUserType")
	public ModeImposition getModeImposition() {
		return modeImposition;
	}

	public void setModeImposition(ModeImposition modeImposition) {
		this.modeImposition = modeImposition;
	}

}
