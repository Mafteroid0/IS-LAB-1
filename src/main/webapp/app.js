import { api, escape as e, positiveLong } from './api.js';

const $ = selector => document.querySelector(selector);
const state = { page: 0, size: 10, sort: 'id', direction: 'asc', filters: {}, coordinates: [], locations: [], user: null, revision: null, modal: null, query: null, syncing: false, tableRequest: 0 };
const labels = { id: 'ID', name: 'Название', coordinates: 'Координаты', creationDate: 'Дата создания', from: 'Отправление', to: 'Назначение', distance: 'Расстояние', rating: 'Рейтинг' };
const date = value => new Date(value + 'Z').toLocaleString('ru-RU');
const locationText = value => value ? `#${value.id} · ${value.name} (${value.x}; ${value.y}; ${value.z})` : 'Не указано';
const coordinateText = value => `#${value.id} (${value.x}; ${value.y})`;
const float = (value, label) => {
  const number = Math.fround(Number(value));
  if (value === '' || !Number.isFinite(number)) throw new Error(`${label}: требуется конечное число в диапазоне float`);
  return number;
};

function notify(message, error = false) {
  $('#notice').hidden = false;
  $('#notice').textContent = message;
  $('#notice').className = error ? 'error' : '';
}

function showAuth() {
  state.user = null;
  state.revision = null;
  state.query = null;
  closeModal();
  $('#app').hidden = true;
  $('#auth').hidden = false;
}

function fail(error) {
  if (error.status === 401) showAuth();
  if ($('#app').hidden) $('#auth-error').textContent = error.message;
  else notify(error.message, true);
}

async function run(action, target) {
  try { await action(); }
  catch (error) {
    if (target && error.status !== 401) target.textContent = error.message;
    else fail(error);
  }
}

function modal(title, content, descriptor) {
  $('#modal-title').textContent = title;
  $('#modal-content').innerHTML = content;
  state.modal = descriptor;
  if (!$('#modal').open) $('#modal').showModal();
}

function closeModal() {
  $('#modal').close();
  state.modal = null;
}

function bindModal(save) {
  $('#modal-form').addEventListener('submit', event => {
    event.preventDefault();
    const button = event.submitter;
    button.disabled = true;
    $('#modal-error').textContent = '';
    run(() => save(new FormData(event.target)), $('#modal-error')).finally(() => { button.disabled = false; });
  });
}

const modalEnd = (title = 'Сохранить', danger = false) => `<p id="stale-warning" class="warning" hidden></p><p id="modal-error" class="error" role="alert"></p><div class="form-actions"><button type="button" class="secondary" data-close>Отмена</button><button type="submit" class="${danger ? 'danger' : ''}">${title}</button></div>`;

function referenceOptions(resource, selected, excluded) {
  const label = resource === 'coordinates' ? coordinateText : locationText;
  return '<option value="">Не выбрано</option>' + state[resource].filter(value => value.id !== excluded).map(value =>
    `<option value="${e(value.id)}" ${value.id === selected ? 'selected' : ''}>${e(label(value))}</option>`).join('');
}

function routeTable(values, sortable = false) {
  if (!values.length) return '<div class="empty">Маршруты не найдены</div>';
  const header = Object.entries(labels).map(([key, label]) => {
    const sort = key === 'from' ? 'fromName' : key === 'to' ? 'toName' : key;
    const arrow = state.sort === sort ? (state.direction === 'asc' ? ' ↑' : ' ↓') : ' ↕';
    return `<th>${sortable && key !== 'coordinates' ? `<button data-sort="${sort}">${label}${arrow}</button>` : label}</th>`;
  }).join('');
  return `<table><thead><tr>${header}<th>Действия</th></tr></thead><tbody>${values.map(value => `<tr>
    <td>${e(value.id)}</td><td class="name"><button class="link" data-view="${e(value.id)}">${e(value.name)}</button></td>
    <td>${e(coordinateText(value.coordinates))}</td><td>${e(date(value.creationDate))}</td>
    <td>${e(locationText(value.from))}</td><td>${e(locationText(value.to))}</td><td>${e(value.distance)}</td><td>${e(value.rating)}</td>
    <td><div class="row-actions"><button class="link" data-edit="${e(value.id)}">Изменить</button><button class="link" data-delete="${e(value.id)}">Удалить</button></div></td>
  </tr>`).join('')}</tbody></table>`;
}

