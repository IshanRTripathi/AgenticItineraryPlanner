import { renderHook, act } from '@testing-library/react';
import { useFormSubmission } from '../useFormSubmission';

describe('useFormSubmission', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it('should initialize with correct default state', () => {
    const { result } = renderHook(() => useFormSubmission());

    expect(result.current.isSubmitting).toBe(false);
    expect(result.current.error).toBe(null);
  });

  it('should handle successful submission', async () => {
    const mockSubmitFn = jest.fn().mockResolvedValue('success');
    const { result } = renderHook(() => useFormSubmission());

    let submissionResult: any;
    await act(async () => {
      submissionResult = await result.current.submit(mockSubmitFn);
    });

    expect(mockSubmitFn).toHaveBeenCalledTimes(1);
    expect(submissionResult).toBe('success');
    expect(result.current.isSubmitting).toBe(false);
    expect(result.current.error).toBe(null);
  });

  it('should handle submission error', async () => {
    const mockError = new Error('Submission failed');
    const mockSubmitFn = jest.fn().mockRejectedValue(mockError);
    const { result } = renderHook(() => useFormSubmission());

    await act(async () => {
      try {
        await result.current.submit(mockSubmitFn);
      } catch (error) {
        // Expected to throw
      }
    });

    expect(mockSubmitFn).toHaveBeenCalledTimes(1);
    expect(result.current.error).toEqual(mockError);
  });

  it('should prevent multiple rapid submissions', async () => {
    const mockSubmitFn = jest.fn().mockResolvedValue('success');
    const { result } = renderHook(() => useFormSubmission({ debounceMs: 1000 }));

    // First submission
    await act(async () => {
      await result.current.submit(mockSubmitFn);
    });

    // Immediate second submission should be blocked
    await act(async () => {
      const result2 = await result.current.submit(mockSubmitFn);
      expect(result2).toBe(null); // Should be blocked
    });

    expect(mockSubmitFn).toHaveBeenCalledTimes(1);
  });

  it('should allow submission after debounce period', async () => {
    const mockSubmitFn = jest.fn().mockResolvedValue('success');
    const { result } = renderHook(() => useFormSubmission({ debounceMs: 1000 }));

    // First submission
    await act(async () => {
      await result.current.submit(mockSubmitFn);
    });

    // Fast-forward time past debounce period
    act(() => {
      jest.advanceTimersByTime(1000);
    });

    // Second submission should now be allowed
    await act(async () => {
      await result.current.submit(mockSubmitFn);
    });

    expect(mockSubmitFn).toHaveBeenCalledTimes(2);
  });

  it('should call onSuccess callback on successful submission', async () => {
    const mockSubmitFn = jest.fn().mockResolvedValue('success');
    const onSuccess = jest.fn();
    const { result } = renderHook(() => useFormSubmission({ onSuccess }));

    await act(async () => {
      await result.current.submit(mockSubmitFn);
    });

    expect(onSuccess).toHaveBeenCalledTimes(1);
  });

  it('should call onError callback on failed submission', async () => {
    const mockError = new Error('Submission failed');
    const mockSubmitFn = jest.fn().mockRejectedValue(mockError);
    const onError = jest.fn();
    const { result } = renderHook(() => useFormSubmission({ onError }));

    await act(async () => {
      try {
        await result.current.submit(mockSubmitFn);
      } catch (error) {
        // Expected to throw
      }
    });

    expect(onError).toHaveBeenCalledTimes(1);
    expect(onError).toHaveBeenCalledWith(mockError);
  });

  it('should reset state when reset is called', async () => {
    const mockError = new Error('Submission failed');
    const mockSubmitFn = jest.fn().mockRejectedValue(mockError);
    const { result } = renderHook(() => useFormSubmission());

    // Cause an error
    await act(async () => {
      try {
        await result.current.submit(mockSubmitFn);
      } catch (error) {
        // Expected to throw
      }
    });

    expect(result.current.error).toEqual(mockError);

    // Reset state
    act(() => {
      result.current.reset();
    });

    expect(result.current.isSubmitting).toBe(false);
    expect(result.current.error).toBe(null);
  });

  it('should maintain submitting state during minimum delay', async () => {
    const mockSubmitFn = jest.fn().mockResolvedValue('success');
    const { result } = renderHook(() => useFormSubmission({ debounceMs: 2000 }));

    // Start submission
    act(() => {
      result.current.submit(mockSubmitFn);
    });

    // Should be submitting immediately
    expect(result.current.isSubmitting).toBe(true);

    // Fast-forward by less than minimum delay
    act(() => {
      jest.advanceTimersByTime(500);
    });

    // Should still be submitting
    expect(result.current.isSubmitting).toBe(true);

    // Fast-forward past minimum delay
    act(() => {
      jest.advanceTimersByTime(500);
    });

    // Should no longer be submitting
    expect(result.current.isSubmitting).toBe(false);
  });
});
