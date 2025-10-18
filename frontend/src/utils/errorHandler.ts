export class AppError extends Error {
  constructor(
    message: string,
    public code?: string,
    public statusCode?: number,
    public details?: any
  ) {
    super(message);
    this.name = 'AppError';
  }
}

export function handleApiError(error: any): AppError {
  if (error instanceof AppError) return error;

  if (error.response) {
    return new AppError(
      error.response.data?.message || 'API request failed',
      error.response.data?.code,
      error.response.status,
      error.response.data
    );
  }

  if (error.request) {
    return new AppError('No response from server', 'NETWORK_ERROR');
  }

  return new AppError(error.message || 'Unknown error occurred', 'UNKNOWN_ERROR');
}

export function getErrorMessage(error: any): string {
  if (error instanceof AppError) return error.message;
  if (error instanceof Error) return error.message;
  if (typeof error === 'string') return error;
  return 'An unexpected error occurred';
}
