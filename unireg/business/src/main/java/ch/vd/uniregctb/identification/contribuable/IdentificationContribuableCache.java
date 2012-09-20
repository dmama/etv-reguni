package ch.vd.uniregctb.identification.contribuable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable;
import ch.vd.uniregctb.evenement.identification.contribuable.TypeDemande;

public class IdentificationContribuableCache {
	private List<String> emetteurIdsNonTraites = new ArrayList<String>();

	private List<Integer> periodeFiscalesNonTraites = new ArrayList<Integer>();

	private List<String> emetteurIdsTraites = new ArrayList<String>();

	private List<Integer> periodeFiscalesTraites = new ArrayList<Integer>();

	private List<String> typeMessageNonTraites = new ArrayList<String>();

	private List<String> typeMessageTraites = new ArrayList<String>();

	private Map<TypeDemande, TypeMessageParEtatCache> typeMessagesParDemandeCache = new HashMap<TypeDemande, TypeMessageParEtatCache>();

	private List<String> listTraitementUsers = new ArrayList<String>();

	private List<IdentificationContribuable.Etat> etatMessageNonTraites = new ArrayList<IdentificationContribuable.Etat>();

	private List<IdentificationContribuable.Etat> etatMessageTraites = new ArrayList<IdentificationContribuable.Etat>();




	public IdentificationContribuableCache() {

	}

	public List<String> getEmetteurIdsNonTraites() {
		return emetteurIdsNonTraites;
	}

	public void setEmetteurIdsNonTraites(List<String> emetteurIdsNonTraites) {
		this.emetteurIdsNonTraites = emetteurIdsNonTraites;
	}

	public List<Integer> getPeriodeFiscalesNonTraites() {
		return periodeFiscalesNonTraites;
	}

	public void setPeriodeFiscalesNonTraites(List<Integer> periodeFiscalesNonTraites) {
		this.periodeFiscalesNonTraites = periodeFiscalesNonTraites;
	}

	public List<String> getEmetteurIdsTraites() {
		return emetteurIdsTraites;
	}

	public void setEmetteurIdsTraites(List<String> emetteurIdsTraites) {
		this.emetteurIdsTraites = emetteurIdsTraites;
	}

	public List<Integer> getPeriodeFiscalesTraites() {
		return periodeFiscalesTraites;
	}

	public void setPeriodeFiscalesTraites(List<Integer> periodeFiscalesTraites) {
		this.periodeFiscalesTraites = periodeFiscalesTraites;
	}

	public List<String> getTypeMessageNonTraites() {
		return typeMessageNonTraites;
	}

	public void setTypeMessageNonTraites(List<String> typeMessageNonTraites) {
		this.typeMessageNonTraites = typeMessageNonTraites;
	}

	public List<String> getTypeMessageTraites() {
		return typeMessageTraites;
	}

	public void setTypeMessageTraites(List<String> typeMessageTraites) {
		this.typeMessageTraites = typeMessageTraites;
	}

	public Map<TypeDemande, TypeMessageParEtatCache> getTypeMessagesParDemandeCache() {
		return typeMessagesParDemandeCache;
	}

	public List<String> getListTraitementUsers() {
		return listTraitementUsers;
	}

	public void setListTraitementUsers(List<String> listTraitementUsers) {
		this.listTraitementUsers = listTraitementUsers;
	}

	public List<IdentificationContribuable.Etat> getEtatMessageNonTraites() {
		return etatMessageNonTraites;
	}

	public void setEtatMessageNonTraites(List<IdentificationContribuable.Etat> etatMessageNonTraites) {
		this.etatMessageNonTraites = etatMessageNonTraites;
	}

	public List<IdentificationContribuable.Etat> getEtatMessageTraites() {
		return etatMessageTraites;
	}

	public void setEtatMessageTraites(List<IdentificationContribuable.Etat> etatMessageTraites) {
		this.etatMessageTraites = etatMessageTraites;
	}
}