package ch.vd.uniregctb.documentfiscal;

import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.editique.EditiqueCompositionService;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.ForsParTypeAt;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.CategorieEntreprise;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeLettreBienvenue;

/**
 * Implémentation du service de gestion des "autres documents fiscaux"
 */
public class AutreDocumentFiscalServiceImpl implements AutreDocumentFiscalService {

	private ParametreAppService parametreAppService;
	private HibernateTemplate hibernateTemplate;
	private PlatformTransactionManager transactionManager;
	private TiersService tiersService;
	private AssujettissementService assujettissementService;
	private DelaisService delaiService;
	private EditiqueCompositionService editiqueCompositionService;
	private EvenementFiscalService evenementFiscalService;

	public void setParametreAppService(ParametreAppService parametreAppService) {
		this.parametreAppService = parametreAppService;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setAssujettissementService(AssujettissementService assujettissementService) {
		this.assujettissementService = assujettissementService;
	}

	public void setDelaiService(DelaisService delaiService) {
		this.delaiService = delaiService;
	}

	public void setEditiqueCompositionService(EditiqueCompositionService editiqueCompositionService) {
		this.editiqueCompositionService = editiqueCompositionService;
	}

	public void setEvenementFiscalService(EvenementFiscalService evenementFiscalService) {
		this.evenementFiscalService = evenementFiscalService;
	}

	@Override
	public EnvoiLettresBienvenueResults envoyerLettresBienvenueEnMasse(RegDate dateTraitement, int delaiCarence, StatusManager statusManager) {
		final EnvoiLettresBienvenueProcessor processor = new EnvoiLettresBienvenueProcessor(parametreAppService, hibernateTemplate, transactionManager, tiersService, assujettissementService, this);
		return processor.run(dateTraitement, delaiCarence, statusManager);
	}

	@Override
	public LettreBienvenue envoyerLettreBienvenueBatch(Entreprise entreprise, RegDate dateTraitement) throws AutreDocumentFiscalException {
		final RegDate dateEnvoi = delaiService.getDateFinDelaiCadevImpressionLettreBienvenue(dateTraitement);
		final RegDate delaiRetour = dateEnvoi.addDays(parametreAppService.getDelaiRetourLettreBienvenue());
		final TypeLettreBienvenue typeLettre = computeTypeLettreBienvenue(entreprise, dateTraitement);

		final LettreBienvenue lettre = new LettreBienvenue();
		lettre.setDateEnvoi(dateEnvoi);
		lettre.setDelaiRetour(delaiRetour);
		lettre.setType(typeLettre);
		lettre.setEntreprise(entreprise);

		final LettreBienvenue saved = hibernateTemplate.merge(lettre);
		try {
			editiqueCompositionService.imprimeLettreBienvenueForBatch(saved, dateTraitement);
			evenementFiscalService.publierEvenementFiscalEmissionLettreBienvenue(saved);
		}
		catch (EditiqueException e) {
			throw new AutreDocumentFiscalException(e);
		}
		return lettre;
	}

	private TypeLettreBienvenue computeTypeLettreBienvenue(Entreprise e, RegDate dateTraitement) throws AutreDocumentFiscalException {

		// if faut tout d'abord regarder le for principal à la date de traitement
		final ForsParTypeAt fors = e.getForsParTypeAt(dateTraitement, false);
		if (fors.principal == null) {
			throw new AutreDocumentFiscalException("Pas de for principal actif à la date de traitement, impossible de déterminer le type d'autorité fiscale du siège fiscal de l'entreprise.");
		}
		final TypeAutoriteFiscale taf = fors.principal.getTypeAutoriteFiscale();

		final TypeLettreBienvenue type;
		if (taf == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
			if (tiersService.isInscriteRC(e, dateTraitement)) {
				type = TypeLettreBienvenue.VD_RC;
			}
			else {
				final CategorieEntreprise categorie = tiersService.getCategorieEntreprise(e, dateTraitement);
				if (categorie == CategorieEntreprise.APM) {
					type = TypeLettreBienvenue.APM_VD_NON_RC;
				}
				else {
					throw new AutreDocumentFiscalException("Entreprise non-APM avec siège vaudois mais non-inscrite au RC.");
				}
			}
		}
		else {
			// regardons les fors secondaires et, surtout leur motif de rattachement
			if (fors.secondaires.isEmpty()) {
				throw new AutreDocumentFiscalException("Pas de for secondaire actif à la date de traitement sur une entreprise avec siège " + taf);
			}

			// quels motifs de rattachement a-t-on trouvés ?
			boolean trouveImmeuble = false;
			boolean trouveEtablissement = false;
			for (ForFiscalSecondaire ffs : fors.secondaires) {
				switch (ffs.getMotifRattachement()) {
				case IMMEUBLE_PRIVE:
					trouveImmeuble = true;
					break;
				case ETABLISSEMENT_STABLE:
					trouveEtablissement = true;
					break;
				default:
					break;
				}
				if (trouveEtablissement && trouveImmeuble) {
					// pas la peine de chercher plus loin, on a déjà trouvé tout ce qu'on cherchait
					break;
				}
			}

			// c'est le cas immeuble qui gagne
			if (trouveImmeuble) {
				type = TypeLettreBienvenue.HS_HC_IMMEUBLE;
			}
			else if (trouveEtablissement) {
				type = TypeLettreBienvenue.HS_HC_ETABLISSEMENT;
			}
			else {
				throw new AutreDocumentFiscalException("Ni for secondaire immeuble, ni for secondaire établissement trouvé, à la date de traitement, sur une entreprise avec siège " + taf);
			}
		}

		return type;
	}
}
