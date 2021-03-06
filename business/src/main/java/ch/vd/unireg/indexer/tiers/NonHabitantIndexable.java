package ch.vd.unireg.indexer.tiers;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.avatar.AvatarService;
import ch.vd.unireg.indexer.IndexerException;
import ch.vd.unireg.indexer.IndexerFormatHelper;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.tiers.IdentificationPersonne;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.CategorieIdentifiant;

public class NonHabitantIndexable extends PersonnePhysiqueIndexable {

	public static final String SUB_TYPE = "nonhabitant";

	public NonHabitantIndexable(AdresseService adresseService, TiersService tiersService, AssujettissementService assujettissementService,
	                            ServiceInfrastructureService serviceInfra, AvatarService avatarService, PersonnePhysique nonHabitant) throws IndexerException {
		super(adresseService, tiersService, assujettissementService, serviceInfra, avatarService, nonHabitant);
	}

	@Override
	public String getSubType() {
		return SUB_TYPE;
	}

	@Override
	protected void fillBaseData(TiersIndexableData data) {
		super.fillBaseData(data);

		String ancienNumAVS = null;
		final Set<IdentificationPersonne> ident = tiers.getIdentificationsPersonnes();
		if (ident != null) {
			for (IdentificationPersonne idPersonne : ident) {
				if (idPersonne.getCategorieIdentifiant() == CategorieIdentifiant.CH_AHV_AVS) {
					ancienNumAVS = idPersonne.getIdentifiant();
				}
			}
		}

		data.setNomRaison(tiers.getNom());
		data.addAutresNom(tiers.getPrenomUsuel());
		data.addAutresNom(tiers.getTousPrenoms());
		data.addAutresNom(tiers.getNom());
		data.addDateNaissance(tiers.getDateNaissance());
		data.addSexe(tiers.getSexe());
		data.addNavs13(StringUtils.trimToNull(tiers.getNumeroAssureSocial()));
		data.addNavs11(StringUtils.trimToNull(ancienNumAVS));
		data.addNom1(tiers.getPrenomUsuel());
		data.addNom1(tiers.getNom());
		data.setNavs13_1(tiers.getNumeroAssureSocial());
		data.setDateDeces(IndexerFormatHelper.dateToString(tiers.getDateDeces(), IndexerFormatHelper.DateStringMode.STORAGE));
	}
}
