package ch.vd.unireg.xml.party.v5.strategy;

import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.xml.Context;
import ch.vd.unireg.xml.ExceptionHelper;
import ch.vd.unireg.xml.ServiceException;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionCode;
import ch.vd.unireg.xml.party.adminauth.v5.AdministrativeAuthority;
import ch.vd.unireg.xml.party.v5.InternalPartyPart;

public class AdminAuthStrategy extends TaxPayerStrategy<AdministrativeAuthority> {

	private static final Logger LOGGER = LoggerFactory.getLogger(AdminAuthStrategy.class);

	@Override
	public AdministrativeAuthority newFrom(Tiers right, @Nullable Set<InternalPartyPart> parts, Context context) throws ServiceException {
		final AdministrativeAuthority aa = new AdministrativeAuthority();
		initBase(aa, right, context);
		initParts(aa, right, parts, context);
		return aa;
	}

	@Override
	public AdministrativeAuthority clone(AdministrativeAuthority right, @Nullable Set<InternalPartyPart> parts) {
		final AdministrativeAuthority aa = new AdministrativeAuthority();
		copyBase(aa, right);
		copyParts(aa, right, parts, CopyMode.EXCLUSIVE);
		return aa;
	}

	@Override
	protected void initBase(AdministrativeAuthority to, Tiers from, Context context) throws ServiceException {
		super.initBase(to, from, context);

		final CollectiviteAdministrative coladm = (CollectiviteAdministrative) from;
		final ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative civil = context.infraService.getCollectivite(coladm.getNumeroCollectiviteAdministrative());
		if (civil == null) {
			final String message = String.format("Impossible de trouver la collectivité avec le n°%d", coladm.getNumeroCollectiviteAdministrative());
			LOGGER.error(message);
			throw ExceptionHelper.newBusinessException(message, BusinessExceptionCode.UNKNOWN_INDIVIDUAL);
		}

		to.setName(civil.getNomCourt());
		to.setAdministrativeAuthorityId(coladm.getNumeroCollectiviteAdministrative());
		to.setDistrictTaxOfficeId(coladm.getIdentifiantDistrictFiscal());
		to.setRegionTaxOfficeId(coladm.getIdentifiantRegionFiscale());
	}

	@Override
	protected void copyBase(AdministrativeAuthority to, AdministrativeAuthority from) {
		super.copyBase(to, from);

		to.setName(from.getName());
		to.setAdministrativeAuthorityId(from.getAdministrativeAuthorityId());
		to.setDistrictTaxOfficeId(from.getDistrictTaxOfficeId());
		to.setRegionTaxOfficeId(from.getRegionTaxOfficeId());
	}
}
