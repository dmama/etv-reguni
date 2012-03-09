package ch.vd.uniregctb.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.indexer.tiers.TopList;

@SuppressWarnings("UnusedDeclaration")
public class SearchTiersResults {

	public static class Entry {
		private String numero;
		private String nom1;
		private String nom2;
		private String dateNaissance;
		private String domicile;
		private String forPrincipal;

		public Entry(TiersIndexedData data) {
			this.numero = FormatNumeroHelper.numeroCTBToDisplay(data.getNumero());
			this.nom1 = data.getNom1();
			this.nom2 = data.getNom2();
			this.dateNaissance = RegDateHelper.dateToDisplayString(data.getRegDateNaissance());
			this.domicile = String.format("%s %s", data.getNpa(), data.getLocaliteOuPays());
			this.forPrincipal = data.getForPrincipal();
		}

		public String getNumero() {
			return numero;
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

		public String getDomicile() {
			return domicile;
		}

		public String getForPrincipal() {
			return forPrincipal;
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
