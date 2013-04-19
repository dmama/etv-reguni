<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<!-- Debut Adresse -->
<form:form name="formAddAdresse" id="formAddAdresse">
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title">
  		<fmt:message key="title.fermeture.adresse">
  			<fmt:param><unireg:numCTB numero="${command.numCTB}"/></fmt:param>
  		</fmt:message>
  	</tiles:put>

	<tiles:put name="body">
		<fieldset>
			<legend><span><fmt:message key="label.adresse.fermeture" /></span></legend>
			<table>

				<tr class="<unireg:nextRowClass/>" >
					<td><fmt:message key="label.utilisationAdresse"/>&nbsp;:</td>
					<td><fmt:message key="option.usage.${command.usage}"/><td>
				</tr>
				<tr class="<unireg:nextRowClass/>" >					
					<td><fmt:message key="label.adresse.complement"/>&nbsp;:</td>
					<td>${command.complements}<td>
				</tr>
				<tr class="<unireg:nextRowClass/>" >
					<td><fmt:message key="label.rueCasePostale"/>&nbsp;:</td>
					<td>${command.rue}<td>
				</tr>
				<c:choose>
					<c:when test="${command.localite != null }">
						<tr class="<unireg:nextRowClass/>" >
							<td><fmt:message key="label.localite"/>&nbsp;:</td>
							<td><c:out value="${command.localite}"/><td>
						</tr>
					</c:when>
					<c:when test="${command.localiteSuisse != null }">
						<tr class="<unireg:nextRowClass/>" >
							<td><fmt:message key="label.localite"/>&nbsp;:</td>
							<td>${command.localiteSuisse}<td>
						</tr>
					</c:when>
				</c:choose>
				<c:if test="${command.paysOFS != null }">
					<tr class="<unireg:nextRowClass/>" >
						<td><fmt:message key="label.pays"/>&nbsp;:</td>
						<td>
							<unireg:pays ofs="${command.paysOFS}" displayProperty="nomCourt" date="${command.regDateDebut}"/>
						<td>
					</tr>
				</c:if>

				<c:if test="${command.source != null }">
					<tr class="<unireg:nextRowClass/>" >
						<td><fmt:message key="label.adresse.source"/>&nbsp;:</td>
						<td><fmt:message key="option.source.${command.source}" /></td>
					</tr>
				</c:if>
				<tr class="<unireg:nextRowClass/>" >
					<td><fmt:message key="label.date.debut"/>&nbsp;:</td>
					<td><fmt:formatDate value="${command.dateDebut}" pattern="dd.MM.yyyy"/><td>
				</tr>
				<tr class="<unireg:nextRowClass/>" >
					<td ><fmt:message key="label.date.fermeture" />&nbsp;:</td>
					<td>
					   <c:if test="${command.id != null}">
								<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
									<jsp:param  name="path" value="dateFin" />
									<jsp:param name="id" value="dateFin" />
								</jsp:include>
						</c:if>
					</td>
				</tr>
			</table>
		</fieldset>
		<table border="0">
			<tr>
				<td width="25%">&nbsp;</td>
				<td width="25%"><input type="submit" id="fermerAdresse" value="<fmt:message key="label.bouton.mettre.a.jour" />"></td>
				<td width="25%">&nbsp;</td>
				<td width="25%"><input type="button" value="<fmt:message key="label.bouton.retour" />" onClick="document.location.href='edit.do?id=' + ${command.numCTB}" /></td>
			</tr>
		</table>
	</tiles:put>
</tiles:insert>

</form:form>



<!-- Fin Adresse -->
