package ch.vd.unireg.declaration.snc.liens.associes;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.message.MessageHelper;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.TypeRapportEntreTiers;

public class LienAssociesSNCServiceImpl implements LienAssociesSNCService {

	private PlatformTransactionManager transactionManager;
	private HibernateTemplate hibernateTemplate;
	private TiersService tiersService;
	private MessageHelper messageHelper;


	@Override
	public LienAssociesSNCEnMasseImporterResults importLienAssociesSNCEnMasse(List<DonneesLienAssocieEtSNC> rapportEntreTiersSnc, final RegDate dateTraitement, StatusManager statusManager) {
		final ImportLienAssociesSNCEnMasseProcessor processor = new ImportLienAssociesSNCEnMasseProcessor(transactionManager, hibernateTemplate, tiersService, this);
		return processor.run(rapportEntreTiersSnc, dateTraitement, statusManager);
	}

	@Override
	public boolean isAllowed(@NotNull Tiers sujet, @NotNull Tiers objet, RegDate dateDebut) throws LienAssociesEtSNCException {
		//FISCPROJ-920: Empêcher tout autres type de tiers de créer des liens Associé/commanditaire.
		if (!(sujet instanceof Contribuable)) {
			throw new LienAssociesEtSNCException(LienAssociesEtSNCException.EnumErreurLienAssocieSNC.MAUVAIS_TYPE_ASSOCIE,
			                                     messageHelper.getMessage("error.mauvais_type_associe." + sujet.getClass().getSimpleName(), FormatNumeroHelper.numeroCTBToDisplay(sujet.getNumero())));
		}

		if (!(objet instanceof Contribuable)) {
			throw new LienAssociesEtSNCException(LienAssociesEtSNCException.EnumErreurLienAssocieSNC.MAUVAIS_TYPE_SNC,
			                                     messageHelper.getMessage("error.mauvais_type_snc." + sujet.getClass().getSimpleName(), FormatNumeroHelper.numeroCTBToDisplay(sujet.getNumero())));
		}

		if (!(sujet instanceof PersonnePhysique) && !(sujet instanceof Entreprise)) {
			throw new LienAssociesEtSNCException(LienAssociesEtSNCException.EnumErreurLienAssocieSNC.MAUVAIS_TYPE_ASSOCIE,
			                                     messageHelper.getMessage("error.mauvais_type_associe." + sujet.getClass().getSimpleName(), FormatNumeroHelper.numeroCTBToDisplay(sujet.getNumero())));
		}
		if (!(objet instanceof Entreprise)) {
			throw new LienAssociesEtSNCException(LienAssociesEtSNCException.EnumErreurLienAssocieSNC.MAUVAIS_TYPE_SNC,
			                                     messageHelper.getMessage("error.mauvais_type_snc." + sujet.getClass().getSimpleName(), FormatNumeroHelper.numeroCTBToDisplay(sujet.getNumero())));
		}

		if (!((Entreprise) objet).isSNC()) {
			throw new LienAssociesEtSNCException(LienAssociesEtSNCException.EnumErreurLienAssocieSNC.TIERS_PAS_SNC, messageHelper.getMessage("error.tiers_pas_snc", FormatNumeroHelper.numeroCTBToDisplay(sujet.getNumero())));
		}
		if (tiersService.existRapportEntreTiers(TypeRapportEntreTiers.LIENS_ASSOCIES_ET_SNC, (Contribuable) objet, (Contribuable) sujet, dateDebut)) {
			throw new LienAssociesEtSNCException(LienAssociesEtSNCException.EnumErreurLienAssocieSNC.CHEVAUCHEMENT_LIEN, messageHelper.getMessage("error.chevauchement_lien"));
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

	public void setMessageHelper(MessageHelper messageHelper) {
		this.messageHelper = messageHelper;
	}
}
