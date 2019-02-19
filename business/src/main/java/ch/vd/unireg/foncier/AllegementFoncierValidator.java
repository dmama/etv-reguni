package ch.vd.unireg.foncier;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.registrefoncier.ImmeubleRF;
import ch.vd.unireg.registrefoncier.RegistreFoncierService;
import ch.vd.unireg.validation.tiers.DateRangeEntityValidator;

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
	@NotNull
	public ValidationResults validate(T entity) {
		final ValidationResults vr = super.validate(entity);
		if (entity.getImmeuble() == null) {
			vr.addError("immeuble", "L'immeuble est une donnée obligatoire.");
		}
		return vr;
	}

	@Override
	protected String getEntityDisplayString(@NotNull T entity) {
		final Optional<ImmeubleRF> immeuble = Optional.ofNullable(entity.getImmeuble());
		return String.format("sur l'immeuble %s de la commune %s",
		                     immeuble.map(i -> registreFoncierService.getNumeroParcelleComplet(i, entity.getDateFin()))
				                     .filter(StringUtils::isNotBlank)
				                     .orElse("?"),
		                     immeuble.map(i -> registreFoncierService.getCommune(i, entity.getDateFin()))
				                     .map(Commune::getNomOfficiel)
				                     .orElse("?"));
	}
}