async function loadRoutes() {
  const request = ++state.tableRequest;
  const params = new URLSearchParams({ ...state.filters, page: state.page, size: state.size, sort: state.sort, direction: state.direction });
  const result = await api(`/routes?${params}`);
  if (request !== state.tableRequest) return;
  state.page = result.page;
  $('#route-table').innerHTML = routeTable(result.items, true);
  $('#page-info').textContent = `Всего: ${result.total} · Страница ${result.page + 1} из ${Math.max(1, Math.ceil(result.total / result.size))}`;
  $('#prev-page').disabled = result.page === 0;
  $('#next-page').disabled = (result.page + 1) * result.size >= result.total;
}

async function loadReferences() {
  const results = await Promise.all([api('/coordinates'), api('/locations')]);
  for (const [index, resource] of ['coordinates', 'locations'].entries()) {
    state[resource] = results[index];
    const fields = resource === 'coordinates' ? ['id', 'x', 'y'] : ['id', 'name', 'x', 'y', 'z'];
    $(`#${resource}-table`).innerHTML = state[resource].length ? `<table><thead><tr>${fields.map(key => `<th>${key === 'name' ? 'Название' : key.toUpperCase()}</th>`).join('')}<th>Действия</th></tr></thead><tbody>${state[resource].map(value => `<tr>${fields.map(key => `<td>${e(value[key])}</td>`).join('')}<td><button class="link" data-reference-edit="${e(value.id)}" data-resource="${resource}">Изменить</button><button class="link" data-reference-delete="${e(value.id)}" data-resource="${resource}">Удалить</button></td></tr>`).join('')}</tbody></table>` : '<div class="empty">Добавьте объект, чтобы использовать его в маршрутах</div>';
  }
  document.querySelectorAll('select[data-ref]').forEach(select => {
    const selected = select.value;
    select.innerHTML = referenceOptions(select.dataset.ref, selected, select.dataset.exclude);
    if (selected && !state[select.dataset.ref].some(value => value.id === selected) && select.closest('dialog')) {
      $('#stale-warning').hidden = false;
      $('#stale-warning').textContent = 'Выбранный связанный объект удалён. Выберите другой.';
    }
  });
}

function detailHtml(value) {
  return `<dl>${Object.entries(labels).map(([key, label]) => {
    let text = value[key];
    if (key === 'creationDate') text = date(text);
    if (key === 'coordinates') text = coordinateText(text);
    if (key === 'from' || key === 'to') text = locationText(text);
    return `<dt>${label}</dt><dd>${e(text)}</dd>`;
  }).join('')}</dl><div class="form-actions"><button data-edit="${e(value.id)}">Изменить</button><button class="danger" data-delete="${e(value.id)}">Удалить</button></div>`;
}

async function viewRoute(id) {
  const value = await api(`/routes/${positiveLong(id)}`);
  modal(`Маршрут #${value.id}`, detailHtml(value), { kind: 'view', resource: 'routes', id });
}

