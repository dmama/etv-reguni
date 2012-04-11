<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<c:if test="${not empty command.situationsFamille || command.situationsFamilleEnErreurMessage != null}">
	<fieldset id="sitFamFieldset">
		<legend><span><fmt:message key="label.situation.famille.fiscale"/></span></legend>

		<c:if test="${command.situationsFamilleEnErreurMessage != null}">
			<div class="flash-warning"><c:out value="${command.situationsFamilleEnErreurMessage}"/></div>
		</c:if>
		<c:if test="${command.situationsFamilleEnErreurMessage == null}">
			<input class="noprint" name="adresse_histo" type="checkbox" onClick="Histo.toggleRowsIsHisto('situationFamille','isSFHisto', 5);" id="isSFHisto" />
			<label class="noprint" for="isSFHisto"><fmt:message key="label.historique" /></label>

			<jsp:include page="../../common/fiscal/situation-famille.jsp">
				<jsp:param name="page" value="visu"/>
			</jsp:include>
		</c:if>

	</fieldset>
</c:if>

<!--[if IE 6]>
<script>
	$(function() {
		$('#isForHisto').click(function() {
			// [SIFISC-380] on force le recalcul des widgets parce que IE6 ne détecte pas le changement autrement.
			$('#sitFamFieldset').addClass('toresize');
			$('#sitFamFieldset').removeClass('toresize');
		});
	});
</script>
<![endif]-->
