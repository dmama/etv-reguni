package ch.vd.unireg.efacture;

import java.math.BigInteger;
import java.util.Date;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.type.TypeDocument;

public class ImpressionDocumentEfactureParams {

	private final Tiers tiers;
	private final TypeDocument typeDocument;
	private final Date dateTraitement;
	private final RegDate dateDemande;
	private final BigInteger noAdherentCourant;
	private final RegDate dateDemandePrecedente;
	private final BigInteger noAdherentPrecedent;

	public ImpressionDocumentEfactureParams(Tiers tiers, TypeDocument typeDoc, Date dateTraitement, RegDate dateDemande, BigInteger noAdherentCourant) {
		this(tiers, typeDoc, dateTraitement, dateDemande, noAdherentCourant, null, null);
	}

	public ImpressionDocumentEfactureParams(Tiers tiers, TypeDocument typeDoc, Date dateTraitement, RegDate dateDemande, BigInteger noAdherentCourant, RegDate dateDemandePrecedente, BigInteger noAdherentPrecedent) {
		this.tiers = tiers;
		this.typeDocument = typeDoc;
		this.dateTraitement = dateTraitement;
		this.dateDemande = dateDemande;
		this.noAdherentCourant = noAdherentCourant;
		this.dateDemandePrecedente = dateDemandePrecedente;
		this.noAdherentPrecedent = noAdherentPrecedent;
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

	public BigInteger getNoAdherentCourant() {
		return noAdherentCourant;
	}

	public RegDate getDateDemandePrecedente() {
		return dateDemandePrecedente;
	}

	public BigInteger getNoAdherentPrecedent() {
		return noAdherentPrecedent;
	}
}
