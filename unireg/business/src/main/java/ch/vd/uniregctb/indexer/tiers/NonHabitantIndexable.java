package ch.vd.uniregctb.indexer.tiers;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.IndexerFormatHelper;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.IdentificationPersonne;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.CategorieIdentifiant;

public class NonHabitantIndexable extends PersonnePhysiqueIndexable {

	public static final String SUB_TYPE = "nonhabitant";

	public NonHabitantIndexable(AdresseService adresseService, TiersService tiersService, ServiceInfrastructureService serviceInfra, PersonnePhysique nonHabitant) throws IndexerException {
		super(adresseService, tiersService, serviceInfra, nonHabitant);
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
		data.addAutresNom(tiers.getPrenom());
		data.addAutresNom(tiers.getNom());
		data.addDateNaissance(tiers.getDateNaissance());
		data.addSexe(tiers.getSexe());
		data.addNavs13(StringUtils.trimToNull(tiers.getNumeroAssureSocial()));
		data.addNavs11(StringUtils.trimToNull(ancienNumAVS));
		data.addNom1(tiers.getPrenom());
		data.addNom1(tiers.getNom());
		data.setNavs13_1(tiers.getNumeroAssureSocial());
		data.setDateDeces(IndexerFormatHelper.dateToString(tiers.getDateDeces(), IndexerFormatHelper.DateStringMode.STORAGE));
	}
}
