<%@ page language="java" contentType="text/html; charset=UTF-8"	pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<tiles:insert template="/WEB-INF/jsp/templates/templateDialog.jsp">

	<tiles:put name="title">
		<fmt:message key="title.rapports.liste"/>
	</tiles:put>

	<tiles:put name="head">
		<style type="text/css">
			.pageheader {
				margin-top: 0px;
			}
		</style>
	</tiles:put>

	<tiles:put name="body">

		<!-- Table des rapports -->
		<fieldset>
			<legend><span><fmt:message key="label.dossiers.apparentes"/></span></legend>

			<div id="loadingSpinner" style="position:absolute;right:1.5em;width:24px;display:none"><img src="<c:url value="/images/loading.gif"/>"/></div>

			<table>
				<tr>
					<td width="25%">
						<input class="noprint" type="checkbox" id="isRapportHisto" <c:if test="${showHisto}">checked="true"</c:if>/>
						<label class="noprint" for="isRapportHisto"><fmt:message key="label.historique"/></label>
					</td>
					<td width="75%">&nbsp;</td>
				</tr>
				<tr>
					<td width="25%"><fmt:message key="label.type.rapport.entre.tiers"/>&nbsp;:</td>
					<td width="75%">
						<form name="form" id="form">
							<select name="typeRapport" id="typeRapportId">
									<option value="">Tous</option>
									<c:forEach var="rapport" items="${typesRapportTiers}">
										<option value="${rapport.key}" <c:if test="${rapportType == rapport.key}">selected</c:if>>${rapport.value}</option>
									</c:forEach>
							</select>
						</form>
					</td>
				</tr>
			</table>

			<c:if test="${empty rapports}">
				<fmt:message key="label.dossiers.apparentes.vide"/>
			</c:if>

			<c:if test="${not empty rapports}">

				<%-- détermine s'il existe  un rapport de type représentation conventionnelle --%>
				<c:set var="hasExtensionExecutionForcee" value="${false}"/>
				<c:set var="hasAutoriteTutelaire" value="${false}"/>
				<c:forEach items="${rapports}" var="rapport">
					<c:if test="${rapport.extensionExecutionForcee != null}">
						<c:set var="hasExtensionExecutionForcee" value="${true}"/>
					</c:if>
					<c:if test="${rapport.nomAutoriteTutelaire != null}">
						<c:set var="hasAutoriteTutelaire" value="${true}"/>
					</c:if>
				</c:forEach>


				<display:table name="${rapports}" id="rapport" class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator"
					partialList="true" size="${rapportsTotalCount}" pagesize="10" sort="external" requestURI="/rapport/list.do">
					<display:column sortable="true" titleKey="label.rapport.tiers" sortName="type">
							<%-- le type technique pour permettre de filtrer selon le type --%>
							<span style="display:none"><c:out value="${rapport.typeRapportEntreTiers}"/></span>
							<%-- le type humain pour l affichage --%>
							<fmt:message key="option.rapport.entre.tiers.${rapport.sensRapportEntreTiers}.${rapport.typeRapportEntreTiers}"/>
							<c:if test="${rapport.toolTipMessage != null}">
								<a href="#tooltip" class="staticTip" id="ret-${rapport_rowNum}">?</a>
								<div id="ret-${rapport_rowNum}-tooltip" style="display:none;">
									<c:out value="${rapport.toolTipMessage}"/>
								</div>
							</c:if>
					</display:column>

					<display:column sortable="true" titleKey="label.date.debut" sortName="dateDebut">
						<fmt:formatDate value="${rapport.dateDebut}" pattern="dd.MM.yyyy"/>
					</display:column>

					<display:column sortable="true" titleKey="label.date.fin" sortName="dateFin">
						<fmt:formatDate value="${rapport.dateFin}" pattern="dd.MM.yyyy"/>
					</display:column>

					<display:column sortable="true" titleKey="label.numero.tiers" sortName="tiersId">
						<c:if test="${rapport.numero != null}">
							<a href="../tiers/visu.do?id=${rapport.numero}&rid=${tiersGeneral.numero}"><unireg:numCTB numero="${rapport.numero}"></unireg:numCTB></a>
						</c:if>
						<c:if test="${rapport.numero == null && rapport.messageNumeroAbsent != null}">
							<div class="flash-warning"><c:out value="${rapport.messageNumeroAbsent}"/></div>
						</c:if>
					</display:column>
					<display:column titleKey="label.nom.raison">
						<c:if test="${rapport.nomCourrier1 != null}">
							<c:out value="${rapport.nomCourrier1}"/>
						</c:if>
						<c:if test="${rapport.nomCourrier2 != null}">
							<br/><c:out value="${rapport.nomCourrier2}"/>
						</c:if>
					</display:column>
					<c:if test="${hasAutoriteTutelaire}">
						<display:column sortable="true" titleKey="label.autorite.tutelaire" property="nomAutoriteTutelaire" sortName="autoriteTutelaireId"/>
					</c:if>
					<c:if test="${hasExtensionExecutionForcee}">
						<display:column sortable="true" titleKey="label.extension.execution.forcee" sortName="extensionExecutionForcee">
								<c:if test="${rapport.extensionExecutionForcee != null}">
									<input type="checkbox" <c:if test="${rapport.extensionExecutionForcee}">checked="true"</c:if> disabled="true"/>
								</c:if>
						</display:column>
					</c:if>

					<display:column style="action">
						<c:if test="${rapport.typeRapportEntreTiers != 'FILIATION'}">
							<unireg:consulterLog entityNature="RapportEntreTiers" entityId="${rapport.id}"/>
						</c:if>
					</display:column>
					<display:setProperty name="paging.banner.all_items_found" value=""/>
					<display:setProperty name="paging.banner.one_item_found" value=""/>
				</display:table>
			</c:if>

		</fieldset>
		
		<script>
			function refresh_rapports_table() {
				var showHisto = $('#isRapportHisto').attr('checked') ? 'true' : 'false';
	            var type = $('#typeRapportId').val();
				$('#loadingSpinner').show();
				$('#dossiersApparentesDiv').load('../rapport/list.do?tiers=${tiersId}&showHisto=' + showHisto + '&type=' + type + '&' + new Date().getTime());
			}

			$(function() {
				activate_static_tooltips();
				$('#isRapportHisto').click(refresh_rapports_table);
				$('#typeRapportId').bind('change keyup',refresh_rapports_table);

				// on change le comportement des liens de pagination : au lieu de charger une nouvelle page, on lance
				// une requête ajax pour charger la nouvelle page dans l onglet courant.
				$('#dossiersApparentesDiv td.pagelinks a, #rapport th.sortable a').each(function(a) {
					//alert($(this).attr('href'));
					$(this).click(function() {
						$('#loadingSpinner').show();
						$('#dossiersApparentesDiv').load($(this).attr('href'));
						return false;
					});
				});
			});
		</script>

		<!-- Table des filiations -->
		<c:if test="${not empty filiations}">
			<fieldset>
				<legend><span><fmt:message key="label.filiations"/></span></legend>

				<display:table name="${filiations}" id="filiation" class="display" pagesize="10" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
					<display:column titleKey="label.rapport.tiers">
							<fmt:message key="option.rapport.entre.tiers.${filiation.sensRapportEntreTiers}.${filiation.typeRapportEntreTiers}"/>
							<c:if test="${filiation.toolTipMessage != null}">
								<a href="#tooltip" class="staticTip" id="fil-${filiation_rowNum}">?</a>
								<div id="fil-${filiation_rowNum}-tooltip" style="display:none;">
									<c:out value="${filiation.toolTipMessage}"/>
								</div>
							</c:if>
					</display:column>

					<display:column titleKey="label.date.debut">
						<fmt:formatDate value="${filiation.dateDebut}" pattern="dd.MM.yyyy"/>
					</display:column>

					<display:column titleKey="label.date.fin">
						<fmt:formatDate value="${filiation.dateFin}" pattern="dd.MM.yyyy"/>
					</display:column>

					<display:column titleKey="label.numero.tiers">
						<c:if test="${filiation.numero != null}">
							<a href="../tiers/visu.do?id=${filiation.numero}&rid=${tiersGeneral.numero}"><unireg:numCTB numero="${filiation.numero}"></unireg:numCTB></a>
						</c:if>
						<c:if test="${filiation.numero == null && filiation.messageNumeroAbsent != null}">
							<div class="flash-warning"><c:out value="${filiation.messageNumeroAbsent}"/></div>
						</c:if>
					</display:column>
					<display:column titleKey="label.nom.raison">
						<c:if test="${filiation.nomCourrier1 != null}">
							<c:out value="${filiation.nomCourrier1}"/>
						</c:if>
						<c:if test="${filiation.nomCourrier2 != null}">
							<br/><c:out value="${filiation.nomCourrier2}"/>
						</c:if>
					</display:column>
					<display:setProperty name="paging.banner.all_items_found" value=""/>
					<display:setProperty name="paging.banner.one_item_found" value=""/>
				</display:table>

			</fieldset>
		</c:if>

		<c:if test="${not empty debiteurs}">
			<fieldset>
				<legend><span><fmt:message key="label.debiteur.is"/></span></legend>
				
				<display:table name="debiteurs" id="debiteur" pagesize="10" class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
					<display:column sortable="true" titleKey="label.numero.debiteur" href="visu.do" paramId="id" paramProperty="numero" sortProperty="numero">
						<a href="../tiers/visu.do?id=${debiteur.numero}"><unireg:numCTB numero="${debiteur.numero}"></unireg:numCTB></a>
					</display:column>
					<display:column sortable="true" titleKey="label.nom.raison">
						<c:if test="${debiteur.nomCourrier1 != null}">
							<c:out value="${debiteur.nomCourrier1}"/>
						</c:if>
						<c:if test="${debiteur.nomCourrier2 != null}">
							<br/><c:out value="${debiteur.nomCourrier2}"/>
						</c:if>
						<c:if test="${debiteur.complementNom != null}">
							<br/><c:out value="${debiteur.complementNom}"/>
						</c:if>
					</display:column>
					<display:column sortable="true" titleKey="label.categorie.is">
						<fmt:message key="option.categorie.impot.source.${debiteur.categorieImpotSource}"/>
					</display:column>
					<display:column sortable="true" property="personneContact" titleKey="label.contact"/>
					<display:column style="action">
						<unireg:consulterLog entityNature="RapportEntreTiers" entityId="${debiteur.id}"/>
					</display:column>
					<display:setProperty name="paging.banner.all_items_found" value=""/>
					<display:setProperty name="paging.banner.one_item_found" value=""/>
				</display:table>
			</fieldset>
		</c:if>

	</tiles:put>

</tiles:insert>
