package ch.vd.uniregctb.foncier;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.registrefoncier.RegistreFoncierService;
import ch.vd.uniregctb.validation.tiers.DateRangeEntityValidator;

public abstract class AllegementFoncierValidator<T extends AllegementFoncier> extends DateRangeEntityValidator<T> {

	private RegistreFoncierService registreFoncierService;

	public void setRegistreFoncierService(RegistreFoncierService registreFoncierService) {
		this.registreFoncierService = registreFoncierService;
	}

	@Override
	protected boolean isDateDebutFutureAllowed() {
		return true;
	}

	@Override
	protected boolean isDateFinFutureAllowed() {
		return true;
	}

	@Override
	protected String getEntityDisplayString(@NotNull T entity) {
		return String.format("sur l'immeuble %s de la commune %s",
		                     StringUtils.defaultIfBlank(registreFoncierService.getNumeroParcelleComplet(entity.getImmeuble(), entity.getDateFin()), "?"),
		                     Optional.ofNullable(registreFoncierService.getCommune(entity.getImmeuble(), entity.getDateFin())).map(Commune::getNomOfficiel).orElse("?"));
	}
}