async function editRoute(id, between = null) {
  const value = id ? await api(`/routes/${id}`) : {};
  await loadReferences();
  modal(id ? `Изменить маршрут #${id}` : between ? 'Новый маршрут между локациями' : 'Новый маршрут', `<form id="modal-form"><div class="form-grid">
    <label class="wide">Название<input name="name" required value="${e(value.name ?? '')}"></label>
    <label class="wide">Координаты<select name="coordinatesId" data-ref="coordinates" required>${referenceOptions('coordinates', value.coordinates?.id)}</select><small>Связанные объекты создаются в разделах «Координаты» и «Локации».</small></label>
    <label>Отправление<select name="fromId" data-ref="locations" ${between ? 'required' : ''}>${referenceOptions('locations', between?.fromId ?? value.from?.id)}</select><small>${between ? 'Обязательно для этой операции' : 'Можно не указывать'}</small></label>
    <label>Назначение<select name="toId" data-ref="locations" required>${referenceOptions('locations', between?.toId ?? value.to?.id)}</select></label>
    <label>Расстояние<input name="distance" type="number" step="any" min="1" required value="${e(value.distance ?? '')}"><small>Строго больше 1</small></label>
    <label>Рейтинг<input name="rating" type="number" step="any" min="0" required value="${e(value.rating ?? '')}"><small>Строго больше 0</small></label>
    </div>${modalEnd()}</form>`, { kind: 'edit', resource: 'routes', id, version: value.version });
  bindModal(async data => {
    const body = Object.fromEntries(data);
    if (!body.name.trim()) throw new Error('Название не должно быть пустым');
    body.coordinatesId = positiveLong(body.coordinatesId, 'Координаты');
    body.fromId = body.fromId ? positiveLong(body.fromId, 'Отправление') : null;
    body.toId = positiveLong(body.toId, 'Назначение');
    body.distance = float(body.distance, 'Расстояние');
    body.rating = float(body.rating, 'Рейтинг');
    if (body.distance <= 1) throw new Error('Расстояние должно быть строго больше 1');
    if (body.rating <= 0) throw new Error('Рейтинг должен быть строго больше 0');
    if (id) body.version = value.version;
    await api(id ? `/routes/${id}` : between ? '/operations/between' : '/routes', id ? 'PUT' : 'POST', body);
    closeModal();
    notify(id ? 'Маршрут изменён' : 'Маршрут добавлен');
    await sync(true);
  });
}

async function deleteRoute(id) {
  const value = await api(`/routes/${id}`);
  modal('Удаление маршрута', `<form id="modal-form"><p>Удалить «${e(value.name)}» (#${e(id)})?</p><p class="muted">Координаты и локации останутся в справочниках.</p>${modalEnd('Удалить', true)}</form>`, { kind: 'delete', resource: 'routes', id, version: value.version });
  bindModal(async () => {
    await api(`/routes/${id}?version=${value.version}`, 'DELETE');
    closeModal();
    notify('Маршрут удалён');
    await sync(true);
  });
}

function editReference(resource, id) {
  const value = state[resource].find(item => item.id === id) || {};
  const isLocation = resource === 'locations';
  if (id && !value.id) throw new Error('Объект уже удалён');
  const fields = ['x', 'y'].map(key => `<label>${key.toUpperCase()}<input name="${key}" type="number" step="any" required value="${e(value[key] ?? '')}"></label>`).join('');
  modal(`${id ? 'Изменить' : 'Добавить'} ${isLocation ? 'локацию' : 'координаты'}${id ? ' #' + id : ''}`, `<form id="modal-form"><div class="form-grid">${isLocation ? `<label class="wide">Название<input name="name" required value="${e(value.name ?? '')}"></label>` : ''}${fields}${isLocation ? `<label>Z<input name="z" type="number" min="-2147483648" max="2147483647" step="1" required value="${e(value.z ?? '')}"></label>` : ''}</div>${id ? '<p class="warning">Изменение затронет все маршруты, использующие этот объект.</p>' : ''}${modalEnd()}</form>`, { kind: 'edit', resource, id, version: value.version });
  bindModal(async data => {
    const body = { x: float(data.get('x'), 'X'), y: float(data.get('y'), 'Y') };
    if (isLocation) {
      body.name = data.get('name');
      body.z = Number(data.get('z'));
      if (!body.name.trim()) throw new Error('Название не должно быть пустым');
      if (!Number.isInteger(body.z) || body.z < -2147483648 || body.z > 2147483647) throw new Error('Z должно быть целым числом в диапазоне int');
    }
    if (id) body.version = value.version;
    await api(id ? `/${resource}/${id}` : `/${resource}`, id ? 'PUT' : 'POST', body);
    closeModal();
    notify(id ? 'Объект изменён' : 'Объект добавлен');
    await sync(true);
  });
}

