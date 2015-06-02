package ch.vd.uniregctb.interfaces.model.helper;

import java.util.Date;

import ch.vd.registre.base.date.DateValidityRange;
import ch.vd.registre.base.date.DateValidityRangeImpl;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.interfaces.model.ForPM;
import ch.vd.uniregctb.interfaces.model.TypeNoOfs;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public abstract class EntrepriseHelper {

	/**
	 * La plage de validité des dates en provenance de Reg-PM.
	 * <p/>
	 * Le 1er janvier 1527 est la plus ancienne date valide trouvée dans la base et elle correspond à la date de fondation de la Société des Fusilliers de la Bourgeoisie des Moudon (PM n°19739).
	 */
	private static final DateValidityRange VALIDITY_RANGE = new DateValidityRangeImpl(RegDate.get(1527, 1, 1), RegDate.get(2399, 12, 31));

	/**
	 * Converti une date Java dans une RegDate en tenant compte de la plage de validité des dates en provenance de Reg-PM.
	 *
	 * @param date une date java
	 * @return une date RegDate complète, ou <b>null</b> si la date passée en paramètre n'est pas dans la place de validité.
	 */
	public static RegDate get(Date date) {
		return RegDateHelper.get(date, VALIDITY_RANGE);
	}

	/**
	 * Détermine et retourne le type d'autorité fiscale complet (VD/CH/HS) à partir du type d'autorité fiscal partiel (CH/HS) utilisé pour les fors PMs.
	 *
	 * @param ffp          un for fiscal PM
	 * @param serviceInfra le service d'infrastructure
	 * @return le type d'autorité fiscale complet
	 */
	public static TypeAutoriteFiscale getTypeAutoriteFiscaleForPM(ForPM ffp, ServiceInfrastructureService serviceInfra) {
		final TypeAutoriteFiscale type;
		switch (ffp.getTypeAutoriteFiscale()) {
		case COMMUNE_CH:
			final Commune commune = getCommuneForPM(ffp, serviceInfra);
			if (commune.isVaudoise()) {
				type = TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;
			}
			else {
				type = TypeAutoriteFiscale.COMMUNE_HC;
			}
			break;
		case PAYS_HS:
			type = TypeAutoriteFiscale.PAYS_HS;
			break;
		default:
			throw new IllegalArgumentException("Type d'autorité fiscale PM inconnu = [" + ffp.getTypeAutoriteFiscale() + ']');
		}
		return type;
	}

	/**
	 * Détermine et retourne la commune d'un for fiscal PM.
	 *
	 * @param ffp          un for fiscal PM
	 * @param serviceInfra le service d'infrastructure
	 * @return la commune du for fiscal; ou <b>null</b> si le for fiscal est situé hors-Suisse.
	 */
	public static Commune getCommuneForPM(ForPM ffp, ServiceInfrastructureService serviceInfra) {
		if (ffp.getTypeAutoriteFiscale() == TypeNoOfs.PAYS_HS) {
			return null;
		}

		final Commune commune;
		try {
			commune = serviceInfra.getCommuneByNumeroOfs(ffp.getNoOfsAutoriteFiscale(), ffp.getDateDebut());
		}
		catch (ServiceInfrastructureException e) {
			throw new IndexerException("Commune pas trouvée: noOfs=" + ffp.getNoOfsAutoriteFiscale(), e);
		}

		if (commune == null) {
			throw new IndexerException("Commune pas trouvée: noOfs=" + ffp.getNoOfsAutoriteFiscale());
		}

		return commune;
	}
}
