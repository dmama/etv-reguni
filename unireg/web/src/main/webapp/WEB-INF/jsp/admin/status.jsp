<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title"><fmt:message key="title.info"/></tiles:put>

  	<tiles:put name="body">
  	
  		<a href="JavaScript:location.reload(true);">
			(<fmt:message key="label.bouton.refresh"/>)
		</a>

		<fieldset>
			<legend><span><fmt:message key="label.batch.actif"/></span></legend> 
			<div id="jobsActif"></div>
		</fieldset>

		<fieldset class="info">
			<legend><span><fmt:message key="title.info.data" /></span></legend>
			<table>
				<unireg:nextRowClass reset="1"/>
				<tr class="<unireg:nextRowClass/>"><td><fmt:message key="label.info.db.nombre.tiers"/></td><td><span class="value"><c:out value="${tiersCount}"/></span></td></tr>
				<tr class="<unireg:nextRowClass/>"><td><fmt:message key="label.info.indexer.nombre.docs"/></td><td><span class="value"><c:out value="${indexCount}"/></span></td></tr>
			</table>
		</fieldset>

		<fieldset class="info">
			<legend><span><fmt:message key="title.info.connection" /></span></legend>
			<table class="display_table">
				<thead>
					<tr>
						<th/><th>Etat</th>
					</tr>
				</thead>
				<tbody>
					<unireg:nextRowClass reset="1"/>
					<tr class="<unireg:nextRowClass/>">
						<td><fmt:message key="label.info.service.civil"/></td>
						<td id="serviceCivilStatus"><c:out value="${serviceCivilStatus}" escapeXml="false"/></td>
					</tr>
					<tr class="<unireg:nextRowClass/>">
						<td><fmt:message key="label.info.service.host.infra"/></td>
						<td id="serviceInfraStatus"><c:out value="${serviceInfraStatus}" escapeXml="false"/></td>
					</tr>
					<tr class="<unireg:nextRowClass/>">
						<td><fmt:message key="label.info.service.fifor"/></td>
						<td id="serviceFidorStatus"><c:out value="${serviceFidorStatus}" escapeXml="false"/></td>
					</tr>
					<tr class="<unireg:nextRowClass/>">
						<td><fmt:message key="label.info.service.securite"/></td>
						<td id="serviceSecuriteStatus"><c:out value="${serviceSecuriteStatus}" escapeXml="false"/></td>
					</tr>
					<tr class="<unireg:nextRowClass/>">
						<td><fmt:message key="label.info.service.brvplus"/></td>
						<td id="bvrPlusStatus"><c:out value="${bvrPlusStatus}" escapeXml="false"/></td>
					</tr>
				</tbody>
			</table>
		</fieldset>

        <fieldset id="stats_fieldset" class="info">
            <legend><span><fmt:message key="title.info.stats" /></span></legend>
            <div id="stats_output" class="asciiart"><c:out value="${stats}" escapeXml="false"/></div>
        </fieldset>
        
		<fieldset class="info">
			<legend><span><fmt:message key="title.info.cache" /></span></legend>
			<c:out value="${cacheStatus}" escapeXml="false"/>
		</fieldset>
		
		<script>
			$(function() {

				// Chargement des batches en cours d'exécution
				Batch.loadRunning($('#jobsActif'), "3s", true);

				// Ajuste la taille des divs *_output par rapport à celles des divs *_fieldset, ceci pour
				// contourner une limitation du style 'overflow:auto' qui ne supporte pas des tailles relatives
				var divs = ['stats'];
				for (d in divs) {
					var fieldset = $('#' + divs[d] + '_fieldset');
					var output = $('#' + divs[d] + '_output');
					output.css('width', fieldset.prop('offsetWidth') - 20);
				}
			});
		</script>

  	</tiles:put>
</tiles:insert>
