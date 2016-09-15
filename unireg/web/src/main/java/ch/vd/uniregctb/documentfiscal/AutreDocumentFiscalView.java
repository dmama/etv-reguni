package ch.vd.uniregctb.documentfiscal;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.MessageSource;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.type.TypeEtatAutreDocumentFiscal;
import ch.vd.uniregctb.utils.WebContextUtils;

public class AutreDocumentFiscalView implements Annulable {

	private final long id;
	private final long tiersId;
	private final TypeEtatAutreDocumentFiscal etat;
	private final RegDate dateEnvoi;
	private final String libelleTypeDocument;
	private final String libelleSousType;
	private final boolean annule;
	private final boolean avecCopieConformeEnvoi;

	public AutreDocumentFiscalView(AutreDocumentFiscal doc, MessageSource messageSource, String typeKey, String subtypeKey) {
		this(doc,
		     typeKey != null ? messageSource.getMessage(typeKey, null, WebContextUtils.getDefaultLocale()) : StringUtils.EMPTY,
		     subtypeKey != null ? messageSource.getMessage(subtypeKey, null, WebContextUtils.getDefaultLocale()) : StringUtils.EMPTY);
	}

	public AutreDocumentFiscalView(AutreDocumentFiscal doc, String libelleType, String libelleSousType) {
		this.id = doc.getId();
		this.tiersId = doc.getEntreprise().getNumero();
		this.etat = doc.getEtat();
		this.dateEnvoi = doc.getDateEnvoi();
		this.libelleTypeDocument = libelleType;
		this.libelleSousType = libelleSousType;
		this.annule = doc.isAnnule();
		this.avecCopieConformeEnvoi = StringUtils.isNoneBlank(doc.getCleArchivage());
	}

	public long getId() {
		return id;
	}

	public long getTiersId() {
		return tiersId;
	}

	public TypeEtatAutreDocumentFiscal getEtat() {
		return etat;
	}

	public RegDate getDateEnvoi() {
		return dateEnvoi;
	}

	public String getLibelleTypeDocument() {
		return libelleTypeDocument;
	}

	public String getLibelleSousType() {
		return libelleSousType;
	}

	@Override
	public boolean isAnnule() {
		return annule;
	}

	public boolean isAvecCopieConformeEnvoi() {
		return avecCopieConformeEnvoi;
	}
}
