import {useEffect, useRef, useState} from 'react';
import {
  DeviceEventEmitter,
  type EmitterSubscription,
  Platform,
} from 'react-native';

import GeminiNanoNative, {
  type GeminiNanoAvailability,
} from './NativeGeminiNano';

export const GEMINI_NANO_STREAM_CHUNK_EVENT = 'GeminiNano:onTextChunk';
export const GEMINI_NANO_STREAM_END_EVENT = 'GeminiNano:onTextStreamEnd';
export const GEMINI_NANO_STREAM_ERROR_EVENT = 'GeminiNano:onTextStreamError';
export const GEMINI_NANO_DOWNLOAD_EVENT = 'GeminiNano:onDownloadStatus';

export type GeminiNanoStreamChunkEvent = {
  chunk: string;
};

export type GeminiNanoStreamEndEvent = {
  fullText?: string;
};

export type GeminiNanoStreamErrorEvent = {
  code: string;
  message: string;
};

export type GeminiNanoDownloadEvent = {
  status:
    | 'pending'
    | 'started'
    | 'in_progress'
    | 'completed'
    | 'failed';
  bytesDownloaded?: number;
  totalBytes?: number;
  code?: string;
  message?: string;
};

type StreamHandlers = {
  onChunk?: (chunk: string) => void;
  onComplete?: (fullText: string) => void;
  onError?: (error: GeminiNanoStreamErrorEvent) => void;
};

type DownloadHandlers = {
  onStatus?: (event: GeminiNanoDownloadEvent) => void;
};

const ensureAndroid = () => {
  if (Platform.OS !== 'android') {
    throw new Error('react-native-gemini-nano は Android 専用です。');
  }
};

const getNativeModule = () => {
  ensureAndroid();

  if (GeminiNanoNative == null) {
    throw new Error(
      'GeminiNano TurboModule が見つかりません。Codegen 実行済みで、Android 側の autolinking と New Architecture が有効か確認してください。',
    );
  }

  return GeminiNanoNative;
};

const addStreamingListeners = ({
  onChunk,
  onComplete,
  onError,
}: StreamHandlers): (() => void) => {
  const subscriptions: EmitterSubscription[] = [
    DeviceEventEmitter.addListener(
      GEMINI_NANO_STREAM_CHUNK_EVENT,
      (event: GeminiNanoStreamChunkEvent) => {
        onChunk?.(event.chunk);
      },
    ),
    DeviceEventEmitter.addListener(
      GEMINI_NANO_STREAM_END_EVENT,
      (event: GeminiNanoStreamEndEvent) => {
        onComplete?.(event.fullText ?? '');
      },
    ),
    DeviceEventEmitter.addListener(
      GEMINI_NANO_STREAM_ERROR_EVENT,
      (event: GeminiNanoStreamErrorEvent) => {
        onError?.(event);
      },
    ),
  ];

  return () => {
    subscriptions.forEach(subscription => subscription.remove());
  };
};

const addDownloadListeners = ({
  onStatus,
}: DownloadHandlers): (() => void) => {
  const subscription = DeviceEventEmitter.addListener(
    GEMINI_NANO_DOWNLOAD_EVENT,
    (event: GeminiNanoDownloadEvent) => {
      onStatus?.(event);
    },
  );

  return () => {
    subscription.remove();
  };
};

export const GeminiNano = {
  isAvailable(): Promise<boolean> {
    return getNativeModule().isAvailable();
  },

  getAvailability(): Promise<GeminiNanoAvailability> {
    return getNativeModule().getAvailability();
  },

  downloadModel(handlers: DownloadHandlers = {}): Promise<void> {
    const removeListeners = addDownloadListeners(handlers);

    return getNativeModule()
      .downloadModel()
      .finally(() => {
        removeListeners();
      });
  },

  generateText(prompt: string): Promise<string> {
    return getNativeModule().generateText(prompt);
  },

  generateTextStream(prompt: string, handlers: StreamHandlers = {}): () => void {
    const removeListeners = addStreamingListeners(handlers);
    getNativeModule().generateTextStream(prompt);
    return removeListeners;
  },

  addStreamingListeners,
  addDownloadListeners,
};

export const useGeminiNano = () => {
  const [isStreaming, setIsStreaming] = useState(false);
  const [streamedText, setStreamedText] = useState('');
  const [error, setError] = useState<GeminiNanoStreamErrorEvent | null>(null);
  const [availability, setAvailability] = useState<GeminiNanoAvailability | null>(
    null,
  );
  const [isCheckingAvailability, setIsCheckingAvailability] = useState(false);
  const [isDownloadingModel, setIsDownloadingModel] = useState(false);
  const [downloadState, setDownloadState] = useState<GeminiNanoDownloadEvent | null>(
    null,
  );
  const cleanupRef = useRef<(() => void) | null>(null);

  useEffect(() => {
    return () => {
      cleanupRef.current?.();
      cleanupRef.current = null;
    };
  }, []);

  const refreshAvailability = async () => {
    setIsCheckingAvailability(true);

    try {
      const nextAvailability = await GeminiNano.getAvailability();
      setAvailability(nextAvailability);
      return nextAvailability;
    } finally {
      setIsCheckingAvailability(false);
    }
  };

  const downloadModel = async () => {
    setIsDownloadingModel(true);
    setDownloadState(null);

    try {
      await GeminiNano.downloadModel({
        onStatus: event => {
          setDownloadState(event);
          if (event.status === 'completed' || event.status === 'failed') {
            setIsDownloadingModel(false);
          }
        },
      });
      await refreshAvailability();
    } catch (nextError) {
      setIsDownloadingModel(false);
      throw nextError;
    }
  };

  const stopStreaming = () => {
    cleanupRef.current?.();
    cleanupRef.current = null;
    setIsStreaming(false);
  };

  const startStreaming = (prompt: string) => {
    stopStreaming();
    setStreamedText('');
    setError(null);
    setIsStreaming(true);

    cleanupRef.current = GeminiNano.generateTextStream(prompt, {
      onChunk: chunk => {
        setStreamedText((current: string) => current + chunk);
      },
      onComplete: fullText => {
        setStreamedText(fullText);
        setIsStreaming(false);
        cleanupRef.current?.();
        cleanupRef.current = null;
      },
      onError: nextError => {
        setError(nextError);
        setIsStreaming(false);
        cleanupRef.current?.();
        cleanupRef.current = null;
      },
    });
  };

  return {
    isAvailable: GeminiNano.isAvailable,
    getAvailability: GeminiNano.getAvailability,
    availability,
    isCheckingAvailability,
    refreshAvailability,
    downloadModel,
    isDownloadingModel,
    downloadState,
    generateText: GeminiNano.generateText,
    generateTextStream: startStreaming,
    stopStreaming,
    isStreaming,
    streamedText,
    error,
  };
};

export default GeminiNano;
