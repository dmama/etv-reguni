package ch.vd.uniregctb.declaration;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.CodeControleHelper;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesMorales;

@Entity
@DiscriminatorValue("DIPM")
public class DeclarationImpotOrdinairePM extends DeclarationImpotOrdinaire {

	private RegDate dateDebutExerciceCommercial;
	private RegDate dateFinExerciceCommercial;

	/**
	 * Suffixe du code de routage (le X dans 21-X), encore appelé code segment
	 */
	private Integer codeSegment;

	/**
	 * Première année où le retour par courrier électronique des déclarations d'impôt est possible.
	 */
	public static final int PREMIERE_ANNEE_RETOUR_ELECTRONIQUE = 2016;

	@Transient
	@Override
	public ContribuableImpositionPersonnesMorales getTiers() {
		return (ContribuableImpositionPersonnesMorales) super.getTiers();
	}

	@Transient
	public DateRange getExerciceCommercial() {
		return new DateRangeHelper.Range(dateDebutExerciceCommercial, dateFinExerciceCommercial);
	}

	/**
	 * @return un nouveau code de contrôle d'une lettre et de cinq chiffres aléatoires
	 */
	public static String generateCodeControle() {
		return CodeControleHelper.generateCodeControleUneLettreCinqChiffres();
	}

	@Column(name = "DATE_DEBUT_EXERCICE")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateDebutExerciceCommercial() {
		return dateDebutExerciceCommercial;
	}

	public void setDateDebutExerciceCommercial(RegDate dateDebutExerciceCommercial) {
		this.dateDebutExerciceCommercial = dateDebutExerciceCommercial;
	}

	@Column(name = "DATE_FIN_EXERCICE")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateFinExerciceCommercial() {
		return dateFinExerciceCommercial;
	}

	public void setDateFinExerciceCommercial(RegDate dateFinExerciceCommercial) {
		this.dateFinExerciceCommercial = dateFinExerciceCommercial;
	}

	@Column(name = "CODE_SEGMENT")
	public Integer getCodeSegment() {
		return codeSegment;
	}

	public void setCodeSegment(Integer codeSegment) {
		this.codeSegment = codeSegment;
	}
}
