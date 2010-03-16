package ch.vd.uniregctb.migreg;

import java.util.ArrayList;
import java.util.Set;

import org.springframework.jdbc.support.rowset.SqlRowSet;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.adresse.AdresseEtrangere;
import ch.vd.uniregctb.adresse.AdresseSuisse;
import ch.vd.uniregctb.adresse.AdresseSupplementaire;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.Tiers;

public class AdressesLoader extends SubElementsFetcher {

	private static final String NO_CONTRIBUABLE = "FK_CONTRIBUABLENO";
	private static final String ADR_PAYS_NO_OFS = "NO_OFS";
	private static final String ADR_NO_RUE = "NO_RUE";
	private static final String ADR_NO_ORDRE_P = "NO_ORDRE_P";
	private static final String ADR_CHEZ = "CHEZ";
	private static final String ADR_RUE = "RUE";
	private static final String ADR_NO_POLICE = "NO_POLICE";
	private static final String ADR_LIEU = "LIEU";

	public AdressesLoader(HostMigratorHelper helper, StatusManager mgr) {
		super(helper, mgr);
	}

	public ArrayList<MigrationError> loadAdressesListCtb(ArrayList<Tiers> listTiers, ArrayList<MigrationError> errors) {

		SqlRowSet srs = readAdressesListCtb(listTiers);
		while (srs != null && srs.next() && !mgr.interrupted()) {
			for (Tiers tiers : listTiers) {
				if (tiers.getNumero().equals(srs.getLong(NO_CONTRIBUABLE))) {

					Set<AdresseTiers> adressesTiers = tiers.getAdressesTiers();
					AdresseSupplementaire adrCtb = null;
					if (tiers.getAdressesTiers() == null || tiers.getAdressesTiers().size() == 0) {
						//listTiers.remove(tiers);
						break;
					}
					adrCtb = (AdresseSupplementaire) adressesTiers.iterator().next();
					AdresseSupplementaire adr = null;
					if (srs.getInt(ADR_PAYS_NO_OFS) == 0 || srs.getInt(ADR_PAYS_NO_OFS) == ServiceInfrastructureService.noOfsSuisse) {
						AdresseSuisse adrS = new AdresseSuisse();
						adr = adrS;
						if (srs.getInt(ADR_NO_RUE) != 0) {
							adrS.setNumeroRue(srs.getInt(ADR_NO_RUE));
						} else {
							adrS.setNumeroOrdrePoste(srs.getInt(ADR_NO_ORDRE_P));
						}
					}
					else {
						AdresseEtrangere adrE = new AdresseEtrangere();
						adr = adrE;
						adrE.setNumeroOfsPays(srs.getInt(ADR_PAYS_NO_OFS));
						adrE.setNumeroPostalLocalite(srs.getString(ADR_LIEU));
					}
					adr.setComplement(srs.getString(ADR_CHEZ));
					adr.setRue(srs.getString(ADR_RUE));
					adr.setNumeroMaison(srs.getString(ADR_NO_POLICE));
					//On ne migre réellement l'adresse du contribuable si le code IROLE du contribuable = N (not Permanente)
					//ou si l'adresse du contribuable est différente de l'adresse courrier en cours de l'individu
					if (!adr.isPermanente()) {
						//Si l'adresse du contribuable ne valide pas, on ne migre pas l'adresse. Unireg se servira de l'adresse de l'individu.
						ValidationResults results = adrCtb.validate();
						if ((results != null && results.getErrors().size()>0) ||
							isAdrCtbEqualAdrInd(adrCtb, adr)) {
							adressesTiers.remove(adrCtb);
							helper.adresseTiersDAO.remove(adrCtb.getId());
						}
					}
					break;
				}
			}
		}

		return errors;
	}

