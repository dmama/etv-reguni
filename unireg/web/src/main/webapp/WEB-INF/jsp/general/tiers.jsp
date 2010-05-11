<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="page" value="${param.page}" />
<c:set var="path" value="${param.path}" />
<c:set var="bind" value="command.${path}" scope="request"/>
<spring:bind path="${bind}" >
	<c:set var="tiersGeneral" value="${status.value}"  scope="request"/>
</spring:bind>
<!-- Debut Caracteristiques generales -->
<fieldset class="information">
	<legend><span>
		<c:if test="${path != 'tiersLie'}">
			<fmt:message key="caracteristiques.tiers" />
		</c:if>
		<c:if test="${path == 'tiersLie'}">
			<fmt:message key="caracteristiques.tiers.lie" />
		</c:if>
	</span></legend>

	<input name="debugNatureTiers" type="hidden" value="<c:out value="${tiersGeneral.natureTiers}"/>"></input>

	<table cellspacing="0" cellpadding="0" border="0">
		<tr>
			<td>

	<table cellspacing="0" cellpadding="5" border="0" class="display_table">
		<spring:bind path="${bind}" >
			<c:if test="${tiersGeneral.annule}">
				<tr class="inactif">
					<td colspan="3" width="100%"><center><fmt:message key="label.tiers.annule.au"/>&nbsp;<fmt:formatDate value="${tiersGeneral.annulationDate}" pattern="dd.MM.yyyy"/></center></td>
				</tr>
			</c:if>
			<authz:authorize ifAnyGranted="ROLE_MODIF_VD_ORD">
				<c:if test="${tiersGeneral.nosIndividusAvecEvenenementCivilNonTraite != null}">
					<tr class="evts-civils-non-traites">
						<td colspan="3" width="100%">
							<center>
								<fmt:message key="label.tiers.evts.non.traites"/>
								<c:forEach var="noInd" items="${tiersGeneral.nosIndividusAvecEvenenementCivilNonTraite}">
									<c:out value="${noInd}"/>
								</c:forEach>
							</center>
						</td>
					</tr>
				</c:if>
			</authz:authorize>
		</spring:bind>
		<c:if test="${fn:length(tiersGeneral.validationResults.errors) > 0 || fn:length(tiersGeneral.validationResults.warnings) > 0}">
			<tr><td colspan="3" width="100%">
				<table class="validation_error" cellspacing="0" cellpadding="0" border="0">
					<tr><td class="heading"><fmt:message key="label.validation.problemes.detectes"/> <span id="val_script">(<a href="#" onclick="javascript:showDetails()"><fmt:message key="label.validation.voir.details"/></a>)</span></td></tr>
					<tr id="val_errors"><td class="details"><ul>
					<c:forEach var="err" items="${tiersGeneral.validationResults.errors}">
						<li class="err"><fmt:message key="label.validation.erreur"/>: <c:out value="${err}"/></li>
					</c:forEach>
					<c:forEach var="warn" items="${tiersGeneral.validationResults.warnings}">
						<li class="warn"><fmt:message key="label.validation.warning"/>: <c:out value="${warn}"/></li>
					</c:forEach>
					</ul></td></tr>
				</table>
				<script type="text/javascript">
					// cache les erreurs par défaut
					var vs = E$('val_errors');
					vs.style.display = 'none';	

					// affiche les erreurs
					function showDetails() {
						var ve = E$('val_errors');
						ve.style.display = '';	
						var vs = E$('val_script');
						vs.style.display = 'none';	
					}
				</script>
			</td></tr>
		</c:if>
		<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
		<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
			<td width="25%"><fmt:message key="label.numero.tiers" />&nbsp;:</td>
			<jsp:include page="numero.jsp">
				<jsp:param name="page" value="${page}" />	
				<jsp:param name="path" value="${path}" />	
			</jsp:include>
			<td width="25%">
				<c:if test="${!tiersGeneral.annule}">
					<c:if test="${page == 'visu' }">
						<jsp:include page="vers.jsp">
							<jsp:param name="path" value="${path}" />
						</jsp:include>
					</c:if>
				</c:if>
			</td>
		</tr>
		<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
		<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
			<td width="25%"><fmt:message key="label.role" />&nbsp;:</td>
			<td width="50%">
				<jsp:include page="role.jsp">
					<jsp:param name="path" value="${path}" />
				</jsp:include>
			</td>
			<td width="25%">
				<c:if test="${page == 'visu' }">
					<authz:authorize ifAnyGranted="ROLE_TESTER, ROLE_ADMIN">
						<form method="post" style="text-align: right; padding-right: 1em" 
							action="<c:url value="/admin/indexation.do"/>?action=reindexTiers&id=${tiersGeneral.numero}">
							<input type="submit" value="<fmt:message key="label.bouton.forcer.reindexation"/>" />
						</form>
					</authz:authorize>
				</c:if>
			</td>
		</tr>
		<jsp:include page="adresse-envoi.jsp">	
			<jsp:param name="path" value="${path}" />
		</jsp:include>

	</table>
	
			</td>
			<td width="130 px">
				<c:choose>
					<c:when test="${tiersGeneral.type eq 'HOMME'}">
						<img src="<c:url value="/images/tiers/homme.png" />">
					</c:when>
					<c:when test="${tiersGeneral.type eq 'FEMME'}">
						<img src="<c:url value="/images/tiers/femme.png" />">
					</c:when>
					<c:when test="${tiersGeneral.type eq 'SEXE_INCONNU'}">
						<img src="<c:url value="/images/tiers/inconnu.png" />">
					</c:when>
					<c:when test="${tiersGeneral.type eq 'MC_MIXTE'}">
						<img src="<c:url value="/images/tiers/menagecommun.png" />">
					</c:when>
					<c:when test="${tiersGeneral.type eq 'MC_HOMME_SEUL'}">
						<img src="<c:url value="/images/tiers/homme_seul.png" />">
					</c:when>
					<c:when test="${tiersGeneral.type eq 'MC_FEMME_SEULE'}">
						<img src="<c:url value="/images/tiers/femme_seule.png" />">
					</c:when>
					<c:when test="${tiersGeneral.type eq 'MC_HOMME_HOMME'}">
						<img src="<c:url value="/images/tiers/homme_homme.png" />">
					</c:when>
					<c:when test="${tiersGeneral.type eq 'MC_FEMME_FEMME'}">
						<img src="<c:url value="/images/tiers/femme_femme.png" />">
					</c:when>
					<c:when test="${tiersGeneral.type eq 'MC_SEXE_INCONNU'}">
						<img src="<c:url value="/images/tiers/mc_inconnu.png" />">
					</c:when>
					<c:when test="${tiersGeneral.type eq 'ENTREPRISE'}">
						<img src="<c:url value="/images/tiers/entreprise.png" />">
					</c:when>
					<c:when test="${tiersGeneral.type eq 'ETABLISSEMENT'}">
						<img src="<c:url value="/images/tiers/etablissement.png" />">
					</c:when>
					<c:when test="${tiersGeneral.type eq 'AUTRE_COMM'}">
						<img src="<c:url value="/images/tiers/autrecommunaute.png" />">
					</c:when>
					<c:when test="${tiersGeneral.type eq 'COLLECT_ADMIN'}">
						<img src="<c:url value="/images/tiers/collectiviteadministrative.png" />">
					</c:when>
					<c:when test="${tiersGeneral.type eq 'DEBITEUR'}">
						<img src="<c:url value="/images/tiers/debiteur.png" />">
					</c:when>
				</c:choose>
			</td>
		</tr>
	</table>
	
</fieldset>
<!-- Fin Caracteristiques generales -->