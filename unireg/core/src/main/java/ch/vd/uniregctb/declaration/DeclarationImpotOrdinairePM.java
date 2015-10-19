package ch.vd.uniregctb.declaration;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesMorales;

@Entity
@DiscriminatorValue("DIPM")
public class DeclarationImpotOrdinairePM extends DeclarationImpotOrdinaire {

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
