package ch.vd.uniregctb.tiers;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * <pre>
 * +--------------+                        +-----------------------------+
 * | Contribuable |                        | DebiteurPrestationImposable |
 * +--------------+                        +-----------------------------+
 *        ^                                                ^
 *        ¦  sujet  +----------------------------+  objet  ¦
 *        +---------| RapportPrestationImposable |---------+
 *                  +----------------------------+
 * </pre>
 */
@Entity
@DiscriminatorValue("RapportPrestationImposable")
public class RapportPrestationImposable extends RapportEntreTiers {

	private static final String EMPLOYEUR = "employeur";
	private static final String SOURCIER = "sourcier";

	private RegDate finDernierElementImposable;

	public RapportPrestationImposable() {
		// empty
	}

	public RapportPrestationImposable(RegDate dateDebut, RegDate dateFin, Contribuable sujet, DebiteurPrestationImposable objet) {
		super(dateDebut, dateFin, sujet, objet);
	}

	public RapportPrestationImposable(RapportPrestationImposable rapport) {
		super(rapport);
		this.finDernierElementImposable = rapport.getFinDernierElementImposable();
	}

	@Override
	@Transient
	public String getDescriptionTypeObjet() {
		return EMPLOYEUR;
	}

	@Override
	@Transient
	public String getDescriptionTypeSujet() {
		return SOURCIER;
	}

	@Column(name = "DATE_FIN_DER_ELE_IMP")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getFinDernierElementImposable() {
		return finDernierElementImposable;
	}

	public void setFinDernierElementImposable(RegDate theFinDernierElementImposable) {
		finDernierElementImposable = theFinDernierElementImposable;
	}

	/*
	 * (non-Javadoc)
	 * @see ch.vd.uniregctb.tiers.RapportEntreTiers#getType()
	 */
	@Override
	@Transient
	public TypeRapportEntreTiers getType() {
		return TypeRapportEntreTiers.PRESTATION_IMPOSABLE;
	}

	/* (non-Javadoc)
	 * @see ch.vd.uniregctb.tiers.RapportEntreTiers#duplicate()
	 */
	@Override
	public RapportEntreTiers duplicate() {
		return new RapportPrestationImposable(this);
	}
}
