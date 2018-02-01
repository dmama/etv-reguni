package ch.vd.uniregctb.indexer.tiers;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.AdressesFiscales;
import ch.vd.uniregctb.avatar.AvatarService;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.common.RueEtNumero;
import ch.vd.uniregctb.indexer.Indexable;
import ch.vd.uniregctb.indexer.IndexableData;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.IndexerFormatHelper;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.LocalizedDateRange;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;

public abstract class TiersIndexable<T extends Tiers> implements Indexable {

	public static final String TYPE = "tiers";

	protected final T tiers;
	protected final AvatarService avatarService;
	protected final TiersService tiersService;
	protected final AdresseService adresseService;
	protected final ServiceInfrastructureService serviceInfra;

	public TiersIndexable(AdresseService adresseService, TiersService tiersService, ServiceInfrastructureService serviceInfra, AvatarService avatarService, T tiers) throws IndexerException {
		Assert.notNull(tiers);
		Assert.notNull(adresseService);
		this.tiers = tiers;
		this.tiersService = tiersService;
		this.adresseService = adresseService;
		this.serviceInfra = serviceInfra;
		this.avatarService = avatarService;
	}

	public String getType() {
		return TYPE;
	}

	public abstract String getSubType();

	public final IndexableData getIndexableData() {

		final TiersIndexableData data = new TiersIndexableData(tiers.getNumero(), TYPE, getSubType());

		fillBaseData(data);
		fillAdresseData(data);
		fillForsData(data);
		fillAssujettissementData(data);

		return data;
	}

	protected void fillBaseData(TiersIndexableData data) {

		data.setNumeros(IndexerFormatHelper.numberToString(tiers.getNumero()));
		data.setDebiteurInactif(tiers.isDebiteurInactif());
		data.setAnnule(tiers.isDesactive(null));
		data.setRoleLigne1(tiers.getRoleLigne1());
		data.setTypeAvatar(avatarService.getTypeAvatar(tiers));

		final Long millisecondes = DateHelper.getCurrentDate().getTime();
		data.setIndexationDate(millisecondes);
	}

	protected void fillAssujettissementData(TiersIndexableData data) {
		data.setRoleLigne2(tiersService.getRoleAssujettissement(tiers, RegDate.get()));
	}

	protected abstract void fillForsData(TiersIndexableData data);

	private void fillAdresseData(TiersIndexableData data) {

		String rue = "";
		String npaCourrier = null;
		String localite = "";
		String localitePays = "";
		String pays = "";
		Boolean estDansLeCanton = null;
		Integer noOfsCommuneVD = null;

		try {
			final AdressesFiscales adrs = adresseService.getAdressesFiscales(tiers, null, false);
			if (adrs != null && !adrs.isEmpty()) {
				if (adrs.courrier != null) {
					rue = new RueEtNumero(adrs.courrier.getRue(), adrs.courrier.getNumero()).getRueEtNumero();
					npaCourrier = adrs.courrier.getNumeroPostal();
					data.addNpaTous(npaCourrier);
					localite = adrs.courrier.getLocalite(); // [UNIREG-2142] on prend la localité abrégée

					final Integer noOfsPays = adrs.courrier.getNoOfsPays();
					final Pays p = (noOfsPays == null ? null : serviceInfra.getPays(noOfsPays, null));
					if (p == null) {
						pays = "";
						localitePays = localite;
					}
					else {
						pays = p.getNomCourt();
						if (p.isSuisse()) {
							localitePays = localite;
						}
						else {
							localitePays = pays;
						}
					}
				}
				if (adrs.domicile != null && !adrs.domicile.isDefault()) {
					data.addNpaTous(adrs.domicile.getNumeroPostal());
					estDansLeCanton = serviceInfra.estDansLeCanton(adrs.domicile);
					if (estDansLeCanton) {
						final Commune c = serviceInfra.getCommuneByAdresse(adrs.domicile, null);
						if (c != null) {
							noOfsCommuneVD = c.getNoOFS();
						}
					}
				}
				if (adrs.poursuite != null && !adrs.poursuite.isDefault()) {
					data.addNpaTous(adrs.poursuite.getNumeroPostal());
				}
				if (adrs.representation != null && !adrs.representation.isDefault()) {
					data.addNpaTous(adrs.representation.getNumeroPostal());
				}
			}

			if (StringUtils.isBlank(data.getNpaTous())) {
				data.setNpaTous(null);
			}
		}
		catch (Exception e) {
			throw new IndexerException(tiers, e);
		}

		data.setRue(rue);
		data.setNpaCourrier(npaCourrier);
		data.setLocalite(localite);
		data.addLocaliteEtPays(localitePays);
		data.setPays(pays);
		data.setDomicileVd(IndexerFormatHelper.booleanToString(estDansLeCanton));
		data.setNoOfsDomicileVd(IndexerFormatHelper.numberToString(noOfsCommuneVD));
	}

	protected String getLocalisationAsString(LocalizedDateRange localisation, Tiers tiers) throws IndexerException {
		try {
			return tiersService.getLocalisationAsString(localisation);
		}
		catch (ServiceInfrastructureException | ObjectNotFoundException e) {
			throw new IndexerException(tiers, e);
		}
	}

	protected static StringBuilder addValue(StringBuilder s, String value) {
		if (s.length() > 0) {
			s.append(' ');
		}
		return s.append(value);
	}
}
