package ch.vd.unireg.tiers.view;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ch.vd.registre.base.date.RegDate;

public class ImmeubleView {

	private final long id;
	private final RegDate dateDebutDroit;
	private final RegDate dateFinDroit;
	private final int noParcelle;
	private final Integer index1;
	private final Integer index2;
	private final Integer index3;
	private final Long estimationFiscale;
	private final String referenceEstimationFiscale;
	private final String nature;

	public ImmeubleView(long idImmeuble, RegDate dateDebutDroit, RegDate dateFinDroit, int noParcelle, Integer index1, Integer index2, Integer index3, Long estimationFiscale, String referenceEstimationFiscale, String nature) {
		this.id = idImmeuble;
		this.dateDebutDroit = dateDebutDroit;
		this.dateFinDroit = dateFinDroit;
		this.noParcelle = noParcelle;
		this.index1 = index1;
		this.index2 = index2;
		this.index3 = index3;
		this.estimationFiscale = estimationFiscale;
		this.referenceEstimationFiscale = referenceEstimationFiscale;
		this.nature = nature;
	}

	public long getId() {
		return id;
	}

	public RegDate getDateDebutDroit() {
		return dateDebutDroit;
	}

	public RegDate getDateFinDroit() {
		return dateFinDroit;
	}

	public int getNoParcelle() {
		return noParcelle;
	}

	public Integer getIndex1() {
		return index1;
	}

	public Integer getIndex2() {
		return index2;
	}

	public Integer getIndex3() {
		return index3;
	}

	public Long getEstimationFiscale() {
		return estimationFiscale;
	}

	public String getReferenceEstimationFiscale() {
		return referenceEstimationFiscale;
	}

	public String getNature() {
		return nature;
	}

	public String getNoParcelleComplet() {
		return Stream.of(noParcelle, index1, index2, index3)
				.filter(Objects::nonNull)
				.map(String::valueOf)
				.collect(Collectors.joining("-"));
	}
}
