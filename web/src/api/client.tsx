import { toast } from "react-toastify";

export class ApiError extends Error {
  readonly status: number;
  readonly body: unknown;

  constructor(
    message: string,
    status: number,
    body: unknown,
  ) {
    super(message);

    this.name = "ApiError";
    this.status = status;
    this.body = body;
  }
}

async function readResponse(res: Response): Promise<unknown> {
  const contentType = res.headers.get("content-type") ?? "";

  if (res.status === 204) {
    return null;
  }

  if (contentType.includes("application/json")) {
    return res.json();
  }

  return res.text();
}

function prepareBody(body: unknown): BodyInit | undefined {
  if (body === undefined || body === null) {
    return undefined;
  }

  if (
    typeof body === "string" ||
    body instanceof FormData ||
    body instanceof Blob ||
    body instanceof ArrayBuffer
  ) {
    return body;
  }

  return JSON.stringify(body);
}

function getContentType(body: unknown): string | undefined {
  if (
    body === undefined ||
    body === null ||
    typeof body === "string" ||
    body instanceof FormData ||
    body instanceof Blob ||
    body instanceof ArrayBuffer
  ) {
    return undefined;
  }

  return "application/json";
}

function getErrorMessage(
  method: string,
  path: string,
  status: number,
  body: unknown,
): string {
  if (
    typeof body === "object" &&
    body !== null &&
    "message" in body &&
    typeof body.message === "string"
  ) {
    return body.message;
  }

  if (typeof body === "string" && body.trim()) {
    return body;
  }

  return `${method} ${path} failed with ${status}`;
}

async function apiRequest<T>(
  method: string,
  path: string,
  options: {
    body?: unknown;
    params?: Record<string, unknown>;
    onError?: (status: number, body: unknown) => void;
  } = {},
): Promise<T> {
  const query = options.params
    ? `?${buildQuery(options.params)}`
    : "";

  const body = prepareBody(options.body);
  const contentType = getContentType(options.body);

  const res = await fetch(`/api${path}${query}`, {
    method,
    headers: {
      Accept: "application/json",
      ...(contentType ? { "Content-Type": contentType } : {}),
    },
    body,
  });

  const responseBody = await readResponse(res);

  if (!res.ok) {
    const message = getErrorMessage(
      method,
      path,
      res.status,
      responseBody,
    );

    if (options.onError) {
      options.onError(res.status, responseBody);
    } else {
      toast.error(message);
    }

    throw new ApiError(message, res.status, responseBody);
  }

  return responseBody as T;
}

export function apiGet<T>(
  path: string,
  params?: Record<string, unknown>,
): Promise<T> {
  return apiRequest<T>("GET", path, { params });
}

export function apiPost<TRequest, TResponse>(
  path: string,
  body?: TRequest,
): Promise<TResponse> {
  return apiRequest<TResponse>("POST", path, { body });
}

export function apiPatch<TRequest, TResponse>(
  path: string,
  body?: TRequest,
): Promise<TResponse> {
  return apiRequest<TResponse>("PATCH", path, { body });
}

export function apiPut<TRequest, TResponse>(
  path: string,
  body?: TRequest,
): Promise<TResponse> {
  return apiRequest<TResponse>("PUT", path, { body });
}

export function apiDelete<TResponse>(
  path: string,
  options?: {
    body?: unknown;
  },
): Promise<TResponse> {
  return apiRequest<TResponse>("DELETE", path, options);
}

export function buildQuery(
  params: Record<string, unknown>,
): string {
  const search = new URLSearchParams();

  for (const [key, value] of Object.entries(params)) {
    if (value === undefined || value === null) {
      continue;
    }

    if (Array.isArray(value)) {
      value.forEach((v) => search.append(key, String(v)));
    } else {
      search.append(key, String(value));
    }
  }

  return search.toString();
}