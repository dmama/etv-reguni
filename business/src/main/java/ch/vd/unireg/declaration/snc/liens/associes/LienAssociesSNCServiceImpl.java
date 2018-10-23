package ch.vd.unireg.declaration.snc.liens.associes;

import java.util.List;

import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.TypeRapportEntreTiers;

public class LienAssociesSNCServiceImpl implements LienAssociesSNCService {

	private PlatformTransactionManager transactionManager;
	private HibernateTemplate hibernateTemplate;
	private TiersService tiersService;


	@Override
	public LienAssociesSNCEnMasseImporterResults importLienAssociesSNCEnMasse(List<DonneesLienAssocieEtSNC> rapportEntreTiersSnc, final RegDate dateTraitement, StatusManager statusManager) {
		final ImportLienAssociesSNCEnMasseProcessor processor = new ImportLienAssociesSNCEnMasseProcessor(transactionManager, hibernateTemplate, tiersService, this);
		return processor.run(rapportEntreTiersSnc, dateTraitement, statusManager);
	}

	@Override
	public boolean isAllowed(Contribuable sujet, Contribuable objet, RegDate dateDebut) throws LienAssociesEtSNCException {
		if (!(sujet instanceof PersonnePhysique) && !(sujet instanceof Entreprise)) {
			throw new LienAssociesEtSNCException(LienAssociesEtSNCException.EnumErreurLienAssocieSNC.MAUVAIS_TYPE_ASSOCIE,
			                                     String.format("Le tiers associé  %s n'est pas d'un type acceptable ici %s.", FormatNumeroHelper.numeroCTBToDisplay(sujet.getNumero()), sujet.getClass().getSimpleName()));
		}
		if (!(objet instanceof Entreprise)) {
			throw new LienAssociesEtSNCException(LienAssociesEtSNCException.EnumErreurLienAssocieSNC.MAUVAIS_TYPE_SNC,
			                                     String.format("Le tiers SNC  %s n'est pas d'un type acceptable ici %s.", FormatNumeroHelper.numeroCTBToDisplay(objet.getNumero()), objet.getClass().getSimpleName()));
		}

		if (!((Entreprise) objet).isSNC()) {
			throw new LienAssociesEtSNCException(LienAssociesEtSNCException.EnumErreurLienAssocieSNC.TIERS_PAS_SNC, String.format("Le tiers objet  %s n'est pas une SNC.", FormatNumeroHelper.numeroCTBToDisplay(objet.getNumero())));
		}
		if (tiersService.existRapportEntreTiers(TypeRapportEntreTiers.LIENS_ASSOCIES_ET_SNC, objet, sujet, dateDebut)) {
			throw new LienAssociesEtSNCException(LienAssociesEtSNCException.EnumErreurLienAssocieSNC.CHEVAUCHEMENT_LIEN, "Deux liens entre les même contribuables ne peuvent se chevaucher dans le temps");
		}
		return Boolean.TRUE;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}


	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}
}
