package ch.vd.uniregctb.indexer.tiers;

import org.springframework.util.Assert;

import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.IndexerFormatHelper;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * @author Sean Paul
 *
 */
public class HabitantIndexable extends PersonnePhysiqueIndexable {

	public static final String SUB_TYPE = "habitant";

	private final Individu individu;

	public HabitantIndexable(AdresseService adresseService, TiersService tiersService, ServiceInfrastructureService serviceInfra, PersonnePhysique hab, Individu individu) throws IndexerException {
		super(adresseService, tiersService, serviceInfra, hab);
		Assert.notNull(individu);
		this.individu = individu;
	}

	@Override
	public String getSubType() {
		return SUB_TYPE;
	}

	@Override
	protected void fillBaseData(TiersIndexableData data) {
		super.fillBaseData(data);

		final PersonnePhysique pp =(PersonnePhysique) tiers;
		data.addAutresNom(individu.getPrenom());
		data.addAutresNom(individu.getNom());
		data.addAutresNom(individu.getNomNaissance());
		data.setDateNaissance(IndexerFormatHelper.objectToString(individu.getDateNaissance()));
		data.setNomRaison(individu.getNom());
		data.addNumeroAssureSocial(individu.getNouveauNoAVS());
		data.addNumeroAssureSocial(individu.getNoAVS11());
		data.setNoSymic(individu.getNumeroRCE());
		data.addNom1(individu.getPrenom());
		data.addNom1(individu.getNom());

		if (pp.getDateDeces() != null) { //surcharge de la date de décès
			data.setDateDeces(IndexerFormatHelper.objectToString(pp.getDateDeces()));
		}
		else {
			data.setDateDeces(IndexerFormatHelper.objectToString(individu.getDateDeces()));
		}
	}
}
