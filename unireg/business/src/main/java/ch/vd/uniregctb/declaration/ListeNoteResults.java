package ch.vd.uniregctb.declaration;

import java.util.ArrayList;
import java.util.List;

import org.hsqldb.lib.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.JobResults;
import ch.vd.uniregctb.tiers.Contribuable;

public class ListeNoteResults extends JobResults<Long, ListeNoteResults> {
	public ListeNoteResults(RegDate dateTraitement) {
		this.dateTraitement = dateTraitement;
	}

	public RegDate getDateTraitement() {
		return dateTraitement;
	}

	public boolean isInterrompu() {
		return interrompu;
	}


	public static class InfoContribuable {
		public final long id;


		public InfoContribuable(long id) {
			this.id = id;

		}
	}

	public static class InfoContribuableWithNote extends InfoContribuable {
		public String localite;

		public InfoContribuableWithNote(long id, String localite) {
			super(id);
			this.localite = localite;
		}
	}

	public static class Erreur extends InfoContribuable {
		public String message;

		public Erreur(long id, String message) {
			super(id);
			this.message = message;
		}
	}

	public RegDate dateTraitement;
	public boolean interrompu;
	public int nbContribuable;
	public int periode;

	public List<InfoContribuableWithNote> listeContribuableAvecNote = new ArrayList<InfoContribuableWithNote>();
	public List<Erreur> erreurs = new ArrayList<Erreur>();

	public ListeNoteResults(RegDate dateTraitement,int periode) {
		this.dateTraitement = dateTraitement;
		this.periode = periode;
	}

	public void addContribuableAvecNote(Contribuable cont){
		   listeContribuableAvecNote.add(new InfoContribuableWithNote(cont.getId(),""));
	}
	public void addErrorException(Long element, Exception e) {
		erreurs.add(new Erreur(element,e.getMessage()));

	}

	public void addAll(ListeNoteResults right) {
		this.nbContribuable+=right.nbContribuable;
		listeContribuableAvecNote.addAll(right.listeContribuableAvecNote);
		erreurs.addAll(right.erreurs);

	}

	public int getPeriode() {
		return periode;
	}

	public void setPeriode(int periode) {
		this.periode = periode;
	}
}
