package ch.vd.uniregctb.tiers.timeline;

import java.util.ArrayList;
import java.util.List;

import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;

/**
 * Données pour la page de visualisation de l'historique des fors fiscaux et assujettissements d'un contribuable
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@SuppressWarnings("UnusedDeclaration")
public class ForsTimelineView {

	// paramètres du formulaire
	private Long tiersId = null;

	// données découlant des paramètres
	private final TimelineTable table = new TimelineTable();
	private final List<AssujettissementException> exceptions = new ArrayList<AssujettissementException>();

	private boolean forPrint = false;
	private String title;
	private String description;

	private final boolean showForsGestion;
	private final boolean showAssujettissements;
	private final boolean showPeriodesImposition;

	public ForsTimelineView(boolean showForsGestion, boolean showAssujettissements, boolean showPeriodesImposition) {
		this.showForsGestion = showForsGestion;
		this.showAssujettissements = showAssujettissements;
		this.showPeriodesImposition = showPeriodesImposition;
	}

	public Long getTiersId() {
		return tiersId;
	}

	public void setTiersId(Long tiersId) {
		this.tiersId = tiersId;
	}

	public boolean isForPrint() {
		return forPrint;
	}

	public void setForPrint(boolean forPrint) {
		this.forPrint = forPrint;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public TimelineTable getTable() {
		return table;
	}

	public void addException(AssujettissementException e) {
		exceptions.add(e);
	}

	public List<AssujettissementException> getExceptions() {
		return exceptions;
	}

	public boolean isShowForsGestion() {
		return showForsGestion;
	}

	public boolean isShowAssujettissements() {
		return showAssujettissements;
	}

	public boolean isShowPeriodesImposition() {
		return showPeriodesImposition;
	}
}