async function deleteReference(resource, id) {
  const value = state[resource].find(item => item.id === id);
  if (!value) throw new Error('Объект уже удалён');
  const usage = await api(`/${resource}/${id}/usage`);
  const label = resource === 'coordinates' ? coordinateText(value) : locationText(value);
  modal(resource === 'coordinates' ? 'Удаление координат' : 'Удаление локации', `<form id="modal-form"><p>Удалить ${e(label)}?</p><p>Связанных маршрутов: ${usage.count}.</p><label>Перенести все ссылки на<select name="replacementId" data-ref="${resource}" data-exclude="${e(id)}" ${usage.count ? 'required' : ''}>${referenceOptions(resource, null, id)}</select><small>При наличии связей замена обязательна. Если выбирать не из чего, сначала создайте другой объект.</small></label>${modalEnd('Удалить', true)}</form>`, { kind: 'delete', resource, id, version: value.version });
  bindModal(async data => {
    const params = new URLSearchParams({ version: value.version });
    if (data.get('replacementId')) params.set('replacementId', data.get('replacementId'));
    await api(`/${resource}/${id}?${params}`, 'DELETE');
    closeModal();
    notify('Объект удалён, связанные маршруты обновлены');
    await sync(true);
  });
}

async function refreshModal() {
  const current = state.modal;
  if (!current?.id) return;
  try {
    const value = current.resource === 'routes' ? await api(`/routes/${current.id}`) : state[current.resource].find(item => item.id === current.id);
    if (state.modal !== current) return;
    if (!value) throw Object.assign(new Error('Объект удалён'), { status: 404 });
    if (current.kind === 'view') $('#modal-content').innerHTML = detailHtml(value);
    else if (current.version !== value.version) {
      $('#stale-warning').hidden = false;
      $('#stale-warning').textContent = 'Объект изменён другим пользователем. Закройте окно и откройте его заново. Ваш ввод пока сохранён.';
      $('#modal-form button[type="submit"]').disabled = true;
    }
  } catch (error) {
    if (state.modal !== current) return;
    if (error.status !== 404) throw error;
    $('#modal-content').innerHTML = '<p class="warning">Объект удалён другим пользователем.</p>';
    state.modal = null;
  }
}

async function loadOperation() {
  const query = state.query;
  if (!query) return;
  let content;
  try {
    const result = await api(query.path);
    content = query.kind === 'average' ? `<div class="empty">${result.value == null ? 'Нет маршрутов для расчёта среднего' : 'Средний рейтинг: ' + e(result.value)}</div>`
      : query.kind === 'count' ? `<div class="empty">Количество маршрутов: ${e(result.count)}</div>`
      : `<p class="result-count">Найдено: ${result.length}</p>${routeTable(result)}`;
  } catch (error) {
    if (error.status !== 404) throw error;
    content = `<div class="empty">${e(error.message)}. Выберите локации заново.</div>`;
  }
  if (state.query !== query) return;
  $('#operation-result').hidden = false;
  $('#operation-title').textContent = query.title;
  $('#operation-table').innerHTML = content;
}

async function setQuery(path, title, kind = 'list') {
  state.query = { path, title, kind };
  await loadOperation();
}

async function sync(force = false) {
  if (!state.user || state.syncing) return;
  state.syncing = true;
  try {
    const result = await api('/state');
    if (force || result.revision !== state.revision) {
      await Promise.all([loadRoutes(), loadReferences(), loadOperation()]);
      await refreshModal();
      state.revision = result.revision;
    }
    $('#sync-status').textContent = 'Автообновление · каждые 2 секунды';
  } catch (error) {
    $('#sync-status').textContent = 'Нет связи · повторное подключение…';
    if (force || error.status === 401) fail(error);
  } finally { state.syncing = false; }
}

function navigate() {
  const sections = ['routes', 'coordinates', 'locations', 'operations'];
  const section = sections.includes(location.hash.slice(1)) ? location.hash.slice(1) : 'routes';
  for (const name of sections) {
    $(`#${name}-section`).hidden = name !== section;
    $(`nav a[href="#${name}"]`).classList.toggle('active', name === section);
  }
}

