package ch.vd.unireg.param.online;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Pair;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.common.CollectionsUtils;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.param.view.DelaisAccordablesOnlinePPView;
import ch.vd.unireg.parametrage.DelaisAccordablesOnlineDIPP;
import ch.vd.unireg.parametrage.ParametreDemandeDelaisOnline;

/**
 * Objet-vue pour l'édition des délais accorables online sur les DIs PP.
 */
public class DelaisOnlinePPView {

	private Long periodeFiscaleId;
	private int annee;
	private List<DelaisAccordablesOnlinePPView> periodes;

	public DelaisOnlinePPView() {
	}

	public DelaisOnlinePPView(@NotNull PeriodeFiscale pf, @NotNull ParametreDemandeDelaisOnline paramsDelais) {
		this.periodeFiscaleId = pf.getId();
		this.annee = pf.getAnnee();
		this.periodes = paramsDelais.getPeriodesDelais().stream()
				.filter(AnnulableHelper::nonAnnule)
				.map(DelaisAccordablesOnlineDIPP.class::cast)
				.sorted(DateRangeComparator::compareRanges)
				.map(DelaisAccordablesOnlinePPView::new)
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

	public List<DelaisAccordablesOnlinePPView> getPeriodes() {
		return periodes;
	}

	public void setPeriodes(List<DelaisAccordablesOnlinePPView> periodes) {
		this.periodes = periodes;
	}

	public void calculeDatesFin() {

		if (this.periodes != null) {
			// on trie les périodes par date de début
			this.periodes.sort(Comparator.comparing(DelaisAccordablesOnlinePPView::getDateDebut));

			// on parcoure la liste et on applique les dates de fin qui vont bien
			DelaisAccordablesOnlinePPView previous = null;
			for (DelaisAccordablesOnlinePPView current : periodes) {
				if (previous != null) {
					previous.setDateFin(current.getDateDebut().getOneDayBefore());
				}
				previous = current;
			}

			// la dernière se termine par défaut au 31.12 de l'année
			if (!periodes.isEmpty()) {
				final DelaisAccordablesOnlinePPView derniere = periodes.get(periodes.size() - 1);
				derniere.setDateFin(RegDate.get(derniere.getDateDebut().year(), 12, 31));
			}
		}
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
			final List<DelaisAccordablesOnlineDIPP> toAddList = this.periodes.stream()
					.map(DelaisAccordablesOnlinePPView::toEntity)
					.collect(Collectors.toCollection(LinkedList::new));
			final List<DelaisAccordablesOnlineDIPP> toRemoveList = paramsDelais.getPeriodesDelais().stream()
					.filter(AnnulableHelper::nonAnnule)
					.map(DelaisAccordablesOnlineDIPP.class::cast)
					.sorted(DateRangeComparator::compareRanges)
					.collect(Collectors.toCollection(LinkedList::new));
			final List<Pair<DelaisAccordablesOnlineDIPP, DelaisAccordablesOnlineDIPP>> toUpdate = CollectionsUtils.extractCommonElements(toAddList, toRemoveList, DelaisOnlinePPView::idsAreEquals);

			// on annule les périodes qui doivent l'être
			toRemoveList.forEach(p -> p.setAnnule(true));

			// on ajoute les nouvelles périodes
			toAddList.forEach(paramsDelais::addPeriodeDelais);

			// on met-à-jour les périodes existantes
			toUpdate.forEach(pair -> {
				final DelaisAccordablesOnlineDIPP edited = pair.getFirst();
				final DelaisAccordablesOnlineDIPP persisted = pair.getSecond();
				edited.copyTo(persisted);
			});
		}
	}

	protected static boolean idsAreEquals(DelaisAccordablesOnlineDIPP left, DelaisAccordablesOnlineDIPP right) {
		final Long leftId = left.getId();
		final Long rightId = right.getId();
		return leftId != null && leftId.equals(rightId);    // des ids nuls sont toujours considérés comme non-égaux
	}
}
