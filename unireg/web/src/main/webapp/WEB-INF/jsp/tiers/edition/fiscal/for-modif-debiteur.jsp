<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="index" value="${param.index}" />

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
  	<tiles:put name="title">
  		<fmt:message key="title.edition.fors">
  			<fmt:param><unireg:numCTB numero="${command.numeroCtb}"/></fmt:param>
  		</fmt:message>
  	</tiles:put>

	<tiles:put name="body">
		<form:form name="formFor" id="formFor">
		<fieldset><legend><span><fmt:message key="label.for.fiscal" /></span></legend>		
		<!-- Debut For -->
		<table border="0">
		
			<tr class="<unireg:nextRowClass/>" >
				<td><fmt:message key="label.date.ouverture" />&nbsp;:</td>
				<td>
					<c:if test="${command.dateOuvertureEditable}">
						<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
							<jsp:param name="path" value="dateOuverture" />
							<jsp:param name="id" value="dateOuverture" />
						</jsp:include>
					</c:if>
					<c:if test="${!command.dateOuvertureEditable}">
						<fmt:formatDate value="${command.dateOuverture}" pattern="dd.MM.yyyy"/>
					</c:if>
				</td>
				<td><fmt:message key="label.date.fermeture" />&nbsp;:</td>
				<td>
					<c:if test="${command.dateFermetureEditable}">
						<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
							<jsp:param name="path" value="dateFermeture" />
							<jsp:param name="id" value="dateFermeture" />
						</jsp:include>
					</c:if>
					<c:if test="${!command.dateFermetureEditable}">
						<fmt:formatDate value="${command.dateFermeture}" pattern="dd.MM.yyyy"/>
					</c:if>
				</td>
			</tr>
			
			<tr class="<unireg:nextRowClass/>" >

				<td><fmt:message key="label.type.for.fiscal"/>&nbsp;:</td>
				<td><fmt:message key="option.type.for.fiscal.${command.typeAutoriteFiscale}" /></td>
				<td>
					<c:choose>
						<c:when test="${command.typeAutoriteFiscale == 'COMMUNE_OU_FRACTION_VD' }"><fmt:message key="option.type.for.fiscal.COMMUNE_OU_FRACTION_VD"/>&nbsp;:</c:when>
						<c:when test="${command.typeAutoriteFiscale == 'COMMUNE_HC' }"><fmt:message key="option.type.for.fiscal.COMMUNE_HC"/>&nbsp;:</c:when>
					</c:choose>
				</td>
				<td>
					<c:choose>
						<c:when test="${command.typeAutoriteFiscale == 'COMMUNE_OU_FRACTION_VD' }">
							<unireg:infra entityId="${command.numeroForFiscalCommune}" entityType="commune" entityPropertyName="nomMinuscule"></unireg:infra>
						</c:when>
						<c:when test="${command.typeAutoriteFiscale == 'COMMUNE_HC' }">
							<unireg:infra entityId="${command.numeroForFiscalCommuneHorsCanton}" entityType="commune" entityPropertyName="nomMinuscule"></unireg:infra>
							(<unireg:infra entityId="${command.numeroForFiscalCommuneHorsCanton}" entityType="commune" entityPropertyName="sigleCanton"></unireg:infra>)
						</c:when>
					</c:choose>
				</td>
			</tr>
		</table>
	</fieldset>
	<form:errors cssClass="error" />
	<table border="0">
		<tr>
			<td width="25%">&nbsp;</td>
			<td width="25%"><input type="submit" id="maj" value="<fmt:message key="label.bouton.mettre.a.jour" />"></td>
			<td width="25%"><input type="button" id="annuler" value="<fmt:message key="label.bouton.annuler" />" onclick="document.location.href='../fiscal/edit-for-debiteur.do?id=' + ${command.numeroCtb}"></td>			
			<td width="25%">&nbsp;</td>
		</tr>
	</table>
	</form:form>
	</tiles:put>
</tiles:insert>

		
