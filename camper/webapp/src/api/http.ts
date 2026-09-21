import { readStoredUser } from '../auth/storage';
import { slowNetworkDelay } from '../lib/slowNetwork';

export interface ApiErrorBody {
  code: string;
  message: string;
}

export class ApiError extends Error {
  status: number;
  code: string;

  constructor(status: number, body: ApiErrorBody) {
    super(body.message);
    this.name = 'ApiError';
    this.status = status;
    this.code = body.code;
  }
}

interface RequestOptions extends Omit<RequestInit, 'body'> {
  body?: unknown;
}

function isErrorBody(value: unknown): value is ApiErrorBody {
  return (
    typeof value === 'object' &&
    value !== null &&
    'code' in value &&
    'message' in value
  );
}

export interface ResponseWithStatus<T> {
  data: T;
  status: number;
}

/**
 * Core fetch wrapper. Adds `Content-Type` and `X-User-Id` (from the auth
 * store), returns `undefined` for 204 responses, tolerates a literal
 * `null` JSON body (e.g. `GET /api/meal-plans?planId=` when nothing
 * matches), and throws `ApiError` parsed from the backend's
 * `{ code, message }` error envelope.
 *
 * Every failure mode surfaces as `ApiError` — never a raw `TypeError`/
 * `SyntaxError` — so callers (and the global toast) only ever have one
 * shape to branch on:
 *  - the network is unreachable (fetch itself rejects) -> status 0, code NETWORK
 *  - a non-OK response whose body isn't the `{code,message}` envelope
 *    (an HTML error page from a gateway or the dev proxy, plain text,
 *    etc.) -> code UNKNOWN, using the response's own status
 *  - an OK response whose body isn't valid JSON -> code INVALID_RESPONSE
 *
 * `request()` below is the everyday entry point (body only); use
 * `requestWithStatus()` when a caller needs to distinguish e.g. 200 from
 * 201 (some endpoints use the status itself to carry meaning, not just
 * the body) rather than inferring it from the response shape.
 */
async function requestCore<T>(path: string, options: RequestOptions = {}): Promise<ResponseWithStatus<T>> {
  const { body, headers, ...rest } = options;

  const finalHeaders: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(headers as Record<string, string> | undefined),
  };

  const storedUser = readStoredUser();
  if (storedUser) {
    finalHeaders['X-User-Id'] = storedUser.id;
  }

  let response: Response;
  try {
    await slowNetworkDelay();
    response = await fetch(path, {
      ...rest,
      headers: finalHeaders,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError(0, { code: 'NETWORK', message: "Can't reach the server." });
  }

  if (response.status === 204) {
    return { data: undefined as T, status: response.status };
  }

  let parsed: unknown = null;
  let parseFailed = false;
  try {
    const text = await response.text();
    parsed = text ? JSON.parse(text) : null;
  } catch {
    parseFailed = true;
  }

  if (!response.ok) {
    const errorBody: ApiErrorBody =
      !parseFailed && isErrorBody(parsed)
        ? parsed
        : { code: 'UNKNOWN', message: `Something went wrong (status ${response.status}).` };
    throw new ApiError(response.status, errorBody);
  }

  if (parseFailed) {
    throw new ApiError(response.status, {
      code: 'INVALID_RESPONSE',
      message: 'The server returned something unexpected.',
    });
  }

  return { data: parsed as T, status: response.status };
}

/** Typed fetch wrapper returning just the parsed body. See `requestCore` for behavior. */
export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { data } = await requestCore<T>(path, options);
  return data;
}

/** Same as `request`, but also returns the response's status — for endpoints where the status itself is meaningful (e.g. 200 vs 201). */
export async function requestWithStatus<T>(path: string, options: RequestOptions = {}): Promise<ResponseWithStatus<T>> {
  return requestCore<T>(path, options);
}
