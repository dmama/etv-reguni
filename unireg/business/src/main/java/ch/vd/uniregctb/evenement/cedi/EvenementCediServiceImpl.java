package ch.vd.uniregctb.evenement.cedi;

import java.util.List;

import org.apache.commons.lang.StringUtils;

import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.ModeleDocumentDAO;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;
import ch.vd.uniregctb.iban.IbanHelper;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.TypeDocument;

public class EvenementCediServiceImpl implements EvenementCediService, EvenementCediHandler {

	private TiersDAO tiersDAO;
	private PeriodeFiscaleDAO periodeFiscaleDAO;
	private ModeleDocumentDAO modeleDocumentDAO;

	public void onEvent(EvenementCedi event) throws EvenementCediException {

		if (event instanceof RetourDI) {
			onRetourDI((RetourDI) event);
		}
		else {
			throw new IllegalArgumentException("Type d'événement inconnu = " + event.getClass());
		}
	}

	protected void onRetourDI(RetourDI scan) throws EvenementCediException {

		// On récupère le contribuable correspondant
		final long ctbId = scan.getNoContribuable();
		final Contribuable ctb = (Contribuable) tiersDAO.get(ctbId);
		if (ctb == null) {
			throw new EvenementCediException("Le contribuable n°" + ctbId + " n'existe pas.");
		}

		final ValidationResults results = ctb.validate();
		if (results.hasErrors()) {
			throw new EvenementCediException("Le contribuable n°" + ctbId + " ne valide pas (" + results.toString() + ").");
		}

		// On s'assure que l'on est bien cohérent avec les données en base
		if (ctb.isDebiteurInactif()) {
			throw new EvenementCediException("Le contribuable n°" + ctbId + " est un débiteur inactif, il n'aurait pas dû recevoir de déclaration d'impôt.");
		}

		final int annee = scan.getPeriodeFiscale();
		final List<Declaration> declarations = ctb.getDeclarationForPeriode(annee);
		if (declarations == null || declarations.isEmpty()) {
			throw new EvenementCediException("Le contribuable n°" + ctbId + " ne possède pas de déclaration pour la période fiscale " + annee + ".");
		}

		final int noSequenceDI = scan.getNoSequenceDI();
		final DeclarationImpotOrdinaire declaration = findDeclaration(noSequenceDI, declarations);
		if (declaration == null) {
			throw new EvenementCediException("Le contribuable n°" + ctbId + " ne possède pas de déclaration pour la période fiscale " + annee + " avec le numéro de séquence " + noSequenceDI + ".");
		}

		// On met-à-jour les informations personnelles
		updateInformationsPersonnelles(ctb, scan);

		// On met-à-jour le type de déclaration
		updateTypeDocument(declaration, scan);
	}

	/**
	 * Met-à-jour le type de la déclaration du contribuable (manuelle, vaudtax) par rapport au format de la déclaration d'impôt réellement retournée.
	 *
	 * @param declaration la déclaration dont on veut mettre-à-jour le type de document
	 * @param scan        les informations renseignées dans le déclaration
	 */
	private void updateTypeDocument(DeclarationImpotOrdinaire declaration, RetourDI scan) {

		final int annee = scan.getPeriodeFiscale();
		final PeriodeFiscale periode = periodeFiscaleDAO.getPeriodeFiscaleByYear(annee);
		final ModeleDocument vaudTax = modeleDocumentDAO.getModelePourDeclarationImpotOrdinaire(periode, TypeDocument.DECLARATION_IMPOT_VAUDTAX);
		final ModeleDocument complete = modeleDocumentDAO.getModelePourDeclarationImpotOrdinaire(periode, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH);
		Assert.notNull(vaudTax);
		Assert.notNull(complete);

		final RetourDI.TypeDocument typeDocumentScanne = scan.getTypeDocument();
		if (typeDocumentScanne != null) {
			final TypeDocument typeDocument = declaration.getModeleDocument().getTypeDocument();
			switch (typeDocumentScanne) {
			case VAUDTAX:
				if (typeDocument == TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL || typeDocument == TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH) {
					declaration.setModeleDocument(vaudTax);
				}
				break;
			case ORDINAIRE:
				if (typeDocument == TypeDocument.DECLARATION_IMPOT_VAUDTAX) {
					declaration.setModeleDocument(complete);
				}
				break;
			default:
				throw new IllegalArgumentException("Type de document inconnu = [" + typeDocumentScanne + "]");
			}
		}
	}

	/**
	 * Met-à-jour les informations personnelles du contribuable à partir des informations renseignées dans la déclaration retournée.
	 *
	 * @param ctb  le contribuable à mettre-à-jour
	 * @param scan les informations renseignées dans le déclaration
	 */
	private void updateInformationsPersonnelles(Contribuable ctb, RetourDI scan) {

		if (StringUtils.isNotBlank(scan.getTitulaireCompte())) {
			ctb.setTitulaireCompteBancaire(LengthConstants.streamlineField(scan.getTitulaireCompte(), LengthConstants.TIERS_PERSONNE, true));
		}

		if (StringUtils.isNotBlank(scan.getIban())) {
			ctb.setNumeroCompteBancaire(LengthConstants.streamlineField(IbanHelper.removeSpaceAndDoUpperCase(scan.getIban()), LengthConstants.TIERS_NUMCOMPTE, false));
		}

		if (StringUtils.isNotBlank(scan.getNoTelephone())) {
			ctb.setNumeroTelephonePrive(LengthConstants.streamlineField(scan.getNoTelephone(), LengthConstants.TIERS_NUMTEL, true));
		}

		if (StringUtils.isNotBlank(scan.getNoMobile())) {
			ctb.setNumeroTelephonePortable(LengthConstants.streamlineField(scan.getNoMobile(), LengthConstants.TIERS_NUMTEL, true));
		}

		if (StringUtils.isNotBlank(scan.getEmail())) {
			ctb.setAdresseCourrierElectronique(LengthConstants.streamlineField(scan.getEmail(), LengthConstants.TIERS_EMAIL, true));
		}
	}

	/**
	 * Recherche la declaration pour une année et un numéro de déclaration dans l'année
	 *
	 * @param noSequenceDI le numéro de séquence de la déclaration
	 * @param declarations les déclaration de la période considérée
	 * @return la déclaration correspondante
	 */
	public static DeclarationImpotOrdinaire findDeclaration(int noSequenceDI, List<Declaration> declarations) {

		DeclarationImpotOrdinaire declaration = null;

		if (declarations != null && !declarations.isEmpty()) {
			for (Declaration d : declarations) {
				DeclarationImpotOrdinaire di = (DeclarationImpotOrdinaire) d;
				if (noSequenceDI != 0) {
					if (di.getNumero() == noSequenceDI) {
						declaration = di;
						break;
					}
				}
				// Dans le cas ou le numero dans l'année n'est pas spécifié on prend la première DI trouvée sur la période
				else {
					declaration = di;
				}
			}
		}

		return declaration;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setPeriodeFiscaleDAO(PeriodeFiscaleDAO periodeFiscaleDAO) {
		this.periodeFiscaleDAO = periodeFiscaleDAO;
	}

	public void setModeleDocumentDAO(ModeleDocumentDAO modeleDocumentDAO) {
		this.modeleDocumentDAO = modeleDocumentDAO;
	}
}
