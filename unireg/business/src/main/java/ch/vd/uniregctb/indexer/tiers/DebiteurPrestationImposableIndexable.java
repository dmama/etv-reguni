package ch.vd.uniregctb.indexer.tiers;

import java.util.HashMap;

import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
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

	public DebiteurPrestationImposableIndexable(AdresseService adresseService, TiersService tiersService, ServiceCivilService serviceCivil, ServiceInfrastructureService serviceInfra,
	                                            DebiteurPrestationImposable dpi) throws IndexerException {
		super(adresseService, tiersService, serviceInfra, dpi, new DebiteurPrestationImposableSubIndexable(tiersService, dpi));

		Contribuable ctb = tiersService.getContribuable(dpi);
		if (ctb != null) {
			if (ctb instanceof PersonnePhysique) {
				PersonnePhysique pp = (PersonnePhysique) ctb;
				if (pp.isHabitant()) {
					Individu ind = serviceCivil.getIndividu(pp.getNumeroIndividu(), DateHelper.getCurrentYear());
					ctbIndexable = new HabitantIndexable(adresseService, tiersService, serviceInfra, pp, ind);
				}
				else {
					ctbIndexable = new NonHabitantIndexable(adresseService, tiersService, serviceInfra, pp);
				}
			}
			else if (ctb instanceof Entreprise) {
				ctbIndexable = new EntrepriseIndexable(adresseService, tiersService, serviceInfra, (Entreprise)ctb);
			}
			else if (ctb instanceof AutreCommunaute) {
				ctbIndexable = new AutreCommunauteIndexable(adresseService, tiersService, serviceInfra, (AutreCommunaute)ctb);
			}
			else if (ctb instanceof CollectiviteAdministrative) {
				ctbIndexable = new CollectiviteAdministrativeIndexable(adresseService, tiersService, serviceInfra, (CollectiviteAdministrative)ctb);
			}
			else if (ctb instanceof MenageCommun) {
				ctbIndexable = new MenageCommunIndexable(adresseService, tiersService, serviceCivil, serviceInfra, ((MenageCommun)ctb));
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
		addValueToMap(values, TiersIndexableData.NUMEROS, dpiKeyValues, TiersSubIndexable.F_NUMERO);
		addValueToMap(values, TiersIndexableData.NOM_RAISON, dpiKeyValues, DebiteurPrestationImposableSubIndexable.F_NOM1);
		addValueToMap(values, TiersIndexableData.CATEGORIE_DEBITEUR_IS, dpiKeyValues, DebiteurPrestationImposableSubIndexable.F_CATEGORIE_IS);

		// CTB
		if (ctbIndexable != null) {
			final HashMap<String, String> ctbKeyValues = ctbIndexable.getKeyValues();
			// Search
			addValueToMap(values, TiersIndexableData.NOM_RAISON, ctbKeyValues, TiersIndexableData.NOM_RAISON);
			addValueToMap(values, TiersIndexableData.NOM_RAISON, ctbKeyValues, DebiteurPrestationImposableSubIndexable.F_COMPLEMENT_NOM);
			addValueToMap(values, TiersIndexableData.AUTRES_NOM, ctbKeyValues, TiersIndexableData.AUTRES_NOM);
			addValueToMap(values, TiersIndexableData.LOCALITE_PAYS, ctbKeyValues, TiersIndexableData.LOCALITE_PAYS);
			addValueToMap(values, TiersIndexableData.NATURE_JURIDIQUE, ctbKeyValues, TiersIndexableData.NATURE_JURIDIQUE);
			addValueToMap(values, TiersIndexableData.NOM_RAISON, dpiKeyValues, DebiteurPrestationImposableSubIndexable.F_NOM2);

			final String nom1 = dpiKeyValues.get(DebiteurPrestationImposableSubIndexable.F_NOM1);
			if (StringUtils.isEmpty(nom1)) {
				// [UNIREG-1376] on va chercher les infos sur le contribuable si elles n'existent pas sur le débiteur
				addValueToMap(values, TiersIndexableData.NOM1, ctbKeyValues, TiersIndexableData.NOM1);
				addValueToMap(values, TiersIndexableData.NOM2, ctbKeyValues, TiersIndexableData.NOM2);
			}
			else {
				addValueToMap(values, TiersIndexableData.NOM1, dpiKeyValues, DebiteurPrestationImposableSubIndexable.F_NOM1);
				addValueToMap(values, TiersIndexableData.NOM2, dpiKeyValues, DebiteurPrestationImposableSubIndexable.F_NOM2);
			}
		}
		else {
			addValueToMap(values, TiersIndexableData.NOM_RAISON, dpiKeyValues, DebiteurPrestationImposableSubIndexable.F_COMPLEMENT_NOM);
			addValueToMap(values, TiersIndexableData.AUTRES_NOM, dpiKeyValues, DebiteurPrestationImposableSubIndexable.F_NOM2);
			addValueToMap(values, TiersIndexableData.NOM1, dpiKeyValues, DebiteurPrestationImposableSubIndexable.F_NOM1);
			addValueToMap(values, TiersIndexableData.NOM2, dpiKeyValues, DebiteurPrestationImposableSubIndexable.F_NOM2);
		}
		return values;
	}

}
