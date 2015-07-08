package ch.vd.uniregctb.indexer.tiers;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.avatar.AvatarService;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.TiersService;

public class CollectiviteAdministrativeIndexable extends ContribuableIndexable<CollectiviteAdministrative> {

//	private static final Logger LOGGER = LoggerFactory.getLogger(CollectiviteAdministrativeIndexable.class);

	public static final String SUB_TYPE = "collectiviteadministrative";

	public CollectiviteAdministrativeIndexable(AdresseService adresseService, TiersService tiersService,
	                                           ServiceInfrastructureService serviceInfra, AvatarService avatarService, CollectiviteAdministrative collectivite) throws IndexerException {
		super(adresseService, tiersService, serviceInfra, avatarService, collectivite);
	}

	@Override
	public String getSubType() {
		return SUB_TYPE;
	}

	@Override
	protected void fillBaseData(TiersIndexableData data) {
		super.fillBaseData(data);

		final long noColAdm = tiers.getNumeroCollectiviteAdministrative();
		final ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative collectiviteCivile;
		collectiviteCivile = serviceInfra.getCollectivite((int) noColAdm);
		if (collectiviteCivile == null) {
			throw new IllegalArgumentException("Impossible de récupérer la collectivité administrative avec le numéro " + noColAdm);
		}

		final String nom = buildNom(collectiviteCivile);
		data.setNom1(nom);
		data.setNomRaison(nom);
	}

	private static String buildNom(ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative collectivite) {
		final List<String> noms = new ArrayList<>(3);
		if (StringUtils.isNotBlank(collectivite.getNomComplet1())) {
			noms.add(StringUtils.trim(collectivite.getNomComplet1()));
		}
		if (StringUtils.isNotBlank(collectivite.getNomComplet2())) {
			noms.add(StringUtils.trim(collectivite.getNomComplet2()));
		}
		if (StringUtils.isNotBlank(collectivite.getNomComplet3())) {
			noms.add(StringUtils.trim(collectivite.getNomComplet3()));
		}

		final StringBuilder b = new StringBuilder();
		for (String part : noms) {
			if (b.length() > 0) {
				b.append(" ");
			}
			b.append(part);
		}
		return b.toString();
	}
}
