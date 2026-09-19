import { config } from "../utils/config";
import { ApiError } from "../types/api";

// ─── Base HTTP helpers ────────────────────────────────────────────────────────

function buildUrl(path: string): string {
  // path must start with /
  return `${config.API_BASE_URL}${path}`;
}

function getHeaders(): Record<string, string> {
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    Accept: "application/json",
  };

  if (!config.USE_MOCK_API && config.API_USERNAME && config.API_PASSWORD) {
    const credentials = btoa(`${config.API_USERNAME}:${config.API_PASSWORD}`);
    headers.Authorization = `Basic ${credentials}`;
  }

  return headers;
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

export async function apiGet<T>(path: string): Promise<T> {
  const response = await fetch(buildUrl(path), {
    method: "GET",
    headers: getHeaders(),
  });
  return handleResponse<T>(response);
}

export async function apiPut<TBody, TResponse = TBody>(
  path: string,
  body: TBody,
): Promise<TResponse> {
  const response = await fetch(buildUrl(path), {
    method: "PUT",
    headers: getHeaders(),
    body: JSON.stringify(body),
  });
  return handleResponse<TResponse>(response);
}
