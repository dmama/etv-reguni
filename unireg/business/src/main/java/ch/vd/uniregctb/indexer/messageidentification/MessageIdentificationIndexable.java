package ch.vd.uniregctb.indexer.messageidentification;

import java.util.Date;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.evenement.identification.contribuable.CriteresPersonne;
import ch.vd.uniregctb.evenement.identification.contribuable.Demande;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable;
import ch.vd.uniregctb.evenement.identification.contribuable.Reponse;
import ch.vd.uniregctb.indexer.Indexable;
import ch.vd.uniregctb.indexer.IndexerFormatHelper;

public class MessageIdentificationIndexable implements Indexable {

	private final IdentificationContribuable message;

	private static final String TYPE = "identification";

	public MessageIdentificationIndexable(IdentificationContribuable message) {
		this.message = message;
	}

	private static Integer toIndex(RegDate date) {
		return date == null ? null : date.index();
	}

	private static Long toIndex(Date date) {
		return date == null ? null : date.getTime();
	}

	@Override
	public MessageIdentificationIndexableData getIndexableData() {

		final Demande demande = message.getDemande();
		final CriteresPersonne criteresPersonne = demande.getPersonne();

		final MessageIdentificationIndexableData data = new MessageIdentificationIndexableData(message.getId(), TYPE, IndexerFormatHelper.enumToString(demande.getTypeDemande()));

		data.setTypeMessage(demande.getTypeMessage());
		data.setPeriodeFiscale(demande.getPeriodeFiscale());
		data.setEmetteur(demande.getEmetteurId());
		data.setPriorite(IndexerFormatHelper.enumToString(demande.getPrioriteEmetteur()));
		data.setDateMessage(toIndex(RegDateHelper.get(demande.getDate())));
		data.setEtat(IndexerFormatHelper.enumToString(message.getEtat()));
		if (criteresPersonne != null) {
			data.setNom(criteresPersonne.getNom());
			data.setPrenoms(criteresPersonne.getPrenoms());
			data.setNavs13(IndexerFormatHelper.nullableStringToString(criteresPersonne.getNAVS13()));
			data.setNavs11(IndexerFormatHelper.nullableStringToString(criteresPersonne.getNAVS11()));
			data.setDateNaissance(toIndex(criteresPersonne.getDateNaissance()));
		}
		else {
			data.setNavs13(IndexerFormatHelper.nullableStringToString(null));
			data.setNavs11(IndexerFormatHelper.nullableStringToString(null));
		}
		data.setVisaTraitement(IndexerFormatHelper.nullableStringToString(message.getTraitementUser()));
		data.setDateTraitement(toIndex(message.getDateTraitement()));

		data.setNavs13Upi(IndexerFormatHelper.nullableStringToString(message.getNAVS13Upi()));
		data.setMontant(demande.getMontant());
		data.setAnnule(IndexerFormatHelper.booleanToString(message.isAnnule()));
		data.setUtilisateurTraitant(IndexerFormatHelper.nullableStringToString(message.getUtilisateurTraitant()));
		data.setTransmetteur(IndexerFormatHelper.nullableStringToString(demande.getTransmetteur()));

		final Reponse reponse = message.getReponse();
		if (reponse != null) {
			data.setNoCtbTrouve(reponse.getNoContribuable());
			data.setIdentifie(IndexerFormatHelper.booleanToString(reponse.getErreur() == null && reponse.getNoContribuable() != null));
			data.setMessageErreur(IndexerFormatHelper.nullableStringToString(reponse.getErreur() != null ? reponse.getErreur().getMessage() : null));
		}
		else {
			data.setIdentifie(IndexerFormatHelper.booleanToString(false));
			data.setMessageErreur(null);
		}

		return data;
	}
}
