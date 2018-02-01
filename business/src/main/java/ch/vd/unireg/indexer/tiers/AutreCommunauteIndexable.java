package ch.vd.unireg.indexer.tiers;

import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.avatar.AvatarService;
import ch.vd.unireg.indexer.IndexerException;
import ch.vd.unireg.indexer.IndexerFormatHelper;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.tiers.AutreCommunaute;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.NatureJuridique;

/**
 * @author Sean Paul
 *
 */
public class AutreCommunauteIndexable extends ContribuableImpositionPersonnesMoralesIndexable<AutreCommunaute> {

	public static final String SUB_TYPE = "autrecommunaute";

	public AutreCommunauteIndexable(AdresseService adresseService, TiersService tiersService, AssujettissementService assujettissementService,
	                                ServiceInfrastructureService serviceInfra, AvatarService avatarService, AutreCommunaute autreCommunaute) throws IndexerException {
		super(adresseService, tiersService, assujettissementService, serviceInfra, avatarService, autreCommunaute);
	}

	@Override
	public String getSubType() {
		return SUB_TYPE;
	}

	@Override
	protected void fillBaseData(TiersIndexableData data) {
		super.fillBaseData(data);

		data.addNomRaison(tiers.getNom());
		data.addNomRaison(tiers.getComplementNom());
		data.setNom1(tiers.getNom());
		data.setNom2(tiers.getComplementNom());
		data.setNatureJuridique(IndexerFormatHelper.enumToString(NatureJuridique.PM));
	}
}
