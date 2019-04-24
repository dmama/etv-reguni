package ch.vd.unireg.param.online;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.utils.Pair;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.common.CollectionsUtils;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.param.view.DelaisAccordablesOnlinePMView;
import ch.vd.unireg.parametrage.DelaisAccordablesOnlineDIPM;
import ch.vd.unireg.parametrage.ParametreDemandeDelaisOnline;

/**
 * Objet-vue pour l'édition des délais accorables online sur les DIs PM.
 */
public class DelaisOnlinePMView {

	private Long periodeFiscaleId;
	private int annee;
	private List<DelaisAccordablesOnlinePMView> periodes;

	public DelaisOnlinePMView() {
	}

	public DelaisOnlinePMView(@NotNull PeriodeFiscale pf, @NotNull ParametreDemandeDelaisOnline paramsDelais) {
		this.periodeFiscaleId = pf.getId();
		this.annee = pf.getAnnee();
		this.periodes = paramsDelais.getPeriodesDelais().stream()
				.filter(AnnulableHelper::nonAnnule)
				.map(DelaisAccordablesOnlineDIPM.class::cast)
				.sorted(Comparator.comparingInt(DelaisAccordablesOnlineDIPM::getIndex))
				.map(DelaisAccordablesOnlinePMView::new)
				.collect(Collectors.toList());
	}

	public Long getPeriodeFiscaleId() {
		return periodeFiscaleId;
	}

	public void setPeriodeFiscaleId(Long periodeFiscaleId) {
		this.periodeFiscaleId = periodeFiscaleId;
	}

	public int getAnnee() {
		return annee;
	}

	public void setAnnee(int annee) {
		this.annee = annee;
	}

	public List<DelaisAccordablesOnlinePMView> getPeriodes() {
		return periodes;
	}

	public void setPeriodes(List<DelaisAccordablesOnlinePMView> periodes) {
		this.periodes = periodes;
	}

	/**
	 * Copie les valeurs métier de cette vue dans le paramètre spécifié. Les valeurs seront ajoutées, supprimées ou mises-à-jour en fonction des cas.
	 */
	public void copyTo(@NotNull ParametreDemandeDelaisOnline paramsDelais) {

		if (this.periodes == null) {
			// on annule toutes les périodes
			paramsDelais.getPeriodesDelais().forEach(p -> p.setAnnule(true));
		}
		else {
			// on détermine quels sont les périodes à ajouter, supprimer et mettre-à-jour
			final List<DelaisAccordablesOnlineDIPM> toAddList = this.periodes.stream()
					.map(DelaisAccordablesOnlinePMView::toEntity)
					.collect(Collectors.toCollection(LinkedList::new));
			final List<DelaisAccordablesOnlineDIPM> toRemoveList = paramsDelais.getPeriodesDelais().stream()
					.filter(AnnulableHelper::nonAnnule)
					.map(DelaisAccordablesOnlineDIPM.class::cast)
					.sorted(Comparator.comparingInt(DelaisAccordablesOnlineDIPM::getIndex))
					.collect(Collectors.toCollection(LinkedList::new));
			final List<Pair<DelaisAccordablesOnlineDIPM, DelaisAccordablesOnlineDIPM>> toUpdate = CollectionsUtils.extractCommonElements(toAddList, toRemoveList, DelaisOnlinePMView::idsAreEquals);

			// on annule les périodes qui doivent l'être
			toRemoveList.forEach(p -> p.setAnnule(true));

			// on ajoute les nouvelles périodes
			toAddList.forEach(paramsDelais::addPeriodeDelais);

			// on met-à-jour les périodes existantes
			toUpdate.forEach(pair -> {
				final DelaisAccordablesOnlineDIPM edited = pair.getFirst();
				final DelaisAccordablesOnlineDIPM persisted = pair.getSecond();
				edited.copyTo(persisted);
			});
		}
	}

	protected static boolean idsAreEquals(DelaisAccordablesOnlineDIPM left, DelaisAccordablesOnlineDIPM right) {
		final Long leftId = left.getId();
		final Long rightId = right.getId();
		return leftId != null && leftId.equals(rightId);    // des ids nuls sont toujours considérés comme non-égaux
	}
}
