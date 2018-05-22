package ch.vd.unireg.evenement.externe;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;
import java.util.Objects;

import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.LengthConstants;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@Entity
@DiscriminatorValue("QuittanceLR")
public class QuittanceLR extends EvenementExterne {

	/**
	 * Contient le numéro de tiers lorsque l'objet tiers ne peut pas être chargé (valeur transiente).
	 */
	private Long tiersId;

	private RegDate dateDebut;
	private RegDate dateFin;
	private TypeQuittance type;

	@Column(name = "QLR_DATE_DEBUT")
	@Type(type = "ch.vd.unireg.hibernate.RegDateUserType")
	public RegDate getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
	}

	@Column(name = "QLR_DATE_FIN")
	@Type(type = "ch.vd.unireg.hibernate.RegDateUserType")
	public RegDate getDateFin() {
		return dateFin;
	}

	public void setDateFin(RegDate dateFin) {
		this.dateFin = dateFin;
	}

	@Column(name = "QLR_TYPE", length = LengthConstants.EVTEXTERNE_QLR_TYPE)
	@Type(type = "ch.vd.unireg.hibernate.TypeQuittanceUserType")
	public TypeQuittance getType() {
		return type;
	}

	public void setType(TypeQuittance type) {
		this.type = type;
	}

	@Transient
	public Long getTiersId() {
		if (tiersId != null && tiers != null) {
			if (!Objects.equals(tiers.getNumero(), tiersId)) {
				throw new IllegalArgumentException();
			}
		}
		if (tiers != null) {
			return tiers.getNumero();
		}
		else {
			return tiersId;
		}
	}

	public void setTiersId(Long tiersId) {
		if (tiers != null) {
			if (!Objects.equals(tiers.getNumero(), tiersId)) {
				throw new IllegalArgumentException();
			}
		}
		this.tiersId = tiersId;
	}
}
