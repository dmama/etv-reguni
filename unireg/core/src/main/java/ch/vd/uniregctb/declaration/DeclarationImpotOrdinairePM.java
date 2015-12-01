package ch.vd.uniregctb.declaration;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesMorales;

@Entity
@DiscriminatorValue("DIPM")
public class DeclarationImpotOrdinairePM extends DeclarationImpotOrdinaire {

	/**
	 * Première année où le retour par courrier électronique des déclarations d'impôt est possible.
	 */
	public static final int PREMIERE_ANNEE_RETOUR_ELECTRONIQUE = 2016;

	@Transient
	@Override
	public ContribuableImpositionPersonnesMorales getTiers() {
		return (ContribuableImpositionPersonnesMorales) super.getTiers();
	}

	/**
	 * @return un nouveau code de contrôle d'une lettre et de cinq chiffres aléatoires
	 */
	public static String generateCodeControle() {
		return generateCodeControleUneLettreCinqChiffres();
	}
}
