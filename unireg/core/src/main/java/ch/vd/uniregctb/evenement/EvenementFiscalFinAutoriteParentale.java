package ch.vd.uniregctb.evenement;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.ForeignKey;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypeEvenementFiscal;

@Entity
@DiscriminatorValue("FinAutoriteParentale")
public class EvenementFiscalFinAutoriteParentale extends EvenementFiscal {

	private static final long serialVersionUID = -2034840661051874963L;

	private PersonnePhysique enfant;

	public EvenementFiscalFinAutoriteParentale() {
	}

	public EvenementFiscalFinAutoriteParentale(PersonnePhysique enfant, Contribuable parent, RegDate dateEvenement) {
		super(parent, dateEvenement, TypeEvenementFiscal.FIN_AUTORITE_PARENTALE, null);
		this.enfant = enfant;
	}

	@JoinColumn(name = "ENFANT_ID")
	@ManyToOne(fetch = FetchType.LAZY)
	@ForeignKey(name = "FK_EV_FSC_ENFANT_ID")
	public PersonnePhysique getEnfant() {
		return enfant;
	}

	public void setEnfant(PersonnePhysique enfant) {
		this.enfant = enfant;
	}
}
