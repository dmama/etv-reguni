<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<!-- Debut Rapport Menage Commun -->
<fieldset class="coupleMenageCommun">
	<legend><span><fmt:message key="label.rapport.menage.commun" /></span></legend>
	<c:if test="${not empty warnings}">
		<table class="warnings" cellspacing="0" cellpadding="0" border="0">
			<tr><td class="heading"><fmt:message key="label.couple.avertissements"/></td></tr>
			<tr id="val_errors"><td class="details"><ul>
			<c:forEach var="warn" items="${warnings}">
				<li class="warn"><c:out value="${warn}"/></li>
			</c:forEach>
			</ul></td></tr>
		</table>
	</c:if>
	<table>
		<c:choose>
			<c:when test="${(command.typeUnion != 'FUSION_MENAGES' && command.typeUnion != 'RECONSTITUTION_MENAGE') || command.dateDebut == null}">
				<tr class="<unireg:nextRowClass/>">
					<td width="25%"><fmt:message key="label.date.debut" />&nbsp;:</td>
					<td width="75%">
						<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
							<jsp:param name="path" value="dateDebut" />
							<jsp:param name="id" value="dateDebut" />
						</jsp:include>
						<FONT COLOR="#FF0000">*</FONT>
					</td>
				</tr>
			</c:when>
			<c:otherwise>
				<tr class="<unireg:nextRowClass/>" >
					<td width="25%"><fmt:message key="label.date.debut" />&nbsp;:</td>
					<td width="75%">
						<fmt:formatDate value="${command.dateDebut}" pattern="dd.MM.yyyy"/>
						<form:hidden id="dateDebut" path="dateDebut"/>
					</td>
				</tr>
			</c:otherwise>
		</c:choose>
		<tr class="<unireg:nextRowClass/>">
			<td width="25%"><fmt:message key="label.commentaire" />&nbsp;:</td>
			<td width="75%">
				<form:textarea path="remarque" id="remarque" cols="80" rows="5"/>
			</td>
		</tr>
	</table>
	
</fieldset>
<!-- Fin Rapport Menage Commun -->