	private boolean isAdrCtbEqualAdrInd(AdresseSupplementaire adrCtb, AdresseSupplementaire adrInd) {

//		Adresse suisse ou adresse étrangère
		if (adrCtb instanceof AdresseEtrangere) {
			if (adrInd instanceof AdresseSuisse) {
				return false;
			}
			AdresseEtrangere adrE = (AdresseEtrangere) adrInd;
			AdresseEtrangere adrECtb = (AdresseEtrangere) adrCtb;
			if (!adrECtb.getNumeroOfsPays().equals(adrE.getNumeroOfsPays())) {
				return false;
			}
			if (!adrECtb.getNumeroPostalLocalite().trim().toLowerCase().equals(adrE.getNumeroPostalLocalite().trim().toLowerCase())) {
				return false;
			}
			if (!adrECtb.getRue().trim().toLowerCase().equals(adrE.getRue().trim().toLowerCase())) {
				return false;
			}
			if (!adrECtb.getComplement().trim().toLowerCase().equals(adrE.getComplement().trim().toLowerCase())) {
				return false;
			}
			if (!adrECtb.getNumeroMaison().trim().toLowerCase().equals(adrE.getNumeroMaison().trim().toLowerCase())) {
				return false;
			}
			return true;
		}
		if (adrInd instanceof AdresseEtrangere) {
			return false;
		}
		AdresseSuisse adrS = (AdresseSuisse) adrInd;
		AdresseSuisse adrSCtb = (AdresseSuisse) adrCtb;
		if (adrSCtb.getNumeroRue() != null && adrSCtb.getNumeroRue() > 0) {
			if (adrS.getNumeroRue() == null || !adrS.getNumeroRue().equals(adrSCtb.getNumeroRue())) {
				return false;
			}
		}
		if (adrSCtb.getNumeroOrdrePoste() != null && adrSCtb.getNumeroOrdrePoste() > 0) {
			if (adrS.getNumeroOrdrePoste() == null || !adrS.getNumeroOrdrePoste().equals(adrSCtb.getNumeroOrdrePoste())) {
				return false;
			}
		}
		if (!adrSCtb.getRue().trim().toLowerCase().equals(adrS.getRue().trim().toLowerCase())) {
			return false;
		}
		if (!adrSCtb.getComplement().trim().toLowerCase().equals(adrS.getComplement().trim().toLowerCase())) {
			return false;
		}
		if (!adrSCtb.getNumeroMaison().trim().toLowerCase().equals(adrS.getNumeroMaison().trim().toLowerCase())) {
			return false;
		}

		return true;
	}

	private SqlRowSet readAdressesListCtb(ArrayList<Tiers> lstTiers) {
		StringBuilder sbCtb = new StringBuilder();
		for (Tiers tiers : lstTiers) {
			sbCtb.append(tiers.getNumero());
			sbCtb.append(",");
		}
		if (sbCtb.length() == 0) {
			return null;
		}
		sbCtb.deleteCharAt(sbCtb.lastIndexOf(","));

		String query =
			"SELECT " +
			"A.TY_ADRESSE, " +
			"A.CHEZ, " +
			"A.RUE, " +
			"A.NO_POLICE, " +
			"A.LIEU, " +
			"B.NO_ORDRE_P, " +
			"B.NO_POSTAL_ACHEMIN, " +
			"B.CHIFFRE_COMPLEMENT, " +
			"B.DESIGN_ABREGEE_MIN, " +
			"C.NO_RUE, " +
			"C.DESIGN_COURRIER, " +
			"D.NO_OFS, " +
			"D.NOM_OFS_MIN, " +
			"R.FK_CONTRIBUABLENO " +
			"FROM "+helper.getTableDb2("REGROUPEMENT_IND")+" R, " +
			helper.getTableDb2("ADR_INDIVIDU")+" A " +
			"LEFT JOIN "+helper.getTableDb2("LOCALITE_POSTALE")+" B ON A.FK_LOC_POSTNO = B.NO_ORDRE_P " +
			"LEFT JOIN "+helper.getTableDb2("RUE")+" C ON A.FK_RUENO = C.NO_RUE " +
			"LEFT JOIN "+helper.getTableDb2("PAYS")+" D ON A.FK_PAYSNO_OFS = D.NO_OFS " +
			"WHERE R.FK_CONTRIBUABLENO IN ("+ sbCtb.toString()+") " +
			"AND R.FK_INDNO = A.FK_INDNO " +
			"AND A.DA_ANNULATION = '0001-01-01' " +
			"AND A.DA_VALIDITE <= CURRENT DATE  " +
			"AND (A.DAF_VALIDITE = '0001-01-01' OR A.DAF_VALIDITE >= CURRENT DATE) " +
			"ORDER BY R.FK_CONTRIBUABLENO, A.TY_ADRESSE";

		HostMigratorHelper.SQL_LOG.debug("Query: "+query);
		return helper.db2Template.queryForRowSet(query);

	}

}
