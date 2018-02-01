package ch.vd.uniregctb.annonceIDE;

import java.util.Date;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.utils.Pair;
import ch.vd.unireg.interfaces.organisation.data.BaseAnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.StatutAnnonce;

/**
 * Vue web du statut d'une annonce à l'IDE.
 */
public class StatutView {
	private StatutAnnonce statut;
	private Date date;
	private List<Pair<String, String>> erreurs;

	public StatutView(StatutAnnonce statut, Date date) {
		this.statut = statut;
		this.date = date;
	}

	public StatutView(@NotNull BaseAnnonceIDE.Statut statut) {
		this.statut = statut.getStatut();
		this.date = statut.getDateStatut();
		this.erreurs = statut.getErreurs();
	}

	public StatutAnnonce getStatut() {
		return statut;
	}

	public void setStatut(StatutAnnonce statut) {
		this.statut = statut;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public List<Pair<String, String>> getErreurs() {
		return erreurs;
	}

	public void setErreurs(List<Pair<String, String>> erreurs) {
		this.erreurs = erreurs;
	}
}
