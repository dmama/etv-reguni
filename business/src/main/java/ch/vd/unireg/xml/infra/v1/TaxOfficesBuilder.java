package ch.vd.unireg.xml.infra.v1;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.xml.infra.taxoffices.v1.TaxOffice;
import ch.vd.unireg.xml.infra.taxoffices.v1.TaxOffices;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.xml.Context;

public abstract class TaxOfficesBuilder {

	@Nullable
	public static TaxOffice newTaxOffice(@Nullable CollectiviteAdministrative ca) {
		if (ca == null) {
			return null;
		}
		final TaxOffice to = new TaxOffice();
		to.setAdmCollNo(ca.getNumeroCollectiviteAdministrative());
		to.setPartyNo(ca.getNumero().intValue());
		return to;
	}

	public static TaxOffices newTaxOffices(CollectiviteAdministrative oid, CollectiviteAdministrative oir) {
		final TaxOffices to = new TaxOffices();
		to.setDistrict(newTaxOffice(oid));
		to.setRegion(newTaxOffice(oir));
		return to;
	}

	/**
	 * @param noOfsCommune le numéro OFS de la commune à analyser
	 * @param dateReference la date de référence du numéro OFS en question
	 * @param context un contexte pour l'accès aux différents services
	 * @return une nouvelle structure {@link TaxOffices} avec les informations qui vont bien
	 * @throws ObjectNotFoundException si la commune est inconnue, n'est pas vaudoise, ou si elle n'est pas associée à un district (ou une région) fiscal(e) dans FiDoR
	 */
	public static TaxOffices newTaxOffices(int noOfsCommune, @Nullable RegDate dateReference, Context context) throws ObjectNotFoundException {
		final Commune commune = context.infraService.getCommuneByNumeroOfs(noOfsCommune, dateReference);
		if (commune == null || !commune.isVaudoise()) {
			throw new ObjectNotFoundException(String.format("Commune %d inconnue dans le canton de Vaud.", noOfsCommune));
		}

		final Integer codeRegion = commune.getCodeRegion();
		final Integer codeDistrict = commune.getCodeDistrict();
		if (codeRegion == null || codeDistrict == null) {
			throw new ObjectNotFoundException("Code(s) région et/ou district inconnu(s) pour la commune.");
		}

		final CollectiviteAdministrative oid = context.tiersDAO.getCollectiviteAdministrativeForDistrict(codeDistrict, false);
		final CollectiviteAdministrative oir = context.tiersDAO.getCollectiviteAdministrativeForRegion(codeRegion);
		return newTaxOffices(oid, oir);
	}
}
