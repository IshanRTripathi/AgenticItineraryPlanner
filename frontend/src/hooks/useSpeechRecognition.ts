import { useState, useEffect, useCallback, useRef } from 'react';

interface SpeechRecognitionHook {
    isListening: boolean;
    transcript: string;
    interimTranscript: string;
    startListening: () => void;
    stopListening: () => void;
    resetTranscript: () => void;
    isSupported: boolean;
    error: string | null;
}

export function useSpeechRecognition(): SpeechRecognitionHook {
    const [isListening, setIsListening] = useState(false);
    const [transcript, setTranscript] = useState('');
    const [interimTranscript, setInterimTranscript] = useState('');
    const [error, setError] = useState<string | null>(null);
    const [isSupported, setIsSupported] = useState(false);

    const recognitionRef = useRef<any>(null);

    useEffect(() => {
        if (typeof window !== 'undefined') {
            const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
            if (SpeechRecognition) {
                setIsSupported(true);
                recognitionRef.current = new SpeechRecognition();
                recognitionRef.current.continuous = true;
                recognitionRef.current.interimResults = true;
                recognitionRef.current.lang = 'en-US';
            }
        }
    }, []);

    const startListening = useCallback(() => {
        if (!recognitionRef.current || isListening) return;

        try {
            setError(null);
            setTranscript('');
            setInterimTranscript('');
            recognitionRef.current.start();
            setIsListening(true);
        } catch (err) {
            console.error('Speech recognition start error:', err);
        }
    }, [isListening]);

    const stopListening = useCallback(() => {
        if (!recognitionRef.current || !isListening) return;

        try {
            recognitionRef.current.stop();
            setIsListening(false);
        } catch (err) {
            console.error('Speech recognition stop error:', err);
        }
    }, [isListening]);

    const resetTranscript = useCallback(() => {
        setTranscript('');
        setInterimTranscript('');
    }, []);

    useEffect(() => {
        if (!recognitionRef.current) return;

        const recognition = recognitionRef.current;

        recognition.onresult = (event: any) => {
            let finalTranscript = '';
            let currentInterimTranscript = '';

            for (let i = event.resultIndex; i < event.results.length; ++i) {
                if (event.results[i].isFinal) {
                    finalTranscript += event.results[i][0].transcript;
                } else {
                    currentInterimTranscript += event.results[i][0].transcript;
                }
            }

            if (finalTranscript) {
                setTranscript(prev => {
                    const newTranscript = prev ? prev + ' ' + finalTranscript : finalTranscript;
                    return newTranscript;
                });
            }
            setInterimTranscript(currentInterimTranscript);
        };

        recognition.onerror = (event: any) => {
            // Ignore 'no-speech' error as it just means silence
            if (event.error === 'no-speech') return;

            console.error('Speech recognition error', event.error);
            setError(event.error);
            setIsListening(false);
        };

        recognition.onend = () => {
            setIsListening(false);
        };

        return () => {
            recognition.onresult = null;
            recognition.onerror = null;
            recognition.onend = null;
        };
    }, []);

    return {
        isListening,
        transcript,
        interimTranscript,
        startListening,
        stopListening,
        resetTranscript,
        isSupported,
        error
    };
}
