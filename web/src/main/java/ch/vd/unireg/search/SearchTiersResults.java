package ch.vd.unireg.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.avatar.TypeAvatar;
import ch.vd.unireg.indexer.tiers.TiersIndexedData;
import ch.vd.unireg.indexer.tiers.TopList;

@SuppressWarnings("UnusedDeclaration")
public class SearchTiersResults {

	public static class Entry {
		private Long numero;
		private String role1;
		private String role2;
		private String nom1;
		private String nom2;
		private String dateNaissance;
		private String npa;
		private String localitePays;
		private String forPrincipal;
		private RegDate dateOuverture;
		private RegDate dateFermeture;
		private RegDate dateOuvertureVD;
		private RegDate dateFermetureVD;
		private boolean annule;
		private boolean debiteurInactif;
		private String tiersType;
		private TypeAvatar typeAvatar;

		public Entry(TiersIndexedData data) {
			this.numero = data.getNumero();
			this.role1 = data.getRoleLigne1();
			this.role2 = data.getRoleLigne2();
			this.nom1 = data.getNom1();
			this.nom2 = data.getNom2();
			this.dateNaissance = RegDateHelper.dateToDisplayString(data.getRegDateNaissanceInscriptionRC());
			this.npa = data.getNpa();
			this.localitePays = data.getLocaliteOuPays();
			this.forPrincipal = data.getForPrincipal();
			this.dateOuverture = RegDateHelper.get(data.getDateOuvertureFor());
			this.dateFermeture = RegDateHelper.get(data.getDateFermetureFor());
			this.dateOuvertureVD = RegDateHelper.get(data.getDateOuvertureForVd());
			this.dateFermetureVD = RegDateHelper.get(data.getDateFermetureForVd());
			this.annule = data.isAnnule();
			this.debiteurInactif = data.isDebiteurInactif();
			this.tiersType = data.getTiersType();
			this.typeAvatar = data.getTypeAvatar();
		}

		public Long getNumero() {
			return numero;
		}

		public String getRole1() {
			return role1;
		}

		public String getRole2() {
			return role2;
		}

		public String getNom1() {
			return nom1;
		}

		public String getNom2() {
			return nom2;
		}

		public String getDateNaissance() {
			return dateNaissance;
		}

		public String getNpa() {
			return npa;
		}

		public String getLocalitePays() {
			return localitePays;
		}

		public String getForPrincipal() {
			return forPrincipal;
		}

		public RegDate getDateOuverture() {
			return dateOuverture;
		}

		public RegDate getDateFermeture() {
			return dateFermeture;
		}

		public RegDate getDateOuvertureVD() {
			return dateOuvertureVD;
		}

		public RegDate getDateFermetureVD() {
			return dateFermetureVD;
		}

		public boolean isAnnule() {
			return annule;
		}

		public boolean isDebiteurInactif() {
			return debiteurInactif;
		}

		public String getTiersType() {
			return tiersType;
		}

		public TypeAvatar getTypeAvatar() {
			return typeAvatar;
		}
	}

	private String summary;
	private String filterDescription;
	private List<Entry> entries;

	public SearchTiersResults(String summary) {
		this.summary = summary;
		this.entries = Collections.emptyList();
	}

	public SearchTiersResults(String summary, TopList<TiersIndexedData> list) {
		this.summary = summary;
		this.entries = new ArrayList<>();
		for (TiersIndexedData data : list) {
			this.entries.add(new Entry(data));
		}
	}

	public String getSummary() {
		return summary;
	}

	public List<Entry> getEntries() {
		return entries;
	}

	public String getFilterDescription() {
		return filterDescription;
	}

	public void setFilterDescription(String filterDescription) {
		this.filterDescription = filterDescription;
	}
}
