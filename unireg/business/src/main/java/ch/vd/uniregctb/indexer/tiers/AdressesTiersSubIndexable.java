package ch.vd.uniregctb.indexer.tiers;


import org.apache.log4j.Logger;
import org.springframework.util.Assert;

import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.indexer.AbstractSubIndexable;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.TypeAdresseTiers;

public class AdressesTiersSubIndexable extends AbstractSubIndexable {

	protected final Logger LOGGER = Logger.getLogger(AdressesTiersSubIndexable.class);

	public static final String SUISSE = "Suisse";

	public static final String F_RUE = "ADR_RUE";
	public static final String F_NPA = "ADR_NPA";
	public static final String F_LOCALITE = "ADR_LOCALITE";
	public static final String F_LOCALITE_PAYS = "ADR_LOCALITE_PAYS";
	public static final String F_PAYS = "ADR_PAYS";
	public static final String F_DOMICILE_VD = "DOMICILE_VD";
	public static final String F_NO_OFS_DOMICILE_VD = "NO_OFS_DOMICILE_VD";

	private final AdresseService adresseService;
	private final ServiceInfrastructureService infraService;
	private final Tiers tiers;

	public AdressesTiersSubIndexable(AdresseService adresseService, Tiers tiers) {

		Assert.notNull(adresseService);
		this.adresseService = adresseService;
		this.infraService = adresseService.getServiceInfra();

		Assert.notNull(tiers);
		this.tiers = tiers;
	}

	@Override
	protected void fillKeyValues(IndexMap map) throws IndexerException {
		String rue = "";
		String npa = "";
		String localite = "";
		String localitePays = "";
		String pays = "";
		Boolean estDansLeCanton = null;
		Integer noOfsCommuneVD = null;

		try {
			// Défaut => adresse courrier
			AdresseGenerique courrier = adresseService.getAdresseFiscale(tiers, TypeAdresseTiers.COURRIER, null);
			if (courrier != null) {
				rue = courrier.getRue();
				npa = courrier.getNumeroPostal();
				localite = courrier.getLocalite();

				final Integer noOfsPays = courrier.getNoOfsPays();
				final Pays p = (noOfsPays == null ? null : adresseService.getServiceInfra().getPays(noOfsPays));
				if (p == null) {
					pays = "";
					localitePays = localite;
				}
				else {
					pays = p.getNomMinuscule();
					if (p.isSuisse()) {
						localitePays = localite;
					}
					else {
						localitePays = pays;
					}
				}
			}
		}
		catch (Exception e) {
			throw new IndexerException(tiers, e);
		}

		try {
			AdresseGenerique domicile = adresseService.getAdresseFiscale(tiers, TypeAdresseTiers.DOMICILE, null);
			// msi/tdq 3.6.09 : on ne doit pas tenir compte des adresses de domicile par défaut car elles n'ont pas de valeur pour
			// déterminer si un contribuable est dans le canton
			if (domicile != null && !domicile.isDefault()) {
				estDansLeCanton = infraService.estDansLeCanton(domicile);
				if (estDansLeCanton) {
					Commune c = infraService.getCommuneByAdresse(domicile);
					if (c != null) {
						noOfsCommuneVD = c.getNoOFSEtendu();
					}
				}
			}
		}
		catch (Exception e) {
			LOGGER.warn("L'adresse de domicile du tiers n°" + tiers.getNumero() + " ne peut être indexée à cause de l'erreur suivante: "
					+ e.getMessage());
			// il y a beaucoup de tiers qui pètent des exceptions sur l'adresse domicile -> on stocke null dans l'indexeur pour l'instant
			//throw new IndexerException(e);
		}

		map.putRawValue(F_RUE, rue);
		map.putRawValue(F_NPA, npa);
		map.putRawValue(F_LOCALITE, localite);
		map.putRawValue(F_LOCALITE_PAYS, localitePays);
		map.putRawValue(F_PAYS, pays);
		map.putRawValue(F_DOMICILE_VD, estDansLeCanton);
		map.putRawValue(F_NO_OFS_DOMICILE_VD, noOfsCommuneVD);
	}
}
