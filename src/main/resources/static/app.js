"use strict";
const reports = {
  "trazabilidad": {
    title: "Detalle de pedidos",
    description: "Consulta los productos de cada pedido, sus cantidades y el importe de cada línea.",
    note: "Cada fila representa un detalle de pedido. Se incluyen todos los estados.",
    columns: [["idPedido","Pedido","id"],["producto","Producto","product"],["categoria","Categoría"],["fecha","Fecha"],["estado","Estado","badge"],["cantidad","Cantidad","number"],["precioUnitario","Precio unitario","money"],["subtotal","Subtotal","money"]]
  },
  "metricas-categorias": {
    title: "Métricas por categoría",
    description: "Compara el importe de los pedidos y el rendimiento de las categorías con actividad recurrente.",
    note: "Se muestran categorías con al menos 3 pedidos. El promedio, máximo y mínimo corresponden a subtotales de líneas.",
    columns: [["categoria","Categoría","product"],["totalPedidos","Pedidos","number"],["totalImporte","Importe total","money"],["promedioSubtotal","Promedio por línea","money"],["mayorSubtotal","Mayor subtotal","money"],["menorSubtotal","Menor subtotal","money"]]
  },
  "segmentacion-productos": {
    title: "Ranking de productos",
    description: "Descubre qué productos concentran las unidades pedidas dentro de cada categoría.",
    note: "Se conserva cada detalle. Las unidades, el puesto y la participación de un producto se repiten en sus líneas. Los empates comparten puesto.",
    columns: [["rankingCategoria","Puesto","rank"],["producto","Producto","product"],["categoria","Categoría"],["idPedido","Pedido","id"],["cantidad","Cantidad de la línea","number"],["unidadesProducto","Unidades del producto","number"],["unidadesCategoria","Unidades de la categoría","number"],["porcentajeProducto","Participación","percent"]]
  },
  "auditoria-pagos": {
    title: "Auditoría de pagos",
    description: "Revisa cuánto se ha pagado por cada pedido y detecta saldos pendientes o excedentes.",
    note: "Se incluyen pedidos con detalles y se suman únicamente pagos registrados. Un saldo negativo indica un excedente.",
    columns: [["idPedido","Pedido","id"],["fecha","Fecha"],["empleado","Empleado"],["estadoPedido","Estado del pedido","badge"],["totalPedido","Importe del pedido","money"],["totalPagado","Pagado","money"],["saldo","Saldo","money"],["clasificacion","Clasificación","badge"]]
  },
  "reporte-filtrado": {
    title: "Reporte con filtros",
    description: "Consulta los pedidos por mes y categoría usando los filtros que necesites.",
    note: "Las fechas de inicio y fin se incluyen en la consulta. El importe corresponde a los productos pedidos.",
    columns: [["periodo","Período"],["categoria","Categoría","product"],["idCategoria","ID categoría","number"],["totalPedidos","Pedidos","number"],["totalImporte","Importe total","money"],["promedioSubtotal","Promedio por línea","money"],["mayorSubtotal","Mayor subtotal","money"],["menorSubtotal","Menor subtotal","money"]]
  }
};
const $ = id => document.getElementById(id);
const money = new Intl.NumberFormat("es-CO", {style:"currency",currency:"COP",maximumFractionDigits:2});
const number = new Intl.NumberFormat("es-CO", {maximumFractionDigits:2});
const pageSize = 10;
let current = "trazabilidad";
let rows = [];
let page = 1;
let loading = false;
let request;
let failed = false;
let params = new URLSearchParams();

