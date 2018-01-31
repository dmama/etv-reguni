<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<%--@elvariable id="command" type="ch.vd.uniregctb.evenement.organisation.view.EvenementOrganisationSummaryView"--%>

<c:if test="${not empty command.evtErreurs}">
	<h3><fmt:message key="label.detail.traitement"/></h3>
	<ul style="max-width: 936px">
		<c:forEach items="${command.evtErreurs}" var="entry">
			<c:if test="${empty entry.callstack}">
				<li><strong><c:out value="${entry.message}"/></strong></li>
			</c:if>
			<c:if test="${not empty entry.callstack}">
				<li><strong><span class="error"><unireg:callstack headerMessage="${entry.message}" callstack="${entry.callstack}"/></span></strong></li>
			</c:if>
		</c:forEach>
	</ul>
</c:if>

<!-- Début de la liste des événements dans un état non final sur ce même tiers -->

<unireg:nextRowClass reset="1"/>
<c:if test="${fn:length(command.nonTraitesSurMemeOrganisation) > 0}">
	<br>
	<h3><fmt:message key="label.evenements.non.traites.entreprise"/></h3>
	<display:table name="command.nonTraitesSurMemeOrganisation" id="aTraiter">
		<display:column style="width: 3em; text-align: center;">
			<c:if test="${aTraiter.id == command.evtId}">
				<img src="<c:url value='/images/pin.png'/>"/>
			</c:if>
		</display:column>
		<display:column titleKey="label.numero.evenement">
			<c:if test="${aTraiter.id == command.evtId}">
				<c:out value="${aTraiter.noEvenement}"/>
			</c:if>
			<c:if test="${aTraiter.id != command.evtId}">
				<a href="visu.do?id=<c:out value='${aTraiter.id}'/>"><c:out value="${aTraiter.noEvenement}"/></a>
			</c:if>
		</display:column>
		<display:column titleKey="label.type.evenement">
			<fmt:message key="option.type.evenement.organisation.${aTraiter.type}" />
		</display:column>
		<display:column titleKey="label.etat.evenement">
			<fmt:message key="option.etat.evenement.${aTraiter.etat}"/>
		</display:column>
		<display:column titleKey="label.date.evenement">
			<c:if test="${aTraiter.date == aTraiter.date}">
				<unireg:regdate regdate="${aTraiter.date}"/>
			</c:if>
			<c:if test="${aTraiter.date != aTraiter.date}">
				<span title="<fmt:message key='label.modification.date.par.correction'/>">
					<unireg:regdate regdate="${aTraiter.date}"/>
					<img src="<c:url value='/images/right-arrow.png'/>" alt="<fmt:message key='label.modification.date.par.correction'/>" height="16px"/>
					<unireg:regdate regdate="${aTraiter.date}"/>
				</span>
			</c:if>
		</display:column>
	</display:table>
</c:if>
<!-- Fin de la liste des événements dans un état non final sur ce même tiers -->
