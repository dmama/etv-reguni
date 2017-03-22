package ch.vd.uniregctb.registrefoncier;

import java.util.Optional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.data.Commune;

public class ResumeImmeubleView {

	private final long idImmeuble;
	private final String noParcelleComplet;
	private final Long estimationFiscale;
	private final String nature;
	private final Integer ofsCommune;
	private final String nomCommune;

	public ResumeImmeubleView(ImmeubleRF immeuble, RegDate dateReference, RegistreFoncierService service) {
		this.idImmeuble = immeuble.getId();
		this.noParcelleComplet = service.getNumeroParcelleComplet(immeuble, dateReference);
		this.estimationFiscale = service.getEstimationFiscale(immeuble, dateReference);
		this.nature = ImmeubleHelper.getNatureImmeuble(immeuble, dateReference, Integer.MAX_VALUE);

		final Optional<Commune> commune = Optional.ofNullable(service.getCommune(immeuble, dateReference));
		ofsCommune = commune.map(Commune::getNoOFS).orElse(null);
		nomCommune = commune.map(Commune::getNomOfficiel).orElse(null);
	}

	public long getIdImmeuble() {
		return idImmeuble;
	}

	public String getNoParcelleComplet() {
		return noParcelleComplet;
	}

	public Long getEstimationFiscale() {
		return estimationFiscale;
	}

	public String getNature() {
		return nature;
	}

	public Integer getOfsCommune() {
		return ofsCommune;
	}

	public String getNomCommune() {
		return nomCommune;
	}
}
