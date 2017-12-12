package ch.vd.uniregctb.registrefoncier.situation.surcharge;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.registrefoncier.CapitastraURLProvider;
import ch.vd.uniregctb.registrefoncier.SituationRF;
import ch.vd.uniregctb.tiers.view.CommuneView;

/**
 * Vue détaillée d'une situation d'un immeuble.
 */
public class SituationFullView {

	/**
	 * Id technique de la situation.
	 */
	private Long id;

	private RegDate dateDebut;
	private RegDate dateFin;

	/**
	 * Le numéro de la parcelle sur laquelle est construit l'immeuble.
	 */
	private int noParcelle;

	/**
	 * Les indexes de la parcelle
	 */
	private String indexes;

	/**
	 * La commune du RF sur laquelle est sis l'immeuble.
	 */
	private CommuneView communeRF;

	/**
	 * L'immeuble concerné par la situation.
	 */
	private ImmeubleSummaryView immeuble;

	/**
	 * Lien url vers le détail d'une situation
	 */
	private String showAction;

	/**
	 * lien url vers la liste
	 */
	private String listAction;

	/**
	 * Paramètres :
	 * <ul>
	 *     <li>Ordre de tri (ascendant/descendant)</li>
	 *     <li>Critère de tri (colonne de tri)</li>
	 *     <li>Numéro de la page (pagination)</li>
	 * </ul>
	 */
	private String params;

	public SituationFullView(@NotNull SituationRF situation, @NotNull CapitastraURLProvider capitastraURLProvider, @NotNull String showAction, String listAction) {
		this.id = situation.getId();
		this.dateDebut = situation.getDateDebut();
		this.dateFin = situation.getDateFin();
		this.noParcelle = situation.getNoParcelle();
		this.indexes = SituationSummaryView.buildIndexes(situation.getIndex1(), situation.getIndex2(), situation.getIndex3());
		this.communeRF = new CommuneView(situation.getCommune().getNoOfs(), situation.getCommune().getNomRf());
		this.immeuble = new ImmeubleSummaryView(situation.getImmeuble(), capitastraURLProvider);
		this.showAction = showAction;
		this.listAction = listAction;
	}

	public Long getId() {
		return id;
	}

	public RegDate getDateDebut() {
		return dateDebut;
	}

	public RegDate getDateFin() {
		return dateFin;
	}

	public int getNoParcelle() {
		return noParcelle;
	}

	public String getIndexes() {
		return indexes;
	}

	public CommuneView getCommuneRF() {
		return communeRF;
	}

	public ImmeubleSummaryView getImmeuble() {
		return immeuble;
	}

	public String getShowAction() { return showAction; }

	public String getListAction() { return listAction; }
}
