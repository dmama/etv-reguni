<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<c:set var="even_pm_authorized" value="false" />

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="head">
		<style>
		</style>
	</tiles:put>

	<tiles:put name="title">
		<fmt:message key="title.suivi.annonces" />
	</tiles:put>

	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>
		<form:form method="get" commandName="view" id="formSuiviAnnonce" action="find.do">

			<fieldset>
				<legend><span><fmt:message key="title.suivi.annonces"/></span></legend>
				<table>
					<tr class="<unireg:nextRowClass/>" >
						<td>Numéro d'annonce&nbsp;:</td>
						<td>
							<form:input  path="noticeId" id="noticeId" cssClass="number"/>
							<form:errors path="noticeId" cssClass="error"/>
						</td>
						<td>Type d'annonce&nbsp;:</td>
						<td>
							<form:select path="type" id="type">
								<form:option value="">Tous</form:option>
								<form:options items="${noticeTypes}"/>
							</form:select>
							<form:errors path="type" cssClass="error"/>
						</td>
					</tr>
					<tr class="<unireg:nextRowClass/>" >
						<td>Numéro cantonal&nbsp;:</td>
						<td>
							<form:input  path="cantonalId" id="cantonalId" cssClass="number"/>
							<form:errors path="cantonalId" cssClass="error"/>
						</td>
						<td rowspan="5">Statuts d'annonce&nbsp;:</td>
						<td rowspan="5">
							<form:select path="status" id="status" multiple="true" size="10">
								<form:options items="${noticeStatuts}"/>
							</form:select>
							<form:errors path="status" cssClass="error"/>
						</td>
					</tr>
					<tr class="<unireg:nextRowClass/>" >
						<td>Raison sociale&nbsp;:</td>
						<td>
							<form:input  path="name" id="name"/>
							<form:errors path="name" cssClass="error"/>
						</td>
					</tr>
					<tr class="<unireg:nextRowClass/>" >
						<td>Date d'annonce&nbsp;:</td>
						<td>Du
							<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
								<jsp:param name="path" value="dateFrom" />
								<jsp:param name="id" value="dateFrom" />
							</jsp:include>
							&nbsp;au
							<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
								<jsp:param name="path" value="dateTo" />
								<jsp:param name="id" value="dateTo" />
							</jsp:include>
						</td>
					</tr>
					<tr class="<unireg:nextRowClass/>" >
						<td>Utilisateur&nbsp;:</td>
						<td>
							<form:input  path="userId" id="userId"/>
							<form:errors path="userId" cssClass="error"/>
						</td>
					</tr>
					<tr class="<unireg:nextRowClass/>" >
						<td>Numéro de tiers&nbsp;:</td>
						<td>
							<form:input  path="tiersId" id="tiersId"/>
							<form:errors path="tiersId" cssClass="error"/>
						</td>
					</tr>

				</table>

				<!-- Debut Boutons -->
				<table border="0">
					<tr class="odd" >
						<td width="25%">
							Nombre résultats par page&nbsp;:
							<form:select path="resultsPerPage">
							<form:option value="10">10</form:option>
							<form:option value="20">20</form:option>
							<form:option value="50">50</form:option>
						</form:select>
						</td>
						<td width="25%">
							<div class="navigation-action"><input type="submit" value="<fmt:message key="label.bouton.rechercher"/>" name="rechercher"/></div>
						</td>
						<td width="25%">
							<div class="navigation-action"><unireg:buttonTo name="label.bouton.effacer" action="/annonceIDE/find.do" params="" method="get"/></div>
						</td>
						<td width="25%">&nbsp;</td>
					</tr>
				</table>

			</fieldset>


			<%--@elvariable id="page" type="org.springframework.data.domain.Page"--%>
			<%--@elvariable id="totalElements" type="java.lang.Integer"--%>
			<display:table name="page.content" id="annonce" class="display_table" pagesize="${page.size}" size="${totalElements}" sort="external" partialList="true"
			               requestURI="/annonceIDE/find.do" decorator="ch.vd.unireg.decorator.TableEntityDecorator">
				<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.auncune.annonce.trouvee"/></span></display:setProperty>
				<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner">1 <fmt:message key="banner.une.annonce.trouvee"/></span></display:setProperty>
				<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.annonces.trouvees"/></span></display:setProperty>
				<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.annonces.trouvees"/></span></display:setProperty>

				<display:column sortable="true" titleKey="label.numero.annonce" sortName="noticeRequestId">
					${annonce.numero}
				</display:column>
				<display:column titleKey="label.numero.tiers.entreprise" >
					<c:if test="${annonce.numeroTiersEntreprise != null}">
						<a href="<c:url value="../tiers/visu.do"/>?id=${annonce.numeroTiersEntreprise}"><unireg:numCTB numero="${annonce.numeroTiersEntreprise}" />&nbsp;</a>
					</c:if>
				</display:column>
				<display:column titleKey="label.numero.evenement.organisation" >
					<c:if test="${annonce.noEvtOrganisation != null}">
						<authz:authorize access="hasAnyRole('ROLE_EVEN_PM')">
							<c:set var="even_pm_authorized" value="true" />
						</authz:authorize>
						<c:choose>
							<c:when test="${even_pm_authorized}">
								<a href="#" onclick="EvtOrg.open_details(${annonce.idEvtOrganisation}, null, null)">${annonce.noEvtOrganisation}</a>
							</c:when>
							<c:otherwise>
								${annonce.noEvtOrganisation}
							</c:otherwise>
						</c:choose>
					</c:if>
				</display:column>
				<display:column sortable="true" titleKey="label.type.annonce" sortName="typeOfNoticeRequest">
					<fmt:message key="option.type.annonce.${annonce.type}" />
				</display:column>
				<display:column sortable="true" titleKey="label.statut.annonce" sortName="status">
					<fmt:message key="option.statut.annonce.${annonce.statut.statut}" />
				</display:column>
				<display:column sortable="true" titleKey="label.no.cantonal.annonce" sortName="cantonalId">
					<unireg:cantonalId cantonalId="${annonce.informationOrganisation.numeroSite}"/>
				</display:column>
				<display:column titleKey="label.no.ide.annonce" >
					<unireg:numIDE numeroIDE="${annonce.noIde}"/>
				</display:column>
				<display:column sortable="true" titleKey="label.nom.entreprise.annonce" sortName="name">
					${annonce.contenu.nom}
				</display:column>
				<display:column sortable="true" titleKey="label.date.annonce" sortName="date">
					<fmt:formatDate value="${annonce.dateAnnonce}" pattern="dd.MM.yyyy HH:mm:ss"/>
				</display:column>
				<display:column sortable="true" titleKey="label.utilisateur.annonce" sortName="userId">
					${annonce.utilisateur.userId}
				</display:column>
				<display:column style="action">
					<a href="#" class="detail" title="Détails de l'annonce" onclick="Annonce.open_details(<c:out value="${annonce.numero}"/>, '<c:out value="${annonce.utilisateur == null ? null : annonce.utilisateur.userId}"/>'); return false;">&nbsp;</a>
				</display:column>
			</display:table>

		</form:form>
	</tiles:put>
</tiles:insert>
