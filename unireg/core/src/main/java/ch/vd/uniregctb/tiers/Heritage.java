package ch.vd.uniregctb.tiers;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * <pre>
 *   +------------------+                   +------------------+
 *   |     Héritier     |                   |      Défunt      |
 *   +------------------+                   +------------------+
 *           ^                                        ^
 *           ¦  sujet  +--------------------+  objet  ¦
 *           +---------|     Héritage       |---------+
 *                     +--------------------+
 * </pre>
 */
@Entity
@DiscriminatorValue("Heritage")
public class Heritage extends RapportEntreTiers {

	private static final String HERITIER = "héritier";
	private static final String DEFUNT = "défunt(e)";

	/**
	 * Vrai si l'héritier est le principal de la communauté d'héritiers (voir SIFISC-24999).
	 */
	private Boolean principalCommunaute = false;

	public Heritage() {
	}

	public Heritage(RegDate dateDebut, RegDate dateFin, PersonnePhysique heritier, PersonnePhysique defunt) {
		super(dateDebut, dateFin, heritier, defunt);
	}

	protected Heritage(Heritage heritage) {
		super(heritage);
	}

	@Override
	public Heritage duplicate() {
		return new Heritage(this);
	}

	@Column(name = "PRINCIPAL_COMM_HERITIERS")
	public Boolean getPrincipalCommunaute() {
		return principalCommunaute;
	}

	public void setPrincipalCommunaute(Boolean principalCommunaute) {
		this.principalCommunaute = principalCommunaute;
	}

	@Transient
	@Override
	public String getDescriptionTypeObjet() {
		return DEFUNT;
	}

	@Transient
	@Override
	public String getDescriptionTypeSujet() {
		return HERITIER;
	}

	@Transient
	@Override
	public TypeRapportEntreTiers getType() {
		return TypeRapportEntreTiers.HERITAGE;
	}

	@Transient
	@Override
	protected String getBusinessName() {
		// redéfini ici à cause de l'accent qui n'apparaît pas dans le nom de la classe...
		return "Héritage";
	}
}
