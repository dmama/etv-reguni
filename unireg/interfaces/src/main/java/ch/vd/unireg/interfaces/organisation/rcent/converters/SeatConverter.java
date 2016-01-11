package ch.vd.unireg.interfaces.organisation.rcent.converters;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationException;
import ch.vd.unireg.interfaces.organisation.data.Domicile;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class SeatConverter extends RangedToRangeBaseConverter<Integer, Domicile> {

	private final ServiceInfrastructureRaw infraService;

	public SeatConverter(ServiceInfrastructureRaw infraService) {
		this.infraService = infraService;
	}

	@NotNull
	@Override
	protected Domicile convert(@NotNull DateRangeHelper.Ranged<Integer> range) {
		final List<Commune> communes = infraService.getCommuneHistoByNumeroOfs(range.getPayload());
		if (communes == null || communes.isEmpty()) {
			throw new ServiceOrganisationException("Commune inconnue : OFS " + range.getPayload());
		}
		final TypeAutoriteFiscale typeAutoriteFiscale = communes.get(0).isVaudoise() ? TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD : TypeAutoriteFiscale.COMMUNE_HC;
		return new Domicile(range.getDateDebut(),
		                    range.getDateFin(),
		                    typeAutoriteFiscale,
		                    range.getPayload());
	}

}
