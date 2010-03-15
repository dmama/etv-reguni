package ch.vd.uniregctb.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

@Entity
@DiscriminatorValue("AnnuleEtRemplace")
public class AnnuleEtRemplace extends RapportEntreTiers {
	/**
	 *
	 */
	private static final long serialVersionUID = -1688097858774925185L;

	public AnnuleEtRemplace() {
		// empty
	}

	public AnnuleEtRemplace(RegDate dateDebut, RegDate dateFin, Tiers sujet, Tiers objet) {
		super(dateDebut, dateFin, sujet, objet);
	}

	public AnnuleEtRemplace(AnnuleEtRemplace annuleEtRemplace) {
		super(annuleEtRemplace);
	}

	@Override
	@Transient
	public TypeRapportEntreTiers getType() {
		return TypeRapportEntreTiers.ANNULE_ET_REMPLACE;
	}

	public RapportEntreTiers duplicate() {
		return new AnnuleEtRemplace(this);
	}

}
