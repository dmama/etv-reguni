package ch.vd.uniregctb.foncier;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;

import ch.vd.uniregctb.common.CodeControleHelper;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.documentfiscal.AutreDocumentFiscalAvecSuivi;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;

@Entity
@DiscriminatorValue(value = "DemandeDegrevement")
public class DemandeDegrevementICI extends AutreDocumentFiscalAvecSuivi {

	/**
	 * l'immeuble considéré pour la demande de dégrèvement.
	 */
	private ImmeubleRF immeuble;

	/**
	 * la période fiscale prévisible de début de validité des informations de dégrèvement à recevoir
	 */
	private Integer periodeFiscale;

	/**
	 * le numéro de séquence de la demande de dégrévement, par contribuable et période fiscale
	 */
	private Integer numeroSequence;

	/**
	 * Le code de contrôle de la demande de dégrèvement
	 */
	private String codeControle;

	/**
	 * @return un nouveau code de contrôle d'une lettre et de cinq chiffres aléatoires
	 */
	public static String generateCodeControle() {
		return CodeControleHelper.generateCodeControleUneLettreCinqChiffres();
	}

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

	@Override
	@Column(name = "DD_PERIODE_FISCALE")
	public Integer getPeriodeFiscale() {
		return periodeFiscale;
	}

	public void setPeriodeFiscale(Integer periodeFiscale) {
		this.periodeFiscale = periodeFiscale;
	}

	@Column(name = "DD_NUMERO_SEQUENCE")
	public Integer getNumeroSequence() {
		return numeroSequence;
	}

	public void setNumeroSequence(Integer numeroSequence) {
		this.numeroSequence = numeroSequence;
	}

	@Column(name = "DD_CODE_CONTROLE", length = LengthConstants.DI_CODE_CONTROLE)
	public String getCodeControle() {
		return codeControle;
	}

	public void setCodeControle(String codeControle) {
		this.codeControle = codeControle;
	}
}
