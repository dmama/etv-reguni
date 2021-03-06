package ch.vd.unireg.declaration;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesMorales;

@Entity
@DiscriminatorValue(value = "QSNC")
public class QuestionnaireSNC extends DeclarationAvecCodeControle {

	private RegDate delaiRetourImprime;


	/**
	 * Première année où le retour par courrier électronique des questionnaires SNC est possible.
	 */
	public static final int PREMIERE_ANNEE_RETOUR_ELECTRONIQUE = 2016;

	@Transient
	@Override
	public boolean isSommable() {
		return false;
	}

	@Transient
	@Override
	public boolean isRappelable() {
		return true;
	}

	@Transient
	@Override
	public ContribuableImpositionPersonnesMorales getTiers() {
		return (ContribuableImpositionPersonnesMorales) super.getTiers();
	}

	/**
	 * @return une date correspondant au délai de retour imprimé sur le document
	 */
	@Column(name = "DELAI_RETOUR_IMPRIME")
	@Type(type = "ch.vd.unireg.hibernate.RegDateUserType")
	public RegDate getDelaiRetourImprime() {
		return delaiRetourImprime;
	}

	public void setDelaiRetourImprime(RegDate delaiRetourImprime) {
		this.delaiRetourImprime = delaiRetourImprime;
	}

}
