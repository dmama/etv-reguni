<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>


<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title"><fmt:message key="label.caracteristiques.evenement" /></tiles:put>
	<tiles:put name="fichierAide">
		<a href="#" onClick="javascript:ouvrirAide('<c:url value='/docs/evenements.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	</tiles:put>
  	<tiles:put name="body">
	  	<form:form method="post" id="formVisuEvenements">
	  	<c:set var="ligneTableau" value="${1}" scope="request" />
		<!-- Debut Caracteristiques generales -->
		<fieldset>
			<legend><span><fmt:message key="label.caracteristiques.evenement" /></span></legend>
			<table>
				<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
				<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
					<td width="25%"><fmt:message key="label.numero.evenement" /> :</td>
					<td width="25%">
						${command.evenement.id}
					</td>
					<td width="25%"><fmt:message key="label.date.evenement" /> :</td>
					<td width="25%"><unireg:regdate regdate="${command.evenement.dateEvenement}"/></td>
				
				</tr>
				<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
				<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
					<td width="25%"><fmt:message key="label.type.evenement" /> :</td>
					<td width="25%"><fmt:message key="option.type.evenement.${command.evenement.type}" /></td>
					<td width="25%"><fmt:message key="label.date.traitement" /> :</td>
					<td width="25%"><fmt:formatDate value="${command.evenement.dateTraitement}" pattern="dd.MM.yyyy HH:mm:ss" /></td>
				</tr>
				<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
				<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
					<td width="25%"><fmt:message key="label.etat.evenement" /> :</td>
					<td width="25%"><fmt:message key="option.etat.evenement.${command.evenement.etat}" /></td>
					<td width="25%"><fmt:message key="label.commune.evenement" /> :</td>
					<td width="25%"><unireg:infra entityId="${command.evenement.numeroOfsCommuneAnnonce}" entityType="commune" entityPropertyName="nomMinuscule"></unireg:infra></td>
				</tr>
			</table>
			
			<c:if test="${not empty command.evenement.erreurs}">
				<display:table 	name="command.evenement.erreurs" id="row" class="error" >
					<display:column property="message" titleKey="label.erreur" class="error"/>
				</display:table>
			</c:if>			
			
		</fieldset>
		<!-- Fin Caracteristiques generales -->
		
		<!-- Debut Individu principal -->
		<fieldset>
			<legend><span><fmt:message key="label.individu" /></span></legend>
			<table>
				<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
				<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
					<td width="50%"><fmt:message key="label.numero.registre.habitant" /> :</td>
					<td width="50%">${command.individuPrincipal.numeroIndividu}</td>
				</tr>
				<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
				<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
					<td width="50%"><fmt:message key="label.nom.prenom" /> :</td>
					<td width="50%">${command.individuPrincipal.nom}&nbsp;${command.individuPrincipal.prenom}</td>
				</tr>
				<c:if test="${command.adressePrincipal.ligne1 != null}">
				<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
				<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
					<td><fmt:message key="label.adresse.courrier.active" />&nbsp;:</td>
					<td>${command.adressePrincipal.ligne1}</td>
				</tr>
				</c:if>
				<c:if test="${command.adressePrincipal.ligne2 != null }">
					<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />	
					<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
						<td>&nbsp;</td>
						<td>${command.adressePrincipal.ligne2}</td>
					</tr>
				</c:if>
				<c:if test="${command.adressePrincipal.ligne3 != null }">
					<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
					<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
						<td>&nbsp;</td>
						<td>${command.adressePrincipal.ligne3}</td>
					</tr>
				</c:if>
				<c:if test="${command.adressePrincipal.ligne4 != null }">
					<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
					<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
						<td>&nbsp;</td>
						<td>${command.adressePrincipal.ligne4}</td>
					</tr>
				</c:if>
				<c:if test="${command.adressePrincipal.ligne5 != null}" >
					<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
					<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
						<td>&nbsp;</td>
						<td>${command.adressePrincipal.ligne5}</td>
					</tr>
				</c:if>
				<c:if test="${command.adressePrincipal.ligne6 != null}" >
					<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
					<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
						<td>&nbsp;</td>
						<td>${command.adressePrincipal.ligne6}</td>
					</tr>
				</c:if>
				<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
				<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
					<td><fmt:message key="label.date.naissance" />&nbsp;:</td>
					<td><fmt:formatDate value="${command.individuPrincipal.dateNaissance}" pattern="dd.MM.yyyy"/></td>
				</tr>
			</table>
		</fieldset>
		<!-- Fin Individu principal -->

		<!-- Debut Individu Conjoint -->
		<c:if test="${command.individuConjoint != null }">
			<fieldset>
				<legend><span><fmt:message key="label.conjoint" /></span></legend>
				<table>
					<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
					<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
						<td width="50%"><fmt:message key="label.numero.registre.habitant" /> :</td>
						<td width="50%">${command.individuConjoint.numeroIndividu}</td>
					</tr>
					<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
					<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
						<td width="50%"><fmt:message key="label.nom.prenom" /> :</td>
						<td width="50%">${command.individuConjoint.nom}&nbsp;${command.individuConjoint.prenom}</td>
					</tr>
					<c:if test="${command.adresseConjoint.ligne1 != null}">
					<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
					<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
						<td><fmt:message key="label.adresse.courrier.active" />&nbsp;:</td>
						<td>${command.adresseConjoint.ligne1}</td>
					</tr>
					</c:if>
					<c:if test="${command.adresseConjoint.ligne2 != null }">
						<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />	
						<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
							<td>&nbsp;</td>
							<td>${command.adresseConjoint.ligne2}</td>
						</tr>
					</c:if>
					<c:if test="${command.adresseConjoint.ligne3 != null }">
						<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
						<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
							<td>&nbsp;</td>
							<td>${command.adresseConjoint.ligne3}</td>
						</tr>
					</c:if>
					<c:if test="${command.adresseConjoint.ligne4 != null }">
						<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
						<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
							<td>&nbsp;</td>
							<td>${command.adresseConjoint.ligne4}</td>
						</tr>
					</c:if>
					<c:if test="${command.adresseConjoint.ligne5 != null}" >
						<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
						<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
							<td>&nbsp;</td>
							<td>${command.adresseConjoint.ligne5}</td>
						</tr>
					</c:if>
					<c:if test="${command.adresseConjoint.ligne6 != null}" >
						<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
						<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
							<td>&nbsp;</td>
							<td>${command.adresseConjoint.ligne6}</td>
						</tr>
					</c:if>
					<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
					<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
						<td><fmt:message key="label.date.naissance" />&nbsp;:</td>
						<td><fmt:formatDate value="${command.individuConjoint.dateNaissance}" pattern="dd.MM.yyyy"/></td>
					</tr>
				</table>
			</fieldset>
		</c:if>
		<!-- Fin Individu Conjoint -->
		
		<!-- Debut List tiers -->
		<c:if test="${not empty command.tiersAssocies}">
		<fieldset>
			<legend><span><fmt:message key="label.tiers.associes" /></span></legend>
			<display:table name="command.tiersAssocies" id="tiersAssocie" pagesize="25" >
				<display:column titleKey="label.numero.tiers" href="../tiers/visu.do" paramId="id" paramProperty="numero" >
					<a href="../tiers/visu.do?id=${tiersAssocie.numero}"><unireg:numCTB numero="${tiersAssocie.numero}" /></a>
				</display:column>
				<display:column titleKey="label.prenom.nom" >
					${tiersAssocie.nomCourrier1}
					<c:if test="${tiersAssocie.nomCourrier2 != null }">
						<br>${tiersAssocie.nomCourrier2}
					</c:if>
				</display:column>
				<display:column  property="localiteOuPays" titleKey="label.localitePays"  />
				<display:column  property="forPrincipal" titleKey="label.for.principal"  />
				<display:column  titleKey="label.date.ouverture.for">
					<unireg:regdate regdate="${tiersAssocie.dateOuvertureFor}"/>
				</display:column>
				<display:column titleKey="label.date.fermeture.for">
					<unireg:regdate regdate="${tiersAssocie.dateFermetureFor}" />
				</display:column>
			</display:table>
		</fieldset>
		</c:if>
		<!-- Fin List tiers -->			
	
		<!-- Debut Boutons -->
		<input type="button" value="<fmt:message key="label.bouton.retour" />" onClick="document.location='list.do';" />
		<c:if test="${(command.evenement.etat == 'A_TRAITER') || (command.evenement.etat == 'EN_ERREUR')}">
			<input type="submit" name="recycler" value="<fmt:message key="label.bouton.recycler" />" />	
		</c:if>
		<c:if test="${(command.evenement.etat != 'TRAITE')}">
			<input type="submit" name="forcer" value="<fmt:message key="label.bouton.forcer" />" />	
		</c:if>
		<!-- Fin Boutons -->
		</form:form>
	</tiles:put>
</tiles:insert>
