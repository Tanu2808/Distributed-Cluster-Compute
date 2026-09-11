import { config } from '../utils/config';
import { ApiError } from '../types/api';

// ─── Base HTTP helpers ────────────────────────────────────────────────────────

function buildUrl(path: string): string {
  // path must start with /
  return `${config.API_BASE_URL}${path}`;
}

async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    let message = `HTTP ${response.status}: ${response.statusText}`;
    try {
      const body = await response.json();
      if (body?.message) message = body.message;
      else if (body?.error) message = body.error;
    } catch {
      // ignore parse errors — keep default message
    }
    throw new ApiError(response.status, response.statusText, message);
  }
  return response.json() as Promise<T>;
}

/**
 * Typed GET request.
 * Throws ApiError on non-2xx responses.
 */
export async function apiGet<T>(path: string): Promise<T> {
  const response = await fetch(buildUrl(path), {
    method: 'GET',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
  });
  return handleResponse<T>(response);
}

/**
 * Typed PUT request with JSON body.
 * Throws ApiError on non-2xx responses.
 */
export async function apiPut<TBody, TResponse = TBody>(
  path: string,
  body: TBody,
): Promise<TResponse> {
  const response = await fetch(buildUrl(path), {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    body: JSON.stringify(body),
  });
  return handleResponse<TResponse>(response);
}
