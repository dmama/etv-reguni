package ch.vd.uniregctb.indexer.tiers;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.indexer.AbstractSubIndexable;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.HistoriqueIndividu;
import ch.vd.uniregctb.interfaces.model.Individu;

public class IndividuSubIndexable extends AbstractSubIndexable {

	//private Logger LOGGER = Logger.getLogger(IndividuSubIndexable.class);

	// EntiteCivile
	public final static String F_NO_INDIVIDU = "NO_INDIVIDU";
	public final static String F_DATE_NAISSANCE = "DATE_NAISSANCE";
	public final static String F_DATE_DECES = "DATE_DECES";
	public final static String F_NOM = "NOM";
	public final static String F_PRENOM = "PRENOM";
	public final static String F_ANCIEN_NUMERO_AVS = "ANCIEN_NUMERO_AVS";
	public final static String F_ETAT_CIVIL = "ETAT_CIVIL";
	public final static String F_NO_ASSURE_SOCIAL = "NO_ASSURE_SOCIAL";
	public final static String F_NO_NUMERO_RCE = "NO_NUMERO_RCE";
	public final static String F_NOM_NAISSANCE = "NOM_NAISSANCE";
	public final static String F_COMMUNE_ORIGINE = "NO_ORIGINE";
	public final static String F_NO_SYMIC = "NO_SYMIC";

	private final Individu individu;
	private final HistoriqueIndividu indHisto;

	public IndividuSubIndexable(Individu ind) {
		individu = ind;
		Assert.notNull(ind);
		indHisto = ind.getDernierHistoriqueIndividu();
		Assert.notNull(indHisto);
	}

	@Override
	protected void fillKeyValues(IndexMap map) throws IndexerException {

		map.putRawValue(F_NO_INDIVIDU, individu.getNoTechnique());
		map.putRawValue(F_DATE_NAISSANCE, individu.getDateNaissance());
		map.putRawValue(F_DATE_DECES, individu.getDateDeces());
		map.putRawValue(F_NO_ASSURE_SOCIAL, individu.getNouveauNoAVS());
		map.putRawValue(F_NO_SYMIC, individu.getNumeroRCE());
		map.putRawValue(F_NOM, indHisto.getNom());
		map.putRawValue(F_PRENOM, indHisto.getPrenom());
		map.putRawValue(F_NOM_NAISSANCE, indHisto.getNomNaissance());
		map.putRawValue(F_ANCIEN_NUMERO_AVS, indHisto.getNoAVS());

		// Recherche l'Etat civil courant
		if (individu.getEtatsCivils() != null) {

			EtatCivil courant = null;
			RegDate lastDate = null;

			for (Object o : individu.getEtatsCivils()) {
				EtatCivil ec = (EtatCivil) o;
				if (lastDate == null
						||
						(ec.getDateDebutValidite() != null && ec.getDateDebutValidite().isAfter(lastDate))
						) {
					courant = ec;
					lastDate = ec.getDateDebutValidite();
				}
			}

			if (courant != null && courant.getTypeEtatCivil() != null) {
				map.putRawValue(F_ETAT_CIVIL, courant.getTypeEtatCivil().toString());
			}
		}
	}

}
