<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="index" value="${param.index}"/>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title">
		<fmt:message key="title.edition.fors">
			<fmt:param><unireg:numCTB numero="${command.tiersId}"/></fmt:param>
		</fmt:message>
	</tiles:put>

	<tiles:put name="body">
		<form:form name="formFor" id="formFor">

			<form:hidden path="id"/>
			<form:hidden path="tiersId"/>
			<form:hidden path="dateDebut"/>
			<form:hidden path="typeAutoriteFiscale"/>
			<form:hidden path="noAutoriteFiscale"/>

			<fieldset>
				<legend><span><fmt:message key="label.for.fiscal"/></span></legend>
				<!-- Debut For -->
				<table border="0">

					<tr class="<unireg:nextRowClass/>">
						<td><fmt:message key="label.date.ouverture"/>&nbsp;:</td>
						<td>
							<c:if test="${command.dateDebutEditable}">
								<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
									<jsp:param name="path" value="dateDebut"/>
									<jsp:param name="id" value="dateDebut"/>
								</jsp:include>
							</c:if>
							<c:if test="${!command.dateDebutEditable}">
								<form:hidden path="dateDebutEditable"/>
								<unireg:regdate regdate="${command.dateDebut}"/>
							</c:if>
						</td>
						<td><fmt:message key="label.date.fermeture"/>&nbsp;:</td>
						<td>
							<c:if test="${command.dateFinEditable}">
								<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
									<jsp:param name="path" value="dateFin"/>
									<jsp:param name="id" value="dateFin"/>
								</jsp:include>
							</c:if>
							<c:if test="${!command.dateFinEditable}">
								<form:hidden path="dateFinEditable"/>
								<unireg:regdate regdate="${command.dateFin}"/>
							</c:if>
						</td>
					</tr>

					<tr class="<unireg:nextRowClass/>">
						<td><fmt:message key="label.type.for.fiscal"/>&nbsp;:</td>
						<td><fmt:message key="option.type.for.fiscal.${command.typeAutoriteFiscale}"/></td>
						<td>
							<c:choose>
								<c:when test="${command.typeAutoriteFiscale == 'COMMUNE_OU_FRACTION_VD' }"><fmt:message key="option.type.for.fiscal.COMMUNE_OU_FRACTION_VD"/>&nbsp;:</c:when>
								<c:when test="${command.typeAutoriteFiscale == 'COMMUNE_HC' }"><fmt:message key="option.type.for.fiscal.COMMUNE_HC"/>&nbsp;:</c:when>
							</c:choose>
						</td>
						<td>
							<c:choose>
								<c:when test="${command.typeAutoriteFiscale == 'COMMUNE_OU_FRACTION_VD' }">
									<unireg:commune ofs="${command.noAutoriteFiscale}" displayProperty="nomMinuscule" date="${command.dateDebut}"/>
								</c:when>
								<c:when test="${command.typeAutoriteFiscale == 'COMMUNE_HC' }">
									<unireg:commune ofs="${command.noAutoriteFiscale}" displayProperty="nomMinuscule" date="${command.dateDebut}"/>
									(<unireg:commune ofs="${command.noAutoriteFiscale}" displayProperty="sigleCanton" date="${command.dateDebut}"/>)
								</c:when>
							</c:choose>
						</td>
					</tr>
				</table>
			</fieldset>
			<table border="0">
				<tr>
					<td width="25%">&nbsp;</td>
					<td width="25%"><input type="submit" value="<fmt:message key="label.bouton.mettre.a.jour" />"></td>
					<td width="25%"><unireg:buttonTo name="Annuler" action="/fiscal/edit-for-debiteur.do" method="GET" params="{id:${command.tiersId}}"/></td>
					<td width="25%">&nbsp;</td>
				</tr>
			</table>
		</form:form>
	</tiles:put>
</tiles:insert>

		
