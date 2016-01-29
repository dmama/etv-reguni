<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title">
		<fmt:message key="title.edition.allegement.fiscal">
			<fmt:param>
				<unireg:numCTB numero="${allegement.pmId}"/>
			</fmt:param>
		</fmt:message>
	</tiles:put>
	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>
		<unireg:bandeauTiers numero="${allegement.pmId}" showAvatar="false" showValidation="false" showEvenementsCivils="false" showLinks="false" titre="Caractéristiques du contribuable"/>

		<form:form id="editAllegementForm" commandName="command" action="edit.do">

			<fieldset>
				<legend><span><fmt:message key="label.allegement.fiscal"/></span></legend>

				<form:hidden path="afId"/>
				<unireg:nextRowClass reset="0"/>
				<table border="0">
					<tr class="<unireg:nextRowClass/>">
						<td width="25%"><fmt:message key="label.date.debut"/>&nbsp;:</td>
						<td width="25%">
							<unireg:regdate regdate="${allegement.dateDebut}"/>
							<form:hidden path="dateDebut"/>
							<form:errors path="dateDebut" cssClass="error"/>
						</td>
						<td width="25%"><fmt:message key="label.date.fin"/>&nbsp;:</td>
						<td width="25%">
							<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
								<jsp:param name="path" value="dateFin"/>
								<jsp:param name="id" value="dateFin"/>
							</jsp:include>
						</td>
					</tr>
					<tr class="<unireg:nextRowClass/>">
						<td><fmt:message key="label.type.impot"/>&nbsp;</td>
						<td>
							<fmt:message key="option.allegement.fiscal.type.impot.${allegement.typeImpot}"/>
						</td>
						<td><fmt:message key="label.type.collectivite"/>&nbsp;</td>
						<td>
							<fmt:message key="option.allegement.fiscal.type.collectivite.${allegement.typeCollectivite}"/>
							<c:if test="${allegement.typeCollectivite == 'COMMUNE' && allegement.noOfsCommune != null}">
								(<unireg:commune ofs="${allegement.noOfsCommune}" displayProperty="nomOfficiel" titleProperty="noOFS" date="${allegement.dateDebut}"/>)
							</c:if>
						</td>
					</tr>
					<tr class="<unireg:nextRowClass/>">
						<td><fmt:message key="label.type.allegement.fiscal"/>&nbsp;</td>
						<td>
							<c:choose>
								<c:when test="${allegement.typeICC != null}">
									<fmt:message key="option.allegement.icc.type.${allegement.typeICC}"/>
								</c:when>
								<c:when test="${allegement.typeIFD != null}">
									<fmt:message key="option.allegement.ifd.type.${allegement.typeIFD}"/>
								</c:when>
							</c:choose>
						</td>
						<td><fmt:message key="label.montant.pourcentage.allegement"/>&nbsp;</td>
						<td>
							<c:choose>
								<c:when test="${allegement.pourcentage != null}">
									<fmt:formatNumber maxIntegerDigits="3" minIntegerDigits="1" maxFractionDigits="2" minFractionDigits="0" value="${allegement.pourcentage}"/>&nbsp;%
								</c:when>
								<c:otherwise>
									<fmt:message key="label.allegement.montant"/>
								</c:otherwise>
							</c:choose>
						</td>
					</tr>
				</table>

			</fieldset>

			<!-- Debut Bouton -->
			<table border="0">
				<tr>
					<td width="25%">&nbsp;</td>
					<td width="25%"><input type="submit" value="<fmt:message key="label.bouton.sauver" />"></td>
					<td width="25%"><unireg:buttonTo name="Retour" action="/allegement/edit-list.do" params="{pmId:${allegement.pmId}}" method="GET"/></td>
					<td width="25%">&nbsp;</td>
				</tr>
			</table>
			<!-- Fin Bouton -->

		</form:form>

	</tiles:put>

</tiles:insert>