package ch.vd.uniregctb.indexer.tiers;

import ch.vd.uniregctb.indexer.IndexerFormatHelper;
import ch.vd.uniregctb.interfaces.model.HistoriqueIndividu;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import org.springframework.util.Assert;

import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * @author Sean Paul
 *
 */
public class HabitantIndexable extends PersonnePhysiqueIndexable {

	public static final String SUB_TYPE = "habitant";

	private Individu individu;

	public HabitantIndexable(AdresseService adresseService, TiersService tiersService, ServiceInfrastructureService serviceInfra, PersonnePhysique hab, Individu individu) throws IndexerException {
		super(adresseService, tiersService, serviceInfra, hab);
		Assert.notNull(individu);
		this.individu = individu;
	}

	public String getSubType() {
		return SUB_TYPE;
	}

	@Override
	protected void fillBaseData(TiersIndexableData data) {
		super.fillBaseData(data);

		final PersonnePhysique pp =(PersonnePhysique) tiers;
		final HistoriqueIndividu histo = individu.getDernierHistoriqueIndividu();

		data.setNumeros(IndexerFormatHelper.objectToString(tiers.getNumero()));
		data.addAutresNom(histo.getPrenom());
		data.addAutresNom(histo.getNom());
		data.addAutresNom(histo.getNomNaissance());
		data.setDateNaissance(IndexerFormatHelper.objectToString(individu.getDateNaissance()));
		data.setNomRaison(histo.getNom());
		data.addNumeroAssureSocial(individu.getNouveauNoAVS());
		data.addNumeroAssureSocial(histo.getNoAVS());
		data.setNoSymic(individu.getNumeroRCE());
		data.addNom1(histo.getPrenom());
		data.addNom1(histo.getNom());

		if (pp.getDateDeces() != null) { //surcharge de la date de décès
			data.setDateDeces(IndexerFormatHelper.objectToString(pp.getDateDeces()));
		}
		else {
			data.setDateDeces(IndexerFormatHelper.objectToString(individu.getDateDeces()));
		}
	}
}
