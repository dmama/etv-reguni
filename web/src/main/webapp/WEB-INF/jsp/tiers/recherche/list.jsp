<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

	<tiles:put name="title"><fmt:message key="title.recherche.tiers"/></tiles:put>
	<tiles:put name="fichierAide">
		<li>
			<a href="#" onClick="ouvrirAide('<c:url value='/docs/recherche.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
		</li>
	</tiles:put>
	<tiles:put name="body">

		<div id="simple-search" style="display:none;">
			<jsp:include page="simple.jsp"/>
		</div>

		<div id="advanced-search">
			<unireg:nextRowClass reset="1"/>
			<form:form method="post" id="formRechercheTiers" action="list.do">
				<fieldset>
					<legend><span><fmt:message key="label.recherche.avancee"/></span></legend>
					<form:errors cssClass="error"/>
					<c:if test="${errorMessage != null}">
						<span class="error">
							<fmt:message key="${errorMessage}"/>
						</span>
					</c:if>
					<jsp:include page="form.jsp">
						<jsp:param name="typeRecherche" value="principale"/>
						<jsp:param name="prefixeEffacer" value="/tiers"/>
					</jsp:include>
				</fieldset>

				<%--@elvariable id="parametresApp" type="ch.vd.unireg.param.view.ParamApplicationView"--%>
				<display:table name="list" id="tiers" pagesize="${parametresApp.nbMaxParPage}" requestURI="/tiers/list.do" class="display_table" sort="list"
				               decorator="ch.vd.unireg.decorator.TableEntityDecorator">
					<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.auncun.tiers.trouve"/></span></display:setProperty>
					<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner">1 <fmt:message key="banner.tiers.trouve"/></span></display:setProperty>
					<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.tiers.trouves"/></span></display:setProperty>
					<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.tiers.trouves"/></span></display:setProperty>

					<display:column sortable="true" titleKey="label.numero.tiers" sortProperty="numero" style="width: 10em;">
						<a href="visu.do?id=${tiers.numero}" style="vertical-align: middle; line-height: 2em;"><unireg:numCTB numero="${tiers.numero}"/></a>
						<c:if test="${tiers.typeAvatar != null}">
							<div style="float: right;">
								<img alt="" src="<c:url value='/tiers/avatar.do'/>?type=${tiers.typeAvatar}&url_memorize=false" style="height: 2em;"/>
							</div>
						</c:if>
					</display:column>
					<display:column sortable="true" titleKey="label.role" style="white-space: nowrap;">
						<c:out value="${tiers.roleLigne1}"/>
						<c:if test="${tiers.roleLigne2 != null}">
							<br><c:out value="${tiers.roleLigne2}"/>
						</c:if>
					</display:column>
					<display:column sortable="true" titleKey="label.nom.raison">
						<c:out value="${tiers.nom1}"/>
						<c:if test="${tiers.nom2 != null}">
							<br><c:out value="${tiers.nom2}"/>
						</c:if>
					</display:column>
					<display:column titleKey="label.date.naissance.ou.rc" sortable="true">
						<c:if test="${tiers.dateNaissanceInscriptionRC != null}">
							<unireg:date date="${tiers.dateNaissanceInscriptionRC}"/>
						</c:if>
					</display:column>
					<display:column sortable="true" titleKey="label.npa">
						<c:out value="${tiers.npa}" />
					</display:column>
					<display:column sortable="true" titleKey="label.localitePays">
						<c:out value="${tiers.localiteOuPays}" />
					</display:column>
					<display:column sortable="true" titleKey="label.for.principal">
						<c:out value="${tiers.forPrincipal}" />
					</display:column>
					<display:column sortable="true" titleKey="label.date.ouverture.for.vd" sortProperty="dateOuvertureForVd">
						<unireg:regdate regdate="${tiers.dateOuvertureForVd}" format="dd.MM.yyyy"/>
					</display:column>
					<display:column sortable="true" titleKey="label.date.fermeture.for.vd" sortProperty="dateFermetureForVd">
						<unireg:regdate regdate="${tiers.dateFermetureForVd}" format="dd.MM.yyyy"/>
					</display:column>

					<display:column sortable="false" titleKey="label.ouvrir.vers">
						<c:if test="${!tiers.annule}">
							<c:choose>
								<c:when test="${urlRetour == null}">
									<unireg:interoperabilite noTiers="${tiers.numero}" natureTiers="${tiers.tiersType}" debiteurInactif="${tiers.debiteurInactif}"/>
								</c:when>
								<c:otherwise>
									<a href="${urlRetour}${tiers.numero}" class="detail" title="<fmt:message key="label.retour.application.appelante" />">&nbsp;</a>
								</c:otherwise>
							</c:choose>
						</c:if>
					</display:column>
				</display:table>

			</form:form>
		</div>

		<script type="text/javascript">
			$(function() {
				<%--@elvariable id="urlRetour" type="java.lang.String"--%>
				<%--@elvariable id="simpleSearchQuery" type="java.lang.String"--%>
				var urlRetour<c:if test="${urlRetour != null}"> = '${urlRetour}'</c:if>;
				var savedSimpleQuery<c:if test="${simpleSearchQuery != null}"> = '${simpleSearchQuery}'</c:if>;

				// on initialise la recherche
				Search.init(urlRetour, savedSimpleQuery);
			});
		</script>

	</tiles:put>
</tiles:insert>
