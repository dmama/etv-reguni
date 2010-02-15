package ch.vd.uniregctb.evenement.common;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;

/**
 * Regroupe tous les tiers individuels et couple liés à un ménage.
 *
 * @author Ludovic Bertin
 *
 */
public class EnsembleTiersCouple {

	/**
	 * Tiers principal.
	 */
	PersonnePhysique principal = null;

	/**
	 * Tiers secondaire (conjoint).
	 */
	PersonnePhysique conjoint = null;

	/**
	 * Tiers du Ménage.
	 */
	MenageCommun menage = null;

	public EnsembleTiersCouple() {

	}

	public EnsembleTiersCouple(MenageCommun menage, PersonnePhysique principal, PersonnePhysique conjoint) {
		this.principal = principal;
		this.conjoint = conjoint;
		this.menage = menage;
	}

	/**
	 * Récupère le tiers principal du couple.
	 *
	 * @return le tiers principal
	 */
	public PersonnePhysique getPrincipal() {
		return principal;
	}

	/**
	 * Positionne le tiers principal du couple.
	 *
	 * @param principal
	 *            le tiers principal
	 */
	public void setPrincipal(PersonnePhysique principal) {
		this.principal = principal;
	}

	/**
	 * Récupère le tiers secondaire du couple.
	 *
	 * @return le tiers secondaire
	 */
	public PersonnePhysique getConjoint() {
		return conjoint;
	}

	/**
	 * Positionne le tiers secondaire du couple.
	 *
	 * @param conjoint
	 *            le tiers secondaire
	 */
	public void setConjoint(PersonnePhysique conjoint) {
		this.conjoint = conjoint;
	}

	/**
	 * Récupère le tiers ménage du couple.
	 *
	 * @return le tiers ménage
	 */
	public MenageCommun getMenage() {
		return menage;
	}

	/**
	 * Positionne le tiers ménage.
	 *
	 * @param menage
	 *            le tiers ménage
	 */
	public void setMenage(MenageCommun menage) {
		this.menage = menage;
	}

	/**
	 * Test que le tiers menage soit rattaché au deux tiers donnés.
	 *
	 * @param tiers
	 *            le premier tiers composant le menage
	 * @param autreTiers
	 *            l'autre tiers composant le menage
	 * @return true si le test est concluant.
	 */
	public boolean estComposeDe(PersonnePhysique tiers, PersonnePhysique autreTiers) {
		
		if (tiers == null) {
			tiers = autreTiers;
			autreTiers = null;
		}
		Assert.notNull(tiers);
		
		boolean tiersPresent = ((principal != null) && (principal.getId().equals(tiers.getId())))
				|| ((conjoint != null) && (conjoint.getId().equals(tiers.getId())));

		boolean autreTiersPresent = true;
		if (autreTiers != null) {
			autreTiersPresent = ((principal != null) && (principal.getId().equals(autreTiers.getId())))
			|| ((conjoint != null) && (conjoint.getId().equals(autreTiers.getId())));
		}

		return tiersPresent && autreTiersPresent;
	}

	/**
	 * Récupère l'autre tiers du couple.
	 *
	 * @param le
	 *            tiers dont on veut le conjoint
	 * @return le tiers conjoint
	 */
	public PersonnePhysique getConjoint(PersonnePhysique tiers) {
		if (principal == tiers)
			return conjoint;

		else if (conjoint == tiers)
			return principal;

		else
			return null;
	}

}
