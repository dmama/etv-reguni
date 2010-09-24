package ch.vd.uniregctb.indexer.tiers;

import java.util.Set;

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

	public String getSubType() {
		return SUB_TYPE;
	}

	@Override
	protected void fillBaseData(TiersIndexableData data) {
		super.fillBaseData(data);

		final PersonnePhysique pp =(PersonnePhysique) tiers;

		String ancienNumAVS = null;
		final Set<IdentificationPersonne> ident = pp.getIdentificationsPersonnes();
		if (ident != null) {
			for (IdentificationPersonne idPersonne : ident) {
				if (idPersonne.getCategorieIdentifiant().equals(CategorieIdentifiant.CH_AHV_AVS)) {
					ancienNumAVS = idPersonne.getIdentifiant();
				}
			}
		}

		data.setNumeros(IndexerFormatHelper.objectToString(tiers.getNumero()));
		data.setNomRaison(pp.getNom());
		data.addAutresNom(pp.getPrenom());
		data.addAutresNom(pp.getNom());
		data.setDateNaissance(IndexerFormatHelper.objectToString(pp.getDateNaissance()));
		data.addNumeroAssureSocial(pp.getNumeroAssureSocial());
		data.addNumeroAssureSocial(ancienNumAVS);
		data.addNom1(pp.getPrenom());
		data.addNom1(pp.getNom());
		data.setDateDeces(IndexerFormatHelper.objectToString(pp.getDateDeces()));
	}
}
