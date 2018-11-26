<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

	<tiles:put name="title">
		<fmt:message key="title.param.periode.fiscale" />
	</tiles:put>
	<tiles:put name="fichierAide">
		<li>
			<a href="#" onClick="ouvrirAide('<c:url value='/docs/parametrage-periode-fiscale.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
		</li>
	</tiles:put>
	<tiles:put name="head">
		<fmt:message key="label.param.confirm.init" var="confirmInit"/>
		<fmt:message key="label.param.confirm.suppr" var="confirmSuppr"/>
		<style type="text/css">
			a.edit, div.button-add, div.checkbox {
				margin: 10px;
			}
			.information {
				width: auto;
			}
			div.checkbox {
				float: right;
			}
			div.emolument {
				text-align: center;
			}

			.colonneTitreParametres {
				width: 35%;
			}

			.colonneModele{
				width: 25%;
			}

			.colonneModeleAction {
				padding-right: 13px;
				width: 25%;
				text-align: right;
			}

			.colonneFeuille{
				width: 17%;
			}

			.colonneFeuilleAction {
				width: 15%;
				text-align: right;
			}

			input[type=checkbox] {
				vertical-align: bottom;
			}
		</style>

		<script type="text/javascript">
			$(document).ready(function() {
				/*
				 * Event Handlers
				 */
				$("select").change( function() {
					$("form").submit();
				});
				$("#initPeriodeFiscale").click( function () {
					return confirm("${confirmInit}");
				});
				$("a.delete").click( function () {
					return confirm("${confirmSuppr}");
				});
			});
		</script>

	</tiles:put>
	<tiles:put name="body">
		<form method="get" id="form" action="list.do">
			<fieldset class="information"><legend><fmt:message key="label.param.periodes"/></legend>
				<div style="margin-top: 5px">
					<fmt:message key="label.param.periode.select"/>:
					<select id="periode" name="pf">
						<%--@elvariable id="periodes" type="java.util.List"--%>
						<c:forEach var="periode" items="${periodes}">
							<c:set var="selected" value=""/>
							<%--@elvariable id="periodeSelectionnee" type="ch.vd.unireg.declaration.PeriodeFiscale"--%>
							<c:if test="${periode.id == periodeSelectionnee.id }">
								<c:set var="selected">
									selected="selected"
								</c:set>
							</c:if>
							<option value="${periode.id}" ${selected}>${periode.annee}</option>
						</c:forEach>
					</select>
				</div>

				<div class="button-add">
					<unireg:raccourciAjouter id="initPeriodeFiscale" link="init-periode.do" tooltip="label.param.init.periode" display="label.param.init.periode"/>
				</div>

				<fmt:message key="label.param.periode.arg" var="titleParametres">
					<fmt:param value="${periodeSelectionnee.annee}" />
				</fmt:message>

				<fieldset style="margin: 10px" class="information">
					<%--@elvariable id="parametrePeriodeFiscalePPVaud" type="ch.vd.unireg.parametrage.ParametrePeriodeFiscalePP"--%>
					<%--@elvariable id="parametrePeriodeFiscalePPHorsCanton" type="ch.vd.unireg.parametrage.ParametrePeriodeFiscalePP"--%>
					<%--@elvariable id="parametrePeriodeFiscalePPHorsSuisse" type="ch.vd.unireg.parametrage.ParametrePeriodeFiscalePP"--%>
					<%--@elvariable id="parametrePeriodeFiscalePPDepense" type="ch.vd.unireg.parametrage.ParametrePeriodeFiscalePP"--%>
					<%--@elvariable id="parametrePeriodeFiscalePPDiplomateSuisse" type="ch.vd.unireg.parametrage.ParametrePeriodeFiscalePP"--%>
					<legend>
						<fmt:message key="label.param.parametres.personnes.physiques"/>
					</legend>
					<a href="pf-edit-pp.do?pf=${periodeSelectionnee.id}" class="edit" title="${titleParametres}"><fmt:message key="label.param.edit"/>&nbsp;</a>

					<div class="checkbox">
						<input type="checkbox" disabled="disabled" <%--@elvariable id="codeControleSurSommationDIPP" type="java.lang.Boolean"--%>
						<c:if test="${codeControleSurSommationDIPP}">checked</c:if>/>
						<fmt:message key="label.param.code.controle.sur.sommation.pp"/>
					</div>
						<%--@elvariable id="parametrePeriodeFiscaleEmomulementSommationDIPP" type="ch.vd.unireg.parametrage.ParametrePeriodeFiscaleEmolument"--%>
					<c:if test="${parametrePeriodeFiscaleEmomulementSommationDIPP.montant != null}">
						<div class="emolument">
							<fmt:message key="label.param.emolument.sommation"/>&nbsp;:
							<span style="font-weight: bold;"><c:out value="${parametrePeriodeFiscaleEmomulementSommationDIPP.montant}"/></span>
							<fmt:message key="label.chf"/>
						</div>
					</c:if>

					<table>
						<tr>
							<th class="colonneTitreParametres">&nbsp;</th>
							<th><fmt:message key="label.param.entete.VD"/></th>
							<th><fmt:message key="label.param.entete.HC"/></th>
							<th><fmt:message key="label.param.entete.HS"/></th>
							<th><fmt:message key="label.param.entete.dep"/></th>
							<th><fmt:message key="label.param.entete.DS"/></th>
						</tr>
						<tr>
							<th><fmt:message key="label.param.som.reg"/></th>
							<td>
								<unireg:date date="${parametrePeriodeFiscalePPVaud.termeGeneralSommationReglementaire}"/>
							</td>
							<td>
								<unireg:date date="${parametrePeriodeFiscalePPHorsCanton.termeGeneralSommationReglementaire}"/>
							</td>
							<td>
								<unireg:date date="${parametrePeriodeFiscalePPHorsSuisse.termeGeneralSommationReglementaire}"/>
							</td>
							<td>
								<unireg:date date="${parametrePeriodeFiscalePPDepense.termeGeneralSommationReglementaire}"/>
							</td>
							<td>
								<unireg:date date="${parametrePeriodeFiscalePPDiplomateSuisse.termeGeneralSommationReglementaire}"/>
							</td>
						</tr>
						<tr>
							<th><fmt:message key="label.param.som.eff"/></th>
							<td>
								<unireg:date date="${parametrePeriodeFiscalePPVaud.termeGeneralSommationEffectif}"/>
							</td>
							<td>
								<unireg:date date="${parametrePeriodeFiscalePPHorsCanton.termeGeneralSommationEffectif}"/>
							</td>
							<td>
								<unireg:date date="${parametrePeriodeFiscalePPHorsSuisse.termeGeneralSommationEffectif}"/>
							</td>
							<td>
								<unireg:date date="${parametrePeriodeFiscalePPDepense.termeGeneralSommationEffectif}"/>
							</td>
							<td>
								<unireg:date date="${parametrePeriodeFiscalePPDiplomateSuisse.termeGeneralSommationEffectif}"/>
							</td>
						</tr>
						<tr>
							<th><fmt:message key="label.param.masse.di"/></th>
							<td>
								<unireg:date date="${parametrePeriodeFiscalePPVaud.dateFinEnvoiMasseDI}"/>
							</td>
							<td>
								<unireg:date date="${parametrePeriodeFiscalePPHorsCanton.dateFinEnvoiMasseDI}"/>
							</td>
							<td>
								<unireg:date date="${parametrePeriodeFiscalePPHorsSuisse.dateFinEnvoiMasseDI}"/>
							</td>
							<td>
								<unireg:date date="${parametrePeriodeFiscalePPDepense.dateFinEnvoiMasseDI}"/>
							</td>
							<td>
								<unireg:date date="${parametrePeriodeFiscalePPDiplomateSuisse.dateFinEnvoiMasseDI}"/>
							</td>
						</tr>
					</table>

				</fieldset>

				<fieldset style="margin: 10px" class="information">
					<%--@elvariable id="parametrePeriodeFiscalePMVaud" type="ch.vd.unireg.parametrage.ParametrePeriodeFiscalePM"--%>
					<%--@elvariable id="parametrePeriodeFiscalePMHorsCanton" type="ch.vd.unireg.parametrage.ParametrePeriodeFiscalePM"--%>
					<%--@elvariable id="parametrePeriodeFiscalePMHorsSuisse" type="ch.vd.unireg.parametrage.ParametrePeriodeFiscalePM"--%>
					<%--@elvariable id="parametrePeriodeFiscalePMUtilitePublique" type="ch.vd.unireg.parametrage.ParametrePeriodeFiscalePM"--%>
					<legend>
						<fmt:message key="label.param.parametres.personnes.morales"/>
					</legend>
					<a href="pf-edit-pm.do?pf=${periodeSelectionnee.id}" class="edit" title="${titleParametres}"><fmt:message key="label.param.edit"/>&nbsp;</a>

					<div class="checkbox">
						<input type="checkbox" disabled="disabled" <%--@elvariable id="codeControleSurSommationDIPM" type="java.lang.Boolean"--%>
						<c:if test="${codeControleSurSommationDIPM}">checked</c:if>/>
						<fmt:message key="label.param.code.controle.sur.sommation.pm"/>
					</div>

					<table>
						<tr>
							<th class="colonneTitreParametres">&nbsp;</th>
							<th><fmt:message key="label.param.entete.VD"/></th>
							<th><fmt:message key="label.param.entete.HC"/></th>
							<th><fmt:message key="label.param.entete.HS"/></th>
							<th><fmt:message key="label.param.entete.utilite.publique"/></th>
						</tr>
						<tr>
							<th><fmt:message key="label.param.pm.delai.imprime"/></th>
							<td>
								<c:if test="${parametrePeriodeFiscalePMVaud.delaiImprimeMois != null}">
									${parametrePeriodeFiscalePMVaud.delaiImprimeMois}
									<fmt:message key="label.param.pm.delai.mois"/>
									<fmt:message key="option.reference.delai.${parametrePeriodeFiscalePMVaud.referenceDelaiInitial}"/>
									<c:if test="${parametrePeriodeFiscalePMVaud.delaiImprimeRepousseFinDeMois}">
										(<fmt:message key="label.param.pm.report.fin.mois"/>)
									</c:if>
								</c:if>
							</td>
							<td>
								<c:if test="${parametrePeriodeFiscalePMHorsCanton.delaiImprimeMois != null}">
									${parametrePeriodeFiscalePMHorsCanton.delaiImprimeMois}
									<fmt:message key="label.param.pm.delai.mois"/>
									<fmt:message key="option.reference.delai.${parametrePeriodeFiscalePMHorsCanton.referenceDelaiInitial}"/>
									<c:if test="${parametrePeriodeFiscalePMHorsCanton.delaiImprimeRepousseFinDeMois}">
										(<fmt:message key="label.param.pm.report.fin.mois"/>)
									</c:if>
								</c:if>
							</td>
							<td>
								<c:if test="${parametrePeriodeFiscalePMHorsSuisse.delaiImprimeMois != null}">
									${parametrePeriodeFiscalePMHorsSuisse.delaiImprimeMois}
									<fmt:message key="label.param.pm.delai.mois"/>
									<fmt:message key="option.reference.delai.${parametrePeriodeFiscalePMHorsSuisse.referenceDelaiInitial}"/>
									<c:if test="${parametrePeriodeFiscalePMHorsSuisse.delaiImprimeRepousseFinDeMois}">
										(<fmt:message key="label.param.pm.report.fin.mois"/>)
									</c:if>
								</c:if>
							</td>
							<td>
								<c:if test="${parametrePeriodeFiscalePMUtilitePublique.delaiImprimeMois != null}">
									${parametrePeriodeFiscalePMUtilitePublique.delaiImprimeMois}
									<fmt:message key="label.param.pm.delai.mois"/>
									<fmt:message key="option.reference.delai.${parametrePeriodeFiscalePMUtilitePublique.referenceDelaiInitial}"/>
									<c:if test="${parametrePeriodeFiscalePMUtilitePublique.delaiImprimeRepousseFinDeMois}">
										(<fmt:message key="label.param.pm.report.fin.mois"/>)
									</c:if>
								</c:if>
							</td>
						</tr>
						<tr>
							<th><fmt:message key="label.param.pm.delai.tolerance"/></th>
							<td>
								<c:if test="${parametrePeriodeFiscalePMVaud.delaiToleranceJoursEffective != null}">
									${parametrePeriodeFiscalePMVaud.delaiToleranceJoursEffective}
									<c:choose>
										<c:when test="${parametrePeriodeFiscalePMVaud.delaiToleranceJoursEffective == 1}">
											<fmt:message key="label.param.pm.delai.jour"/>
										</c:when>
										<c:otherwise>
											<fmt:message key="label.param.pm.delai.jours"/>
										</c:otherwise>
									</c:choose>
									<c:if test="${parametrePeriodeFiscalePMVaud.delaiTolereRepousseFinDeMois}">
										(<fmt:message key="label.param.pm.report.fin.mois"/>)
									</c:if>
								</c:if>
							</td>
							<td>
								<c:if test="${parametrePeriodeFiscalePMHorsCanton.delaiToleranceJoursEffective != null}">
									${parametrePeriodeFiscalePMHorsCanton.delaiToleranceJoursEffective}
									<c:choose>
										<c:when test="${parametrePeriodeFiscalePMHorsCanton.delaiToleranceJoursEffective == 1}">
											<fmt:message key="label.param.pm.delai.jour"/>
										</c:when>
										<c:otherwise>
											<fmt:message key="label.param.pm.delai.jours"/>
										</c:otherwise>
									</c:choose>
									<c:if test="${parametrePeriodeFiscalePMHorsCanton.delaiTolereRepousseFinDeMois}">
										(<fmt:message key="label.param.pm.report.fin.mois"/>)
									</c:if>
								</c:if>
							</td>
							<td>
								<c:if test="${parametrePeriodeFiscalePMHorsSuisse.delaiToleranceJoursEffective != null}">
									${parametrePeriodeFiscalePMHorsSuisse.delaiToleranceJoursEffective}
									<c:choose>
										<c:when test="${parametrePeriodeFiscalePMHorsSuisse.delaiToleranceJoursEffective == 1}">
											<fmt:message key="label.param.pm.delai.jour"/>
										</c:when>
										<c:otherwise>
											<fmt:message key="label.param.pm.delai.jours"/>
										</c:otherwise>
									</c:choose>
									<c:if test="${parametrePeriodeFiscalePMHorsSuisse.delaiTolereRepousseFinDeMois}">
										(<fmt:message key="label.param.pm.report.fin.mois"/>)
									</c:if>
								</c:if>
							</td>
							<td>
								<c:if test="${parametrePeriodeFiscalePMUtilitePublique.delaiToleranceJoursEffective != null}">
									${parametrePeriodeFiscalePMUtilitePublique.delaiToleranceJoursEffective}
									<c:choose>
										<c:when test="${parametrePeriodeFiscalePMUtilitePublique.delaiToleranceJoursEffective == 1}">
											<fmt:message key="label.param.pm.delai.jour"/>
										</c:when>
										<c:otherwise>
											<fmt:message key="label.param.pm.delai.jours"/>
										</c:otherwise>
									</c:choose>
									<c:if test="${parametrePeriodeFiscalePMUtilitePublique.delaiTolereRepousseFinDeMois}">
										(<fmt:message key="label.param.pm.report.fin.mois"/>)
									</c:if>
								</c:if>
							</td>
						</tr>
					</table>

				</fieldset>

				<fieldset style="margin: 10px" class="information">
					<%--@elvariable id="parametrePeriodeFiscaleSNC" type="ch.vd.unireg.parametrage.ParametrePeriodeFiscaleSNC"--%>
					<legend>
						<fmt:message key="label.param.parametres.questionnaires.snc"/>
					</legend>
					<a href="pf-edit-snc.do?pf=${periodeSelectionnee.id}" class="edit" title="${titleParametres}"><fmt:message key="label.param.edit"/>&nbsp;</a>
					<div class="checkbox">
						<input type="checkbox" disabled="disabled" <%--@elvariable id="codeControleSurRappelQSNC" type="java.lang.Boolean"--%>
						<c:if test="${codeControleSurRappelQSNC}">checked</c:if>/>
						<fmt:message key="label.param.code.controle.sur.rappel.snc"/>
					</div>
					<table>
						<tr>
							<th class="colonneTitreParametres"><fmt:message key="label.param.rappel.reg"/></th>
							<td>
								<unireg:date date="${parametrePeriodeFiscaleSNC.termeGeneralRappelImprime}"/>
							</td>
						</tr>
						<tr>
							<th><fmt:message key="label.param.rappel.eff"/></th>
							<td>
								<unireg:date date="${parametrePeriodeFiscaleSNC.termeGeneralRappelEffectif}"/>
							</td>
						</tr>
					</table>

				</fieldset>

				<fieldset style="margin: 10px" class="information">
					<legend><fmt:message key="label.param.modele"/></legend>
					<div class="button-add">
						<a href="modele-add.do?pf=${periodeSelectionnee.id}" class="add" title="${titleParametres}"><fmt:message key="label.param.add"/></a>
					</div>
					<table>
						<tr>
							<th class="colonneModele"><fmt:message key="title.param.periode" /></th>
							<th class="colonneModele"><fmt:message key="title.param.type"/></th>
							<th class="colonneModele">&nbsp;</th>
							<th class="colonneModeleAction"><fmt:message key="title.param.action"/></th>
						</tr>
						<%--@elvariable id="modeles" type="java.util.List"--%>
						<c:forEach var="modele" items="${modeles}">
							<tr class="odd">
								<td >${periodeSelectionnee.annee}</td>
								<td ><fmt:message key="option.type.document.${modele.typeDocument}"/></td>
								<td >
									<c:if test="${not empty error_modele[modele.id]}">
										<span class="error">${error_modele[modele.id]}</span>
									</c:if>&nbsp;
								</td>
								<td style="" class="colonneModeleAction">
									<unireg:raccourciAnnuler link="modele-suppr.do?md=${modele.id}&pf=${periodeSelectionnee.id}"/>
								</td>
							</tr>
						</c:forEach>
					</table>
					<c:if test="${not empty modeles}">
						<div style="margin-top: 5px"><fmt:message key="label.param.modele.select"/>:
							<select name="md" id="modele">
								<c:forEach var="modele" items="${modeles}">
									<c:set var="selected" value=""/>
									<%--@elvariable id="modeleSelectionne" type="ch.vd.unireg.declaration.ModeleDocument"--%>
									<c:if test="${modele.id == modeleSelectionne.id }">
										<c:set var="selected">
											selected="selected"
										</c:set>
									</c:if>
									<option value="${modele.id}" ${selected}>
										<fmt:message key="option.type.document.${modele.typeDocument}"/>
									</option>
								</c:forEach>
							</select>
						</div>

						<fieldset style="margin: 10px" class="information">
							<legend><fmt:message key="title.param.modele.feuille"/></legend>
							<table border="0">
								<tr>
									<td>
										<fmt:message key="option.type.document.${modeleSelectionne.typeDocument}" var="libTypeDocument"/>
										<fmt:message key="label.param.periode.et.modele" var="periodeEtModele">
											<fmt:param value="${periodeSelectionnee.annee}" />
											<fmt:param value="${libTypeDocument}" />
										</fmt:message>
										<a href="feuille/add.do?pf=${periodeSelectionnee.id}&md=${modeleSelectionne.id}" class="add" title="${periodeEtModele}"><fmt:message key="label.param.add"/></a>
									</td>
									<td width="25%">&nbsp;</td>
									<td width="25%">&nbsp;</td>
									<td width="25%">&nbsp;</td>
									<td width="25%">&nbsp;</td>
								</tr>
							</table>
							<table>
								<tr>
									<th class="colonneFeuille"><fmt:message key="title.param.periode" /></th>
									<th class="colonneFeuille"><fmt:message key="title.param.num.cadev"/></th>
									<th class="colonneFeuille"><fmt:message key="title.param.num.form.aci"/></th>
									<th class="colonneFeuille"><fmt:message key="title.param.int.feuille"/></th>
									<th class="colonneFeuille">&nbsp;</th>
									<th class="colonneFeuilleAction" ><fmt:message key="title.param.action"/></th>
								</tr>
								<%--@elvariable id="feuilles" type="java.util.List"--%>
								<c:forEach var="feuille" varStatus="i" items="${feuilles}">
									<tr class="odd">
										<td class="colonneFeuille">${periodeSelectionnee.annee}</td>
										<td class="colonneFeuille">${feuille.noCADEV}</td>
										<td class="colonneFeuille">${feuille.noFormulaireACI}</td>
										<td class="colonneFeuille">
											<c:out value="${feuille.intituleFeuille}"/>
											<c:if test="${feuille.principal}">
												<span class="info_icon" style="margin-left: 1em;" title="Feuillet principal"></span>
											</c:if>
										</td>
										<td class="colonneFeuille">
											<c:if test="${not empty error_feuille[feuille.id]}">
												<span class="error">${error_feuille[feuille.id]}</span>
											</c:if>&nbsp;
										</td>
										<td class="colonneFeuilleAction" class="colonneAction">
											<c:if test="${i.index > 0}">
												<unireg:raccourciMoveUp link="feuille/move.do?mfd=${feuille.id}&dir=UP" tooltip="Monte d'un cran la feuille"/>
												<c:if test="${i.index == fn:length(feuilles) - 1}">
													<a href="#" class="padding noprint">&nbsp;</a>
												</c:if>
											</c:if>
											<c:if test="${i.index < fn:length(feuilles) - 1}">
												<unireg:raccourciMoveDown link="feuille/move.do?mfd=${feuille.id}&dir=DOWN" tooltip="Descend d'un cran la feuille"/>
											</c:if>
											<unireg:raccourciModifier link="feuille/edit.do?pf=${periodeSelectionnee.id}&md=${modeleSelectionne.id}&mfd=${feuille.id}" tooltip="${periodeEtModele}"/>
											<unireg:raccourciAnnuler link="feuille/suppr.do?pf=${periodeSelectionnee.id}&md=${modeleSelectionne.id}&mfd=${feuille.id}"/>
										</td>
									</tr>
								</c:forEach>
							</table>
						</fieldset>
					</c:if>
				</fieldset>
			</fieldset>
		</form>
	</tiles:put>
</tiles:insert>