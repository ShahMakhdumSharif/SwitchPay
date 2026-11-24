export const SESSION_STORAGE_KEY = "switchpay_session";

export function normalizeApiError(err) {
  const response = err?.response;
  const data = response?.data;

  const status = data?.status ?? response?.status ?? 500;
  const error = data?.error ?? response?.statusText ?? "Error";
  const path = data?.path ?? err?.config?.url ?? "";
  const fieldErrors = Array.isArray(data?.fieldErrors) ? data.fieldErrors : [];

  const fieldErrorMap = {};
  for (const item of fieldErrors) {
    if (item?.field && item?.message) {
      fieldErrorMap[item.field] = item.message;
    }
  }

  let message = data?.message;
  if (!message) {
    if (status === 400) {
      message = "Please check the highlighted form fields.";
    } else if (status === 401) {
      message = "Authentication required or session expired.";
    } else if (status === 403) {
      message = "You do not have permission to perform this action.";
    } else if (status === 404) {
      message = "The requested resource was not found.";
    } else if (status === 409) {
      message = "A record with these details already exists.";
    } else if (status === 422) {
      message = "The request violates a business rule.";
    } else {
      message = "Something went wrong. Please try again.";
    }
  }

  return {
    timestamp: data?.timestamp ?? new Date().toISOString(),
    status,
    error,
    message,
    path,
    fieldErrors,
    fieldErrorMap,
  };
}
