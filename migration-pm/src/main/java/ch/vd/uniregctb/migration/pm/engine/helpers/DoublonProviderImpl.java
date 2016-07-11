package ch.vd.uniregctb.migration.pm.engine.helpers;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;
import ch.vd.uniregctb.migration.pm.utils.DataLoadHelper;

/**
 * Implémentation du service d'identification des doublons (= des entreprises qui doivent être migrées
 * en tenant compte du fait qu'elles représentent un doublon)
 * <br/>
 * Cette implémentation utilise la raison sociale (qui commence ou pas par une astérisque) ainsi qu'un
 * fichier de données externes qui liste des numéros de contribuable explicitement
 */
public class DoublonProviderImpl implements DoublonProvider, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(DoublonProviderImpl.class);

	private String nomFichierSource;
	private Set<Long> idsExplicites;

	public void setNomFichierSource(String nomFichierSource) {
		this.nomFichierSource = nomFichierSource;
	}

	@Override
	public void afterPropertiesSet() throws Exception {

		// on va lire le fichier et remplir la liste des numéros de contribuables explicite avec
		final Pattern pattern = Pattern.compile("([0-9]{1,5})(;.*)?");
		if (StringUtils.isNotBlank(nomFichierSource)) {

			// les données extraites du fichier
			List<Long> data;

			LOGGER.info("Chargement du fichier " + nomFichierSource + " des identifiants de contribuables 'doublons'.");
			try (FileInputStream fis = new FileInputStream(nomFichierSource);
			     Reader reader = new InputStreamReader(fis);
			     BufferedReader br = new BufferedReader(reader)) {

				data = DataLoadHelper.loadData(br, pattern, matcher -> Long.valueOf(matcher.group(1)));
			}
			this.idsExplicites = new HashSet<>(data);
			LOGGER.info(data.size() + " ligne(s) chargée(s) du fichier " + nomFichierSource + " (soit " + idsExplicites.size() + " contribuables distincts).");
		}
		else {
			this.idsExplicites = Collections.emptySet();
		}
	}

	@Override
	public boolean isDoublon(RegpmEntreprise regpm) {
		// la règle dit : la ligne 1 de la raison sociale commence par une étoile...
		// et aussi : ou alors le numéro est listé explicitement
		return (StringUtils.isNotBlank(regpm.getRaisonSociale1()) && regpm.getRaisonSociale1().startsWith("*")) || idsExplicites.contains(regpm.getId());
	}
}