function cellText(value, type) {
  if (value === null || value === undefined) return "—";
  if (type === "money") return money.format(value);
  if (type === "number") return number.format(value);
  if (type === "id") return "#" + String(value).padStart(3, "0");
  return String(value);
}
function makeCell(value, type) {
  const td = document.createElement("td");
  if (["money","number","percent","rank"].includes(type)) td.className = "numeric";
  if (type === "product") td.className = "product";
  if (type === "badge") {
    const badge = document.createElement("span");
    const key = String(value).toUpperCase();
    badge.className = "badge" + (["PAGADO","ENTREGADO"].includes(key) ? " positive" : ["PARCIAL","PENDIENTE","EN PREPARACIÓN","RECIBIDO"].includes(key) ? " warning" : ["EXCEDENTE","CANCELADO","DATOS_INVALIDOS"].includes(key) ? " negative" : "");
    badge.textContent = value;
    td.append(badge);
  } else if (type === "rank") {
    const badge = document.createElement("span");
    badge.className = "rank";
    badge.textContent = value;
    td.append(badge);
  } else if (type === "percent") {
    const wrap = document.createElement("span");
    wrap.className = "share";
    const bar = document.createElement("span");
    bar.className = "bar";
    bar.setAttribute("aria-hidden","true");
    const fill = document.createElement("i");
    fill.style.width = Math.max(0, Math.min(100, Number(value))) + "%";
    bar.append(fill);
    const label = document.createElement("span");
    label.textContent = number.format(value) + " %";
    wrap.append(bar,label);
    td.append(wrap);
  } else td.textContent = cellText(value,type);
  return td;
}
function summary() {
  const sum = field => rows.reduce((total,row) => total + Number(row[field] || 0),0);
  const distinct = field => new Set(rows.map(row => row[field])).size;
  let cards;
  if (current === "trazabilidad") cards = [
    ["Pedidos",number.format(distinct("idPedido")),"Pedidos distintos"],
    ["Unidades pedidas",number.format(sum("cantidad")),"Todos los estados"],
    ["Importe de los pedidos",money.format(sum("subtotal")),"Suma de subtotales"]
  ];
  if (current === "metricas-categorias") cards = [
    ["Categorías",number.format(rows.length),"Con al menos 3 pedidos"],
    ["Importe de las categorías",money.format(sum("totalImporte")),"Categorías mostradas"],
    ["Mayor importe", rows.length ? money.format(Math.max(...rows.map(r=>r.totalImporte))) : "—","Total de una categoría"]
  ];
  if (current === "segmentacion-productos") cards = [
    ["Productos",number.format(distinct("idProducto")),"Productos con pedidos"],
    ["Categorías",number.format(distinct("idCategoria")),"Grupos del ranking"],
    ["Unidades pedidas",number.format(sum("cantidad")),"Suma de cantidades por línea"]
  ];
  if (current === "auditoria-pagos") cards = [
    ["Pedidos auditados",number.format(rows.length),"Pedidos con detalles"],
    ["Pagos registrados",money.format(sum("totalPagado")),"Pagos vigentes"],
    ["Pedidos con saldo pendiente",number.format(rows.filter(r=>r.saldo>0).length),"Saldo mayor que cero"]
  ];
  if (current === "reporte-filtrado") cards = [
    ["Categorías",number.format(distinct("idCategoria")),"Según los filtros aplicados"],
    ["Importe del reporte",money.format(sum("totalImporte")),"Productos pedidos"],
    ["Meses consultados",number.format(distinct("periodo")),"Períodos con resultados"]
  ];
  $("metrics").replaceChildren();
  cards.forEach(([label,value,detail])=>{
    const card=document.createElement("div");
    card.className="metric";
    [["label",label],["value",loading||failed?"—":value],["detail",detail]].forEach(([className,text])=>{
      const el=document.createElement("div");el.className=className;el.textContent=text;card.append(el);
    });
    $("metrics").append(card);
  });
}
function render() {
  const config=reports[current];
  summary();
  $("table-head").replaceChildren();
  const heading=document.createElement("tr");
  config.columns.forEach(([,label,type])=>{
    const th=document.createElement("th");th.scope="col";th.textContent=label;
    if(["money","number","rank","percent"].includes(type)) th.className="numeric";
    heading.append(th);
  });
  $("table-head").append(heading);
  $("table-body").replaceChildren();
  const query=$("search").value.trim().toLocaleLowerCase("es");
  const filtered=rows.filter(row=>config.columns.some(([key,,type])=>cellText(row[key],type).toLocaleLowerCase("es").includes(query)));
  const pages=Math.max(1,Math.ceil(filtered.length/pageSize));
  page=Math.min(page,pages);
  filtered.slice((page-1)*pageSize,page*pageSize).forEach(row=>{
    const tr=document.createElement("tr");
    config.columns.forEach(([key,,type])=>tr.append(makeCell(row[key],type)));
    $("table-body").append(tr);
  });
  $("record-count").textContent=loading?"Consultando datos…":failed?"No se pudieron cargar los resultados":number.format(filtered.length)+" registros"+(query?" de "+number.format(rows.length):"");
  $("message").hidden=!(loading||failed||filtered.length===0);
  $("table-wrap").hidden=loading||failed||filtered.length===0;
  $("message").className="message"+(failed?" error":"");
  if(!failed) $("message").textContent=loading?"Cargando el reporte…":query?"No hay resultados para esa búsqueda.":"No hay datos para esta consulta. Puedes cambiar o limpiar los filtros.";
  $("page-info").textContent=loading||failed||filtered.length===0?"":((page-1)*pageSize+1)+"–"+Math.min(page*pageSize,filtered.length)+" de "+filtered.length+" registros";
  $("previous").disabled=loading||failed||page===1;
  $("next").disabled=loading||failed||page===pages;
  $("refresh").disabled=loading;
  $("download-json").disabled=loading||failed;
  $("search").disabled=loading||failed;
  $("table-card")?.setAttribute("aria-busy",String(loading));
}
async function load() {
  request?.abort();
  const controller=new AbortController();
  request=controller;
  const config=reports[current];
  loading=true;failed=false;rows=[];page=1;
  const url="/api/reportes/"+current+(current==="reporte-filtrado"&&params.size?"?"+params.toString():"");
  $("json-link").href=url;
  $("connection").textContent="Consultando la API…";$("connection").className="";
  $("updated").textContent="Actualizando datos…";
  render();
  try{
    const response=await fetch(url,{signal:controller.signal,headers:{Accept:"application/json"}});
    const data=await response.json();
    if(!response.ok) throw new Error(data.mensaje||"No fue posible cargar el reporte (HTTP "+response.status+").");
    if(!Array.isArray(data)) throw new Error("La API devolvió una respuesta inesperada.");
    rows=data;
    $("connection").textContent="API conectada";$("connection").className="ready";
    $("updated").textContent="Actualizado a las "+new Intl.DateTimeFormat("es-CO",{hour:"2-digit",minute:"2-digit",second:"2-digit"}).format(new Date());
  }catch(error){
    if(controller.signal.aborted) return;
    failed=true;
    $("message").textContent=error instanceof TypeError?"No se pudo conectar con la API. Comprueba que los contenedores estén en ejecución y pulsa Actualizar datos.":error.message;
    $("connection").textContent="Consulta sin completar";$("connection").className="failed";
    $("updated").textContent="No se han actualizado los datos";
  }finally{
    if(request===controller){loading=false;render();}
  }
}
function downloadJson() {
  if (loading || failed) return;
  const content = JSON.stringify(rows, null, 2) + "\n";
  const blob = new Blob([content], {type: "application/json;charset=utf-8"});
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = "modo-shake-" + current + ".json";
  document.body.append(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 10000);
}
$("download-json").addEventListener("click", downloadJson);
function selectReport(key) {
  current=key;
  $("report-title").textContent=reports[key].title;
  $("description").textContent=reports[key].description;
  $("report-note").textContent=reports[key].note;
  $("caption").textContent=reports[key].title;
  $("filters").hidden=key!=="reporte-filtrado";
  $("search").value="";
  document.querySelectorAll("[data-report]").forEach(button=>{
    if(button.dataset.report===key)button.setAttribute("aria-current","page");
    else button.removeAttribute("aria-current");
  });
  load();
}
document.querySelectorAll("[data-report]").forEach(button=>button.addEventListener("click",()=>selectReport(button.dataset.report)));
$("refresh").addEventListener("click",load);
$("search").addEventListener("input",()=>{page=1;render();});
$("previous").addEventListener("click",()=>{page--;render();});
$("next").addEventListener("click",()=>{page++;render();});
$("filters").addEventListener("submit",event=>{
  event.preventDefault();
  params=new URLSearchParams();
  new FormData(event.currentTarget).forEach((value,key)=>{if(String(value).trim())params.set(key,String(value).trim());});
  $("search").value="";
  load();
});
$("filters").addEventListener("reset",()=>{params=new URLSearchParams();$("search").value="";load();});
selectReport(current);