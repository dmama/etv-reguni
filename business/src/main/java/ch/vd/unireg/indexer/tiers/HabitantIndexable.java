package ch.vd.unireg.indexer.tiers;

import org.apache.commons.lang3.StringUtils;

import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.avatar.AvatarService;
import ch.vd.unireg.indexer.IndexerException;
import ch.vd.unireg.indexer.IndexerFormatHelper;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersService;

/**
 * @author Sean Paul
 *
 */
public class HabitantIndexable extends PersonnePhysiqueIndexable {

	public static final String SUB_TYPE = "habitant";

	private final Individu individu;

	public HabitantIndexable(AdresseService adresseService, TiersService tiersService, AssujettissementService assujettissementService,
	                         ServiceInfrastructureService serviceInfra, AvatarService avatarService, PersonnePhysique hab, Individu individu) throws IndexerException {
		super(adresseService, tiersService, assujettissementService, serviceInfra, avatarService, hab);
		if (individu == null) {
			throw new IllegalArgumentException();
		}
		this.individu = individu;
	}

	@Override
	public String getSubType() {
		return SUB_TYPE;
	}

	@Override
	protected void fillBaseData(TiersIndexableData data) {
		super.fillBaseData(data);

		data.addAutresNom(individu.getPrenomUsuel());
		data.addAutresNom(individu.getTousPrenoms());
		data.addAutresNom(individu.getNom());
		data.addAutresNom(individu.getNomNaissance());
		data.addDateNaissance(individu.getDateNaissance());
		data.addSexe(individu.getSexe());
		data.setNomRaison(individu.getNom());
		data.addNavs13(StringUtils.trimToNull(individu.getNouveauNoAVS()));
		data.addNavs11(StringUtils.trimToNull(individu.getNoAVS11()));
		data.setNoSymic(individu.getNumeroRCE());
		data.addNom1(individu.getPrenomUsuel());
		data.addNom1(individu.getNom());
		data.setNavs13_1(individu.getNouveauNoAVS());

		if (tiers.getDateDeces() != null) { //surcharge de la date de décès
			data.setDateDeces(IndexerFormatHelper.dateToString(tiers.getDateDeces(), IndexerFormatHelper.DateStringMode.STORAGE));
		}
		else {
			data.setDateDeces(IndexerFormatHelper.dateToString(individu.getDateDeces(), IndexerFormatHelper.DateStringMode.STORAGE));
		}
	}
}