async function signedIn(user) {
  state.user = user.username;
  state.revision = null;
  $('#username').textContent = user.username;
  $('#auth').hidden = true;
  $('#app').hidden = false;
  $('#operation-result').hidden = true;
  $('#auth-form').reset();
  $('#auth-error').textContent = '';
  navigate();
  await sync(true);
}

$('#auth-form').addEventListener('submit', event => {
  event.preventDefault();
  const form = event.target;
  const action = event.submitter.value;
  const body = Object.fromEntries(new FormData(form));
  form.querySelectorAll('button').forEach(button => { button.disabled = true; });
  run(async () => { await signedIn(await api(`/auth/${action}`, 'POST', body)); }, $('#auth-error')).finally(() => {
    form.querySelectorAll('button').forEach(button => { button.disabled = false; });
  });
});
$('#logout').onclick = () => run(async () => { await api('/auth/logout', 'POST'); showAuth(); });
$('#refresh').onclick = () => sync(true);
$('#close-modal').onclick = closeModal;
$('#modal').addEventListener('close', () => { state.modal = null; });
$('#new-route').onclick = () => run(() => editRoute());
$('#new-coordinates').onclick = () => run(() => editReference('coordinates'));
$('#new-location').onclick = () => run(() => editReference('locations'));
$('#find-id').onsubmit = event => { event.preventDefault(); run(() => viewRoute(new FormData(event.target).get('id'))); };
$('#filters').onsubmit = event => {
  event.preventDefault(); state.filters = Object.fromEntries(new FormData(event.target)); state.page = 0; run(loadRoutes);
};
$('#filters').onreset = () => { state.filters = {}; state.page = 0; run(loadRoutes); };
$('#page-size').onchange = event => { state.size = Number(event.target.value); state.page = 0; run(loadRoutes); };
$('#prev-page').onclick = () => { state.page--; run(loadRoutes); };
$('#next-page').onclick = () => { state.page++; run(loadRoutes); };
window.addEventListener('hashchange', navigate);
document.addEventListener('click', event => {
  const button = event.target.closest('button');
  if (!button) return;
  const data = button.dataset;
  run(async () => {
    if ('close' in data) closeModal();
    if (data.view) await viewRoute(data.view);
    if (data.edit) await editRoute(data.edit);
    if (data.delete) await deleteRoute(data.delete);
    if (data.referenceEdit) editReference(data.resource, data.referenceEdit);
    if (data.referenceDelete) await deleteReference(data.resource, data.referenceDelete);
    if (data.sort) {
      state.direction = state.sort === data.sort && state.direction === 'asc' ? 'desc' : 'asc';
      state.sort = data.sort; state.page = 0; await loadRoutes();
    }
  });
});
for (const name of ['average', 'count', 'contains', 'between', 'create']) {
  $(`#op-${name}`).onsubmit = event => {
    event.preventDefault();
    const values = Object.fromEntries(new FormData(event.target));
    const button = event.submitter;
    button.disabled = true;
    run(async () => {
      if (name === 'average') await setQuery('/operations/average-rating', 'Средний рейтинг', 'average');
      if (name === 'count') await setQuery(`/operations/rating-less?${new URLSearchParams({ rating: float(values.rating, 'Порог') })}`, `Рейтинг меньше ${values.rating}`, 'count');
      if (name === 'contains') await setQuery(`/operations/name-contains?${new URLSearchParams(values)}`, `Название содержит «${values.part}»`);
      if (name === 'between') await setQuery(`/operations/between?${new URLSearchParams(values)}`, 'Маршруты между выбранными локациями');
      if (name === 'create') await editRoute(null, values);
    }).finally(() => { button.disabled = false; });
  };
}
setInterval(() => sync(), 2000);
run(async () => {
  try { await signedIn(await api('/auth/me')); }
  catch (error) { if (error.status === 401) showAuth(); else throw error; }
});
