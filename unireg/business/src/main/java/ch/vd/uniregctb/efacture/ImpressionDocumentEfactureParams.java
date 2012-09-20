package ch.vd.uniregctb.efacture;

import java.util.Date;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.TypeDocument;

public class ImpressionDocumentEfactureParams {

	private Tiers tiers;
	private TypeDocument typeDocument;
	private Date dateTraitement;
	private RegDate dateDemande;

	public ImpressionDocumentEfactureParams(Tiers tiers, TypeDocument typeDoc,Date dateTraitement,RegDate dateDemande) {
		this.tiers = tiers;
		this.typeDocument = typeDoc;
		this.dateTraitement = dateTraitement;
		this.dateDemande = dateDemande;
	}

	public Tiers getTiers() {
		return tiers;
	}

	public TypeDocument getTypeDocument() {
		return typeDocument;
	}

	public Date getDateTraitement() {
		return dateTraitement;
	}

	public RegDate getDateDemande() {
		return dateDemande;
	}
}
