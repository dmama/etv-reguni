package ch.vd.uniregctb.documentfiscal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.TiersNotFoundException;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.foncier.DemandeDegrevementICI;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.EtatEntreprise;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.TypeEtatDocumentFiscal;
import ch.vd.uniregctb.type.TypeEtatEntreprise;

public class AutreDocumentFiscalManagerImpl implements AutreDocumentFiscalManager, MessageSourceAware {

	private TiersService tiersService;
	private MessageSource messageSource;
	private AutreDocumentFiscalService autreDocumentFiscalService;
	private ServiceInfrastructureService infraService;

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setAutreDocumentFiscalService(AutreDocumentFiscalService autreDocumentFiscalService) {
		this.autreDocumentFiscalService = autreDocumentFiscalService;
	}

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	@Override
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	@Transactional(rollbackFor = Throwable.class)
	@Override
	public ResultatQuittancement quittanceLettreBienvenue(long noCtb, RegDate dateRetour) {
		final Entreprise entreprise;
		try {
			entreprise = getEntreprise(noCtb);
		}
		catch (TiersNotFoundException e) {
			return ResultatQuittancement.entrepriseInexistante();
		}

		final List<LettreBienvenue> lettresBienvenue = entreprise.getAutresDocumentsFiscaux(LettreBienvenue.class, true, false);
		if (lettresBienvenue.isEmpty()) {
			return ResultatQuittancement.rienAQuittancer(TypeAutreDocumentFiscalQuittanceable.LETTRE_BIENVENUE);
		}

		for (LettreBienvenue candidate : CollectionsUtils.revertedOrder(lettresBienvenue)) {
			if (candidate.getEtat() != TypeEtatDocumentFiscal.RETOURNE) {
				candidate.setDateRetour(dateRetour);
				return ResultatQuittancement.ok();
			}
		}
		return ResultatQuittancement.rienAQuittancer(TypeAutreDocumentFiscalQuittanceable.LETTRE_BIENVENUE);
	}

	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@Override
	public List<AutreDocumentFiscalView> getAutresDocumentsFiscauxSansSuivi(long noCtb) {
		final Entreprise entreprise = getEntreprise(noCtb);
		final List<AutreDocumentFiscal> adfs = entreprise.getAutresDocumentsFiscaux(AutreDocumentFiscal.class, true, true);
		if (adfs.isEmpty()) {
			return Collections.emptyList();
		}

		final List<AutreDocumentFiscalView> views = new ArrayList<>(adfs.size());
		for (AutreDocumentFiscal adf : CollectionsUtils.revertedOrder(adfs)) {
			if (!(adf instanceof AutreDocumentFiscalAvecSuivi)) {
				views.add(AutreDocumentFiscalViewFactory.buildView(adf, infraService, messageSource));
			}
		}
		return views;
	}

	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@Override
	public List<AutreDocumentFiscalView> getAutresDocumentsFiscauxAvecSuivi(long noCtb) {
		final Entreprise entreprise = getEntreprise(noCtb);

		final List<AutreDocumentFiscal> adfs = entreprise.getAutresDocumentsFiscaux(AutreDocumentFiscal.class, true, true);
		if (adfs.isEmpty()) {
			return Collections.emptyList();
		}

		final List<AutreDocumentFiscalView> views = new ArrayList<>(adfs.size());
		for (AutreDocumentFiscal adf : CollectionsUtils.revertedOrder(adfs)) {
			if (adf instanceof AutreDocumentFiscalAvecSuivi && !(adf instanceof DemandeDegrevementICI)) {
				views.add(AutreDocumentFiscalViewFactory.buildView(adf, infraService, messageSource));
			}
		}
		return views;
	}

	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@Override
	public boolean hasAnyEtat(long noCtb, TypeEtatEntreprise... types) {
		if (types == null || types.length == 0) {
			return false;
		}
		final Set<TypeEtatEntreprise> typesCherches = EnumSet.noneOf(TypeEtatEntreprise.class);
		typesCherches.addAll(Arrays.asList(types));

		final Entreprise entreprise = getEntreprise(noCtb);
		final List<EtatEntreprise> etats = entreprise.getEtatsNonAnnulesTries();
		for (EtatEntreprise etat : etats) {
			if (typesCherches.contains(etat.getType())) {
				return true;
			}
		}
		return false;
	}

	@NotNull
	private Entreprise getEntreprise(long id) {
		final Tiers tiers = tiersService.getTiers(id);
		if (tiers == null || !(tiers instanceof Entreprise)) {
			throw new TiersNotFoundException(id);
		}
		return (Entreprise) tiers;
	}

	@Transactional(rollbackFor = Throwable.class)
	@Override
	public EditiqueResultat createAndPrint(ImprimerAutreDocumentFiscalView view) throws AutreDocumentFiscalException {
		final Entreprise entreprise = getEntreprise(view.getNoEntreprise());
		switch (view.getTypeDocument()) {
		case AUTORISATION_RADIATION:
			return autreDocumentFiscalService.envoyerAutorisationRadiationRCOnline(entreprise, RegDate.get(), view.getDateReference());
		case DEMANDE_BILAN_FINAL:
			return autreDocumentFiscalService.envoyerDemandeBilanFinalOnline(entreprise, RegDate.get(), view.getPeriodeFiscale(), view.getDateReference());
		case LETTRE_TYPE_INFORMATION_LIQUIDATION:
			return autreDocumentFiscalService.envoyerLettreTypeInformationLiquidationOnline(entreprise, RegDate.get());
		}
		throw new IllegalArgumentException("Type de document non-supporté : " + view.getTypeDocument());
	}
}
