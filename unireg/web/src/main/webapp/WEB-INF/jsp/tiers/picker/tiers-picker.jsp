<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<%@ taglib uri="http://www.unireg.com/uniregTagLib" prefix="unireg" %>

<tiles:insert template="/WEB-INF/jsp/templates/templateDialog.jsp">
	<tiles:put name="body">

		<div id="tiers-picker">
			<input type="text" id="tiers-picker-query" class="text ui-widget-content ui-corner-all" placeholder="Entrez vos critères de recherche ici" autofocus/>
			<div id="tiers-picker-results"><%-- cette DIV est mise-à-jour par Ajax --%></div>
			<script>
		    $(function() {
				// fallback autofocus pour les browsers qui ne le supportent pas
				if (!("autofocus" in document.createElement("input"))) {
					$('#tiers-picker-query').focus();
				}
			});
			</script>
		</div>

	</tiles:put>
</tiles:insert>
