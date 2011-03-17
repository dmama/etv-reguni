package ch.vd.uniregctb.indexer.tiers;

import org.apache.log4j.Logger;
import org.springframework.util.Assert;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.indexer.IndexableData;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.IndexerFormatHelper;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public abstract class TiersIndexable {

	private Logger LOGGER = Logger.getLogger(TiersIndexable.class);

	public static final String TYPE = "tiers";

	protected final Tiers tiers;
	protected final TiersService tiersService;
	protected final AdresseService adresseService;
	protected final ServiceInfrastructureService serviceInfra;

	public TiersIndexable(AdresseService adresseService, TiersService tiersService, ServiceInfrastructureService serviceInfra, Tiers tiers) throws IndexerException {
		Assert.notNull(tiers);
		Assert.notNull(adresseService);
		this.tiers = tiers;
		this.tiersService = tiersService;
		this.adresseService = adresseService;
		this.serviceInfra = serviceInfra;
	}

	public String getType() {
		return TYPE;
	}

	public abstract String getSubType();

	public IndexableData getIndexableData() {

		TiersIndexableData data = new TiersIndexableData(tiers.getNumero(), TYPE, getSubType());

		fillBaseData(data);
		fillAdresseData(data);
		fillForsData(data);

		return data;
	}

	protected void fillBaseData(TiersIndexableData data) {

		data.setNumeros(IndexerFormatHelper.objectToString(tiers.getNumero()));
		data.setDebiteurInactif(IndexerFormatHelper.objectToString(tiers.isDebiteurInactif()));
		data.setAnnule(IndexerFormatHelper.objectToString(tiers.isDesactive(null)));
		data.setRoleLigne1(tiers.getRoleLigne1());
		data.setRoleLigne2(tiersService.getRoleAssujettissement(tiers, RegDate.get()));

		final Long millisecondes = DateHelper.getCurrentDate().getTime();
		data.setIndexationDate(IndexerFormatHelper.objectToString(millisecondes));
	}

	protected abstract void fillForsData(TiersIndexableData data);

	private void fillAdresseData(TiersIndexableData data) {

		String rue = "";
		String npa = "";
		String localite = "";
		String localitePays = "";
		String pays = "";
		Boolean estDansLeCanton = null;
		Integer noOfsCommuneVD = null;

		try {
			// Défaut => adresse courrier
			AdresseGenerique courrier = adresseService.getAdresseFiscale(tiers, TypeAdresseFiscale.COURRIER, null, false);
			if (courrier != null) {
				rue = courrier.getRue();
				npa = courrier.getNumeroPostal();
				localite = courrier.getLocalite(); // [UNIREG-2142] on prend la localité abrégée

				final Integer noOfsPays = courrier.getNoOfsPays();
				final Pays p = (noOfsPays == null ? null : serviceInfra.getPays(noOfsPays));
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
			final AdresseGenerique domicile = adresseService.getAdresseFiscale(tiers, TypeAdresseFiscale.DOMICILE, null, false);
			// msi/tdq 3.6.09 : on ne doit pas tenir compte des adresses de domicile par défaut car elles n'ont pas de valeur pour
			// déterminer si un contribuable est dans le canton
			if (domicile != null && !domicile.isDefault()) {
				estDansLeCanton = serviceInfra.estDansLeCanton(domicile);
				if (estDansLeCanton) {
					final Commune c = serviceInfra.getCommuneByAdresse(domicile, null);
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

		data.setRue(rue);
		data.setNpa(npa);
		data.setLocalite(localite);
		data.addLocaliteEtPays(localitePays);
		data.setPays(pays);
		data.setDomicileVd(IndexerFormatHelper.objectToString(estDansLeCanton));
		data.setNoOfsDomicileVd(IndexerFormatHelper.objectToString(noOfsCommuneVD));
	}

	protected String getForCommuneAsString(ForFiscal forF) throws IndexerException {

		String forStr= "";

		try {
			TypeAutoriteFiscale typeForFiscal = forF.getTypeAutoriteFiscale();

			// Commune vaudoise
			if (typeForFiscal == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
					Commune com = serviceInfra.getCommuneByNumeroOfsEtendu(forF.getNumeroOfsAutoriteFiscale(), forF.getDateFin());
					if (com == null) {
						throw new IndexerException("Commune pas trouvée: noOfsEtendu=" + forF.getNumeroOfsAutoriteFiscale());
					}
					forStr = com.getNomMinuscule();
			}
			// Commune suisse
			else if (typeForFiscal == TypeAutoriteFiscale.COMMUNE_HC) {
					Commune com = serviceInfra.getCommuneByNumeroOfsEtendu(forF.getNumeroOfsAutoriteFiscale(), forF.getDateFin());
					if (com == null) {
						throw new IndexerException("Commune pas trouvée: noOfs=" + forF.getNumeroOfsAutoriteFiscale());
					}
					forStr = com.getNomMinuscule();
			}
			// Pays
			else if (typeForFiscal == TypeAutoriteFiscale.PAYS_HS) {
					Pays p = serviceInfra.getPays(forF.getNumeroOfsAutoriteFiscale());
					if (p == null) {
						throw new IndexerException("Pays pas trouvé: noOfs=" + forF.getNumeroOfsAutoriteFiscale());
					}
					forStr = p.getNomMinuscule();
			}
			else {
				ch.vd.registre.base.utils.Assert.fail("Le Type du For doit toujours etre présent");
			}
		}
		catch (InfrastructureException e) {
			throw new IndexerException(forF.getTiers(), e);
		}

		return forStr;
	}

	protected static StringBuilder addValue(StringBuilder s, String value) {
		if (s.length() > 0) {
			s.append(" ");
		}
		return s.append(value);
	}
}
