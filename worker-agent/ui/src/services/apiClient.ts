const BASE_URL = '/api';

export async function fetchApi<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(`${BASE_URL}${endpoint}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
  });

  if (!response.ok) {
    try {
      const errorData = await response.json();
      throw new Error(errorData.message || errorData.error || `API request failed: ${response.statusText}`);
    } catch (e) {
      if (e instanceof Error && e.message !== 'Unexpected end of JSON input') {
        throw e;
      }
      throw new Error(`API request failed: ${response.statusText}`);
    }
  }

  const text = await response.text();
  if (!text) {
    return {} as T;
  }
  try {
    return JSON.parse(text) as T;
  } catch {
    return {} as T;
  }
}

