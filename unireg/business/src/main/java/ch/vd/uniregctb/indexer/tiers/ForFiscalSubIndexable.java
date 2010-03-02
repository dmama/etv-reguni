package ch.vd.uniregctb.indexer.tiers;

import java.util.Set;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.indexer.AbstractSubIndexable;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.IndexerFormatHelper;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.ForDebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class ForFiscalSubIndexable extends AbstractSubIndexable {

	//private Logger LOGGER = Logger.getLogger(ForFiscalSubIndexable.class);

	public static final String F_FOR_PRINCIPAL_ACTIF = "FOR_PRINCIPAL_ACTIF";
	public static final String F_NO_OFS_FOR_PRINCIPAL_ACTIF = "NO_OFS_FOR_PRINCIPAL_ACTIF";
	public static final String F_TYPE_OFS_FOR_PRINCIPAL_ACTIF = "TYPE_OFS_FOR_PRINCIPAL_ACTIF";
	public static final String F_DERNIER_FOR_PRINCIPAL = "DERNIER_FOR_PRINCIPAL";
	public static final String F_NO_OFS_DERNIER_FOR_PRINCIPAL = "NO_OFS_DERNIER_FOR_PRINCIPAL";
	public static final String F_DATE_OUVERTURE_DERNIER_FOR = "DATE_OUVERTURE_DERNIER_FOR";
	public static final String F_DATE_FERMETURE_DERNIER_FOR = "DATE_FERMETURE_DERNIER_FOR";

	public static final String F_AUTRES_FORS = "AUTRES_FORS";
	public static final String F_NOS_OFS_AUTRES_FORS = "NOS_OFS_AUTRES_FORS";

	private final ServiceInfrastructureService infraService;
	private final Tiers tiers;

	public ForFiscalSubIndexable(ServiceInfrastructureService infraService, Tiers tiers) {

		Assert.notNull(infraService);
		this.infraService = infraService;

		Assert.notNull(tiers);
		this.tiers = tiers;
	}

	@Override
	protected void fillKeyValues(IndexMap map) throws IndexerException {

		// For principal actif
		final ForFiscalPrincipal principalActif = tiers.getForFiscalPrincipalAt(null);
		if (principalActif != null) {
			map.putRawValue(F_FOR_PRINCIPAL_ACTIF, getForCommuneAsString(principalActif));
			map.putRawValue(F_TYPE_OFS_FOR_PRINCIPAL_ACTIF, principalActif.getTypeAutoriteFiscale().toString());
			map.putRawValue(F_NO_OFS_FOR_PRINCIPAL_ACTIF, principalActif.getNumeroOfsAutoriteFiscale().toString());
		}
		final ForDebiteurPrestationImposable forDebiteur = tiers.getForDebiteurPrestationImposableAt(null);
		if (forDebiteur != null) {
			map.putRawValue(F_FOR_PRINCIPAL_ACTIF, getForCommuneAsString(forDebiteur));
			map.putRawValue(F_TYPE_OFS_FOR_PRINCIPAL_ACTIF, forDebiteur.getTypeAutoriteFiscale().toString());
			map.putRawValue(F_NO_OFS_FOR_PRINCIPAL_ACTIF, forDebiteur.getNumeroOfsAutoriteFiscale().toString());
		}

		// For principal
		final ForFiscalPrincipal dernierPrincipal = tiers.getDernierForFiscalPrincipal();
		if (dernierPrincipal != null) {
			map.putRawValue(F_DERNIER_FOR_PRINCIPAL, getForCommuneAsString(dernierPrincipal));
			map.putRawValue(F_NO_OFS_DERNIER_FOR_PRINCIPAL, dernierPrincipal.getNumeroOfsAutoriteFiscale().toString());

			map.putRawValue(F_DATE_OUVERTURE_DERNIER_FOR, IndexerFormatHelper.objectToString(dernierPrincipal.getDateDebut()));
			map.putRawValue(F_DATE_FERMETURE_DERNIER_FOR, IndexerFormatHelper.objectToString(dernierPrincipal.getDateFin()));
		}
		final ForDebiteurPrestationImposable dernierDebiteur = tiers.getDernierForDebiteur();
		if (dernierDebiteur != null) {
			map.putRawValue(F_DERNIER_FOR_PRINCIPAL, getForCommuneAsString(dernierDebiteur));
			map.putRawValue(F_NO_OFS_DERNIER_FOR_PRINCIPAL, dernierDebiteur.getNumeroOfsAutoriteFiscale().toString());

			map.putRawValue(F_DATE_OUVERTURE_DERNIER_FOR, IndexerFormatHelper.objectToString(dernierDebiteur.getDateDebut()));
			map.putRawValue(F_DATE_FERMETURE_DERNIER_FOR, IndexerFormatHelper.objectToString(dernierDebiteur.getDateFin()));
		}

		// Autre fors
		final Set<ForFiscal> fors = tiers.getForsFiscaux();
		if (fors != null) {

			StringBuilder autresFors = new StringBuilder();
			StringBuilder noOfsAutresFors = new StringBuilder();
			for (ForFiscal forF : fors) {
				addValue(autresFors, getForCommuneAsString(forF));
				addValue(noOfsAutresFors, forF.getNumeroOfsAutoriteFiscale().toString());
			}

			map.putRawValue(F_AUTRES_FORS, autresFors.toString());
			map.putRawValue(F_NOS_OFS_AUTRES_FORS, noOfsAutresFors.toString());
		}
	}

	private static StringBuilder addValue(StringBuilder s, String value) {
		if (s.length() > 0) {
			s.append(" ");
		}
		return s.append(value);
	}

	private String getForCommuneAsString(ForFiscal forF) throws IndexerException {

		String forStr= "";

		try {
			TypeAutoriteFiscale typeForFiscal = forF.getTypeAutoriteFiscale();

			// Commune vaudoise
			if (typeForFiscal == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
					Commune com = infraService.getCommuneByNumeroOfsEtendu(forF.getNumeroOfsAutoriteFiscale(), forF.getDateFin());
					if (com == null) {
						throw new IndexerException("Commune pas trouvée: noOfsEtendu=" + forF.getNumeroOfsAutoriteFiscale());
					}
					forStr = com.getNomMinuscule();
			}
			// Commune suisse
			else if (typeForFiscal == TypeAutoriteFiscale.COMMUNE_HC) {
					Commune com = infraService.getCommuneByNumeroOfsEtendu(forF.getNumeroOfsAutoriteFiscale(), forF.getDateFin());
					if (com == null) {
						throw new IndexerException("Commune pas trouvée: noOfs=" + forF.getNumeroOfsAutoriteFiscale());
					}
					forStr = com.getNomMinuscule();
			}
			// Pays
			else if (typeForFiscal == TypeAutoriteFiscale.PAYS_HS) {
					Pays p = infraService.getPays(forF.getNumeroOfsAutoriteFiscale());
					if (p == null) {
						throw new IndexerException("Pays pas trouvé: noOfs=" + forF.getNumeroOfsAutoriteFiscale());
					}
					forStr = p.getNomMinuscule();
			}
			else {
				Assert.fail("Le Type du For doit toujours etre présent");
			}
		}
		catch (InfrastructureException e) {
			throw new IndexerException(forF.getTiers(), e);
		}

		return forStr;
	}
}
