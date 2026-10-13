export async function api(path, method = 'GET', body) {
  const response = await fetch(`api${path}`, {
    method,
    credentials: 'same-origin',
    headers: { 'Content-Type': 'application/json', 'X-Requested-With': 'RouteLab' },
    body: body === undefined ? undefined : JSON.stringify(body)
  });
  const result = response.status === 204 ? null : await response.json();
  if (!response.ok) {
    const fields = Object.entries(result.fields || {}).map(([field, message]) => `${field}: ${message}`);
    const error = new Error([result.message || 'Ошибка запроса', ...fields].join('\n'));
    error.status = response.status;
    throw error;
  }
  return result;
}

export const escape = value => String(value ?? '—').replace(/[&<>"']/g, char => ({
  '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
})[char]);

export function positiveLong(value, label = 'ID') {
  if (!/^[1-9]\d*$/.test(value) || BigInt(value) > 9223372036854775807n)
    throw new Error(`${label}: требуется целое число от 1 до 9223372036854775807`);
  return value;
}
