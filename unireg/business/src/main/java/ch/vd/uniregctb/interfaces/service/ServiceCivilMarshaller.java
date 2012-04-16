package ch.vd.uniregctb.interfaces.service;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.HistoriqueCommune;
import ch.vd.uniregctb.common.DonneesCivilesException;
import ch.vd.uniregctb.common.NomPrenom;
import ch.vd.uniregctb.interfaces.model.AdressesCivilesActives;
import ch.vd.uniregctb.interfaces.model.AdressesCivilesHistoriques;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.Origine;
import ch.vd.uniregctb.interfaces.model.Permis;
import ch.vd.uniregctb.interfaces.model.Tutelle;
import ch.vd.uniregctb.utils.UniregModeHelper;

/**
 * Service civil qui permet de choisir l'implémentation RegPP ou RcPers à utiliser
 */
public class ServiceCivilMarshaller implements ServiceCivilService, InitializingBean {

	private static final Logger LOGGER = Logger.getLogger(ServiceCivilMarshaller.class);

	private UniregModeHelper modeHelper;
	private ServiceCivilService target;
	private ServiceCivilService regpp;
	private ServiceCivilService rcpers;

	@SuppressWarnings("UnusedDeclaration")
	public void setModeHelper(UniregModeHelper modeHelper) {
		this.modeHelper = modeHelper;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setRegpp(ServiceCivilService regpp) {
		this.regpp = regpp;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setRcpers(ServiceCivilService rcpers) {
		this.rcpers = rcpers;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		final String targetName = modeHelper.getServiceCivilSource();
		if ("regpp".equalsIgnoreCase(targetName)) {
			LOGGER.info("Utilisation du service civil RegPP.");
			target = regpp;
		}
		else if ("rcpers".equalsIgnoreCase(targetName)) {
			LOGGER.info("Utilisation du service civil RcPers.");
			target = rcpers;
		}
		else {
			throw new IllegalArgumentException("La valeur [" + targetName + "] est incorrect pour le choix du service civil");
		}
	}

	@Override
	public AdressesCivilesActives getAdresses(long noIndividu, RegDate date, boolean strict) throws DonneesCivilesException {
		return target.getAdresses(noIndividu, date, strict);
	}

	@Override
	public AdressesCivilesHistoriques getAdressesHisto(long noIndividu, boolean strict) throws DonneesCivilesException {
		return target.getAdressesHisto(noIndividu, strict);
	}

	@Override
	public Individu getIndividu(long noIndividu, @Nullable RegDate date, AttributeIndividu... parties) {
		return target.getIndividu(noIndividu, date, parties);
	}

	@Override
	public Individu getConjoint(Long noIndividuPrincipal, @Nullable RegDate date) {
		return target.getConjoint(noIndividuPrincipal, date);
	}

	@Override
	public Long getNumeroIndividuConjoint(Long noIndividuPrincipal, RegDate date) {
		return target.getNumeroIndividuConjoint(noIndividuPrincipal, date);
	}

	@Override
	public Set<Long> getNumerosIndividusConjoint(Long noIndividuPrincipal) {
		return target.getNumerosIndividusConjoint(noIndividuPrincipal);
	}

	@Override
	public List<Individu> getIndividus(Collection<Long> nosIndividus, RegDate date, AttributeIndividu... parties) {
		return target.getIndividus(nosIndividus, date, parties);
	}

	@Override
	public Collection<Origine> getOrigines(long noTechniqueIndividu, RegDate date) {
		return target.getOrigines(noTechniqueIndividu, date);
	}

	@Override
	public Collection<Permis> getPermis(long noIndividu, @Nullable RegDate date) {
		return target.getPermis(noIndividu, date);
	}

	@Override
	public Tutelle getTutelle(long noTechniqueIndividu, RegDate date) {
		return target.getTutelle(noTechniqueIndividu, date);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Collection getNationalites(long noTechniqueIndividu, RegDate date) {
		return target.getNationalites(noTechniqueIndividu, date);
	}

	@Override
	public String getNomPrenom(Individu individu) {
		return target.getNomPrenom(individu);
	}

	@Override
	public NomPrenom getDecompositionNomPrenom(Individu individu) {
		return target.getDecompositionNomPrenom(individu);
	}

	@Override
	public EtatCivil getEtatCivilActif(long noIndividu, RegDate date) {
		return target.getEtatCivilActif(noIndividu, date);
	}

	@Override
	public boolean isWarmable() {
		return target.isWarmable();
	}

	@Override
	public List<HistoriqueCommune> getCommunesDomicileHisto(RegDate depuis, long noIndividu, boolean strict, boolean seulementVaud) throws DonneesCivilesException, ServiceInfrastructureException {
		return target.getCommunesDomicileHisto(depuis, noIndividu, strict, seulementVaud);
	}
}
