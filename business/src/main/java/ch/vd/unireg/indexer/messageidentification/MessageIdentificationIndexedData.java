package ch.vd.unireg.indexer.messageidentification;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.Document;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.identification.contribuable.Demande;
import ch.vd.unireg.evenement.identification.contribuable.IdentificationContribuable;
import ch.vd.unireg.evenement.identification.contribuable.TypeDemande;
import ch.vd.unireg.indexer.lucene.DocumentExtractorHelper;
import ch.vd.unireg.indexer.lucene.LuceneHelper;

public class MessageIdentificationIndexedData implements Serializable {

	private static final long serialVersionUID = 3228468161794785715L;

	private final Long id;
	private final String typeMesssage;
	private final TypeDemande typeDemande;
	private final Integer periodeFiscale;
	private final String emetteurId;
	private final Demande.PrioriteEmetteur priorite;
	private final RegDate dateMessage;
	private final IdentificationContribuable.Etat etat;
	private final String nom;
	private final String prenoms;
	private final String navs13;
	private final String navs11;
	private final RegDate dateNaissance;
	private final String visaTraitement;
	private final Date dateTraitement;

	private final Long noContribuableIdentifie;
	private final String navs13Upi;
	private final Long montant;
	private final boolean annule;
	private final String utilisateurTraitant;
	private final String transmetteur;
	private final boolean identifie;
	private final String messageErreur;

	private static final Map<String, TypeDemande> DOCSUBTYPE_TYPE_DEMANDE_MAP = buildDocSubtypeTypeDemandeMap();

	/**
	 * Comme le doc subtype est minusculisé avant d'être sauvegardé, il faut en tenir compte
	 * au moment de la récupération
	 */
	private static Map<String, TypeDemande> buildDocSubtypeTypeDemandeMap() {
		final Map<String, TypeDemande> map = new HashMap<>(TypeDemande.values().length);
		for (TypeDemande type : TypeDemande.values()) {
			map.put(type.name().toLowerCase(), type);
		}
		return map;
	}

	public MessageIdentificationIndexedData(Document doc) {
		this.id = DocumentExtractorHelper.getLongValue(LuceneHelper.F_ENTITYID, doc);
		this.typeMesssage = DocumentExtractorHelper.getDocValue(MessageIdentificationIndexableData.TYPE_MESSAGE, doc);
		this.typeDemande = DocumentExtractorHelper.getValue(LuceneHelper.F_DOCSUBTYPE, doc, DOCSUBTYPE_TYPE_DEMANDE_MAP::get);
		this.periodeFiscale = DocumentExtractorHelper.getIntegerValue(MessageIdentificationIndexableData.PERIODE_FISCALE, doc);
		this.emetteurId = DocumentExtractorHelper.getDocValue(MessageIdentificationIndexableData.EMETTEUR, doc);
		this.priorite = DocumentExtractorHelper.getEnumValue(MessageIdentificationIndexableData.PRIORITE, doc, Demande.PrioriteEmetteur.class);
		this.dateMessage = DocumentExtractorHelper.getRegDateValue(MessageIdentificationIndexableData.DATE_MESSAGE, doc, false);
		this.etat = DocumentExtractorHelper.getEnumValue(MessageIdentificationIndexableData.ETAT, doc, IdentificationContribuable.Etat.class);
		this.nom = DocumentExtractorHelper.getDocValue(MessageIdentificationIndexableData.NOM, doc);
		this.prenoms = DocumentExtractorHelper.getDocValue(MessageIdentificationIndexableData.PRENOMS, doc);
		this.navs13 = DocumentExtractorHelper.getDocValue(MessageIdentificationIndexableData.NAVS13, doc);
		this.navs11 = DocumentExtractorHelper.getDocValue(MessageIdentificationIndexableData.NAVS11, doc);
		this.dateNaissance = DocumentExtractorHelper.getRegDateValue(MessageIdentificationIndexableData.DATE_NAISSANCE, doc, true);
		this.visaTraitement = DocumentExtractorHelper.getDocValue(MessageIdentificationIndexableData.VISA_TRAITEMENT, doc);
		this.dateTraitement = fromTimestamp(MessageIdentificationIndexableData.DATE_TRAITEMENT, doc);

		this.noContribuableIdentifie = DocumentExtractorHelper.getLongValue(MessageIdentificationIndexableData.CTB_TROUVE, doc);
		this.navs13Upi = DocumentExtractorHelper.getDocValue(MessageIdentificationIndexableData.AVS_UPI, doc);
		this.montant = DocumentExtractorHelper.getLongValue(MessageIdentificationIndexableData.MONTANT, doc);
		this.annule = DocumentExtractorHelper.getBooleanValue(MessageIdentificationIndexableData.ANNULE, doc, Boolean.FALSE);
		this.utilisateurTraitant = StringUtils.trimToNull(DocumentExtractorHelper.getDocValue(MessageIdentificationIndexableData.UTILISATEUR_TRAITANT, doc));
		this.transmetteur = DocumentExtractorHelper.getDocValue(MessageIdentificationIndexableData.TRANSMETTEUR, doc);
		this.identifie = DocumentExtractorHelper.getBooleanValue(MessageIdentificationIndexableData.IDENTIFIE, doc, noContribuableIdentifie != null);
		this.messageErreur = DocumentExtractorHelper.getDocValue(MessageIdentificationIndexableData.ERREUR, doc);
	}

	private static Date fromTimestamp(String key, Document doc) {
		final Long ts = DocumentExtractorHelper.getLongValue(key, doc);
		if (ts != null) {
			return new Date(ts);
		}
		else {
			return null;
		}
	}

	public Long getId() {
		return id;
	}

	public String getTypeMesssage() {
		return typeMesssage;
	}

	public TypeDemande getTypeDemande() {
		return typeDemande;
	}

	public Integer getPeriodeFiscale() {
		return periodeFiscale;
	}

	public String getEmetteurId() {
		return emetteurId;
	}

	public Demande.PrioriteEmetteur getPriorite() {
		return priorite;
	}

	public RegDate getDateMessage() {
		return dateMessage;
	}

	public IdentificationContribuable.Etat getEtat() {
		return etat;
	}

	public String getNom() {
		return nom;
	}

	public String getPrenoms() {
		return prenoms;
	}

	public String getNavs13() {
		return navs13;
	}

	public String getNavs11() {
		return navs11;
	}

	public RegDate getDateNaissance() {
		return dateNaissance;
	}

	public String getVisaTraitement() {
		return visaTraitement;
	}

	public Date getDateTraitement() {
		return dateTraitement;
	}

	public Long getNoContribuableIdentifie() {
		return noContribuableIdentifie;
	}

	public String getNavs13Upi() {
		return navs13Upi;
	}

	public Long getMontant() {
		return montant;
	}

	public boolean isAnnule() {
		return annule;
	}

	public String getUtilisateurTraitant() {
		return utilisateurTraitant;
	}

	public String getTransmetteur() {
		return transmetteur;
	}

	public boolean isIdentifie() {
		return identifie;
	}

	public String getMessageErreur() {
		return messageErreur;
	}
}
