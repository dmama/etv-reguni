<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<!-- Debut CTB associé + Rapports prestation -->
<c:if test="${command.allowedOnglet.RPT}">
	<table border="0">
		<tr>
			<td>
				<unireg:raccourciModifier link="../rapports-prestation/edit.do?id=${command.tiers.numero}" tooltip="Modifier les rapports de prestation" display="label.bouton.modifier"/>
			</td>
		</tr>
	</table>
</c:if>

<fieldset>
	<legend><span><fmt:message key="label.contribuable.associe" /></span></legend>
	
	<input class="noprint" name="rt_histo" type="checkbox" onClick="Histo.toggleRowsIsActif('contribuableAssocie','isCtbAssoHisto', 0);" id="isCtbAssoHisto" />
	<fmt:message key="label.historique" />
		
	<jsp:include page="../common/contribuable-associe.jsp">
		<jsp:param name="page" value="visu"/>
	</jsp:include>
</fieldset>

<fieldset>
	<legend><span><fmt:message key="label.rapports.prestation" /></span></legend>

	<script type="text/javascript">
		function toggleBooleanParam(url, name, default_value){
			var regexp = new RegExp(name + "=([a-z]*)", "i");
			var match = regexp.exec(url);
			if (match == null) {
				// le paramètre n'existe pas, on l'ajoute
				var newUrl = new String(url);

				if (newUrl.charAt(newUrl.length - 1) == '#') { // supprime le trailing # si nécessaire
					newUrl = newUrl.substr(0, newUrl.length - 1);
				}
				return newUrl + '&' + name + '=' + default_value;
			}
			else {
				// le paramètre existe, on toggle sa valeur
				var oldvalue = (match[1] == 'true');
				var newvalue = !oldvalue;
				var param = name + "=" + newvalue;
				var newUrl = new String(url);
				newUrl = newUrl.replace(regexp, param);

				if (!newvalue) {
					// on recommence à la première page lorsqu'on passe de la liste complète à la liste partielle
					newUrl = newUrl.replace(/-p=[0-9]*/, "-p=1");
				}

				return newUrl;
			}
		}
	</script>


	<table border="0">
		<tr>
			<td>
				<input class="noprint" name="rapportsPrestationHisto" type="checkbox" id="rapportsPrestationHisto" <c:if test="${command.rapportsPrestationHisto}">checked</c:if> onClick="window.location = toggleBooleanParam(window.location, 'rapportsPrestationHisto', true)" />
				<label class="noprint" for="rapportsPrestationHisto"><fmt:message key="label.historique" /></label>
			</td>

			<td id="timeline" align="right">
				<a href="<c:url value="/rapports-prestation/list.do?idDpi=" /><c:out value="${command.tiers.numero}" />" >Liste complète</a>
			</td>
		</tr>
	</table>

	<jsp:include page="../common/rapports-prestation.jsp">
		<jsp:param name="page" value="visu"/>
	</jsp:include>
</fieldset>

<!-- Fin CTB associé + Rapports prestation -->
