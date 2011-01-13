package ch.vd.uniregctb.declaration;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

@Entity
@DiscriminatorValue("SOMMEE")
public class EtatDeclarationSommee extends EtatDeclaration {

	private RegDate dateEnvoiCourrier;

	public EtatDeclarationSommee() {
		super();
	}

	@Override
	@Transient
	public TypeEtatDeclaration getEtat() {
		return TypeEtatDeclaration.SOMMEE;
	}

	public EtatDeclarationSommee(RegDate dateObtention,RegDate dateEnvoiCourrier) {
		super(dateObtention);
		this.dateEnvoiCourrier = dateEnvoiCourrier;
	}
	@Column(name = "DATE_ENVOI_COURRIER")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateEnvoiCourrier() {
		return dateEnvoiCourrier;
	}

	public void setDateEnvoiCourrier(RegDate dateEnvoiCourrier) {
		this.dateEnvoiCourrier = dateEnvoiCourrier;
	}

	@Override
	public String toString() {
	final String desc = super.toString();
	final String dateEnvoiStr = dateEnvoiCourrier != null ? RegDateHelper.dateToDisplayString(dateEnvoiCourrier) : "?";
	return String.format(", Courrier envoyé le %s)",dateEnvoiStr);
	}
}
