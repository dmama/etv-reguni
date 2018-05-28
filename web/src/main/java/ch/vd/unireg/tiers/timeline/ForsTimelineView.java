package ch.vd.unireg.tiers.timeline;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.metier.assujettissement.AssujettissementException;
import ch.vd.unireg.tiers.NatureTiers;

/**
 * Données pour la page de visualisation de l'historique des fors fiscaux et assujettissements d'un contribuable
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@SuppressWarnings("UnusedDeclaration")
public class ForsTimelineView {

	// paramètres du formulaire
	private Long tiersId = null;
	private NatureTiers natureTiers = null;

	// données découlant des paramètres
	private final TimelineTable table;
	private final List<AssujettissementException> exceptions = new ArrayList<>();

	private boolean forPrint = false;
	private String title;
	private String description;

	private final boolean showForsGestion;
	private final boolean showAssujettissementsSource;
	private final boolean showAssujettissementsRole;
	private final boolean showAssujettissements;
	private final boolean showPeriodesImposition;
	private final boolean showPeriodesImpositionIS;
	private final boolean showPeriodesExploitation;

	public ForsTimelineView(boolean invertedTime, boolean showForsGestion, boolean showAssujettissementsSource, boolean showAssujettissementsRole, boolean showAssujettissements,
	                        boolean showPeriodesImposition, boolean showPeriodesImpositionIS, boolean showPeriodesExploitation, RegDate bigBang) {
		this.showForsGestion = showForsGestion;
		this.showAssujettissementsSource = showAssujettissementsSource;
		this.showAssujettissementsRole = showAssujettissementsRole;
		this.showAssujettissements = showAssujettissements;
		this.showPeriodesImposition = showPeriodesImposition;
		this.showPeriodesImpositionIS = showPeriodesImpositionIS;
		this.showPeriodesExploitation = showPeriodesExploitation;
		this.table = new TimelineTable(invertedTime, bigBang);
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

	public boolean isShowAssujettissementsSource() {
		return showAssujettissementsSource;
	}

	public boolean isShowAssujettissementsRole() {
		return showAssujettissementsRole;
	}

	public boolean isShowPeriodesImposition() {
		return showPeriodesImposition;
	}

	public boolean isShowPeriodesImpositionIS() {
		return showPeriodesImpositionIS;
	}

	public boolean isShowPeriodesExploitation() {
		return showPeriodesExploitation;
	}

	public boolean isInvertedTime() {
		return table.invertedTime;
	}

	public NatureTiers getNatureTiers() {
		return natureTiers;
	}

	public void setNatureTiers(NatureTiers natureTiers) {
		this.natureTiers = natureTiers;
	}
}
