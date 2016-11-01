<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<!-- Debut Etiquettes -->
<unireg:setAuth var="autorisations" tiersId="${command.tiers.numero}"/>
<c:if test="${autorisations.etiquettes}">
	<table border="0">
		<tr>
			<td>
				<c:if test="${empty param['message'] && empty param['retour']}">
					<unireg:raccourciModifier link="../etiquette/edit-list.do?tiersId=${command.tiers.numero}" tooltip="Modifier les étiquettes" display="label.bouton.modifier"/>
				</c:if>
			</td>
		</tr>
	</table>
</c:if>

<fieldset>
	<c:choose>
		<c:when test="${command.natureTiers == 'MenageCommun'}">
			<legend><span>
				<fmt:message key="label.etiquettes.tiers.nomme">
					<fmt:param value="${command.nomPrenomPrincipal}"/>
				</fmt:message>
			</span></legend>
		</c:when>
		<c:otherwise>
			<legend><span><fmt:message key="label.etiquettes" /></span></legend>
		</c:otherwise>
	</c:choose>

	<c:choose>
		<c:when test="${not empty command.etiquettes}">

			<input class="noprint" name="etiq-histo-prn" type="checkbox" onClick="Histo.toggleRowsIsHistoFromClass('etiq-prn','etiq-histo-prn', 'histo-only');" id="etiq-histo-prn" />
			<label class="noprint" for="etiq-histo-prn"><fmt:message key="label.historique" /></label>

			<unireg:nextRowClass reset="1"/>
			<display:table name="command.etiquettes" id="etiquette" htmlId="etiq-prn" sort="list" class="display" decorator="ch.vd.uniregctb.decorator.TableAnnuableDateRangeDecorator">
				<display:column sortable="true" titleKey="label.libelle" sortProperty="libelle" style="width: 20%;">
					<c:out value="${etiquette.libelle}"/>
				</display:column>
				<display:column sortable="true" titleKey="label.date.debut" sortProperty="dateDebut" style="width: 15%;">
					<unireg:regdate regdate="${etiquette.dateDebut}" format="dd.MM.yyyy"/>
				</display:column>
				<display:column sortable="true" titleKey="label.date.fin" sortProperty="dateFin" style="width: 15%;">
					<unireg:regdate regdate="${etiquette.dateFin}" format="dd.MM.yyyy"/>
				</display:column>
				<display:column sortable="false" titleKey="label.commentaire">
					<c:out value="${etiquette.commentaire}"/>
				</display:column>
				<display:column class="action" style="width: 10%;">
					<unireg:consulterLog entityNature="Etiquette" entityId="${etiquette.id}"/>
				</display:column>

				<display:setProperty name="paging.banner.all_items_found" value=""/>
				<display:setProperty name="paging.banner.one_item_found" value=""/>
			</display:table>

			<script type="text/javascript">
				$(function() {
					// premier calcul des lignes à cacher par défaut
					Histo.toggleRowsIsHistoFromClass('etiq-prn','etiq-histo-prn', 'histo-only');
				});
			</script>

		</c:when>
		<c:otherwise>
			<span style="font-style: italic;"><fmt:message key="label.aucune.etiquette.trouvee"/></span>
		</c:otherwise>
	</c:choose>

</fieldset>

<!-- Cas du conjoint pour les ménages communs -->
<c:if test="${command.natureTiers == 'MenageCommun' && command.nomPrenomConjoint != null}">

	<fieldset>
		<legend><span>
			<fmt:message key="label.etiquettes.tiers.nomme">
				<fmt:param value="${command.nomPrenomConjoint}"/>
			</fmt:message>
		</span></legend>

		<c:choose>
			<c:when test="${not empty command.etiquettesConjoint}">

				<input class="noprint" name="etiq-histo-cjt" type="checkbox" onClick="Histo.toggleRowsIsHistoFromClass('etiq-cjt','etiq-histo-cjt', 'histo-only');" id="etiq-histo-cjt" />
				<label class="noprint" for="etiq-histo-cjt"><fmt:message key="label.historique" /></label>

				<unireg:nextRowClass reset="1"/>
				<display:table name="command.etiquettesConjoint" id="etiquette" htmlId="etiq-cjt" sort="list" class="display" decorator="ch.vd.uniregctb.decorator.TableAnnuableDateRangeDecorator">
					<display:column sortable="true" titleKey="label.libelle" sortProperty="libelle" style="width: 20%;">
						<c:out value="${etiquette.libelle}"/>
					</display:column>
					<display:column sortable="true" titleKey="label.date.debut" sortProperty="dateDebut" style="width: 15%;">
						<unireg:regdate regdate="${etiquette.dateDebut}" format="dd.MM.yyyy"/>
					</display:column>
					<display:column sortable="true" titleKey="label.date.fin" sortProperty="dateFin" style="width: 15%;">
						<unireg:regdate regdate="${etiquette.dateFin}" format="dd.MM.yyyy"/>
					</display:column>
					<display:column sortable="false" titleKey="label.commentaire">
						<c:out value="${etiquette.commentaire}"/>
					</display:column>
					<display:column class="action" style="width: 10%;">
						<unireg:consulterLog entityNature="Etiquette" entityId="${etiquette.id}"/>
					</display:column>

					<display:setProperty name="paging.banner.all_items_found" value=""/>
					<display:setProperty name="paging.banner.one_item_found" value=""/>
				</display:table>

				<script type="text/javascript">
					$(function() {
						// premier calcul des lignes à cacher par défaut
						Histo.toggleRowsIsHistoFromClass('etiq-cjt','etiq-histo-cjt', 'histo-only');
					});
				</script>

			</c:when>
			<c:otherwise>
				<span style="font-style: italic;"><fmt:message key="label.aucune.etiquette.trouvee"/></span>
			</c:otherwise>
		</c:choose>

	</fieldset>

</c:if>

<!-- Fin Etiquettes -->
		

		

		