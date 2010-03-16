package ch.vd.uniregctb.indexer.tiers;

import java.util.HashMap;

import org.apache.commons.lang.StringUtils;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.tiers.AutreCommunaute;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * @author Mickaël Jackson
 *
 */
public class DebiteurPrestationImposableIndexable extends TiersIndexable {

	public static final String SUB_TYPE = "debiteurprestationimposable";

	private ContribuableIndexable ctbIndexable;

	/**
	 * Permet l'indexation d'un Debiteur Impot Source
	 *
	 * @param debiteurImpotSources
	 * @param entreprise
	 * @throws IndexerException
	 */
	public DebiteurPrestationImposableIndexable(AdresseService adresseService, TiersService tiersService, DebiteurPrestationImposable dpi) throws IndexerException {
		super(adresseService, tiersService, dpi, new DebiteurPrestationImposableSubIndexable(tiersService, dpi));

		Contribuable ctb = dpi.getContribuable();
		if (ctb != null) {
			if (ctb instanceof PersonnePhysique) {
				PersonnePhysique pp = (PersonnePhysique) ctb;
				if (pp.isHabitant()) {
					Individu ind = adresseService.getServiceCivilService().getIndividu(pp.getNumeroIndividu(), DateHelper.getCurrentYear());
					ctbIndexable = new HabitantIndexable(adresseService, tiersService, pp, ind);
				}
				else {
					ctbIndexable = new NonHabitantIndexable(adresseService, tiersService, pp);
				}
			}
			else if (ctb instanceof Entreprise) {
				ctbIndexable = new EntrepriseIndexable(adresseService, tiersService, (Entreprise)ctb);
			}
			else if (ctb instanceof AutreCommunaute) {
				ctbIndexable = new AutreCommunauteIndexable(adresseService, tiersService, (AutreCommunaute)ctb);
			}
			else if (ctb instanceof CollectiviteAdministrative) {
				ctbIndexable = new CollectiviteAdministrativeIndexable(adresseService, tiersService, (CollectiviteAdministrative)ctb);
			}
			else if (ctb instanceof MenageCommun) {
				ctbIndexable = new MenageCommunIndexable(adresseService, tiersService, ((MenageCommun)ctb));
			}
			else {
				Assert.fail("Type de contribuable inconnu = " + ctb.getNatureTiers());
			}
		}
	}

	/**
	 * @see ch.vd.uniregctb.indexer.Indexable#getSubType()
	 */
	public String getSubType() {
		return SUB_TYPE;
	}

	@Override
	public HashMap<String, String> getKeyValues() throws IndexerException {
		HashMap<String, String> values = super.getKeyValues();

		// DPI
		final HashMap<String, String> dpiKeyValues = tiersSubIndexable.getKeyValues();
		// Search
		addValueToMap(values, TiersSearchFields.NUMEROS, dpiKeyValues, TiersSubIndexable.F_NUMERO);
		addValueToMap(values, TiersSearchFields.NOM_RAISON, dpiKeyValues, DebiteurPrestationImposableSubIndexable.F_NOM1);

		// CTB
		if (ctbIndexable != null) {
			final HashMap<String, String> ctbKeyValues = ctbIndexable.getKeyValues();
			// Search
			addValueToMap(values, TiersSearchFields.NOM_RAISON, ctbKeyValues, TiersSearchFields.NOM_RAISON);
			addValueToMap(values, TiersSearchFields.NOM_RAISON, ctbKeyValues, DebiteurPrestationImposableSubIndexable.F_COMPLEMENT_NOM);
			addValueToMap(values, TiersSearchFields.AUTRES_NOM, ctbKeyValues, TiersSearchFields.AUTRES_NOM);
			addValueToMap(values, TiersSearchFields.LOCALITE_PAYS, ctbKeyValues, TiersSearchFields.LOCALITE_PAYS);
			addValueToMap(values, TiersSearchFields.NATURE_JURIDIQUE, ctbKeyValues, TiersSearchFields.NATURE_JURIDIQUE);
			addValueToMap(values, TiersSearchFields.NOM_RAISON, dpiKeyValues, DebiteurPrestationImposableSubIndexable.F_NOM2);

			final String nom1 = dpiKeyValues.get(DebiteurPrestationImposableSubIndexable.F_NOM1);
			if (StringUtils.isEmpty(nom1)) {
				// [UNIREG-1376] on va chercher les infos sur le contribuable si elles n'existent pas sur le débiteur
				addValueToMap(values, TiersIndexedData.NOM1, ctbKeyValues, TiersIndexedData.NOM1);
				addValueToMap(values, TiersIndexedData.NOM2, ctbKeyValues, TiersIndexedData.NOM2);
			}
			else {
				addValueToMap(values, TiersIndexedData.NOM1, dpiKeyValues, DebiteurPrestationImposableSubIndexable.F_NOM1);
				addValueToMap(values, TiersIndexedData.NOM2, dpiKeyValues, DebiteurPrestationImposableSubIndexable.F_NOM2);
			}
		}
		else {
			addValueToMap(values, TiersSearchFields.NOM_RAISON, dpiKeyValues, DebiteurPrestationImposableSubIndexable.F_COMPLEMENT_NOM);
			addValueToMap(values, TiersSearchFields.AUTRES_NOM, dpiKeyValues, DebiteurPrestationImposableSubIndexable.F_NOM2);
			addValueToMap(values, TiersIndexedData.NOM1, dpiKeyValues, DebiteurPrestationImposableSubIndexable.F_NOM1);
			addValueToMap(values, TiersIndexedData.NOM2, dpiKeyValues, DebiteurPrestationImposableSubIndexable.F_NOM2);
		}
		return values;
	}

}
