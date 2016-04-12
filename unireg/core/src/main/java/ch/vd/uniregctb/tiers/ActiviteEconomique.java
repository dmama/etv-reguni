package ch.vd.uniregctb.tiers;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * <pre>
 *   +----------------+                   +------------------+
 *   | Contribuable   |                   | Etablissement    |
 *   +----------------+                   +------------------+
 *           ^                                        ^
 *           ¦  sujet  +--------------------+  objet  ¦
 *           +---------| ActiviteEconomique |---------+
 *                     +--------------------+
 * </pre>
 */
@Entity
@DiscriminatorValue("ActiviteEconomique")
public class ActiviteEconomique extends RapportEntreTiers {

	private static final String PERSONNE = "personne";
	private static final String ETABLISSEMENT = "établissement";

	private boolean principal;

	public ActiviteEconomique() {
		// empty
	}

	public ActiviteEconomique(RegDate dateDebut, RegDate dateFin, Contribuable personneMoraleOuPhysique, Etablissement etablissement, boolean principal) {
		super(dateDebut, dateFin, personneMoraleOuPhysique, etablissement);
		this.principal = principal;
	}

	protected ActiviteEconomique(ActiviteEconomique src) {
		super(src);
		this.principal = src.principal;
	}

	@Column(name = "ETB_PRINCIPAL")
	public boolean isPrincipal() {
		return principal;
	}

	public void setPrincipal(boolean principal) {
		this.principal = principal;
	}

	@Override
	@Transient
	public TypeRapportEntreTiers getType() {
		return TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE;
	}

	@Override
	@Transient
	public String getDescriptionTypeObjet() {
		return ETABLISSEMENT;
	}

	@Override
	@Transient
	public String getDescriptionTypeSujet() {
		return PERSONNE;
	}

	@Override
	public RapportEntreTiers duplicate() {
		return new ActiviteEconomique(this);
	}

}
