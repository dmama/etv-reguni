package ch.vd.uniregctb.degrevement;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.documentfiscal.AutreDocumentFiscalAvecSuivi;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;

@Entity
@DiscriminatorValue(value = "DemandeDegrevement")
public class DemandeDegrevement extends AutreDocumentFiscalAvecSuivi {

	/**
	 * l'immeuble considéré pour la demande de dégrèvement.
	 */
	private ImmeubleRF immeuble;

	/**
	 * Le motif d'envoi de la demande de dégrèvement.
	 */
	private MotifEnvoiDD motifEnvoi;

	/**
	 * La date de début de validité du dégrèvement.
	 */
	private RegDate dateDebut;

	/**
	 * La date de début de validité du dégrèvement.
	 */
	@Nullable
	private RegDate dateFin;

	/**
	 * Le délai de retour de la demande après rappel.
	 */
	private RegDate delaiRappel;

	// configuration hibernate : l'immeuble ne possède pas les droits (les droits pointent vers les immeubles, c'est tout)
	@ManyToOne
	@JoinColumn(name = "DD_IMMEUBLE_ID")
	@ForeignKey(name = "FK_DD_RF_IMMEUBLE_ID")
	@Index(name = "IDX_DD_RF_IMMEUBLE_ID", columnNames = "IMMEUBLE_ID")
	public ImmeubleRF getImmeuble() {
		return immeuble;
	}

	public void setImmeuble(ImmeubleRF immeuble) {
		this.immeuble = immeuble;
	}

	@Column(name = "DD_MOTIF_ENVOI", length = LengthConstants.MOTIF_ENVOI_DD)
	@Enumerated(EnumType.STRING)
	public MotifEnvoiDD getMotifEnvoi() {
		return motifEnvoi;
	}

	public void setMotifEnvoi(MotifEnvoiDD motifEnvoi) {
		this.motifEnvoi = motifEnvoi;
	}

	@Nullable
	@Column(name = "DD_DATE_DEBUT")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(@Nullable RegDate dateDebut) {
		this.dateDebut = dateDebut;
	}

	@Nullable
	@Column(name = "DD_DATE_FIN")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateFin() {
		return dateFin;
	}

	public void setDateFin(@Nullable RegDate dateFin) {
		this.dateFin = dateFin;
	}

	@Nullable
	@Column(name = "DD_DELAI_RAPPEL")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDelaiRappel() {
		return delaiRappel;
	}

	public void setDelaiRappel(RegDate delaiRappel) {
		this.delaiRappel = delaiRappel;
	}
}
