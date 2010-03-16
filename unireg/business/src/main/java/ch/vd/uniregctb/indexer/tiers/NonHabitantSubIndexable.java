package ch.vd.uniregctb.indexer.tiers;

import java.util.Iterator;

import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.tiers.IdentificationPersonne;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.CategorieIdentifiant;

public class NonHabitantSubIndexable extends PersonnePhysiqueSubIndexable {

	public final static String F_ID = "ID";
	public final static String F_NOM = "NOM";
	public final static String F_PRENOM = "PRENOM";
	public final static String F_DATE_NAISSANCE = "DATE_NAISSANCE";
	public final static String F_DATE_DECES = "DATE_DECES";
	public final static String F_ANCIEN_NUMERO_AVS = "ANCIEN_NUMERO_AVS";
	public final static String F_NO_ASSURE_SOCIAL = "NO_ASSURE_SOCIAL";
	public final static String F_ADRESSE_EMAIL = "ADRESSE_EMAIL";

	private final PersonnePhysique nonHab;

	public NonHabitantSubIndexable(TiersService tiersService, PersonnePhysique nonHab) throws IndexerException {
		super(tiersService, nonHab);
		this.nonHab = nonHab;
	}

	@Override
	protected void fillKeyValues(IndexMap map) throws IndexerException {
		super.fillKeyValues(map);
		map.putRawValue(F_ID, nonHab.getId());
		map.putRawValue(F_NOM, nonHab.getNom());
		map.putRawValue(F_PRENOM, nonHab.getPrenom());
		map.putRawValue(F_DATE_NAISSANCE, nonHab.getDateNaissance());
		map.putRawValue(F_DATE_DECES, nonHab.getDateDeces());
		map.putRawValue(F_NO_ASSURE_SOCIAL, nonHab.getNumeroAssureSocial());

		String ancienNumAVS = null;
		if (nonHab.getIdentificationsPersonnes() != null) {
			Iterator<IdentificationPersonne> iterator = nonHab.getIdentificationsPersonnes().iterator();
			while (iterator.hasNext()) {
				IdentificationPersonne idPersonne = iterator.next();
				if (idPersonne.getCategorieIdentifiant().equals(CategorieIdentifiant.CH_AHV_AVS)) {
					ancienNumAVS = idPersonne.getIdentifiant();
				}
			}
		}
		if (ancienNumAVS != null) {
			map.putRawValue(F_ANCIEN_NUMERO_AVS, ancienNumAVS);
		}
		map.putRawValue(F_ADRESSE_EMAIL, nonHab.getAdresseCourrierElectronique());
	}
}
