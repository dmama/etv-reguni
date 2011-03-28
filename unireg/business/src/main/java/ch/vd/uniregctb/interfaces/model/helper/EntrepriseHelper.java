package ch.vd.uniregctb.interfaces.model.helper;

import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.ForPM;
import ch.vd.uniregctb.interfaces.model.TypeNoOfs;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public abstract class EntrepriseHelper {

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
			throw new IllegalArgumentException("Type d'autorité fiscale PM inconnu = [" + ffp.getTypeAutoriteFiscale() + "]");
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
			commune = serviceInfra.getCommuneByNumeroOfsEtendu(ffp.getNoOfsAutoriteFiscale(), ffp.getDateDebut());
		}
		catch (ServiceInfrastructureException e) {
			throw new IndexerException("Commune pas trouvée: noOfsEtendu=" + ffp.getNoOfsAutoriteFiscale(), e);
		}

		if (commune == null) {
			throw new IndexerException("Commune pas trouvée: noOfsEtendu=" + ffp.getNoOfsAutoriteFiscale());
		}

		return commune;
	}
}
