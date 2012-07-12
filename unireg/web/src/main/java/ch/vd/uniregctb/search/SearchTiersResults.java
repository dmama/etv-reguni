package ch.vd.uniregctb.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.indexer.tiers.TopList;

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
		private boolean annule;
		private boolean debiteurInactif;

		public Entry(TiersIndexedData data) {
			this.numero = data.getNumero();
			this.role1 = data.getRoleLigne1();
			this.role2 = data.getRoleLigne2();
			this.nom1 = data.getNom1();
			this.nom2 = data.getNom2();
			this.dateNaissance = RegDateHelper.dateToDisplayString(data.getRegDateNaissance());
			this.npa = data.getNpa();
			this.localitePays = data.getLocaliteOuPays();
			this.forPrincipal = data.getForPrincipal();
			this.dateOuverture = RegDate.get(data.getDateOuvertureFor());
			this.dateFermeture = RegDate.get(data.getDateFermetureFor());
			this.annule = data.isAnnule();
			this.debiteurInactif = data.isDebiteurInactif();
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

		public boolean isAnnule() {
			return annule;
		}

		public boolean isDebiteurInactif() {
			return debiteurInactif;
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
		this.entries = new ArrayList<Entry>();
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
