package ch.vd.uniregctb.declaration;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;
import java.util.Date;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.type.TypeDocument;

@Entity
public abstract class DeclarationImpotOrdinaire extends Declaration {

	/**
	 * Numéro de séquence de la déclaration pour une période fiscale. La première déclaration prends le numéro 1.
	 */
	private Integer numero;

	private Date dateImpressionChemiseTaxationOffice;

	private RegDate delaiRetourImprime;

	private Long retourCollectiviteAdministrativeId;

	/**
	 * <code>true</code> si la DI a été créée comme une "di libre", c'est-à-dire une DI sur la période courante (au moment de sa création) sans fin d'assujettissement connue (comme un décès ou un départ
	 * HS)
	 */
	private boolean libre;

	@Column(name = "RETOUR_COLL_ADMIN_ID")
	@ForeignKey(name = "FK_DECL_RET_COLL_ADMIN_ID")
	public Long getRetourCollectiviteAdministrativeId() {
		return retourCollectiviteAdministrativeId;
	}

	public void setRetourCollectiviteAdministrativeId(Long retourCollectiviteAdministrativeId) {
		this.retourCollectiviteAdministrativeId = retourCollectiviteAdministrativeId;
	}

	@Column(name = "NUMERO")
	public Integer getNumero() {
		return numero;
	}

	public void setNumero(Integer theNumero) {
		numero = theNumero;
	}

	@Column(name = "DATE_IMPR_CHEMISE_TO")
	public Date getDateImpressionChemiseTaxationOffice() {
		return dateImpressionChemiseTaxationOffice;
	}

	public void setDateImpressionChemiseTaxationOffice(Date theDateImpressionChemiseTaxationOffice) {
		dateImpressionChemiseTaxationOffice = theDateImpressionChemiseTaxationOffice;
	}

	/**
	 * [UNIREG-1740] Le délai de retour tel que devant être imprimé sur le déclaration papier. Ce délai peut être nul, auquel cas on utilisera le délai accordé comme valeur de remplacement.
	 *
	 * @return une date correspondant au délai de retour; ou <i>null</i> si l'information n'est pas disponible.
	 */
	@Column(name = "DELAI_RETOUR_IMPRIME")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDelaiRetourImprime() {
		return delaiRetourImprime;
	}

	public void setDelaiRetourImprime(RegDate delaiRetourImprime) {
		this.delaiRetourImprime = delaiRetourImprime;
	}

	@Column(name = "LIBRE")
	public boolean isLibre() {
		return libre;
	}

	public void setLibre(boolean libre) {
		this.libre = libre;
	}

	@Transient
	public TypeDocument getTypeDeclaration() {
		final ModeleDocument modele = getModeleDocument();
		if (modele == null) {
			return null;
		}
		return modele.getTypeDocument();
	}

	@Transient
	@Override
	public Contribuable getTiers() {
		return (Contribuable) super.getTiers();
	}
}
