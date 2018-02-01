package ch.vd.unireg.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.type.TypeRapportEntreTiers;

/**
 * <pre>
 * +------------------+                              +------------------+
 * | PersonnePhysique | (pupille)       (conseiller) | PersonnePhysique |
 * +------------------+                              +------------------+
 *         ^                                                  ^
 *         ¦  sujet          +--------------+          objet  ¦
 *         +-----------------| ConseilLegal |-----------------+
 *                           +--------------+
 * </pre>
 */
@Entity
@DiscriminatorValue("ConseilLegal")
public class ConseilLegal extends RepresentationLegale {

	private static final String CONSEILLER_LEGAL = "conseiller légal";

	public ConseilLegal() {
		// vide, nécessaire pour la persistence
	}

	protected ConseilLegal(ConseilLegal conseilLegal) {
		super(conseilLegal);
	}

	/**
	 * Conseil légal entre une pupille et un conseiller, potentiellement ordonné par une autorité tutélaire (justice de paix)
	 */
	public ConseilLegal(RegDate dateDebut, RegDate dateFin, PersonnePhysique pupille, Tiers conseiller, CollectiviteAdministrative autoriteTutelaire) {
		super(dateDebut, dateFin, pupille, conseiller, autoriteTutelaire);
	}

	@Override
	public RapportEntreTiers duplicate() {
		return new ConseilLegal(this);
	}

	@Override
	@Transient
	public TypeRapportEntreTiers getType() {
		return TypeRapportEntreTiers.CONSEIL_LEGAL;
	}

	@Override
	@Transient
	public String getDescriptionTypeObjet() {
		return CONSEILLER_LEGAL;
	}
}