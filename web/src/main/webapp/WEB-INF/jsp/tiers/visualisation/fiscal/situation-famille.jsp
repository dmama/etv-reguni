<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<c:if test="${command.withSituationsFamille || command.situationsFamilleEnErreurMessage != null}">
	<fieldset id="sitFamFieldset">
		<legend><span><fmt:message key="label.situation.famille.fiscale"/></span></legend>

		<c:choose>
			<c:when test="${command.situationsFamilleEnErreurMessage != null}">
				<div class="flash-warning"><c:out value="${command.situationsFamilleEnErreurMessage}"/></div>
			</c:when>
			<c:otherwise>
				<input class="noprint" name="adresse_histo" type="checkbox" <c:if test="${command.situationsFamilleHisto}">checked</c:if> onClick="window.location.href = App.toggleBooleanParam(window.location, 'situFamilleHisto', true);" id="isSFHisto" />
				<label class="noprint" for="isSFHisto"><fmt:message key="label.historique" /></label>

				<jsp:include page="../../common/fiscal/situation-famille.jsp">
					<jsp:param name="page" value="visu"/>
				</jsp:include>
			</c:otherwise>
		</c:choose>

	</fieldset>
</c:if>
