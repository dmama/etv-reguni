package ch.vd.uniregctb.registrefoncier.situation.surcharge;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.registrefoncier.SituationRF;
import ch.vd.uniregctb.tiers.view.CommuneView;

/**
 * Vue résumée d'une situation d'un immeuble RF.
 */
public class SituationSummaryView {

	/**
	 * Id technique propre à Unireg.
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
	 * La fraction de commune surchargée.
	 */
	@Nullable
	private CommuneView communeSurchargee;

	/**
	 * L'immeuble concerné par la situation.
	 */
	private ImmeubleSummaryView immeuble;

	public SituationSummaryView(@NotNull SituationRF situation, @NotNull ServiceInfrastructureService serviceInfrastructureService) {
		this.id = situation.getId();
		this.dateDebut = situation.getDateDebut();
		this.dateFin = situation.getDateFin();
		this.noParcelle = situation.getNoParcelle();
		this.indexes = buildIndexes(situation.getIndex1(), situation.getIndex2(), situation.getIndex3());
		this.communeRF = new CommuneView(situation.getCommune().getNoOfs(), situation.getCommune().getNomRf());
		this.communeSurchargee = resolveCommune(situation.getNoOfsCommuneSurchargee(), situation.getDateDebut(), serviceInfrastructureService);
		this.immeuble = new ImmeubleSummaryView(situation.getImmeuble(), null);
	}

	@Nullable
	private CommuneView resolveCommune(@Nullable Integer noOfsCommune, @Nullable RegDate dateDebut, @NotNull ServiceInfrastructureService serviceInfrastructureService) {
		if (noOfsCommune == null) {
			return null;
		}
		final Commune commune = serviceInfrastructureService.getCommuneByNumeroOfs(noOfsCommune, dateDebut);
		if (commune == null) {
			throw new ObjectNotFoundException("La commune avec le numéro Ofs=[" + noOfsCommune + "] n'existe pas en date du " + RegDateHelper.dateToDisplayString(dateDebut));
		}
		return new CommuneView(commune.getNoOFS(), commune.getNomOfficiel());
	}

	@Nullable
	public static String buildIndexes(@Nullable Integer index1, @Nullable Integer index2, @Nullable Integer index3) {
		if (index1 == null) {
			return null;
		}
		String indexes = String.valueOf(index1);
		if (index2 == null) {
			return indexes;
		}
		indexes += "/" + String.valueOf(index2);
		if (index3 == null) {
			return indexes;
		}
		indexes += "/" + String.valueOf(index3);
		return indexes;
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

	public CommuneView getCommuneSurchargee() {
		return communeSurchargee;
	}

	public ImmeubleSummaryView getImmeuble() {
		return immeuble;
	}
}
